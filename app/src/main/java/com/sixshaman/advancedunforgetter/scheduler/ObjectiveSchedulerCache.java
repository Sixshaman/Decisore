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

import android.util.Log;
import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import com.sixshaman.advancedunforgetter.list.ObjectiveListCache;
import com.sixshaman.advancedunforgetter.utils.LockedReadFile;
import com.sixshaman.advancedunforgetter.utils.LockedWriteFile;
import com.sixshaman.advancedunforgetter.utils.TaskIdGenerator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

//The class to schedule all deferred tasks. The model for the scheduler UI
public class ObjectiveSchedulerCache
{
    public static final String SCHEDULER_FILENAME = "TaskScheduler.json";

    //The list of all the task pools.
    //All single tasks are task pools with a single SingleTaskSource
    //All single task chains are task pools with a single TaskChain
    private ArrayList<TaskPool> mTaskPools;

    //Creates a new task scheduler that is bound to mainList
    public ObjectiveSchedulerCache()
    {
        mTaskPools = new ArrayList<>();
    }

    //Updates the task scheduler. Returns the list of objectives ready to-do
    public ArrayList<EnlistedObjective> dumpReadyObjectives(final ObjectiveListCache listCache, LocalDateTime enlistDateTime)
    {
        ArrayList<EnlistedObjective> result = new ArrayList<>();

        ArrayList<TaskPool> changedPools = new ArrayList<>(); //Rebuild task pool list after each update event
        for(TaskPool pool: mTaskPools)
        {
            if(pool.isSingleSingleTaskPool())
            {
                //Don't need to check if the last task is done
                EnlistedObjective task = pool.getRandomTask(enlistDateTime);
                result.add(task);
            }
            else
            {
                //Only add the task to the main list if the last task provided by the pool is done
                if(!listCache.isObjectiveInList(pool.getLastProvidedTaskId()))
                {
                    EnlistedObjective task = pool.getRandomTask(enlistDateTime);
                    result.add(task);
                }
            }

            pool.updateTaskSources(enlistDateTime);
            if(pool.getTaskSourceCount() != 0) //Don't add empty pools
            {
                changedPools.add(pool);
            }
        }

        mTaskPools = changedPools;
        return result;
    }

    //Loads tasks from JSON config file
    public boolean invalidate(LockedReadFile schedulerReadFile)
    {
        mTaskPools.clear();

        try
        {
            String fileContents = mConfigFile.read();
            JSONObject jsonObject = new JSONObject(fileContents);

            JSONArray poolsJsonArray = jsonObject.getJSONArray("POOLS");
            for(int i = 0; i < poolsJsonArray.length(); i++)
            {
                JSONObject poolObject = poolsJsonArray.optJSONObject(i);
                if(poolObject != null)
                {
                    TaskPool pool = TaskPool.fromJSON(poolObject);
                    if(pool != null)
                    {
                        mTaskPools.add(pool);
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

            for(TaskPool pool: mTaskPools)
            {
                tasksJsonArray.put(pool.toJSON());
            }

            jsonObject.put("POOLS", tasksJsonArray);

            mConfigFile.write(jsonObject.toString());
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
        TaskPool pool = new TaskPool("", "");
        addTaskChainToPool(pool, name, description);
        mTaskPools.add(pool);
    }

    //Creates a new explicit task pool
    public void addObjectivePool(String name, String description)
    {
        TaskPool pool = new TaskPool(name, description);
        mTaskPools.add(pool);
    }

    //Creates a new explicit task chain and adds it to the provided pool
    public void addObjectiveChainToPool(TaskPool pool, String name, String description)
    {
        TaskChain chain = new TaskChain(name, description);
        pool.addTaskSource(chain);
    }

    //Adds a general task to task pool pool or task chain chain scheduled to be added at deferTime with repeat duration repeatDuration and repeat probability repeatProbability
    public boolean addObjective(TaskPool pool, TaskChain chain, ScheduledObjective scheduledObjective)
    {
        LocalDateTime currentTime = LocalDateTime.now();

        ScheduledObjective scheduledObjective = new ScheduledObjective(objectiveId, objectiveName, objectiveDescription, currentTime, deferTime,
                                                                       objectiveTags, repeatDuration, repeatProbability);

        //Calculate the next time to do the task
        scheduledObjective.reschedule(deferTime);

        if(pool == null && chain == null) //Add a single task source to the new pool
        {
            //Neither task chain nor pool is provided, create an implicit pool and add task there
            SingleTaskSource taskSource = new SingleTaskSource(scheduledObjective);

            TaskPool implicitPool = new TaskPool("", "");
            implicitPool.addTaskSource(taskSource);

            mTaskPools.add(implicitPool);
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
        for(TaskPool pool: mTaskPools)
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
