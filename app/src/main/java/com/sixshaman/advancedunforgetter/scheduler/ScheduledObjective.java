package com.sixshaman.advancedunforgetter.scheduler;

import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
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

public class ScheduledObjective
{
    private static final String JSON_TASK_ID                    = "Id";
    private static final String JSON_TASK_NAME                  = "Name";
    private static final String JSON_TASK_DESCRIPTION           = "Description";
    private static final String JSON_TASK_TAGS                  = "Tags";
    private static final String JSON_TASK_CREATE_DATE           = "DateCreated";
    private static final String JSON_TASK_SCHEDULE_DATE         = "DateScheduled";
    private static final String JSON_TASK_REGULAR_SCHEDULE_DATE = "DateScheduledRegular";
    private static final String JSON_TASK_IS_ACTIVE             = "IsActive";
    private static final String JSON_TASK_REPEAT_DURATION       = "RepeatDuration";
    private static final String JSON_TASK_REPEAT_PROBABILITY    = "RepeatProbability";

    //The objective ID
    private final long mId;

    //The date when the objective was created and added to the scheduler
    private LocalDateTime mDateCreated;

    //The date when the objective will be added to the main task list next time
    private LocalDateTime mScheduledAddDate;

    //The date when the objective is regularly added to the main list
    private LocalDateTime mRegularScheduledAddDate;

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

    //Serializes the task into its JSON representation
    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();

        try
        {
            result.put(JSON_TASK_ID, Long.toString(mId));

            result.put(JSON_TASK_NAME,        mName);
            result.put(JSON_TASK_DESCRIPTION, mDescription);

            JSONArray jsonTagArray = new JSONArray(mTags);
            result.put(JSON_TASK_TAGS, jsonTagArray);

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:nnnnnnnnn");

            if(mDateCreated != null)
            {
                String createdDateString = dateTimeFormatter.format(mDateCreated);
                result.put(JSON_TASK_CREATE_DATE, createdDateString);
            }

            if(mScheduledAddDate != null)
            {
                String scheduledDateString = dateTimeFormatter.format(mScheduledAddDate);
                result.put(JSON_TASK_SCHEDULE_DATE, scheduledDateString);
            }

            if(mRegularScheduledAddDate != null)
            {
                String regularScheduledDateString = dateTimeFormatter.format(mRegularScheduledAddDate);
                result.put(JSON_TASK_REGULAR_SCHEDULE_DATE, regularScheduledDateString);
            }

            result.put(JSON_TASK_IS_ACTIVE, Boolean.toString(mIsActive));

            result.put(JSON_TASK_REPEAT_DURATION,    Long.toString(mRepeatDuration.toMinutes()));
            result.put(JSON_TASK_REPEAT_PROBABILITY, Float.toString(mRepeatProbability));
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    //Creates a task from its JSON representation
    public static ScheduledObjective fromJSON(JSONObject jsonObject)
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:nnnnnnnnn");

        long id = jsonObject.optLong(JSON_TASK_ID, -1);

        String name        = jsonObject.optString(JSON_TASK_NAME);
        String description = jsonObject.optString(JSON_TASK_DESCRIPTION);

        String createdDateString          = jsonObject.optString(JSON_TASK_CREATE_DATE);
        String scheduledDateString        = jsonObject.optString(JSON_TASK_SCHEDULE_DATE);
        String regularScheduledDateString = jsonObject.optString(JSON_TASK_REGULAR_SCHEDULE_DATE);

        String isActiveString = jsonObject.optString(JSON_TASK_IS_ACTIVE);

        String repeatDurationString    = jsonObject.optString(JSON_TASK_REPEAT_DURATION);
        String repeatProbabilityString = jsonObject.optString(JSON_TASK_REPEAT_PROBABILITY);

        ArrayList<String> taskTags = new ArrayList<>();
        JSONArray tagsJSONArray = jsonObject.optJSONArray(JSON_TASK_TAGS);
        if(tagsJSONArray != null)
        {
            for(int i = 0; i < tagsJSONArray.length(); i++)
            {
                String tagStr = (String)tagsJSONArray.opt(i);
                if(!tagStr.isEmpty())
                {
                    taskTags.add(tagStr);
                }
            }
        }

        LocalDateTime createdDate = null;
        try //Dumb java, time formatting mistake IS NOT an exception, it's a normal situation that should be handled differently
        {
            createdDate = LocalDateTime.parse(createdDateString, dateTimeFormatter);
        }
        catch (DateTimeParseException e)
        {
            e.printStackTrace();
        }

        LocalDateTime scheduledDate = null;
        try
        {
            scheduledDate = LocalDateTime.parse(scheduledDateString, dateTimeFormatter);
        }
        catch (DateTimeParseException e)
        {
            e.printStackTrace();
        }

        LocalDateTime regularScheduleDate = null;
        try
        {
            regularScheduleDate = LocalDateTime.parse(regularScheduledDateString, dateTimeFormatter);
        }
        catch (DateTimeParseException e)
        {
            e.printStackTrace();
        }

        //Java can't into proper parsing... And I thought C++ is bad
        boolean isActive = true;
        if(isActiveString != null && !isActiveString.isEmpty())
        {
            if(isActiveString.equalsIgnoreCase("false"))
            {
                isActive = false;
            }
        }

        Long repeatDurationMinutes = null;
        try
        {
            repeatDurationMinutes = Long.parseLong(repeatDurationString);
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }

        Float repeatProbability = null;
        try
        {
            repeatProbability = Float.parseFloat(repeatProbabilityString);
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }

        ScheduledObjective objective = null;
        if(id != -1 && !name.isEmpty() && createdDate != null && scheduledDate != null && repeatDurationMinutes != null && repeatProbability != null)
        {
            Duration repeatDuration = Duration.ofMinutes(repeatDurationMinutes);
            objective = new ScheduledObjective(id, name, description, createdDate, scheduledDate, taskTags, repeatDuration, repeatProbability);

            if(regularScheduleDate != null)
            {
                objective.mRegularScheduledAddDate = regularScheduleDate; //Can be different from scheduledDate
            }

            if(!isActive)
            {
                objective.pause();
            }
        }

        return objective;
    }

    //Reschedules the objective to the new enlist date
    public void reschedule(LocalDateTime referenceTime)
    {
        //Cannot reschedule non-repeated tasks and won't reschedule paused tasks
        if(mRepeatProbability < 0.0001f || !mIsActive)
        {
            return;
        }

        while(mRegularScheduledAddDate.isBefore(referenceTime)) //Simulate the passing of time
        {
            long hoursToAdd = 0;
            if(mRepeatProbability > 0.9999f) //If it's a strictly repeated task, just add the duration
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

    //Transforms scheduled task to an enlisted
    public EnlistedObjective toEnlisted(LocalDateTime enlistDate)
    {
        return new EnlistedObjective(mId, mDateCreated, enlistDate, mName, mDescription, mTags);
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

    String getName()
    {
        return mName;
    }

    String getDescription()
    {
        return mDescription;
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