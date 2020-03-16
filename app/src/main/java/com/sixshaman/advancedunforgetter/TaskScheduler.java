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

public class TaskScheduler
{
    //The list of all the tasks scheduled to be done later
    private ArrayList<ScheduledTask> mScheduledTasks;

    //Generated task ids
    private TaskIdGenerator mIdGenerator;

    public TaskScheduler()
    {
        mScheduledTasks = new ArrayList<>();
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

        if(deferTime.isAfter(currentTime))
        {
            Task task = new Task(taskId, currentTime, taskName, taskDescription, taskTags);
            task.setAddedDate(deferTime);

            //Since it's a one-time task, it doesn't have a repeat duration
            Duration repeatDuration = Duration.ofMinutes(0);
            ScheduledTask scheduledTask = new ScheduledTask(task, repeatDuration, 0.0f);

            mScheduledTasks.add(scheduledTask);
        }
    }

    private void moveTaskToMainList(Task task)
    {
        //TODO
    }
}
