package com.sixshaman.advancedunforgetter.utils;

public class TaskIdGenerator
{
    private long mNextGivenId;

    //Constructs a new task id generator
    public TaskIdGenerator()
    {
        mNextGivenId = 0;
    }

    public long getNextId()
    {
        long currId = mNextGivenId;

        mNextGivenId = mNextGivenId + 1;
        return currId;
    }
}
