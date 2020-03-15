package com.sixshaman.advancedunforgetter;

interface TaskSource
{
    enum SourceState
    {
        //Declares that this source is valid and can give a task
        SOURCE_STATE_REGULAR,

        //Declares that this source is temporary empty, but will probably be able to give tasks later
        SOURCE_STATE_EMPTY,

        //Declares that this source is no longer valid and won't give any more tasks
        SOURCE_STATE_FINISHED
    }

    //Obtains a single task
    ScheduledTask obtainTask();

    //Gets the task source state
    SourceState getState();
}
