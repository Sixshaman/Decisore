package com.sixshaman.advancedunforgetter;

import java.util.ArrayDeque;

public class TaskChain implements TaskSource
{
    //The tasks that this chain will provide one-by-one. Since Java doesn't have any non-deque Queue implementation, we will use ArrayDeque
    private ArrayDeque<ScheduledTask> mTasks;

    //The id of the task that was most recently provided by this chain.
    private long mLastProvidedTaskId;

    //Creates a new task chain
    TaskChain()
    {
        mTasks              = new ArrayDeque<ScheduledTask>();
        mLastProvidedTaskId = 0;
    }

    //Adds a task to the chain
    public void addTaskToChain(ScheduledTask task)
    {
        if(mTasks != null) //mTasks can be null if the chain is finished
        {
            mTasks.addLast(task);
        }
    }

    public long getLastProvidedTaskId()
    {
        return mLastProvidedTaskId;
    }

    @Override
    public ScheduledTask obtainTask()
    {
        if(mTasks != null)
        {
            mLastProvidedTaskId = mTasks.getFirst().getTask().getId();
            return mTasks.removeFirst();
        }
        else
        {
            return null;
        }
    }

    @Override
    public SourceState getState()
    {
        if(mTasks == null) //The chain is finished and cannot be a task source anymore
        {
            return SourceState.SOURCE_STATE_FINISHED;
        }
        else if(mTasks.isEmpty()) //The chain is valid but cannot provide task at this moment
        {
            return SourceState.SOURCE_STATE_EMPTY;
        }
        else
        {
            return SourceState.SOURCE_STATE_REGULAR;
        }
    }
}
