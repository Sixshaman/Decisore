package com.sixshaman.advancedunforgetter.scheduler;

import com.sixshaman.advancedunforgetter.utils.Task;
import com.sixshaman.advancedunforgetter.utils.RandomUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

class ScheduledTask
{
    //The task itself
    private Task mTask;

    //Is it active or paused? If paused, the scheduler can't add it to the task list
    private boolean mIsActive;

    //When to repeat the task
    private Duration mRepeatDuration;

    //For the tasks that are added to the list "sometimes"
    private float mRepeatProbability;

    //Creates a new active scheduled task ready to be used by the task scheduler
    ScheduledTask(Task task, Duration repeatDuration, float repeatProbability)
    {
        mIsActive = true;

        mTask              = task;
        mRepeatDuration    = repeatDuration;
        mRepeatProbability = repeatProbability;
    }

    //Reschedules the task to the new add date
    void reschedule(LocalDateTime referenceTime)
    {
        //Cannot reschedule non-repeated tasks and won't reschedule paused tasks
        if(mRepeatProbability == 0.0f || !mIsActive)
        {
            return;
        }

        //Replace the old task
        mTask = new Task(mTask.getId(), mTask.getCreatedDate(), mTask.getName(), mTask.getDescription(), mTask.getTags());

        if(mRepeatProbability == 1.0f) //If it's a strictly repeated task, just add the duration
        {
            LocalDateTime nextDateTime = referenceTime.plusHours(mRepeatDuration.toHours());
            nextDateTime = nextDateTime.truncatedTo(ChronoUnit.HOURS);
            mTask.setAddedDate(nextDateTime);
        }
        else //If it's a time-to-time task, set a random date
        {
            //Time-to-time tasks are repeated using normal distribution
            long randomHoursToAdd = RandomUtils.getInstance().getRandomGauss(mRepeatDuration.toHours(), mRepeatProbability);
            if(randomHoursToAdd < 1)
            {
                //Clamp the value just in case
                randomHoursToAdd = 1;
            }

            LocalDateTime nextDateTime = referenceTime.plusHours(randomHoursToAdd);
            nextDateTime = nextDateTime.truncatedTo(ChronoUnit.HOURS);
            mTask.setAddedDate(nextDateTime);
        }
    }

    //Pauses the task so it's not repeated anymore
    void pause()
    {
        mIsActive = false;
    }

    //Unpauses the task
    void unpause()
    {
        mIsActive = false;
    }

    boolean isActive()
    {
        return mIsActive;
    }

    Task getTask()
    {
        return mTask;
    }

    Duration getRepeatDuration()
    {
        return mRepeatDuration;
    }

    float getRepeatProbability()
    {
        return mRepeatProbability;
    }
}