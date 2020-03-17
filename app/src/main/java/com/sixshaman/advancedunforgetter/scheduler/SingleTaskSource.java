package com.sixshaman.advancedunforgetter.scheduler;

import com.sixshaman.advancedunforgetter.utils.Task;

import java.time.LocalDateTime;

//A task source that contains a single task
public class SingleTaskSource implements TaskSource
{
    //The task that this source can provide
    private ScheduledTask mTask;

    //The state of the source. After returning the task the source declares itself EMPTY if it's a repeated task or FINISHED if
    private SourceState mState;

    //Creates a task source from a task
    SingleTaskSource(ScheduledTask task)
    {
        mTask  = task;
        mState = SourceState.SOURCE_STATE_REGULAR;
    }

    @Override
    public Task obtainTask(LocalDateTime referenceTime)
    {
        if(getState(referenceTime) == SourceState.SOURCE_STATE_REGULAR)
        {
            //Becomes invalid if it's not a repeated task
            if(mTask.getRepeatProbability() == 0.0f) //Uhm... Since I always assign 0 directly, I probably don't have to consider precision error margin
            {
                mState = SourceState.SOURCE_STATE_FINISHED;
            }
            else
            {
                mTask.reschedule(referenceTime);
                mState = SourceState.SOURCE_STATE_EMPTY;
            }

            return mTask.getTask();
        }
        else
        {
            return null;
        }
    }

    @Override
    public SourceState getState(LocalDateTime referenceTime)
    {
        if(mState == SourceState.SOURCE_STATE_EMPTY)
        {
            //We need to update the task state to check if it's available again
            if(mTask.isActive() && referenceTime.isAfter(mTask.getTask().getAddedDate()))
            {
                return SourceState.SOURCE_STATE_REGULAR;
            }
            else
            {
                return SourceState.SOURCE_STATE_EMPTY;
            }
        }
        else
        {
            return mState;
        }
    }
}