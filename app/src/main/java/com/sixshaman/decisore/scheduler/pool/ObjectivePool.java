package com.sixshaman.decisore.scheduler.pool;

import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.list.EnlistedObjective;
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
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;

//A pool that can randomly choose from several objective sources
public class ObjectivePool extends SchedulerElement
{
    ///The view holder of the pool
    private PoolViewHolder mPoolViewHolder;

    //The list of all the objective sources the pool can choose from
    private final ArrayList<PoolElement> mObjectiveSources;

    //The minimum frequency at which the pool can provide objectives (0 specifies the "instant" pool)
    private Duration mProduceFrequency;

    //The date-time at which the last objective was provided (valid only for non-instant pools)
    private LocalDateTime mLastUpdate;

    //The id of the objective that was most recently provided by this pool.
    private long mLastProvidedObjectiveId;

    //Does this pool get deleted immediately after finishing every objective?
    private boolean mIsAutoDelete;

    //Can this pool produce objectives that are supposed to be finished yesterday and earlier, even when it's unavailable ?
    private boolean mIsUnstoppable;

    //Constructs a new objective pool
    public ObjectivePool(long id, String name, String description)
    {
        super(id, name, description);

        mObjectiveSources = new ArrayList<>();

        mLastProvidedObjectiveId = -1;

        mIsAutoDelete  = false;
        mIsUnstoppable = false;

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
            result.put("Id",       getId());
            result.put("ParentId", getParentId());

            result.put("Name",        getName());
            result.put("Description", getDescription());

            result.put("LastId", Long.toString(mLastProvidedObjectiveId));

            result.put("IsActive", Boolean.toString(!isPaused()));

            result.put("IsAutoDelete",  Boolean.toString(mIsAutoDelete));
            result.put("IsUnstoppable", Boolean.toString(mIsUnstoppable));

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

    public static ObjectivePool fromJSON(JSONObject jsonObject)
    {
        try
        {
            long id       = jsonObject.getLong("Id");
            long parentId = jsonObject.getLong("ParentId");

            String name        = jsonObject.optString("Name");
            String description = jsonObject.optString("Description");

            String lastIdString = jsonObject.optString("LastId");
            Long lastId = null;
            try
            {
                lastId = Long.parseLong(lastIdString);
            }
            catch(NumberFormatException e)
            {
                e.printStackTrace();
            }

            if(lastId == null)
            {
                return null;
            }

            String produceFrequencyString = jsonObject.optString("ProduceFrequency");
            String lastProducedDateString = jsonObject.optString("LastUpdate");

            String isAutoDeleteString  = jsonObject.optString("IsAutoDelete");
            String isUnstoppableString = jsonObject.optString("IsUnstoppable");

            ObjectivePool objectivePool = new ObjectivePool(id, name, description);
            objectivePool.setParentId(parentId);
            objectivePool.setLastProvidedObjectiveId(lastId);

            String isActiveString = jsonObject.optString("IsActive");

            objectivePool.setPaused(!isActiveString.isEmpty()           && isActiveString.equalsIgnoreCase("false"));
            objectivePool.setAutoDelete(!isAutoDeleteString.isEmpty()   && isAutoDeleteString.equalsIgnoreCase("true"));
            objectivePool.setUnstoppable(!isUnstoppableString.isEmpty() && isUnstoppableString.equalsIgnoreCase("true"));

            JSONArray sourcesJsonArray = jsonObject.getJSONArray("Sources");
            for(int i = 0; i < sourcesJsonArray.length(); i++)
            {
                JSONObject sourceObject = sourcesJsonArray.optJSONObject(i);
                if(sourceObject != null)
                {
                    String     sourceType = sourceObject.optString("Type");
                    JSONObject sourceData = sourceObject.optJSONObject("Data");

                    if(sourceData != null)
                    {
                        if(sourceType.equals("ObjectiveChain"))
                        {
                            ObjectiveChain chain = ObjectiveChain.fromJSON(sourceData);
                            if(chain != null)
                            {
                                objectivePool.addObjectiveSource(chain);
                            }
                        }
                        else if(sourceType.equals("ScheduledObjective"))
                        {
                            ScheduledObjective objective = ScheduledObjective.fromJSON(sourceData);
                            if(objective != null)
                            {
                                objectivePool.addObjectiveSource(objective);
                            }
                        }
                    }
                }
            }

            if(!produceFrequencyString.isEmpty())
            {
                try
                {
                    long produceFrequencyMinutes = Long.parseLong(produceFrequencyString);
                    Duration produceFrequency = Duration.ofMinutes(produceFrequencyMinutes);

                    objectivePool.setProduceFrequency(produceFrequency);

                    if(!lastProducedDateString.isEmpty())
                    {
                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:nnnnnnnnn");
                        LocalDateTime lastProducedDate = LocalDateTime.parse(lastProducedDateString, dateTimeFormatter);

                        objectivePool.setLastUpdate(lastProducedDate);
                    }
                }
                catch(NumberFormatException | DateTimeParseException e)
                {
                    e.printStackTrace();
                }
            }

            return objectivePool;
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    //Gets an objective from a random source
    @Override
    public EnlistedObjective obtainEnlistedObjective(HashSet<Long> ignoredObjectiveIds, LocalDateTime referenceTime, int dayStartHour)
    {
        if(isPaused())
        {
            return null;
        }

        //Only add the objective to the list if the previous objective from the pool is finished (i.e. isn't in blockingObjectiveIds)
        if(!isAvailable(ignoredObjectiveIds, referenceTime, dayStartHour))
        {
            return null;
        }

        //Check non-empty sources only
        ArrayList<Integer> availableSourceIndices = new ArrayList<>();
        for(int i = 0; i < mObjectiveSources.size(); i++)
        {
            if(mObjectiveSources.get(i).isAvailable(ignoredObjectiveIds, referenceTime, dayStartHour))
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

        EnlistedObjective resultObjective = randomPoolElement.obtainEnlistedObjective(ignoredObjectiveIds, referenceTime, dayStartHour);
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
                if(mIsUnstoppable)
                {
                    if(mLastUpdate.equals(LocalDateTime.MIN)) //This is the first objective produced by the pool
                    {
                        mLastUpdate = resultObjective.getCreatedDate();
                    }
                    else
                    {
                        mLastUpdate = mLastUpdate.minusHours(dayStartHour).truncatedTo(ChronoUnit.DAYS).plusHours(dayStartHour).plusHours(mProduceFrequency.toHours()).minusHours(dayStartHour).truncatedTo(ChronoUnit.DAYS).plusHours(dayStartHour);
                    }
                }
                else
                {
                    mLastUpdate = referenceTime;
                }
            }
        }

        return resultObjective;
    }

    @Override
    public void updateDayStart(LocalDateTime referenceTime, int oldStartHour, int newStartHour)
    {
        for(PoolElement poolElement: mObjectiveSources)
        {
            poolElement.updateDayStart(referenceTime, oldStartHour, newStartHour);
        }
    }

    @Override
    public boolean isRelatedToObjective(long objectiveId)
    {
        return findSourceForObjective(objectiveId) != null;
    }

    @Override
    public boolean mergeRelatedObjective(ScheduledObjective objective)
    {
        PoolElement source = findSourceForObjective(objective.getId());
        if(source == null)
        {
            return false;
        }

        return source.mergeRelatedObjective(objective);
    }

    @Override
    public long getLargestRelatedId()
    {
        long maxId = 0;
        for(PoolElement source: mObjectiveSources)
        {
            long sourceMaxId = source.getLargestRelatedId();
            if(sourceMaxId > maxId)
            {
                maxId = sourceMaxId;
            }
        }

        return maxId;
    }

    @Override
    public ObjectiveChain getRelatedChainById(long id)
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

    @Override
    public ObjectiveChain getChainForObjectiveById(long objectiveId)
    {
        for(PoolElement source: mObjectiveSources)
        {
            if(source instanceof ObjectiveChain)
            {
                ObjectiveChain chain = ((ObjectiveChain)source);
                if(chain.isRelatedToObjective(objectiveId))
                {
                    return chain;
                }
            }
        }

        return null;
    }

    @Override
    public ScheduledObjective getRelatedObjectiveById(long objectiveId)
    {
        for(PoolElement source: mObjectiveSources)
        {
            ScheduledObjective relatedObjective = source.getRelatedObjectiveById(objectiveId);
            if(relatedObjective != null)
            {
                return relatedObjective;
            }
        }

        return null;
    }

    public PoolElement findSourceForObjective(long objectiveId)
    {
        for(PoolElement source: mObjectiveSources)
        {
            if(source.isRelatedToObjective(objectiveId))
            {
                return source;
            }
        }

        return null;
    }

    @Override
    public boolean isAvailable(HashSet<Long> blockingObjectiveIds, LocalDateTime referenceTime, int dayStartHour)
    {
        if(!mLastUpdate.equals(LocalDateTime.MIN))
        {
            LocalDateTime nextUpdate = mLastUpdate.minusHours(dayStartHour).truncatedTo(ChronoUnit.DAYS).plusHours(dayStartHour).plus(mProduceFrequency).minusHours(dayStartHour).truncatedTo(ChronoUnit.DAYS).plusHours(dayStartHour);
            if (!mProduceFrequency.isZero() && !nextUpdate.isBefore(referenceTime))
            {
                return false;
            }
        }

        return !blockingObjectiveIds.contains(mLastProvidedObjectiveId);
    }

    @Override
    public boolean isValid()
    {
        return !mIsAutoDelete || !mObjectiveSources.isEmpty() || (mLastProvidedObjectiveId == -1); //Do not allow to delete a pool that was just created
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

    public void setAutoDelete(boolean autoDelete)
    {
        mIsAutoDelete = autoDelete;
    }

    public void setUnstoppable(boolean unstoppable)
    {
        mIsUnstoppable = unstoppable;
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
