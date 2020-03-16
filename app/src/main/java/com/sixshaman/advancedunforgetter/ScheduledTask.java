package com.sixshaman.advancedunforgetter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

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
    public void reschedule()
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
            mTask.setAddedDate(LocalDateTime.now().plusHours(mRepeatDuration.toHours()));
        }
        else //If it's a time-to-time task, set a random date
        {
            Random random = new Random();

            //Time-to-time tasks are repeated using normal distribution with mean set to mRepeatDuration
            float repeatMean   = (float)mRepeatDuration.toHours();
            float repeatStdDev = repeatMean * mRepeatProbability;

            float randomHoursToAdd = (float)(random.nextGaussian() * repeatStdDev + repeatMean);
            if(randomHoursToAdd < 1.0f)
            {
                //Clamp the value just in case
                randomHoursToAdd = 1.0f;
            }

            mTask.setAddedDate(LocalDateTime.now().plusHours((long)randomHoursToAdd));
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