package com.sixshaman.decisore.scheduler.objective;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

public class ScheduledObjectiveLatestLoader implements ScheduledObjectiveLoader
{
    public ScheduledObjective fromJSON(JSONObject jsonObject)
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:nnnnnnnnn");

        long id = jsonObject.optLong("Id", -1);

        String name        = jsonObject.optString("Name");
        String description = jsonObject.optString("Description");

        String createdDateString          = jsonObject.optString("DateCreated");
        String scheduledDateString        = jsonObject.optString("DateScheduled");
        String regularScheduledDateString = jsonObject.optString("DateScheduledRegular");

        String isActiveString = jsonObject.optString("IsActive");

        String repeatDurationString    = jsonObject.optString("RepeatDuration");
        String repeatProbabilityString = jsonObject.optString("RepeatProbability");

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

        ScheduledObjective objective = null;
        if(id != -1 && !name.isEmpty() && createdDate != null && scheduledDate != null && repeatDurationMinutes != null && repeatProbability != null)
        {
            Duration repeatDuration = Duration.ofMinutes(repeatDurationMinutes);
            objective = new ScheduledObjective(id, name, description, createdDate, scheduledDate, objectiveTags, repeatDuration, repeatProbability);

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
}
