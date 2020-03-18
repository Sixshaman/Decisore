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

import com.sixshaman.advancedunforgetter.list.TaskList;
import com.sixshaman.advancedunforgetter.utils.TaskIdGenerator;
import com.sixshaman.advancedunforgetter.utils.Task;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

//The class to schedule all deferred tasks. The model for the scheduler UI
public class TaskScheduler
{
    //The list of all the task pools.
    //All single tasks are task pools with a single SingleTaskSource
    //All single task chains are task pools with a single TaskChain
    private ArrayList<TaskPool> mTaskPools;

    //Generated task ids
    private TaskIdGenerator mIdGenerator;

    //The main list of tasks that scheduler adds tasks to
    private TaskList mMainList;

    //Creates a new task scheduler that is bound to mainList
    public TaskScheduler(TaskList mainList)
    {
        mTaskPools = new ArrayList<>();

        mIdGenerator = new TaskIdGenerator();

        mMainList = mainList;
    }

    //Updates the task scheduler: adds all ready-to-be-done tasks to the main list, reschedules tasks, updates chains and pools
    public void update()
    {
        ArrayList<TaskPool> changedPools = new ArrayList<>(); //Rebuild task pool list after each update

        LocalDateTime currentDateTime = LocalDateTime.now();
        for(TaskPool pool: mTaskPools)
        {
            if(pool.isSingleSingleTaskPool())
            {
                //Don't need to check if the last task is done
                Task task = pool.getRandomTask(currentDateTime);
                moveTaskToMainList(task);
            }
            else
            {
                //Only add the task to the main list if the last task provided by the pool is done
                if(!mMainList.isTaskInList(pool.getLastProvidedTaskId()))
                {
                    Task task = pool.getRandomTask(currentDateTime);
                    moveTaskToMainList(task);
                }
            }

            pool.updateTaskSources(currentDateTime);
            if(pool.getTaskSourceCount() != 0) //Don't add empty pools
            {
                changedPools.add(pool);
            }
        }

        mTaskPools = changedPools;
    }

    //Creates a new one-time task and immediately adds it to the main list
    public void addImmediateTask(String taskName, String taskDescription, ArrayList<String> taskTags)
    {
        long          taskId      = mIdGenerator.generateNextId();
        LocalDateTime currentTime = LocalDateTime.now();

        Task task = new Task(taskId, currentTime, taskName, taskDescription, taskTags);
        task.setAddedDate(currentTime);

        moveTaskToMainList(task);
    }

    //Creates a new one-time task and schedules it to be added to the main list later
    public void addDeferredTask(LocalDateTime deferTime, String taskName, String taskDescription, ArrayList<String> taskTags)
    {
        addTask(null,null, deferTime, Duration.ofHours(0),0.0f, taskName, taskDescription, taskTags);
    }

    //Creates a new repeated task
    public void addRepeatedTask(Duration repeatDuration, String taskName, String taskDescription, ArrayList<String> taskTags)
    {
        addTask(null,null, LocalDateTime.now(), repeatDuration,1.0f, taskName, taskDescription, taskTags);
    }

    //Creates a new time-to-time task
    public void addTimeToTimeTask(Duration approxRepeatDuration, String taskName, String taskDescription, ArrayList<String> taskTags)
    {
        addTask(null,null, LocalDateTime.now(), approxRepeatDuration,0.5f, taskName, taskDescription, taskTags);
    }

    //Creates a new one-time immediate task and adds it to the selected task chain
    public void addImmediateChainedTask(TaskChain chain, String taskName, String taskDescription, ArrayList<String> taskTags)
    {
        addTask(null, chain, LocalDateTime.now(), Duration.ofHours(0), 0.0f, taskName, taskDescription, taskTags);
    }

    //Creates a new one-time deferred task and adds it to the selected task chain
    public void addDeferredChainedTask(TaskChain chain, LocalDateTime deferTime, String taskName, String taskDescription, ArrayList<String> taskTags)
    {
        addTask(null, chain, deferTime, Duration.ofHours(0), 0.0f, taskName, taskDescription, taskTags);
    }

    //Creates a new task and adds it to the provided pool
    public void addPoolTask(TaskPool pool, LocalDateTime deferTime, String taskName, String taskDescription, ArrayList<String> taskTags)
    {
        addTask( pool, null, deferTime, Duration.ofHours(0), 0.0f, taskName, taskDescription, taskTags);
    }

    //Creates a new explicit task chain
    public void addTaskChain(String name, String description)
    {
        //Create a new unnamed task pool to hold the chain
        TaskPool pool = new TaskPool("", "");
        addTaskChainToPool(pool, name, description);
        mTaskPools.add(pool);
    }

    //Creates a new explicit task pool
    public void addTaskPool(String name, String description)
    {
        TaskPool pool = new TaskPool(name, description);
        mTaskPools.add(pool);
    }

    //Creates a new explicit task chain and adds it to the provided pool
    public void addTaskChainToPool(TaskPool pool, String name, String description)
    {
        TaskChain chain = new TaskChain(name, description);
        pool.addTaskSource(chain);
    }

    //Adds a general task to task pool pool or task chain chain scheduled to be added at deferTime with repeat duration repeatDuration and repeat probability repeatProbability
    private void addTask(TaskPool pool, TaskChain chain, LocalDateTime deferTime,
                         Duration repeatDuration, float repeatProbability,
                         String taskName, String taskDescription, ArrayList<String> taskTags)
    {
        long          taskId      = mIdGenerator.generateNextId();
        LocalDateTime currentTime = LocalDateTime.now();

        Task task                   = new Task(taskId, currentTime, taskName, taskDescription, taskTags);
        ScheduledTask scheduledTask = new ScheduledTask(task, repeatDuration, repeatProbability);

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
    }

    //Moves the task to the main task list
    private void moveTaskToMainList(Task task)
    {
        if(task != null)
        {
            mMainList.addTask(task);
        }
    }
}
