package com.sixshaman.advancedunforgetter;

import java.util.ArrayList;
import java.util.Date;

class Task implements TaskSource
{
    //The date when the task was created and added to the task scheduler
    private Date mDateCreated;

    //The date when the task was added to the main task list
    private Date mDateAdded;

    //The date when the task was finished and added to the archive
    private Date mDateFinished;

    //The task name
    private String mName;

    //The task description
    private String mDescription;

    //Task tags (why not?)
    private ArrayList<String> mTags;

    //Creates a new unfinished, not added to the list task
    public Task(Date creationDate, String name, String description, ArrayList<String> tags)
    {
        mName        = name;
        mDescription = description;

        mTags = tags;

        mDateCreated  = creationDate;
        mDateAdded    = null;
        mDateFinished = null;
    }

    //Returns true if tag is in mTags, otherwise returns false
    boolean isTagMatched(String tag)
    {
        return mTags.contains(tag);
    }

    //Sets the list addition date
    void setAddedDate(Date listAddDate)
    {
        mDateAdded = listAddDate;
    }

    //Sets the finish date
    void setFinishedDate(Date finishDate)
    {
        mDateFinished = finishDate;
    }

    //Returns the date when the task was created
    Date getCreatedDate()
    {
        return mDateCreated;
    }

    //Returns the date when the task was added to the list
    Date getAddedDate()
    {
        return mDateAdded;
    }

    //Returns the date when the task was finished and moved to the archive
    Date getFinishedDate()
    {
        return mDateFinished;
    }

    //Returns the task name
    String getName()
    {
        return mName;
    }

    //Returns the task description
    String getDescription()
    {
        return mDescription;
    }

    //Returns the task tags
    ArrayList<String> getTags()
    {
        return mTags;
    }

    @Override
    public Task obtainTask()
    {
        return this;
    }
}
