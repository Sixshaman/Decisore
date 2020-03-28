package com.sixshaman.advancedunforgetter.utils;

import android.support.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.ArrayList;

public class Task implements Comparable<Long>
{
    private static final String TAG = "Task";

    private static final String JSON_TASK_ID          = "Id";
    private static final String JSON_TASK_NAME        = "Name";
    private static final String JSON_TASK_DESCRIPTION = "Description";
    private static final String JSON_TASK_TAGS        = "Tags";
    private static final String JSON_TASK_CREATE_DATE = "DateCreated";
    private static final String JSON_TASK_ADD_DATE    = "DateAdded";
    private static final String JSON_TASK_FINISH_DATE = "DateFinished";
    private static final String JSON_TASK_CHARM       = "Charm";

    //The task ID
    private long mId;

    //The date when the task was created and added to the task scheduler
    private LocalDateTime mDateCreated;

    //The date when the task was added to the main task list
    private LocalDateTime mDateAdded;

    //The date when the task was finished and added to the archive
    private LocalDateTime mDateFinished;

    //The task name
    private String mName;

    //The task description
    private String mDescription;

    //Task tags (why not?)
    private ArrayList<String> mTags;

    //The task rating / how much the user likes the task. Only for sorting purposes
    private float mCharm;

    //Creates a new unfinished, not added to the list task
    public Task(long id, LocalDateTime creationDate, String name, String description, ArrayList<String> tags)
    {
        mId = id;

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
        mDateAdded    = null;
        mDateFinished = null;

        mCharm = 0.5f;
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
            String addedDateString    = dateTimeFormatter.format(mDateAdded);
            String createdDateString  = dateTimeFormatter.format(mDateCreated);
            String finishedDateString = dateTimeFormatter.format(mDateFinished);

            result.put(JSON_TASK_ADD_DATE,    addedDateString);
            result.put(JSON_TASK_CREATE_DATE, createdDateString);
            result.put(JSON_TASK_FINISH_DATE, finishedDateString);

            result.put(JSON_TASK_CHARM, Float.toString(mCharm));
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    //Creates a task from its JSON representation
    public static Task fromJSON(JSONObject jsonObject)
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:nnnnnnnnn");

        long id = jsonObject.optLong(JSON_TASK_ID, -1);

        String name        = jsonObject.optString(JSON_TASK_NAME);
        String description = jsonObject.optString(JSON_TASK_DESCRIPTION);

        String createdDateString = jsonObject.optString(JSON_TASK_CREATE_DATE);
        String addedDateString   = jsonObject.optString(JSON_TASK_ADD_DATE);
        String finishDateString  = jsonObject.optString(JSON_TASK_FINISH_DATE);

        float charm = (float)jsonObject.optDouble(JSON_TASK_CHARM, -1.0);

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

        LocalDateTime addedDate = null;
        try
        {
            addedDate = LocalDateTime.parse(addedDateString,   dateTimeFormatter);
        }
        catch (DateTimeParseException e)
        {
            e.printStackTrace();
        }

        LocalDateTime finishedDate = null;
        try
        {
            finishedDate = LocalDateTime.parse(finishDateString,  dateTimeFormatter);
        }
        catch (DateTimeParseException e)
        {
            e.printStackTrace();
        }

        Task task = null;
        if(id != -1 && !name.isEmpty() && createdDate != null)
        {
            task = new Task(id, createdDate, name, description, taskTags);
        }
        else
        {
            return null; //Can't create even a basic task
        }

        if(addedDate != null)
        {
            task.setAddedDate(addedDate);
        }

        if(finishedDate != null)
        {
            task.setFinishedDate(finishedDate);
        }

        if(charm >= 0.0f)
        {
            task.setCharm(charm);
        }

        return task;
    }

    //Returns true if tag is in mTags, otherwise returns false
    public boolean isTagMatched(String tag)
    {
        return mTags.contains(tag);
    }

    //Sets the list addition date
    public void setAddedDate(LocalDateTime listAddDate)
    {
        mDateAdded = listAddDate;
    }

    //Sets the finish date
    public void setFinishedDate(LocalDateTime finishDate)
    {
        mDateFinished = finishDate;
    }

    public void setCharm(float charm)
    {
        mCharm = charm;
    }

    //Returns the task id
    public long getId()
    {
        return mId;
    }

    //Returns the date when the task was created
    public LocalDateTime getCreatedDate()
    {
        return mDateCreated;
    }

    //Returns the date when the task was added to the list
    public LocalDateTime getAddedDate()
    {
        return mDateAdded;
    }

    //Returns the date when the task was finished and moved to the archive
    public LocalDateTime getFinishedDate()
    {
        return mDateFinished;
    }

    //Returns the task name
    public String getName()
    {
        return mName;
    }

    //Returns the task description
    public String getDescription()
    {
        return mDescription;
    }

    //Returns the task tags
    public ArrayList<String> getTags()
    {
        return mTags;
    }

    public float getCharm()
    {
        return mCharm;
    }

    @Override
    public int compareTo(@NonNull Long id)
    {
        return Long.compare(mId, id);
    }
}
