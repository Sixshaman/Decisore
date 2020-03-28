package com.sixshaman.advancedunforgetter.utils;

public class TaskIdGenerator
{
    private long mNextGivenId;

    //Constructs a new task id generator
    public TaskIdGenerator()
    {
        mNextGivenId = 0;
    }

    public void setFirstId(long id)
    {
        mNextGivenId = id + 1;
    }

    public long generateNextId()
    {
        long currId = mNextGivenId;

        mNextGivenId = mNextGivenId + 1;
        return currId;
    }
}
