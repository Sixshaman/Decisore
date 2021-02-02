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
import com.sixshaman.advancedunforgetter.list.EnlistedTask;
import com.sixshaman.advancedunforgetter.list.TaskList;
import com.sixshaman.advancedunforgetter.utils.BaseFileLockException;
import com.sixshaman.advancedunforgetter.utils.LockedFile;
import com.sixshaman.advancedunforgetter.utils.TaskIdGenerator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

//The class to schedule all deferred tasks. The model for the scheduler UI
public class TaskScheduler
{
    //Our next big thing is to add a scheduler activity!
    //The plan:
    // - Main scheduler tab, showing the list of deferred tasks and daily/time-to-time tasks
    // - Task chain tab, showing the list of individual task chains. Click on an element opens a new window with the list of tasks in the chain
    // - Task pool tab, showing the list of task pools. Click on an element opens a new window with pool info and the list of task sources. Click on a task source opens another window with task source info.

    //Architecture planning counts as working on this project :P
    // - So, main scheduler. Pressing "+" will show options!
    //Options are ONLY for individual tasks (daily, time-to-time, etc)
    //On top there's a hamburger menu.
    //All task chains and task pools are there. Not in options on "+".

    public static class SchedulerFileLockException extends BaseFileLockException
    {
    }

    private static final String SCHEDULER_FILENAME = "TaskScheduler.json";

    //The list of all the task pools.
    //All single tasks are task pools with a single SingleTaskSource
    //All single task chains are task pools with a single TaskChain
    private ArrayList<TaskPool> mTaskPools;

    //Generated task ids
    private TaskIdGenerator mIdGenerator;

    //The main list of tasks that scheduler adds tasks to
    private TaskList mMainList;

    //The file to store the scheduler data
    private LockedFile mConfigFile;

    //Creates a new task scheduler that is bound to mainList
    public TaskScheduler()
    {
        mTaskPools = new ArrayList<>();

        mIdGenerator = new TaskIdGenerator();

        mMainList = null;
    }

    //Sets the folder to store the JSON config file
    public void setConfigFolder(String folder)
    {
        mConfigFile = new LockedFile(folder + "/" + SCHEDULER_FILENAME);
    }

    //Sets the task list to send scheduled tasks into
    public void setTaskList(TaskList mainList)
    {
        mMainList = mainList;
    }

    //Updates the task scheduler: adds all ready-to-be-done tasks to the main list, reschedules tasks, updates chains and pools
    public void update() throws TaskList.ListFileLockException, SchedulerFileLockException
    {
        ArrayList<TaskPool> changedPools = new ArrayList<>(); //Rebuild task pool list after each update

        LocalDateTime currentDateTime = LocalDateTime.now();
        for(TaskPool pool: mTaskPools)
        {
            if(pool.isSingleSingleTaskPool())
            {
                //Don't need to check if the last task is done
                EnlistedTask task = pool.getRandomTask(currentDateTime);
                mMainList.addTask(task);
            }
            else
            {
                //Only add the task to the main list if the last task provided by the pool is done
                if(!mMainList.isTaskInList(pool.getLastProvidedTaskId()))
                {
                    EnlistedTask task = pool.getRandomTask(currentDateTime);
                    mMainList.addTask(task);
                }
            }

            pool.updateTaskSources(currentDateTime);
            if(pool.getTaskSourceCount() != 0) //Don't add empty pools
            {
                changedPools.add(pool);
            }
        }

        mTaskPools = changedPools;
        saveScheduledTasks();
    }

    public void waitLock()
    {
        try
        {
            while(!mConfigFile.lock())
            {
                Log.d("LOCK", "Can't lock the scheduler config file!");
                Thread.sleep(100);
            }
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public boolean tryLock()
    {
        return mConfigFile.lock();
    }

    public void unlock()
    {
        mConfigFile.unlock();
    }

    //Loads tasks from JSON config file
    public void loadScheduledTasks() throws SchedulerFileLockException
    {
        if(!mConfigFile.isLocked())
        {
            throw new SchedulerFileLockException();
        }

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
        }
    }

    //Saves scheduled tasks in JSON config file
    public void saveScheduledTasks() throws SchedulerFileLockException
    {
        if(!mConfigFile.isLocked())
        {
            throw new SchedulerFileLockException();
        }

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
        }
    }

    //Creates a new one-time task and immediately adds it to the main list
    public void addImmediateTask(String taskName, String taskDescription, ArrayList<String> taskTags) throws TaskList.ListFileLockException
    {
        if(!mMainList.isLocked())
        {
            throw new TaskList.ListFileLockException();
        }

        long          taskId      = mIdGenerator.generateNextId();
        LocalDateTime currentTime = LocalDateTime.now();

        EnlistedTask task = new EnlistedTask(taskId, currentTime, currentTime, taskName, taskDescription, taskTags);

        mMainList.addTask(task);
    }

    //Creates a new one-time task and schedules it to be added to the main list later
    public void addDeferredTask(LocalDateTime deferTime, String taskName, String taskDescription, ArrayList<String> taskTags) throws SchedulerFileLockException
    {
        addTask(null,null, deferTime, Duration.ofHours(0),0.0f, taskName, taskDescription, taskTags);
    }

    //Creates a new repeated task
    public void addRepeatedTask(Duration repeatDuration, String taskName, String taskDescription, ArrayList<String> taskTags) throws SchedulerFileLockException
    {
        addTask(null,null, LocalDateTime.now(), repeatDuration,1.0f, taskName, taskDescription, taskTags);
    }

    //Creates a new time-to-time task
    public void addTimeToTimeTask(Duration approxRepeatDuration, String taskName, String taskDescription, ArrayList<String> taskTags) throws SchedulerFileLockException
    {
        addTask(null,null, LocalDateTime.now(), approxRepeatDuration,0.5f, taskName, taskDescription, taskTags);
    }

    //Creates a new one-time immediate task and adds it to the selected task chain
    public void addImmediateChainedTask(TaskChain chain, String taskName, String taskDescription, ArrayList<String> taskTags) throws SchedulerFileLockException
    {
        addTask(null, chain, LocalDateTime.now(), Duration.ofHours(0), 0.0f, taskName, taskDescription, taskTags);
    }

    //Creates a new one-time deferred task and adds it to the selected task chain
    public void addDeferredChainedTask(TaskChain chain, LocalDateTime deferTime, String taskName, String taskDescription, ArrayList<String> taskTags) throws SchedulerFileLockException
    {
        addTask(null, chain, deferTime, Duration.ofHours(0), 0.0f, taskName, taskDescription, taskTags);
    }

    //Creates a new task and adds it to the provided pool
    public void addPoolTask(TaskPool pool, LocalDateTime deferTime, String taskName, String taskDescription, ArrayList<String> taskTags) throws SchedulerFileLockException
    {
        addTask( pool, null, deferTime, Duration.ofHours(0), 0.0f, taskName, taskDescription, taskTags);
    }

    //Creates a new explicit task chain
    public void addTaskChain(String name, String description) throws SchedulerFileLockException
    {
        //Create a new unnamed task pool to hold the chain
        TaskPool pool = new TaskPool("", "");
        addTaskChainToPool(pool, name, description);
        mTaskPools.add(pool);
        saveScheduledTasks();
    }

    //Creates a new explicit task pool
    public void addTaskPool(String name, String description) throws SchedulerFileLockException
    {
        TaskPool pool = new TaskPool(name, description);
        mTaskPools.add(pool);
        saveScheduledTasks();
    }

    //Creates a new explicit task chain and adds it to the provided pool
    public void addTaskChainToPool(TaskPool pool, String name, String description) throws SchedulerFileLockException
    {
        TaskChain chain = new TaskChain(name, description);
        pool.addTaskSource(chain);
        saveScheduledTasks();
    }

    //Adds a general task to task pool pool or task chain chain scheduled to be added at deferTime with repeat duration repeatDuration and repeat probability repeatProbability
    private void addTask(TaskPool pool, TaskChain chain, LocalDateTime deferTime,
                         Duration repeatDuration, float repeatProbability,
                         String taskName, String taskDescription, ArrayList<String> taskTags) throws SchedulerFileLockException
    {
        long          taskId      = mIdGenerator.generateNextId();
        LocalDateTime currentTime = LocalDateTime.now();

        ScheduledTask scheduledTask = new ScheduledTask(taskId, taskName, taskDescription, currentTime, deferTime,
                                                        taskTags, repeatDuration, repeatProbability);

        //Calculate the next time to do the task
        scheduledTask.reschedule(deferTime);

        if(pool == null && chain == null) //Add a single task source to the new pool
        {
            //Neither task chain nor pool are provided, create an implicit pool and add task there
            SingleTaskSource taskSource = new SingleTaskSource(scheduledTask);

            TaskPool implicitPool = new TaskPool("", "");
            implicitPool.addTaskSource(taskSource);

            mTaskPools.add(implicitPool);
        }
        else if(pool == null)
        {
            //Task chain provided, add the task there
            chain.addTaskToChain(scheduledTask);
        }
        else
        {
            //Task pool provided, add the task there
            SingleTaskSource taskSource = new SingleTaskSource(scheduledTask);
            pool.addTaskSource(taskSource);
        }

        saveScheduledTasks();
    }

    //Sets the start id for the task id generator
    public void setLastTaskId(long id)
    {
        long maxId = id;
        for(TaskPool pool: mTaskPools)
        {
            long poolMaxId = pool.getMaxTaskId();
            if(maxId > poolMaxId)
            {
                maxId = poolMaxId;
            }
        }

        mIdGenerator.setFirstId(maxId);
    }
}
