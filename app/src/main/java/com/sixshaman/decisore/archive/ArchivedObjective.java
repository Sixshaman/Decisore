package com.sixshaman.decisore.archive;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

//The finished archived objective that will stay in the past
public class ArchivedObjective
{
    private static final String JSON_OBJECTIVE_NAME = "Name";
    private static final String JSON_OBJECTIVE_DESCRIPTION = "Description";
    private static final String JSON_OBJECTIVE_TAGS = "Tags";
    private static final String JSON_OBJECTIVE_CREATE_DATE = "DateCreated";
    private static final String JSON_OBJECTIVE_ADD_DATE = "DateAdded";
    private static final String JSON_OBJECTIVE_FINISH_DATE = "DateFinished";

    //The date when the objective was created and added to the scheduler
    private final LocalDateTime mDateCreated;

    //The date when the objective was added to the main list
    private final LocalDateTime mDateEnlisted;

    //The date when the objective was finished
    private final LocalDateTime mDateFinished;

    //The objective name
    private final String mName;

    //The objective description
    private final String mDescription;

    //Objective tags (why not?)
    private final ArrayList<String> mTags;

    public ArchivedObjective(LocalDateTime creationDate, LocalDateTime addedDate, LocalDateTime finishDate, String name, String description, ArrayList<String> tags)
    {
        mDateCreated  = creationDate;
        mDateEnlisted = addedDate;
        mDateFinished = finishDate;

        mName        = name;
        mDescription = description;

        mTags = tags;
    }

    //Serializes the objective into its JSON representation
    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();

        try
        {
            result.put(JSON_OBJECTIVE_NAME,        mName);
            result.put(JSON_OBJECTIVE_DESCRIPTION, mDescription);

            JSONArray jsonTagArray = new JSONArray(mTags);
            result.put(JSON_OBJECTIVE_TAGS, jsonTagArray);

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:nnnnnnnnn");

            if(mDateCreated != null)
            {
                String createdDateString = dateTimeFormatter.format(mDateCreated);
                result.put(JSON_OBJECTIVE_CREATE_DATE, createdDateString);
            }

            if(mDateEnlisted != null)
            {
                String addedDateString = dateTimeFormatter.format(mDateEnlisted);
                result.put(JSON_OBJECTIVE_ADD_DATE, addedDateString);
            }

            if(mDateFinished != null)
            {
                String finishDateString = dateTimeFormatter.format(mDateFinished);
                result.put(JSON_OBJECTIVE_FINISH_DATE, finishDateString);
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    //Creates an objective from its JSON representation
    public static ArchivedObjective fromJSON(JSONObject jsonObject)
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:nnnnnnnnn");

        String name        = jsonObject.optString(JSON_OBJECTIVE_NAME);
        String description = jsonObject.optString(JSON_OBJECTIVE_DESCRIPTION);

        String createdDateString = jsonObject.optString(JSON_OBJECTIVE_CREATE_DATE);
        String addedDateString   = jsonObject.optString(JSON_OBJECTIVE_ADD_DATE);
        String finishDateString  = jsonObject.optString(JSON_OBJECTIVE_FINISH_DATE);

        ArrayList<String> taskTags = new ArrayList<>();
        JSONArray tagsJSONArray = jsonObject.optJSONArray(JSON_OBJECTIVE_TAGS);
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

        LocalDateTime addedDate = null;
        try
        {
            addedDate = LocalDateTime.parse(addedDateString, dateTimeFormatter);
        }
        catch (DateTimeParseException e)
        {
            e.printStackTrace();
        }

        LocalDateTime finishDate = null;
        try
        {
            finishDate = LocalDateTime.parse(finishDateString, dateTimeFormatter);
        }
        catch (DateTimeParseException e)
        {
            e.printStackTrace();
        }

        if(!name.isEmpty() && createdDate != null && addedDate != null && finishDate != null)
        {
            return new ArchivedObjective(createdDate, addedDate, finishDate, name, description, taskTags);
        }
        else
        {
            return null; //Can't create even a basic objective
        }
    }

    @SuppressWarnings("unused")
    public LocalDateTime getCreationDate()
    {
        return mDateCreated;
    }

    @SuppressWarnings("unused")
    public LocalDateTime getEnlistmentDate()
    {
        return mDateEnlisted;
    }

    @SuppressWarnings("unused")
    public LocalDateTime getFinishDate()
    {
        return mDateFinished;
    }

    public String getName()
    {
        return mName;
    }

    public String getDescription()
    {
        return mDescription;
    }
}
