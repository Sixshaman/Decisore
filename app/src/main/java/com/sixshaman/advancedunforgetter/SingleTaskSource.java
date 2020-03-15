package com.sixshaman.advancedunforgetter;

//Contains a single
public class SingleTaskSource implements TaskSource
{
    //A task that this task source can give
    private ScheduledTask mTask;

    //The state of the source. After returning the task the source declares itself EMPTY if it's a repeated task or FINISHED if
    private SourceState mState;

    //Creates a task source from a task
    public SingleTaskSource(ScheduledTask task)
    {
        mTask  = task;
        mState = SourceState.SOURCE_STATE_REGULAR;
    }

    @Override
    public ScheduledTask obtainTask()
    {
        if(mState == SourceState.SOURCE_STATE_REGULAR)
        {
            //Becomes invalid if it's not a repeated task
            if(mTask.getRepeatProbability() == 0.0f) //Uhm... Since I always assign 0 directly, I probably don't have to concider precision error margin
            {
                mState = SourceState.SOURCE_STATE_FINISHED;
            }
            else
            {
                mState = SourceState.SOURCE_STATE_EMPTY;
            }

            return mTask;
        }
        else
        {
            return null;
        }
    }

    @Override
    public SourceState getState()
    {
        return mState;
    }
}
