package com.sixshaman.advancedunforgetter;

import javax.xml.datatype.Duration;

public class ScheduledTask
{
    //The task itself
    private Task mTask;

    //Is it active or paused?
    private boolean mActive;

    //When to repeat the task
    private Duration mRepeatDuration;

    //For the tasks that are added to the list "sometimes"
    private float mRepeatProbability;

    //Creates a new active scheduled task ready to be used by the task scheduler
    ScheduledTask(Task task, Duration repeatDuration, float repeatProbability)
    {
        mActive = true;

        mTask              = task;
        mRepeatDuration    = repeatDuration;
        mRepeatProbability = repeatProbability;
    }

    //Pauses the task so it's not repeated anymore
    void pause()
    {
        mActive = false;
    }

    //Unpauses the task
    void unpause()
    {
        mActive = false;
    }

    boolean isActive()
    {
        return mActive;
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