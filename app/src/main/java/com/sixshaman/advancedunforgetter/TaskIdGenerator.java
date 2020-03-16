package com.sixshaman.advancedunforgetter;

public class TaskIdGenerator
{
    private long mNextGivenId;

    //Constructs a new task id generator
    public TaskIdGenerator()
    {

    }

    public long getNextId()
    {
        long currId = mNextGivenId;

        mNextGivenId = mNextGivenId + 1;
        return currId;
    }
}
