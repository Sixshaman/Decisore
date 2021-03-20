package com.sixshaman.advancedunforgetter.scheduler;

/*

Scheduler update happens:
- After opening the application
- Every hour on schedule. The user can't change the period.

If during the update the scheduler finds a task that has list add date greater or equal the current date, it adds it to the main list and removes it from the scheduler.

After removing the task from the scheduler:
- If it's a one-time task (repeat probability is 0), nothing else is needed.
- If it's a strictly periodic task (repeat probability is 1), the new task is added to the scheduler. It has the same creation date, but the list add date is the current one + period.
- If it's not a strictly periodic task (0 < repeat probability < 1), then ULTRARANDOM ALGORITHM decides the next list add date and a new task with it is added to the scheduler.

*/

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import com.sixshaman.advancedunforgetter.list.ObjectiveListCache;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain.ObjectiveChain;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain.ObjectiveChainLatestLoader;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.ObjectivePool;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.ObjectivePoolLatestLoader;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.PoolElement;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjective;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjectiveLatestLoader;
import com.sixshaman.advancedunforgetter.utils.LockedReadFile;
import com.sixshaman.advancedunforgetter.utils.LockedWriteFile;
import com.sixshaman.advancedunforgetter.utils.ValueHolder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayList;

//The class to schedule all deferred tasks. The model for the scheduler UI
public class ObjectiveSchedulerCache
{
    public static final String SCHEDULER_FILENAME = "TaskScheduler.json";

    static final int SCHEDULER_VERSION_1_0 = 1000;
    static final int SCHEDULER_VERSION_1_1 = 1001;

    static final int SCHEDULER_VERSION_CURRENT = SCHEDULER_VERSION_1_1;

    //The list of all the scheduler elements
    private ArrayList<SchedulerElement> mSchedulerElements;

    //The view holder of the cached data
    private SchedulerCacheViewHolder mSchedulerViewHolder;

    //Creates a new task scheduler that is bound to mainList
    public ObjectiveSchedulerCache()
    {
        mSchedulerElements = new ArrayList<>();
    }

    public void attachToSchedulerView(RecyclerView recyclerView)
    {
        mSchedulerViewHolder = new ObjectiveSchedulerCache.SchedulerCacheViewHolder();
        recyclerView.setAdapter(mSchedulerViewHolder);
    }

    //Updates the task scheduler. Returns the list of objectives ready to-do
    public ArrayList<EnlistedObjective> dumpReadyObjectives(final ObjectiveListCache listCache, LocalDateTime enlistDateTime)
    {
        ArrayList<EnlistedObjective> result = new ArrayList<>();

        ArrayList<SchedulerElement> changedElements = new ArrayList<>(); //Rebuild task pool list after each update event
        for(SchedulerElement element: mSchedulerElements)
        {
            if(element instanceof ScheduledObjective)
            {
                ScheduledObjective scheduledObjective = (ScheduledObjective)element;
                if(listCache.getObjective(scheduledObjective.getId()) == null)
                {
                    //The list doesn't contain this objective
                    EnlistedObjective enlistedObjective = scheduledObjective.toEnlisted(enlistDateTime);
                    result.add(enlistedObjective);

                    if(scheduledObjective.isRepeatable())
                    {
                        changedElements.add(scheduledObjective);
                    }
                }
            }
            else if(element instanceof ObjectiveChain)
            {
                ObjectiveChain objectiveChain = (ObjectiveChain)element;
                if(listCache.getObjective(objectiveChain.getFirstObjective().getId()) == null)
                {
                    EnlistedObjective objective = objectiveChain.obtainObjective(enlistDateTime);
                    if(objective != null)
                    {
                        result.add(objective);
                    }

                    changedElements.add(objectiveChain);
                }
            }
            else if(element instanceof ObjectivePool)
            {
                ObjectivePool objectivePool = (ObjectivePool)element;

                //Only add the objective to the list if the previous objective from the pool is finished (i.e. isn't in list)
                //This is true for all types of objectives
                if(listCache.getObjective(objectivePool.getLastProvidedObjectiveId()) == null)
                {
                    EnlistedObjective objective = objectivePool.getRandomObjective(enlistDateTime);
                    if(objective != null)
                    {
                        result.add(objective);
                    }
                }

                changedElements.add(objectivePool);
            }
        }

        mSchedulerElements = changedElements;
        return result;
    }

    //Loads tasks from JSON config file
    public boolean invalidate(LockedReadFile schedulerReadFile)
    {
        mSchedulerElements.clear();

        try
        {
            String fileContents = schedulerReadFile.read();
            JSONObject jsonObject = new JSONObject(fileContents);

            int version = jsonObject.optInt("VERSION", SCHEDULER_VERSION_1_0);
            if(version == SCHEDULER_VERSION_CURRENT)
            {
                ScheduledObjectiveLatestLoader objectiveLatestLoader = new ScheduledObjectiveLatestLoader();
                ObjectiveChainLatestLoader     chainLatestLoader     = new ObjectiveChainLatestLoader();
                ObjectivePoolLatestLoader      poolLatestLoader      = new ObjectivePoolLatestLoader();

                JSONArray elementsArray = jsonObject.getJSONArray("ELEMENTS");

                for(int i = 0; i < elementsArray.length(); i++)
                {
                    JSONObject elementObject = elementsArray.getJSONObject(i);

                    String     elementType = elementObject.getString("Type");
                    JSONObject elementData = elementObject.getJSONObject("Data");

                    switch (elementType)
                    {
                        case "ObjectivePool":
                            ObjectivePool pool = poolLatestLoader.fromJSON(elementData);
                            mSchedulerElements.add(pool);
                            break;
                        case "ObjectiveChain":
                            ObjectiveChain chain = chainLatestLoader.fromJSON(elementData);
                            mSchedulerElements.add(chain);
                            break;
                        case "ScheduledObjective":
                            ScheduledObjective objective = objectiveLatestLoader.fromJSON(elementData);
                            mSchedulerElements.add(objective);
                            break;
                        default:
                            return false;
                    }
                }
            }
            else
            {
                mSchedulerElements = SchedulerOldVersionLoader.loadSchedulerElementsOld(this, jsonObject, version);
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
            return false;
        }

        if(mSchedulerViewHolder != null)
        {
            mSchedulerViewHolder.notifyDataSetChanged();
        }

        return true;
    }

    //Saves scheduled tasks in JSON config file
    public boolean flush(LockedWriteFile schedulerWriteFile)
    {
        try
        {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("VERSION", SCHEDULER_VERSION_CURRENT);

            JSONArray tasksJsonArray = new JSONArray();

            for(SchedulerElement element: mSchedulerElements)
            {
                JSONObject elementObject = new JSONObject();

                elementObject.put("Type", element.getElementName());
                elementObject.put("Data", element.toJSON());

                tasksJsonArray.put(element.toJSON());
            }

            jsonObject.put("ELEMENTS", tasksJsonArray);

            schedulerWriteFile.write(jsonObject.toString());
        }
        catch(JSONException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    //Creates a new explicit task chain
    public boolean addObjectiveChain(ObjectivePool pool, String name, String description)
    {
        long chainId = getMaxChainId() + 1;
        ObjectiveChain chain = new ObjectiveChain(chainId, name, description);

        if(pool == null)
        {
            mSchedulerElements.add(chain);
            if(mSchedulerViewHolder != null)
            {
                mSchedulerViewHolder.notifyItemInserted(mSchedulerElements.size() - 1);
                mSchedulerViewHolder.notifyItemRangeChanged(mSchedulerElements.size() - 1, mSchedulerViewHolder.getItemCount());
            }
        }
        else
        {
            pool.addObjectiveSource(chain);
            if(mSchedulerViewHolder != null)
            {
                int index = mSchedulerElements.indexOf(pool);
                mSchedulerViewHolder.notifyItemChanged(index);
            }
        }

        return true;
    }

    //Creates a new explicit task pool
    public boolean addObjectivePool(String name, String description)
    {
        long poolId = getMaxPoolId() + 1;

        ObjectivePool pool = new ObjectivePool(poolId, name, description);
        mSchedulerElements.add(pool);

        if(mSchedulerViewHolder != null)
        {
            mSchedulerViewHolder.notifyItemInserted(mSchedulerElements.size() - 1);
            mSchedulerViewHolder.notifyItemRangeChanged(mSchedulerElements.size() - 1, mSchedulerViewHolder.getItemCount());
        }

        return true;
    }

    //Adds a general task to task pool pool or task chain chain scheduled to be added at deferTime with repeat duration repeatDuration and repeat probability repeatProbability
    public boolean addObjective(ObjectivePool pool, ObjectiveChain chain, ScheduledObjective scheduledObjective)
    {
        if(pool == null && chain == null) //Add a single task source
        {
            //Neither task chain nor pool is provided
            mSchedulerElements.add(scheduledObjective);

            if(mSchedulerViewHolder != null)
            {
                mSchedulerViewHolder.notifyItemInserted(mSchedulerElements.size() - 1);
                mSchedulerViewHolder.notifyItemRangeChanged(mSchedulerElements.size() - 1, mSchedulerViewHolder.getItemCount());
            }

            return true;
        }
        else if(pool == null)
        {
            //Chain is provided, add the objective there
            chain.addTaskToChain(scheduledObjective);

            if(mSchedulerViewHolder != null)
            {
                for(int i = 0; i < mSchedulerElements.size(); i++)
                {
                    if(mSchedulerElements.get(i) instanceof ObjectivePool && ((ObjectivePool)mSchedulerElements.get(i)).containsSource(chain))
                    {
                        mSchedulerViewHolder.notifyItemChanged(i);
                    }
                }

                int plainChainIndex = mSchedulerElements.indexOf(chain);
                if(plainChainIndex != -1)
                {
                    mSchedulerViewHolder.notifyItemChanged(plainChainIndex);
                }
            }

            return true;
        }
        else if(chain == null)
        {
            //Objective pool provided, add the objective there
            pool.addObjectiveSource(scheduledObjective);

            if(mSchedulerViewHolder != null)
            {
                int index = mSchedulerElements.indexOf(pool);
                mSchedulerViewHolder.notifyItemChanged(index);
            }

            return true;
        }

        return false;
    }

    public boolean putObjectiveBack(ScheduledObjective objective)
    {
        //Rule: already existing scheduled objective consumes the newly added one if the newly added one was scheduled after existing
        //Example 1: a daily objective was rescheduled for tomorrow. It gets consumed.
        //Example 2: a weekly objective was rescheduled for tomorrow and the next time it gets added is 2 days after. It gets added both tomorrow and 2 days after

        boolean alreadyExisting = false;
        for(SchedulerElement schedulerElement: mSchedulerElements)
        {
            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool objectivePool = (ObjectivePool)schedulerElement;

                PoolElement source = objectivePool.findSourceForObjective(objective.getId());
                if(source != null)
                {
                    if(source instanceof ObjectiveChain)
                    {
                        if(!((ObjectiveChain) source).putBack(objective))
                        {
                            return false;
                        }
                    }
                    else if(source instanceof ScheduledObjective)
                    {
                        ScheduledObjective scheduledObjective = (ScheduledObjective)source;
                        if(objective.getId() == scheduledObjective.getId())
                        {
                            scheduledObjective.rescheduleUnregulated(objective.getScheduledEnlistDate());
                            return true;
                        }
                    }

                    alreadyExisting = true;
                }
            }
            else if(schedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain objectiveChain = (ObjectiveChain)schedulerElement;

                if(objectiveChain.containedObjective(objective.getId()))
                {
                    objectiveChain.putBack(objective);
                }
            }
            else if(schedulerElement instanceof ScheduledObjective)
            {
                ScheduledObjective scheduledObjective = (ScheduledObjective)schedulerElement;

                if(scheduledObjective.getId() == objective.getId())
                {
                    alreadyExisting = true;
                }
            }
        }

        //Simply add the objective if no source contains it
        if(!alreadyExisting)
        {
            return addObjective(null, null, objective);
        }

        return true;
    }

    public long getMaxObjectiveId()
    {
        long maxId = 0;
        for(SchedulerElement schedulerElement: mSchedulerElements)
        {
            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool objectivePool = (ObjectivePool)schedulerElement;

                long poolMaxId = objectivePool.getMaxObjectiveId();
                if(poolMaxId > maxId)
                {
                    maxId = poolMaxId;
                }
            }
            else if(schedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain objectiveChain = (ObjectiveChain)schedulerElement;

                long chainMaxId = objectiveChain.getMaxObjectiveId();
                if(chainMaxId > maxId)
                {
                    maxId = chainMaxId;
                }
            }
            else if(schedulerElement instanceof ScheduledObjective)
            {
                ScheduledObjective scheduledObjective = (ScheduledObjective)schedulerElement;

                if(scheduledObjective.getId() > maxId)
                {
                    maxId = scheduledObjective.getId();
                }
            }
        }

        return maxId;
    }

    public long getMaxChainId()
    {
        long maxId = 0;
        for(SchedulerElement schedulerElement: mSchedulerElements)
        {
            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool objectivePool = (ObjectivePool)schedulerElement;

                long poolMaxId = objectivePool.getMaxChainId();
                if(poolMaxId > maxId)
                {
                    maxId = poolMaxId;
                }
            }
            else if(schedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain objectiveChain = (ObjectiveChain)schedulerElement;

                long chainId = objectiveChain.getId();
                if(chainId > maxId)
                {
                    maxId = chainId;
                }
            }
        }

        return maxId;
    }

    public long getMaxPoolId()
    {
        long maxId = 0;
        for(SchedulerElement schedulerElement: mSchedulerElements)
        {
            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool objectivePool = (ObjectivePool)schedulerElement;

                long poolId = objectivePool.getId();
                if(poolId > maxId)
                {
                    maxId = poolId;
                }
            }
        }

        return maxId;
    }

    public ObjectivePool getPoolById(long id)
    {
        for(SchedulerElement schedulerElement: mSchedulerElements)
        {
            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool objectivePool = (ObjectivePool)schedulerElement;
                if (objectivePool.getId() == id)
                {
                    return objectivePool;
                }
            }
        }

        return null;
    }

    public ObjectiveChain getChainById(long id)
    {
        for(SchedulerElement schedulerElement: mSchedulerElements)
        {
            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool  objectivePool  = (ObjectivePool)schedulerElement;
                ObjectiveChain objectiveChain = objectivePool.getChainById(id);

                if(objectiveChain != null)
                {
                    return objectiveChain;
                }
            }
            else if(schedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain objectiveChain = (ObjectiveChain)schedulerElement;
                if(objectiveChain.getId() == id)
                {
                    return objectiveChain;
                }
            }
        }

        return null;
    }

    private class SchedulerCacheViewHolder extends RecyclerView.Adapter<SchedulerElementViewHolder>
    {
        private Context mContext;

        @NonNull
        @Override
        public SchedulerElementViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
        {
            mContext = viewGroup.getContext();

            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_objective_source_view, viewGroup, false);
            return new SchedulerElementViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SchedulerElementViewHolder taskViewHolder, int position)
        {
            String viewName = "";
            final ValueHolder<String> viewDescription = new ValueHolder<>("");

            int iconId = 0;

            SchedulerElement schedulerElement = mSchedulerElements.get(position);
            if(schedulerElement instanceof ObjectivePool) //Implicit pool
            {
                ObjectivePool pool = (ObjectivePool)schedulerElement;

                viewName = pool.getName();
                viewDescription.setValue(pool.getDescription());

                viewDescription.setValue("Number of sources: " + pool.getSourceCount());

                iconId = R.drawable.ic_scheduler_pool;
            }
            else if(schedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain chain = (ObjectiveChain)schedulerElement;

                viewName = chain.getName();

                ScheduledObjective objective = chain.getFirstObjective();
                viewDescription.setValue("Next objective: " + objective.getName());

                iconId = R.drawable.ic_scheduler_chain;
            }
            else if(schedulerElement instanceof ScheduledObjective)
            {
                ScheduledObjective objective = (ScheduledObjective)schedulerElement;

                viewName = objective.getName();
                viewDescription.setValue("Scheduled to: " + objective.getScheduledEnlistDate());

                iconId = R.drawable.ic_scheduler_objective;
            }

            taskViewHolder.mTextView.setText(viewName);
            taskViewHolder.mIconView.setImageResource(iconId);

            taskViewHolder.setSourceMetadata(ObjectiveSchedulerCache.this, schedulerElement);
        }

        @Override
        public int getItemCount()
        {
            return mSchedulerElements.size();
        }
    }
}
