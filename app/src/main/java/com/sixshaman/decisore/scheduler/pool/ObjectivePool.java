package com.sixshaman.decisore.scheduler.pool;

import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.list.EnlistedObjective;
import com.sixshaman.decisore.list.ObjectiveListCache;
import com.sixshaman.decisore.scheduler.chain.ObjectiveChain;
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import com.sixshaman.decisore.scheduler.SchedulerElement;
import com.sixshaman.decisore.utils.RandomUtils;
import com.sixshaman.decisore.utils.ValueHolder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

//A pool that can randomly choose from several objective sources
public class ObjectivePool implements SchedulerElement
{
    ///The view holder of the pool
    private PoolViewHolder mPoolViewHolder;

    //The id of the pool
    private final long mId;

    //The name of the pool
    private String mName;

    //The description of the pool
    private String mDescription;

    //The list of all the objective sources the pool can choose from
    private final ArrayList<PoolElement> mObjectiveSources;

    //The minimum frequency at which the pool can provide objectives (0 specifies the "instant" pool)
    private Duration mProduceFrequency;

    //The date-time at which the last objective was provided (valid only for non-instant pools)
    private LocalDateTime mLastUpdate;

    //The id of the objective that was most recently provided by this pool.
    private long mLastProvidedObjectiveId;

    //The flag that shows that the pool is active (i.e. not paused)
    boolean mIsActive;

    //Constructs a new objective pool
    public ObjectivePool(long id, String name, String description)
    {
        mId = id;

        mName        = name;
        mDescription = description;

        mObjectiveSources = new ArrayList<>();

        mLastProvidedObjectiveId = -1;

        mIsActive = true;

        mLastUpdate       = LocalDateTime.MIN;
        mProduceFrequency = Duration.ZERO;
    }

    public void attachToPoolView(RecyclerView recyclerView, ObjectiveSchedulerCache schedulerCache)
    {
        mPoolViewHolder = new ObjectivePool.PoolViewHolder(schedulerCache);
        recyclerView.setAdapter(mPoolViewHolder);

        mPoolViewHolder.notifyDataSetChanged();
    }

    public void attachAllChainViews(LongSparseArray<RecyclerView> chainItemViews, ObjectiveSchedulerCache schedulerCache)
    {
        for(PoolElement poolElement: mObjectiveSources)
        {
            if(poolElement instanceof ObjectiveChain)
            {
                ObjectiveChain chain = (ObjectiveChain)poolElement;
                RecyclerView chainView = chainItemViews.get(chain.getId(), null);
                if(chainView != null)
                {
                    chain.attachToChainView(chainView, schedulerCache);
                }
            }
        }
    }

    public void addObjectiveSource(PoolElement source)
    {
        mObjectiveSources.add(source);

        if(mPoolViewHolder != null)
        {
            mPoolViewHolder.notifyItemInserted(mObjectiveSources.size() - 1);
            mPoolViewHolder.notifyItemRangeChanged(mObjectiveSources.size() - 1, mObjectiveSources.size());
        }
    }

    public boolean deleteChainById(long chainId)
    {
        int plainIndexToDelete = -1;

        for(int i = 0; i < mObjectiveSources.size(); i++)
        {
            PoolElement poolElement = mObjectiveSources.get(i);
            if(poolElement instanceof ObjectiveChain)
            {
                ObjectiveChain objectiveChain = (ObjectiveChain)poolElement;
                if(objectiveChain.getId() == chainId)
                {
                    plainIndexToDelete = i;
                }
            }
        }

        if(plainIndexToDelete != -1)
        {
            mObjectiveSources.remove(plainIndexToDelete);

            if(mPoolViewHolder != null)
            {
                mPoolViewHolder.notifyItemRemoved(plainIndexToDelete);
                mPoolViewHolder.notifyItemRangeChanged(plainIndexToDelete, mPoolViewHolder.getItemCount());
            }
        }

        return (plainIndexToDelete != -1);
    }

    public boolean deleteObjectiveById(long objectiveId)
    {
        boolean complexDeleteSucceeded = false;
        int     plainIndexToDelete     = -1;

        for(int i = 0; i < mObjectiveSources.size(); i++)
        {
            PoolElement poolElement = mObjectiveSources.get(i);

            if(poolElement instanceof ObjectiveChain)
            {
                ObjectiveChain objectiveChain = (ObjectiveChain)poolElement;
                boolean deleteSucceeded = objectiveChain.deleteObjectiveById(objectiveId);

                if(mPoolViewHolder != null && deleteSucceeded)
                {
                    mPoolViewHolder.notifyItemChanged(i);
                }
                else if(deleteSucceeded)
                {
                    complexDeleteSucceeded = true;
                }
            }
            else if(poolElement instanceof ScheduledObjective)
            {
                ScheduledObjective scheduledObjective = (ScheduledObjective)poolElement;
                if(scheduledObjective.getId() == objectiveId)
                {
                    plainIndexToDelete = i;
                }
            }
        }

        if(plainIndexToDelete != -1)
        {
            mObjectiveSources.remove(plainIndexToDelete);

            if(mPoolViewHolder != null)
            {
                mPoolViewHolder.notifyItemRemoved(plainIndexToDelete);
                mPoolViewHolder.notifyItemRangeChanged(plainIndexToDelete, mPoolViewHolder.getItemCount());
            }
        }

        return (plainIndexToDelete != -1) || complexDeleteSucceeded;
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();

        try
        {
            result.put("Id", mId);

            result.put("Name",        mName);
            result.put("Description", mDescription);

            result.put("LastId", Long.toString(mLastProvidedObjectiveId));

            result.put("IsActive", Boolean.toString(mIsActive));

            result.put("ProduceFrequency", Long.toString(mProduceFrequency.toMinutes()));

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:nnnnnnnnn");
            String lastUpdateString = dateTimeFormatter.format(mLastUpdate);
            result.put("LastUpdate", lastUpdateString);

            JSONArray sourcesArray = new JSONArray();
            for(PoolElement element: mObjectiveSources)
            {
                //Pools can't contain pools
                if(element.getElementName().equals(getElementName()))
                {
                    return null;
                }

                JSONObject objectiveSourceObject = new JSONObject();

                objectiveSourceObject.put("Type", element.getElementName());
                objectiveSourceObject.put("Data", element.toJSON());

                sourcesArray.put(objectiveSourceObject);
            }

            result.put("Sources", sourcesArray);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    //Gets an objective from a random source
    @Override
    public EnlistedObjective obtainEnlistedObjective(HashSet<Long> blockingObjectiveIds, LocalDateTime referenceTime, int dayStartHour)
    {
        if(!mIsActive)
        {
            return null;
        }

        //Only add the objective to the list if the previous objective from the pool is finished (i.e. isn't in blockingObjectiveIds)
        if(!isAvailable(blockingObjectiveIds, referenceTime))
        {
            return null;
        }

        //Check non-empty sources only
        ArrayList<Integer> availableSourceIndices = new ArrayList<>();
        for(int i = 0; i < mObjectiveSources.size(); i++)
        {
            if(mObjectiveSources.get(i).isAvailable(blockingObjectiveIds, referenceTime))
            {
                availableSourceIndices.add(i);
            }
        }

        if(availableSourceIndices.size() == 0)
        {
            return null;
        }

        int  randomSourceIndexIndex = (int) RandomUtils.getInstance().getRandomUniform(0, availableSourceIndices.size() - 1);
        PoolElement randomPoolElement = mObjectiveSources.get(availableSourceIndices.get(randomSourceIndexIndex));

        EnlistedObjective resultObjective = randomPoolElement.obtainEnlistedObjective(blockingObjectiveIds, referenceTime, dayStartHour);
        if(!randomPoolElement.isValid())
        {
            //Delete finished objectives
            mObjectiveSources.remove(availableSourceIndices.get(randomSourceIndexIndex).intValue());
        }

        if(resultObjective != null)
        {
            mLastProvidedObjectiveId = resultObjective.getId();

            if(!mProduceFrequency.isZero())
            {
                mLastUpdate = referenceTime;
            }
        }

        return resultObjective;
    }

    public long getMaxChainId()
    {
        long maxId = 0;
        for(PoolElement source: mObjectiveSources)
        {
            long sourceMaxId = 0;
            if(source instanceof ObjectiveChain)
            {
                sourceMaxId = ((ObjectiveChain)source).getId();
            }

            if(sourceMaxId > maxId)
            {
                maxId = sourceMaxId;
            }
        }

        return maxId;
    }

    public long getMaxObjectiveId()
    {
        long maxId = 0;
        for(PoolElement source: mObjectiveSources)
        {
            long sourceMaxId = 0;
            if(source instanceof ObjectiveChain)
            {
                sourceMaxId = ((ObjectiveChain) source).getMaxObjectiveId();
            }
            else if(source instanceof ScheduledObjective)
            {
                sourceMaxId = ((ScheduledObjective)source).getId();
            }

            if(sourceMaxId > maxId)
            {
                maxId = sourceMaxId;
            }
        }

        return maxId;
    }

    public ObjectiveChain getChainById(long id)
    {
        for(PoolElement source: mObjectiveSources)
        {
            if(source instanceof ObjectiveChain)
            {
                ObjectiveChain chain = ((ObjectiveChain)source);
                if(chain.getId() == id)
                {
                    return chain;
                }
            }
        }

        return null;
    }

    public ScheduledObjective getObjectiveById(long objectiveId)
    {
        for(PoolElement source: mObjectiveSources)
        {
            if(source instanceof ObjectiveChain)
            {
                ObjectiveChain     chain     = ((ObjectiveChain)source);
                ScheduledObjective objective = chain.getObjectiveById(objectiveId);

                if(objective != null)
                {
                    return objective;
                }
            }
            else if(source instanceof ScheduledObjective)
            {
                ScheduledObjective objective = ((ScheduledObjective)source);

                if(objective.getId() == objectiveId)
                {
                    return objective;
                }
            }
        }

        return null;
    }

    public PoolElement findSourceForObjective(long objectiveId)
    {
        for(PoolElement source: mObjectiveSources)
        {
            if(source instanceof ObjectiveChain)
            {
                if(((ObjectiveChain)source).containedObjective(objectiveId))
                {
                    return source;
                }
            }
            else if(source instanceof ScheduledObjective)
            {
                if(((ScheduledObjective) source).getId() == objectiveId)
                {
                    return source;
                }
            }
        }

        return null;
    }

    @Override
    public boolean isAvailable(HashSet<Long> blockingObjectiveIds, LocalDateTime referenceTime)
    {
        if(!mProduceFrequency.isZero() && !mLastUpdate.plus(mProduceFrequency).isBefore(referenceTime))
        {
            return false;
        }

        return !blockingObjectiveIds.contains(mLastProvidedObjectiveId);
    }

    public long getId()
    {
        return mId;
    }

    @Override
    public boolean isPaused()
    {
        return !mIsActive;
    }

    @Override
    public void setPaused(boolean paused)
    {
        mIsActive = !paused;
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public String getName()
    {
        return mName;
    }

    @Override
    public String getDescription()
    {
        return mDescription;
    }

    public void setName(String name)
    {
        mName = name;
    }

    public void setDescription(String description)
    {
        mDescription = description;
    }

    //Returns the number of objective sources in pool
    public int getSourceCount()
    {
        return mObjectiveSources.size();
    }

    //Returns the source with given index
    public PoolElement getSource(int position)
    {
        return mObjectiveSources.get(position);
    }

    //Returns true if the pool contains the source
    public boolean containsSource(PoolElement source)
    {
        return mObjectiveSources.contains(source);
    }

    public void setProduceFrequency(Duration produceFrequency)
    {
        mProduceFrequency = produceFrequency;
    }

    //Only to be used for JSON loading, consider it private
    void setLastUpdate(LocalDateTime lastUpdate)
    {
        mLastUpdate = lastUpdate;
    }

    //Gets the last provided objective id, to check if it has been finished yet
    public long getLastProvidedObjectiveId()
    {
        return mLastProvidedObjectiveId;
    }

    //Sets the last provided objective id
    public void setLastProvidedObjectiveId(long lastId)
    {
        mLastProvidedObjectiveId = lastId;
    }

    public boolean isEmpty()
    {
        return mObjectiveSources.isEmpty();
    }

    @Override
    public String getElementName()
    {
        return "ObjectivePool";
    }

    private class PoolViewHolder extends RecyclerView.Adapter<PoolElementViewHolder>
    {
        private final ObjectiveSchedulerCache mSchedulerCache;

        PoolViewHolder(ObjectiveSchedulerCache schedulerCache)
        {
            mSchedulerCache = schedulerCache;
        }

        @NonNull
        @Override
        public PoolElementViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
        {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_pool_element_view, viewGroup, false);
            return new PoolElementViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PoolElementViewHolder objectiveViewHolder, int position)
        {
            String viewName = "";
            final ValueHolder<String> viewDescription = new ValueHolder<>("");

            int iconId = 0;

            PoolElement poolElement = mObjectiveSources.get(position);
            if(poolElement instanceof ObjectiveChain)
            {
                ObjectiveChain chain = (ObjectiveChain)poolElement;

                viewName = chain.getName();

                ScheduledObjective objective = chain.getFirstObjective();
                if(objective != null)
                {
                    viewDescription.setValue("Next objective: " + objective.getName());
                }

                iconId = R.drawable.ic_scheduler_chain;
            }
            else if(poolElement instanceof ScheduledObjective)
            {
                ScheduledObjective objective = (ScheduledObjective)poolElement;

                viewName = objective.getName();
                viewDescription.setValue("Scheduled to: " + objective.getScheduledEnlistDate());

                iconId = R.drawable.ic_scheduler_objective;
            }

            objectiveViewHolder.mTextView.setText(viewName);
            objectiveViewHolder.mIconView.setImageResource(iconId);

            objectiveViewHolder.setSourceMetadata(poolElement, mSchedulerCache);
        }

        @Override
        public int getItemCount()
        {
            return mObjectiveSources.size();
        }
    }
}
