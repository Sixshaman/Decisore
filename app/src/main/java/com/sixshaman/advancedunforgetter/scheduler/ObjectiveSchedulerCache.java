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
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.archive.ObjectiveArchiveCache;
import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import com.sixshaman.advancedunforgetter.list.ObjectiveListCache;
import com.sixshaman.advancedunforgetter.utils.LockedReadFile;
import com.sixshaman.advancedunforgetter.utils.LockedWriteFile;
import com.sixshaman.advancedunforgetter.utils.ValueHolder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Observable;

//The class to schedule all deferred tasks. The model for the scheduler UI
public class ObjectiveSchedulerCache
{
    public static final String SCHEDULER_FILENAME = "TaskScheduler.json";

    //The list of all the task pools.
    //All single tasks are task pools with a single SingleTaskSource
    //All single task chains are task pools with a single TaskChain
    private ArrayList<ObjectivePool> mObjectivePools;

    //The view holder of the cached data
    private SchedulerCacheViewHolder mSchedulerViewHolder;

    //Creates a new task scheduler that is bound to mainList
    public ObjectiveSchedulerCache()
    {
        mObjectivePools = new ArrayList<>();
    }

    public void attachToView(RecyclerView recyclerView)
    {
        mSchedulerViewHolder = new ObjectiveSchedulerCache.SchedulerCacheViewHolder();
        recyclerView.setAdapter(mSchedulerViewHolder);
    }

    //Updates the task scheduler. Returns the list of objectives ready to-do
    public ArrayList<EnlistedObjective> dumpReadyObjectives(final ObjectiveListCache listCache, LocalDateTime enlistDateTime)
    {
        ArrayList<EnlistedObjective> result = new ArrayList<>();

        ArrayList<ObjectivePool> changedPools = new ArrayList<>(); //Rebuild task pool list after each update event
        for(ObjectivePool pool: mObjectivePools)
        {
            //Only add the objective to the list if the previous objective from the pool is finished
            //This is true for all types of objectives
            if(listCache.getObjective(pool.getLastProvidedObjectiveId()) != null)
            {
                EnlistedObjective objective = pool.getRandomObjective(enlistDateTime);
                if(objective != null)
                {
                    result.add(objective);
                }
            }

            pool.updateObjectiveSources(enlistDateTime);
            if(pool.getTaskSourceCount() != 0) //Don't add empty pools
            {
                changedPools.add(pool);
            }
        }

        mObjectivePools = changedPools;
        return result;
    }

    //Loads tasks from JSON config file
    public boolean invalidate(LockedReadFile schedulerReadFile)
    {
        mObjectivePools.clear();

        try
        {
            String fileContents = schedulerReadFile.read();
            JSONObject jsonObject = new JSONObject(fileContents);

            JSONArray poolsJsonArray = jsonObject.getJSONArray("POOLS");
            for(int i = 0; i < poolsJsonArray.length(); i++)
            {
                JSONObject poolObject = poolsJsonArray.optJSONObject(i);
                if(poolObject != null)
                {
                    ObjectivePool pool = ObjectivePool.fromJSON(poolObject);
                    if(pool != null)
                    {
                        mObjectivePools.add(pool);
                    }
                }
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
            JSONObject jsonObject    = new JSONObject();
            JSONArray tasksJsonArray = new JSONArray();

            for(ObjectivePool pool: mObjectivePools)
            {
                tasksJsonArray.put(pool.toJSON());
            }

            jsonObject.put("POOLS", tasksJsonArray);

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
    public void addObjectiveChain(String name, String description)
    {
        //Create a new unnamed task pool to hold the chain
        ObjectivePool pool = new ObjectivePool("", "");
        addObjectiveChainToPool(pool, name, description);
        mObjectivePools.add(pool);

        if(mSchedulerViewHolder != null)
        {
            mSchedulerViewHolder.notifyItemInserted(mObjectivePools.size() - 1);
            mSchedulerViewHolder.notifyItemRangeChanged(mObjectivePools.size() - 1, mSchedulerViewHolder.getItemCount());
        }
    }

    //Creates a new explicit task pool
    public void addObjectivePool(String name, String description)
    {
        ObjectivePool pool = new ObjectivePool(name, description);
        mObjectivePools.add(pool);

        if(mSchedulerViewHolder != null)
        {
            mSchedulerViewHolder.notifyItemInserted(mObjectivePools.size() - 1);
            mSchedulerViewHolder.notifyItemRangeChanged(mObjectivePools.size() - 1, mSchedulerViewHolder.getItemCount());
        }
    }

    //Creates a new explicit task chain and adds it to the provided pool
    public void addObjectiveChainToPool(ObjectivePool pool, String name, String description)
    {
        ObjectiveChain chain = new ObjectiveChain(name, description);
        pool.addTaskSource(chain);

        if(mSchedulerViewHolder != null)
        {
            int index = mObjectivePools.indexOf(pool);
            mSchedulerViewHolder.notifyItemChanged(index);
        }
    }

    //Adds a general task to task pool pool or task chain chain scheduled to be added at deferTime with repeat duration repeatDuration and repeat probability repeatProbability
    public boolean addObjective(ObjectivePool pool, ObjectiveChain chain, ScheduledObjective scheduledObjective)
    {
        if(pool == null && chain == null) //Add a single task source to the new pool
        {
            //Neither task chain nor pool is provided, create an implicit pool and add task there
            SingleObjectiveSource taskSource = new SingleObjectiveSource(scheduledObjective);

            ObjectivePool implicitPool = new ObjectivePool("", "");
            implicitPool.addTaskSource(taskSource);

            mObjectivePools.add(implicitPool);
            if(mSchedulerViewHolder != null)
            {
                mSchedulerViewHolder.notifyItemInserted(mObjectivePools.size() - 1);
                mSchedulerViewHolder.notifyItemRangeChanged(mObjectivePools.size() - 1, mSchedulerViewHolder.getItemCount());
            }
        }
        else if(pool == null)
        {
            //Chain is provided, add the objective there
            chain.addTaskToChain(scheduledObjective);

            if(mSchedulerViewHolder != null)
            {
                for(int i = 0; i < mObjectivePools.size(); i++)
                {
                    if(mObjectivePools.get(i).containsSource(chain))
                    {
                        mSchedulerViewHolder.notifyItemChanged(i);
                    }
                }
            }
        }
        else
        {
            //Task pool provided, add the task there
            SingleObjectiveSource taskSource = new SingleObjectiveSource(scheduledObjective);
            pool.addTaskSource(taskSource);

            if(mSchedulerViewHolder != null)
            {
                int index = mObjectivePools.indexOf(pool);
                mSchedulerViewHolder.notifyItemChanged(index);
            }
        }

        return true;
    }

    public boolean putObjectiveBack(ScheduledObjective objective)
    {
        //Rule: already existing scheduled objective consumes the newly added one if the newly added one was scheduled after existing
        //Example 1: a daily objective was rescheduled for tomorrow. It gets consumed.
        //Example 2: a weekly objective was rescheduled for tomorrow and the next time it gets added is 2 days after. It gets added both tomorrow and 2 days after

        boolean alreadyExisting = false;
        for(ObjectivePool objectivePool: mObjectivePools)
        {
            ObjectiveSource source = objectivePool.findSourceForObjective(objective.getId());
            if(source != null)
            {
                if(!source.putBack(objective))
                {
                    return false;
                }

                alreadyExisting = true;
            }
        }

        //Simply add the objective if no source contains it
        if(!alreadyExisting)
        {
            return addObjective(null, null, objective);
        }

        return true;
    }

    //Sets the start id for the task id generator
    public long getMaxObjectiveId()
    {
        long maxId = -1;
        for(ObjectivePool pool: mObjectivePools)
        {
            long poolMaxId = pool.getMaxTaskId();
            if(poolMaxId > maxId)
            {
                maxId = poolMaxId;
            }
        }

        return maxId;
    }

    private class SchedulerCacheViewHolder extends RecyclerView.Adapter<ObjectiveSchedulerCache.SchedulerSourceViewHolder>
    {
        private Context mContext;

        @NonNull
        @Override
        public ObjectiveSchedulerCache.SchedulerSourceViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
        {
            mContext = viewGroup.getContext();

            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_objective_source_view, viewGroup, false);
            return new ObjectiveSchedulerCache.SchedulerSourceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ObjectiveSchedulerCache.SchedulerSourceViewHolder taskViewHolder, int position)
        {
            String viewName = "";
            final ValueHolder<String> viewDescription = new ValueHolder<>("");

            ObjectivePool pool = mObjectivePools.get(position);
            if(pool.getSourceCount() == 1 && pool.getName().equals("")) //Implicit pool
            {
                ObjectiveSource source = pool.getSource(0);
                if(source instanceof ObjectiveChain)
                {
                    ObjectiveChain chain = (ObjectiveChain)source;
                    if(chain.getObjectiveCount() == 1 && chain.getName().equals("")) //Implicit chain
                    {
                        ScheduledObjective objective = chain.getObjective(0);
                        viewDescription.setValue("Objective, scheduled to: " + objective.getScheduledEnlistDate());
                    }
                    else
                    {
                        viewName = chain.getName();

                        ScheduledObjective objective = chain.getObjective(0);
                        viewDescription.setValue("Chain, next objective: " + objective.getName());
                    }
                }
                else if(source instanceof SingleObjectiveSource)
                {
                    SingleObjectiveSource singleSource = (SingleObjectiveSource)source;
                    viewName = singleSource.getObjective().getName();
                    viewDescription.setValue("Objective, scheduled to: " + singleSource.getObjective().getScheduledEnlistDate());
                }
            }
            else
            {
                viewName = pool.getName();
                viewDescription.setValue(pool.getDescription());

                viewDescription.setValue("Pool, number of sources: " + pool.getSourceCount());
            }

            taskViewHolder.mTextView.setText(viewName);

            taskViewHolder.mParentLayout.setOnClickListener(view -> Toast.makeText(mContext, viewDescription.getValue(), Toast.LENGTH_LONG).show());
        }

        @Override
        public int getItemCount()
        {
            return mObjectivePools.size();
        }
    }

    static class SchedulerSourceViewHolder extends RecyclerView.ViewHolder
    {
        TextView mTextView;

        ConstraintLayout mParentLayout;

        public SchedulerSourceViewHolder(View itemView)
        {
            super(itemView);

            mTextView = itemView.findViewById(R.id.textScheduledSourceName);

            mParentLayout = itemView.findViewById(R.id.layoutScheduledSourceView);
        }
    }
}
