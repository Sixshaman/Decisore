package com.sixshaman.advancedunforgetter;

import java.util.ArrayDeque;

public class TaskChain implements TaskSource
{
    //The tasks that this chain will provide one-by-one. Since Java doesn't have any non-deque Queue implementation, we will use ArrayDeque
    private ArrayDeque<ScheduledTask> mTasks;

    //Creates a new task chain
    TaskChain()
    {
        mTasks = new ArrayDeque<ScheduledTask>();
    }

    //Adds a task to the chain
    void addTaskToChain(ScheduledTask task)
    {
        if(mTasks != null) //mTasks can be null if the chain is finished
        {
            mTasks.add(task);
        }
    }

    @Override
    public ScheduledTask obtainTask()
    {
        if(mTasks != null)
        {
            return mTasks.remove();
        }
        else
        {
            return null;
        }
    }

    /*

    Scheduler update happens:
    - After opening the application
    = Every hour on schedule. The user can't change the period.

    If during the update the scheduler finds a task that has list add date greater or equal the current date, it adds it to the list and removes from the scheduler.

    After removing the task from the scheduler:
    - If it's a one-time task (repeat probability is 0), nothing else is needed.
    - If it's a strictly periodic task (repeat probability is 1), the new task is added to the scheduler. It has the same creation date, but the list add date is the current one + period.
    - If it's not a strictly periodic task (0 < repeat probability < 1), then ULTRARANDOM ALGORITHM decides the next list add date and a new task with it is added to the scheduler.
    
    */

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
