package com.sixshaman.advancedunforgetter;

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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

//The class to schedule all deferred tasks. The model for the scheduler UI
public class TaskScheduler
{
    //The list of all the tasks scheduled to be done later
    private ArrayList<ScheduledTask> mScheduledTasks;

    //The list of all the task pools. Task chains are included here too - all task chains implicitly create a pool
    private ArrayList<TaskPool> mTaskPools;

    //Generated task ids
    private TaskIdGenerator mIdGenerator;

    //Creates a new task scheduler
    public TaskScheduler()
    {
        mScheduledTasks = new ArrayList<>();

        mTaskPools = new ArrayList<>();

        mIdGenerator = new TaskIdGenerator();
    }

    //Updates the task scheduler: adds all ready-to-be-done tasks to the main list, reschedules tasks, updates chains and pools
    public void update()
    {
        ArrayList<ScheduledTask> changedScheduledTasks = new ArrayList<>(); //Rebuild task list after each update

        LocalDateTime currentDateTime = LocalDateTime.now();

        //Update scheduled tasks first
        for(ScheduledTask scheduledTask: mScheduledTasks)
        {
            if(scheduledTask.isActive()) //Only check active tasks
            {
                if(currentDateTime.isAfter(scheduledTask.getTask().getAddedDate()))
                {
                    //Add the task to the main list
                    moveTaskToMainList(scheduledTask.getTask());

                    //DON'T add it back if it's a one-time deferred task
                    if(scheduledTask.getRepeatProbability() > 0.0f)
                    {
                        scheduledTask.reschedule(currentDateTime);
                        changedScheduledTasks.add(scheduledTask);
                    }
                }
                else
                {
                    //Nothing changed
                    changedScheduledTasks.add(scheduledTask);
                }
            }
            else
            {
                //Nothing changed
                changedScheduledTasks.add(scheduledTask);
            }
        }



        mScheduledTasks = changedScheduledTasks;
    }

    //Creates a new one-time task and immediately adds it to the main list
    public void addImmediateTask(String taskName, String taskDescription, ArrayList<String> taskTags)
    {
        long          taskId      = mIdGenerator.getNextId();
        LocalDateTime currentTime = LocalDateTime.now();

        Task task = new Task(taskId, currentTime, taskName, taskDescription, taskTags);
        task.setAddedDate(currentTime);

        moveTaskToMainList(task);
    }

    //Creates a new one-time task and schedules it to be added to the main list later
    public void addDeferredTask(LocalDateTime deferTime, String taskName, String taskDescription, ArrayList<String> taskTags)
    {
        long          taskId      = mIdGenerator.getNextId();
        LocalDateTime currentTime = LocalDateTime.now();

        //Check just in case
        if(deferTime.isAfter(currentTime))
        {
            Task task = new Task(taskId, currentTime, taskName, taskDescription, taskTags);
            task.setAddedDate(deferTime);

            //Since it's a one-time task, it doesn't have a repeat duration
            Duration repeatDuration = Duration.ofHours(0);
            ScheduledTask scheduledTask = new ScheduledTask(task, repeatDuration, 0.0f);

            mScheduledTasks.add(scheduledTask);
        }
    }

    //Creates a new repeated task
    public void addRepeatedTask(Duration repeatDuration, String taskName, String taskDescription, ArrayList<String> taskTags)
    {
        long          taskId      = mIdGenerator.getNextId();
        LocalDateTime currentTime = LocalDateTime.now();

        Task task                   = new Task(taskId, currentTime, taskName, taskDescription, taskTags);
        ScheduledTask scheduledTask = new ScheduledTask(task, repeatDuration, 1.0f);

        //Calculate the next time to do the task
        scheduledTask.reschedule(currentTime);
        mScheduledTasks.add(scheduledTask);
    }

    //Creates a new time-to-time task
    public void addTimeToTimeTask(Duration approxRepeatDuration, String taskName, String taskDescription, ArrayList<String> taskTags)
    {
        long          taskId      = mIdGenerator.getNextId();
        LocalDateTime currentTime = LocalDateTime.now();

        Task task                   = new Task(taskId, currentTime, taskName, taskDescription, taskTags);
        ScheduledTask scheduledTask = new ScheduledTask(task, approxRepeatDuration, 0.5f);

        //Calculate the next time to do the task
        scheduledTask.reschedule(currentTime);
        mScheduledTasks.add(scheduledTask);
    }

    //Creates a new one-time immediate task and adds it to the selected task chain
    public void addImmediateChainedTask(TaskChain chain, String taskName, String taskDescription, ArrayList<String> taskTags)
    {
        long          taskId      = mIdGenerator.getNextId();
        LocalDateTime currentTime = LocalDateTime.now();

        Task task = new Task(taskId, currentTime, taskName, taskDescription, taskTags);
        task.setAddedDate(currentTime);

        //Since it's a one-time task, it doesn't have a repeat duration
        Duration repeatDuration = Duration.ofHours(0);
        ScheduledTask scheduledTask = new ScheduledTask(task, repeatDuration, 0.0f);

        //Don't add to mScheduledTasks, since this one will be maintaied by the chain
        chain.addTaskToChain(scheduledTask);
    }

    //Creates a new one-time deferred task and adds it to the selected task chain
    public void addDeferredChainedTask(TaskChain chain, LocalDateTime deferTime, String taskName, String taskDescription, ArrayList<String> taskTags)
    {
        long          taskId      = mIdGenerator.getNextId();
        LocalDateTime currentTime = LocalDateTime.now();

        //Check just in case
        if(deferTime.isAfter(currentTime))
        {
            Task task = new Task(taskId, currentTime, taskName, taskDescription, taskTags);
            task.setAddedDate(deferTime);

            //Since it's a one-time task, it doesn't have a repeat duration
            Duration repeatDuration = Duration.ofHours(0);
            ScheduledTask scheduledTask = new ScheduledTask(task, repeatDuration, 0.0f);

            //Don't add to mScheduledTasks, since this one will be maintaied by the chain
            chain.addTaskToChain(scheduledTask);
        }
    }

    public void addTaskPool()
    {
        
    }

    private void moveTaskToMainList(Task task)
    {
        if(task != null)
        {
            //TODO
        }
    }
}
