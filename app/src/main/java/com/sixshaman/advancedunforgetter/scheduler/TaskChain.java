package com.sixshaman.advancedunforgetter.scheduler;

import com.sixshaman.advancedunforgetter.utils.Task;
import java.time.LocalDateTime;
import java.util.ArrayDeque;

public class TaskChain implements TaskSource
{
    //Task chain name
    private String mName;

    //Task chain description
    private String mDescription;

    //The tasks that this chain will provide one-by-one. Since Java doesn't have any non-deque Queue implementation, we will use ArrayDeque
    private ArrayDeque<ScheduledTask> mTasks;

    //Creates a new task chain
    TaskChain(String name, String description)
    {
        mName        = name;
        mDescription = description;

        mTasks = new ArrayDeque<>();
    }

    //Adds a task to the chain
    void addTaskToChain(ScheduledTask task)
    {
        if(mTasks != null) //mTasks can be null if the chain is finished
        {
            mTasks.addLast(task);
        }
    }

    @Override
    public Task obtainTask(LocalDateTime referenceTime)
    {
        if(mTasks != null && !mTasks.isEmpty())
        {
            Task firstTask = mTasks.getFirst().getTask();
            if(referenceTime.isAfter(firstTask.getAddedDate()))
            {
                return mTasks.removeFirst().getTask();
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    @Override
    public SourceState getState(LocalDateTime referenceTime)
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
            ScheduledTask firstTask = mTasks.getFirst();
            if(firstTask.isActive() && referenceTime.isAfter(firstTask.getTask().getAddedDate())) //Also return EMPTY state if we can't provide the first task at this time
            {
                return SourceState.SOURCE_STATE_REGULAR;
            }
            else
            {
                return SourceState.SOURCE_STATE_EMPTY;
            }
        }
    }
}
