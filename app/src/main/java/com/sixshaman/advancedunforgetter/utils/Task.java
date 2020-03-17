package com.sixshaman.advancedunforgetter.utils;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class Task
{
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

        mTags = tags;

        mDateCreated  = creationDate;
        mDateAdded    = null;
        mDateFinished = null;

        mCharm = 0.5f;
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
}
