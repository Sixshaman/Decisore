package com.sixshaman.decisore.scheduler.objective;

import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.sixshaman.decisore.list.EnlistedObjective;
import com.sixshaman.decisore.scheduler.chain.ObjectiveChain;
import com.sixshaman.decisore.scheduler.pool.PoolElement;
import com.sixshaman.decisore.utils.ParseUtils;
import com.sixshaman.decisore.utils.RandomUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.Provider;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

public class ScheduledObjective extends PoolElement
{
    //The date when the objective was created and added to the scheduler
    final LocalDateTime mDateCreated;

    //The date when the objective will be added to the main list next time
    LocalDateTime mScheduledAddDate;

    //The date when the objective is regularly added to the main list
    LocalDateTime mRegularScheduledAddDate;

    //Objective tags (why not?)
    final ArrayList<String> mTags;

    //Is it valid?
    boolean mIsValid;

    //When to repeat the objective
    final Duration mRepeatDuration;

    //For the objectives that are added to the list "sometimes"
    final float mRepeatProbability;

    //Creates a new active scheduled objective ready to be used by the scheduler
    public ScheduledObjective(long id, String name, String description, LocalDateTime createdDate, LocalDateTime scheduleDate, ArrayList<String> tags, Duration repeatDuration, float repeatProbability)
    {
        super(id, name, description);

        mIsValid  = true;

        mDateCreated             = createdDate;
        mScheduledAddDate        = scheduleDate;
        mRegularScheduledAddDate = scheduleDate;

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
            result.put("Id",       Long.toString(getId()));
            result.put("ParentId", Long.toString(getParentId()));

            result.put("Name",        getName());
            result.put("Description", getDescription());

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

            result.put("IsActive", Boolean.toString(!isPaused()));

            result.put("RepeatDuration",    Long.toString(mRepeatDuration.toMinutes()));
            result.put("RepeatProbability", Float.toString(mRepeatProbability));
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static ScheduledObjective fromJSON(JSONObject jsonObject)
    {
        try
        {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:nnnnnnnnn");

            long id       = jsonObject.getLong("Id");
            long parentId = jsonObject.getLong("ParentId");

            String name        = jsonObject.getString("Name");
            String description = jsonObject.getString("Description");

            String createdDateString          = jsonObject.getString("DateCreated");
            String scheduledDateString        = jsonObject.getString("DateScheduled");
            String regularScheduledDateString = jsonObject.getString("DateScheduledRegular");

            String isActiveString = jsonObject.getString("IsActive");

            String repeatDurationString    = jsonObject.getString("RepeatDuration");
            String repeatProbabilityString = jsonObject.getString("RepeatProbability");

            ArrayList<String> objectiveTags = new ArrayList<>();
            JSONArray tagsJSONArray = jsonObject.optJSONArray("Tags");
            if(tagsJSONArray != null)
            {
                for(int i = 0; i < tagsJSONArray.length(); i++)
                {
                    String tagStr = (String)tagsJSONArray.opt(i);
                    if(!tagStr.isEmpty())
                    {
                        objectiveTags.add(tagStr);
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
            if(!isActiveString.isEmpty())
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

            if(id != -1 && !name.isEmpty() && createdDate != null && scheduledDate != null && repeatDurationMinutes != null && repeatProbability != null)
            {
                Duration repeatDuration = Duration.ofMinutes(repeatDurationMinutes);
                ScheduledObjective objective = new ScheduledObjective(id, name, description, createdDate, scheduledDate, objectiveTags, repeatDuration, repeatProbability);

                objective.setParentId(parentId);
                if(regularScheduleDate != null)
                {
                    objective.mRegularScheduledAddDate = regularScheduleDate; //Can be different from scheduledDate
                }

                if(!isActive)
                {
                    objective.setPaused(true);
                }

                return objective;
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    //Reschedules the objective to the new enlist date
    public void reschedule(LocalDateTime referenceTime, int dayStartTime)
    {
        //Cannot reschedule non-repeated objectives and won't reschedule paused objectives
        if(mRepeatProbability < 0.0001f || isPaused())
        {
            return;
        }

        mRegularScheduledAddDate = mRegularScheduledAddDate.minusHours(dayStartTime).truncatedTo(ChronoUnit.DAYS).plusHours(dayStartTime);

        while(mRegularScheduledAddDate.isBefore(referenceTime)) //Simulate the passing of time
        {
            long hoursToAdd;
            if(mRepeatProbability > 0.9999f) //If it's a strictly repeated objective, just add the duration
            {
                hoursToAdd = mRepeatDuration.toHours();
            }
            else //Occasional objectives are repeated using normal distribution
            {
                hoursToAdd = RandomUtils.getInstance().getRandomGauss(mRepeatDuration.toHours(), mRepeatProbability);
            }

            hoursToAdd = Math.max(hoursToAdd, 24); //Never add less than a day
            mRegularScheduledAddDate = mRegularScheduledAddDate.plusHours(hoursToAdd);
        }

        //Day starts at 6AM
        mRegularScheduledAddDate = mRegularScheduledAddDate.minusHours(dayStartTime).truncatedTo(ChronoUnit.DAYS).plusHours(dayStartTime);
        mScheduledAddDate        = mRegularScheduledAddDate;
    }

    //Reschedules the objective to the new enlist date (possibly out-of-order)
    public void rescheduleUnregulated(LocalDateTime newEnlistDate)
    {
        mScheduledAddDate = newEnlistDate;
    }

    public boolean isRepeatable()
    {
        return mRepeatProbability > 0.0001f;
    }

    @Override
    public boolean isValid()
    {
        return mIsValid;
    }

    @Override
    public String getElementName()
    {
        return "ScheduledObjective";
    }

    @Override
    public EnlistedObjective obtainEnlistedObjective(HashSet<Long> ignoredObjectiveIds, LocalDateTime referenceTime, int dayStartHour)
    {
        if(!isAvailable(ignoredObjectiveIds, referenceTime, dayStartHour))
        {
            if(isRepeatable() && ignoredObjectiveIds.contains(getId()))
            {
                //Need to reschedule the objective so that next time no new objective gets added
                reschedule(referenceTime, dayStartHour);
            }

            return null;
        }

        EnlistedObjective enlistedObjective = new EnlistedObjective(getId(), getParentId(), mDateCreated, referenceTime, getName(), getDescription(), mTags);
        if(!isRepeatable())
        {
            mIsValid = false;
        }
        else
        {
            reschedule(referenceTime, dayStartHour);
        }

        return enlistedObjective;
    }

    @Override
    public void updateDayStart(LocalDateTime referenceTime, int oldStartHour, int newStartHour)
    {
        if(mScheduledAddDate.isAfter(referenceTime))
        {
            mRegularScheduledAddDate = mRegularScheduledAddDate.minusHours(oldStartHour).plusHours(newStartHour);
            mScheduledAddDate        = mScheduledAddDate.minusHours(oldStartHour).plusHours(newStartHour);
        }
    }

    @Override
    public boolean isRelatedToObjective(long objectiveId)
    {
        return getId() == objectiveId;
    }

    @Override
    public boolean mergeRelatedObjective(ScheduledObjective objective)
    {
        if(!isRelatedToObjective(objective.getId()))
        {
            return false;
        }

        rescheduleUnregulated(objective.getScheduledEnlistDate());
        return true;
    }

    @Override
    public ScheduledObjective getRelatedObjectiveById(long objectiveId)
    {
        if(isRelatedToObjective(objectiveId))
        {
            return this;
        }

        return null;
    }

    @Override
    public ObjectiveChain getRelatedChainById(long chainId)
    {
        //There's never a chain related
        return null;
    }

    @Override
    public ObjectiveChain getChainForObjectiveById(long objectiveId)
    {
        //There's never a chain related
        return null;
    }

    @Override
    public long getLargestRelatedId()
    {
        return getId();
    }

    @Override
    public boolean isAvailable(HashSet<Long> blockingObjectiveIds, LocalDateTime referenceTime, int dayStartHour)
    {
        return !isPaused() && referenceTime.isAfter(getScheduledEnlistDate()) && !blockingObjectiveIds.contains(getId());
    }

    @SuppressWarnings("unused")
    public LocalDateTime getCreationDate()
    {
        return mDateCreated;
    }

    public LocalDateTime getScheduledEnlistDate()
    {
        return mScheduledAddDate;
    }

    @SuppressWarnings("unused")
    Duration getRepeatDuration()
    {
        return mRepeatDuration;
    }

    @SuppressWarnings("unused")
    float getRepeatProbability()
    {
        return mRepeatProbability;
    }
}