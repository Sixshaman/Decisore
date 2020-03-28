package com.sixshaman.advancedunforgetter.scheduler;

import com.sixshaman.advancedunforgetter.list.EnlistedTask;
import com.sixshaman.advancedunforgetter.utils.RandomUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

class ScheduledTask
{
    //The task ID
    private long mId;

    //The date when the task was created and added to the task scheduler
    private LocalDateTime mDateCreated;

    //The date when the task will be added to the main task list next time
    private LocalDateTime mScheduledAddDate;

    //The task name
    private String mName;

    //The task description
    private String mDescription;

    //Task tags (why not?)
    private ArrayList<String> mTags;

    //Is it active or paused? If paused, the scheduler won't add it to the task list even after the mScheduledAddDate
    private boolean mIsActive;

    //When to repeat the task
    private Duration mRepeatDuration;

    //For the tasks that are added to the list "sometimes"
    private float mRepeatProbability;

    //Creates a new active scheduled task ready to be used by the task scheduler
    ScheduledTask(long id, String name, String description, LocalDateTime createdDate, LocalDateTime scheduleDate, ArrayList<String> tags, Duration repeatDuration, float repeatProbability)
    {
        mIsActive = true;

        mId = id;

        mDateCreated      = createdDate;
        mScheduledAddDate = scheduleDate;

        mName        = name;
        mDescription = description;

        mRepeatDuration    = repeatDuration;
        mRepeatProbability = repeatProbability;
    }

    //Reschedules the task to the new enlist date
    void reschedule(LocalDateTime referenceTime)
    {
        //Cannot reschedule non-repeated tasks and won't reschedule paused tasks
        if(mRepeatProbability == 0.0f || !mIsActive)
        {
            return;
        }

        if(mRepeatProbability == 1.0f) //If it's a strictly repeated task, just add the duration
        {
            LocalDateTime nextDateTime = referenceTime.plusHours(mRepeatDuration.toHours());
            mScheduledAddDate = nextDateTime.truncatedTo(ChronoUnit.HOURS);
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
            mScheduledAddDate = nextDateTime.truncatedTo(ChronoUnit.HOURS);
        }
    }

    //Transforms scheduled task to an enlisted
    public EnlistedTask toEnlisted(LocalDateTime enlistDate)
    {
        return new EnlistedTask(mId, mDateCreated, enlistDate, mName, mDescription, mTags);
    }

    public long getId()
    {
        return mId;
    }

    LocalDateTime getCreationDate()
    {
        return mDateCreated;
    }

    LocalDateTime getScheduledEnlistDate()
    {
        return mScheduledAddDate;
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

    Duration getRepeatDuration()
    {
        return mRepeatDuration;
    }

    float getRepeatProbability()
    {
        return mRepeatProbability;
    }
}