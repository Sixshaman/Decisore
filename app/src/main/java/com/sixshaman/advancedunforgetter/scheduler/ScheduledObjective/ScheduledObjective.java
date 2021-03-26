package com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective;

import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.PoolElement;
import com.sixshaman.advancedunforgetter.utils.RandomUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class ScheduledObjective implements PoolElement
{
    //The objective ID
    final long mId;

    //The date when the objective was created and added to the scheduler
    LocalDateTime mDateCreated;

    //The date when the objective will be added to the main list next time
    LocalDateTime mScheduledAddDate;

    //The date when the objective is regularly added to the main list
    LocalDateTime mRegularScheduledAddDate;

    //The objective name
    String mName;

    //The objective description
    String mDescription;

    //Objective tags (why not?)
    ArrayList<String> mTags;

    //Is it active or paused? If paused, the scheduler won't add it to the objective list even after the mScheduledAddDate
    boolean mIsActive;

    //When to repeat the objective
    Duration mRepeatDuration;

    //For the objectives that are added to the list "sometimes"
    float mRepeatProbability;

    //Creates a new active scheduled objective ready to be used by the scheduler
    public ScheduledObjective(long id, String name, String description, LocalDateTime createdDate, LocalDateTime scheduleDate, ArrayList<String> tags, Duration repeatDuration, float repeatProbability)
    {
        mIsActive = true;

        mId = id;

        mDateCreated             = createdDate;
        mScheduledAddDate        = scheduleDate;
        mRegularScheduledAddDate = scheduleDate;

        mName        = name;
        mDescription = description;

        mRepeatDuration    = repeatDuration;
        mRepeatProbability = repeatProbability;

        if(tags == null)
        {
            mTags = new ArrayList<>();
        }
        else
        {
            mTags = tags;
        }
    }

    //Serializes the objective into its JSON representation
    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();

        try
        {
            result.put("Id", Long.toString(mId));

            result.put("Name",        mName);
            result.put("Description", mDescription);

            JSONArray jsonTagArray = new JSONArray(mTags);
            result.put("Tags", jsonTagArray);

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:nnnnnnnnn");

            if(mDateCreated != null)
            {
                String createdDateString = dateTimeFormatter.format(mDateCreated);
                result.put("DateCreated", createdDateString);
            }

            if(mScheduledAddDate != null)
            {
                String scheduledDateString = dateTimeFormatter.format(mScheduledAddDate);
                result.put("DateScheduled", scheduledDateString);
            }

            if(mRegularScheduledAddDate != null)
            {
                String regularScheduledDateString = dateTimeFormatter.format(mRegularScheduledAddDate);
                result.put("DateScheduledRegular", regularScheduledDateString);
            }

            result.put("IsActive", Boolean.toString(mIsActive));

            result.put("RepeatDuration",    Long.toString(mRepeatDuration.toMinutes()));
            result.put("RepeatProbability", Float.toString(mRepeatProbability));
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    //Reschedules the objective to the new enlist date
    public void reschedule(LocalDateTime referenceTime)
    {
        //Cannot reschedule non-repeated objectives and won't reschedule paused objectives
        if(mRepeatProbability < 0.0001f || !mIsActive)
        {
            return;
        }

        mRegularScheduledAddDate = mRegularScheduledAddDate.plusDays(1).minusHours(6).truncatedTo(ChronoUnit.DAYS).plusHours(6); //Add at least one day

        while(mRegularScheduledAddDate.isBefore(referenceTime)) //Simulate the passing of time
        {
            long hoursToAdd = 0;
            if(mRepeatProbability > 0.9999f) //If it's a strictly repeated objective, just add the duration
            {
                hoursToAdd = mRepeatDuration.toHours();
            }
            else //Occasional objectives are repeated using normal distribution
            {
                hoursToAdd = RandomUtils.getInstance().getRandomGauss(mRepeatDuration.toHours(), mRepeatProbability);
                if(hoursToAdd < 1)
                {
                    //Clamp the value just in case
                    hoursToAdd = 1;
                }
            }

            mRegularScheduledAddDate = mRegularScheduledAddDate.plusHours(hoursToAdd);
        }

        //Day starts at 6AM
        mScheduledAddDate = mRegularScheduledAddDate.truncatedTo(ChronoUnit.DAYS).plusHours(6);
    }

    //Reschedules the objective to the new enlist date (possibly out-of-order)
    public void rescheduleUnregulated(LocalDateTime newEnlistDate)
    {
        if(newEnlistDate.isBefore(mScheduledAddDate))
        {
           mScheduledAddDate = newEnlistDate;
        }
    }

    public boolean isRepeatable()
    {
        return mRepeatProbability > 0.0001f;
    }

    //Transforms scheduled objective to an enlisted
    public EnlistedObjective toEnlisted(LocalDateTime enlistDate)
    {
        return new EnlistedObjective(mId, mDateCreated, enlistDate, mName, mDescription, mTags);
    }

    @Override
    public boolean isPaused()
    {
        return !mIsActive;
    }

    @Override
    public String getElementName()
    {
        return "ScheduledObjective";
    }

    @Override
    public boolean isAvailable(LocalDateTime referenceTime)
    {
        return !isPaused() && referenceTime.isAfter(getScheduledEnlistDate());
    }

    public long getId()
    {
        return mId;
    }

    public LocalDateTime getCreationDate()
    {
        return mDateCreated;
    }

    public LocalDateTime getScheduledEnlistDate()
    {
        return mScheduledAddDate;
    }

    @Override
    public String getName()
    {
        return mName;
    }

    @Override
    public String getDescription()
    {
        return mDescription;
    }

    public void setName(String name)
    {
        mName = name;
    }

    public void setDescription(String description)
    {
        mDescription = description;
    }

    //Pauses the objective so it's not repeated anymore
    void pause()
    {
        mIsActive = false;
    }

    //Unpauses the objective
    void unpause()
    {
        mIsActive = false;
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