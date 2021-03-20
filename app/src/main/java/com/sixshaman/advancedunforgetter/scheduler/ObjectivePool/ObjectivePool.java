package com.sixshaman.advancedunforgetter.scheduler.ObjectivePool;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain.ObjectiveChain;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjective;
import com.sixshaman.advancedunforgetter.scheduler.SchedulerElement;
import com.sixshaman.advancedunforgetter.scheduler.SchedulerElementViewHolder;
import com.sixshaman.advancedunforgetter.utils.RandomUtils;
import com.sixshaman.advancedunforgetter.utils.ValueHolder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayList;

//A pool that can randomly choose from several task sources
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

    //The list of all the task sources the pool can choose from
    private ArrayList<PoolElement> mObjectiveSources;

    //The id of the task that was most recently provided by this pool.
    private long mLastProvidedObjectiveId;

    //The flag that shows that the pool is active (i.e. not paused)
    boolean mIsActive;

    //Constructs a new task pool
    public ObjectivePool(long id, String name, String description)
    {
        mId = id;

        mName        = name;
        mDescription = description;

        mObjectiveSources = new ArrayList<>();

        setLastProvidedObjectiveId(0);

        mIsActive = true;
    }

    public void attachToSPoolView(RecyclerView recyclerView)
    {
        mPoolViewHolder = new ObjectivePool.PoolViewHolder();
        recyclerView.setAdapter(mPoolViewHolder);
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

            JSONArray sourcesArray = new JSONArray();
            for(PoolElement element: mObjectiveSources)
            {
                //Pools can't contain pools
                if(element.getElementName().equals(getElementName()))
                {
                    return null;
                }

                JSONObject taskSourceObject = new JSONObject();

                taskSourceObject.put("Type", element.getElementName());
                taskSourceObject.put("Data", element.toJSON());

                sourcesArray.put(taskSourceObject);
            }

            result.put("Sources", sourcesArray);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    //Gets a task from a random source
    public EnlistedObjective getRandomObjective(LocalDateTime referenceTime)
    {
        if(!mIsActive)
        {
            return null;
        }

        //Check non-empty sources only
        ArrayList<PoolElement> availableSources = new ArrayList<>();
        for(PoolElement source: mObjectiveSources)
        {
            if(source.isAvailable(referenceTime))
            {
                availableSources.add(source);
            }
        }

        if(availableSources.size() == 0)
        {
            return null;
        }

        int  randomSourceIndex  = (int) RandomUtils.getInstance().getRandomUniform(0, availableSources.size() - 1);
        PoolElement randomPoolElement = availableSources.get(randomSourceIndex);

        EnlistedObjective resultObjective = null;
        if(randomPoolElement instanceof ObjectiveChain)
        {
            resultObjective = ((ObjectiveChain)randomPoolElement).obtainObjective(referenceTime);
        }
        else if(randomPoolElement instanceof ScheduledObjective)
        {
            ScheduledObjective scheduledObjective = (ScheduledObjective)randomPoolElement;
            resultObjective = scheduledObjective.toEnlisted(referenceTime);

            if(scheduledObjective.isRepeatable())
            {
                scheduledObjective.reschedule(referenceTime);
            }
            else
            {
                //Delete finished objectives
                mObjectiveSources.remove(randomSourceIndex);
            }
        }

        if(resultObjective != null)
        {
            setLastProvidedObjectiveId(resultObjective.getId());
        }

        return resultObjective;
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

    public long getMaxObjectiveId()
    {
        long maxId = -1;
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
    public boolean isPaused()
    {
        return !mIsActive;
    }

    public long getId()
    {
        return mId;
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

    public int getTaskSourceCount()
    {
        return mObjectiveSources.size();
    }

    //Adds a new task source to choose from
    public void addObjectiveSource(PoolElement source)
    {
        mObjectiveSources.add(source);
    }

    //Pauses the pool
    void pause()
    {
        mIsActive = false;
    }

    //Unpauses the pool
    void unpause()
    {
        mIsActive = true;
    }

    //Gets the last provided task id, to check if it has been finished yet
    public long getLastProvidedObjectiveId()
    {
        return mLastProvidedObjectiveId;
    }

    private void setLastProvidedObjectiveId(long objectiveId)
    {
        mLastProvidedObjectiveId = objectiveId;
    }

    @Override
    public String getElementName()
    {
        return "ObjectivePool";
    }

    private class PoolViewHolder extends RecyclerView.Adapter<PoolElementViewHolder>
    {
        private Context mContext;

        @NonNull
        @Override
        public PoolElementViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
        {
            mContext = viewGroup.getContext();

            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_pool_element_view, viewGroup, false);
            return new PoolElementViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PoolElementViewHolder taskViewHolder, int position)
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
                viewDescription.setValue("Next objective: " + objective.getName());

                iconId = R.drawable.ic_scheduler_chain;
            }
            else if(poolElement instanceof ScheduledObjective)
            {
                ScheduledObjective objective = (ScheduledObjective)poolElement;

                viewName = objective.getName();
                viewDescription.setValue("Scheduled to: " + objective.getScheduledEnlistDate());

                iconId = R.drawable.ic_scheduler_objective;
            }

            taskViewHolder.mTextView.setText(viewName);
            taskViewHolder.mIconView.setImageResource(iconId);

            taskViewHolder.setSourceMetadata(poolElement);
        }

        @Override
        public int getItemCount()
        {
            return mObjectiveSources.size();
        }
    }
}
