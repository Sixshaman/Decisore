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

import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import com.sixshaman.advancedunforgetter.list.ObjectiveListCache;
import com.sixshaman.advancedunforgetter.utils.LockedReadFile;
import com.sixshaman.advancedunforgetter.utils.LockedWriteFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayList;

//The class to schedule all deferred tasks. The model for the scheduler UI
public class ObjectiveSchedulerCache
{
    public static final String SCHEDULER_FILENAME = "TaskScheduler.json";

    //The list of all the task pools.
    //All single tasks are task pools with a single SingleTaskSource
    //All single task chains are task pools with a single TaskChain
    private ArrayList<ObjectivePool> mObjectivePools;

    //Creates a new task scheduler that is bound to mainList
    public ObjectiveSchedulerCache()
    {
        mObjectivePools = new ArrayList<>();
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
            if(!listCache.isObjectiveInList(pool.getLastProvidedObjectiveId()))
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
    }

    //Creates a new explicit task pool
    public void addObjectivePool(String name, String description)
    {
        ObjectivePool pool = new ObjectivePool(name, description);
        mObjectivePools.add(pool);
    }

    //Creates a new explicit task chain and adds it to the provided pool
    public void addObjectiveChainToPool(ObjectivePool pool, String name, String description)
    {
        TaskChain chain = new TaskChain(name, description);
        pool.addTaskSource(chain);
    }

    //Adds a general task to task pool pool or task chain chain scheduled to be added at deferTime with repeat duration repeatDuration and repeat probability repeatProbability
    public boolean addObjective(ObjectivePool pool, TaskChain chain, ScheduledObjective scheduledObjective)
    {
        if(pool == null && chain == null) //Add a single task source to the new pool
        {
            //Neither task chain nor pool is provided, create an implicit pool and add task there
            SingleTaskSource taskSource = new SingleTaskSource(scheduledObjective);

            ObjectivePool implicitPool = new ObjectivePool("", "");
            implicitPool.addTaskSource(taskSource);

            mObjectivePools.add(implicitPool);
        }
        else if(pool == null)
        {
            //Task chain provided, add the task there
            chain.addTaskToChain(scheduledObjective);
        }
        else
        {
            //Task pool provided, add the task there
            SingleTaskSource taskSource = new SingleTaskSource(scheduledObjective);
            pool.addTaskSource(taskSource);
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
            if(maxId > poolMaxId)
            {
                maxId = poolMaxId;
            }
        }

        return maxId;
    }
}
