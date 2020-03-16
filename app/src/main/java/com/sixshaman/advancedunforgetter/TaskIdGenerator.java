package com.sixshaman.advancedunforgetter;

class TaskIdGenerator
{
    private long mNextGivenId;

    //Constructs a new task id generator
    TaskIdGenerator()
    {
        mNextGivenId = 0;
    }

    long getNextId()
    {
        long currId = mNextGivenId;

        mNextGivenId = mNextGivenId + 1;
        return currId;
    }
}
