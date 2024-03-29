package com.sixshaman.decisore.list;

import androidx.annotation.NonNull;
import com.sixshaman.decisore.archive.ArchivedObjective;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

public class EnlistedObjective implements Comparable<Long>
{
    private static final String JSON_OBJECTIVE_ID          = "Id";
    private static final String JSON_OBJECTIVE_PARENT_ID   = "ParentId";
    private static final String JSON_OBJECTIVE_NAME        = "Name";
    private static final String JSON_OBJECTIVE_DESCRIPTION = "Description";
    private static final String JSON_OBJECTIVE_TAGS        = "Tags";
    private static final String JSON_OBJECTIVE_CREATE_DATE = "DateCreated";
    private static final String JSON_OBJECTIVE_ADD_DATE    = "DateAdded";
    private static final String JSON_OBJECTIVE_CHARM       = "Charm";

    //The objective ID
    private final long mId;

    //The id of the parent chain/pool
    private final long mParentId;

    //The date when the objective was created and added to the objective scheduler
    private final LocalDateTime mDateCreated;

    //The date when the objective was added to the main objective list
    private final LocalDateTime mDateEnlisted;

    //The objective name
    private String mName;

    //The objective description
    private String mDescription;

    //Objective tags (why not?)
    private final ArrayList<String> mTags;

    //The objective rating / how much the user likes the objective. Only for sorting purposes
    private float mCharm;

    //Creates a new unfinished, not added to the list objective
    public EnlistedObjective(long id, long parentId, LocalDateTime creationDate, LocalDateTime addedDate, String name, String description, ArrayList<String> tags)
    {
        mId       = id;
        mParentId = parentId;

        mName        = name;
        mDescription = description;

        if(tags == null)
        {
            mTags = new ArrayList<>();
        }
        else
        {
            mTags = tags;
        }

        mDateCreated  = creationDate;
        mDateEnlisted = addedDate;

        mCharm = 0.5f;
    }

    //Serializes the objective into its JSON representation
    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();

        try
        {
            result.put(JSON_OBJECTIVE_ID,        Long.toString(mId));
            result.put(JSON_OBJECTIVE_PARENT_ID, Long.toString(mParentId));

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

            result.put(JSON_OBJECTIVE_CHARM, Float.toString(mCharm));
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    //Creates an objective from its JSON representation
    public static EnlistedObjective fromJSON(JSONObject jsonObject)
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:nnnnnnnnn");

        try
        {
            long id       = jsonObject.getLong(JSON_OBJECTIVE_ID);
            long parentId = jsonObject.getLong(JSON_OBJECTIVE_PARENT_ID);

            String name        = jsonObject.getString(JSON_OBJECTIVE_NAME);
            String description = jsonObject.getString(JSON_OBJECTIVE_DESCRIPTION);

            String createdDateString = jsonObject.getString(JSON_OBJECTIVE_CREATE_DATE);
            String addedDateString   = jsonObject.getString(JSON_OBJECTIVE_ADD_DATE);

            float charm = (float)jsonObject.optDouble(JSON_OBJECTIVE_CHARM, 0.5);

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

            if(id != -1 && !name.isEmpty() && createdDate != null && addedDate != null)
            {
                EnlistedObjective objective = new EnlistedObjective(id, parentId, createdDate, addedDate, name, description, taskTags);
                objective.setCharm(charm);
                return objective;
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return null; //Can't create even a basic objective
    }

    public ArchivedObjective toArchived(LocalDateTime finishDate)
    {
        return new ArchivedObjective(mDateCreated, mDateEnlisted, finishDate, mName, mDescription, mTags);
    }

    public ScheduledObjective toScheduled(LocalDateTime nextEnlistDate)
    {
        ScheduledObjective scheduledObjective = new ScheduledObjective(mId, mName, mDescription, mDateCreated, nextEnlistDate, mTags, Duration.ZERO, 0.0f);
        scheduledObjective.setParentId(getParentId());
        return scheduledObjective;
    }

    //Returns true if tag is in mTags, otherwise returns false
    @SuppressWarnings("unused")
    public boolean isTagMatched(String tag)
    {
        return mTags.contains(tag);
    }

    public void setCharm(float charm)
    {
        mCharm = charm;
    }

    //Returns the objective id
    public long getId()
    {
        return mId;
    }

    public long getParentId()
    {
        return mParentId;
    }

    //Returns the date when the objective was created
    @SuppressWarnings("unused")
    public LocalDateTime getCreatedDate()
    {
        return mDateCreated;
    }

    //Returns the date when the objective was added to the list
    public LocalDateTime getEnlistDate()
    {
        return mDateEnlisted;
    }

    //Returns the objective name
    public String getName()
    {
        return mName;
    }

    //Returns the objective description
    public String getDescription()
    {
        return mDescription;
    }

    //Returns the objective tags
    @SuppressWarnings("unused")
    public ArrayList<String> getTags()
    {
        return mTags;
    }

    @SuppressWarnings("unused")
    public float getCharm()
    {
        return mCharm;
    }

    //Sets the name
    public void setName(String name)
    {
        mName = name;
    }

    public void setDescription(String description)
    {
        mDescription = description;
    }

    @Override
    public int compareTo(@NonNull Long id)
    {
        return Long.compare(mId, id);
    }
}
