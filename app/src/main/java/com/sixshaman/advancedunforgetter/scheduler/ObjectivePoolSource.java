package com.sixshaman.advancedunforgetter.scheduler;

import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import org.json.JSONObject;

import java.time.LocalDateTime;

interface ObjectivePoolSource
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

    //Gets the largest id for the tasks in the source
    long getMaxTaskId();

    //Returns true if the source contains the objective with given id
    boolean containedObjective(long objectiveId);

    //Put the objective back into the source
    boolean putBack(ScheduledObjective objective);

    //Serializes the task source into its JSON representation
    JSONObject toJSON();

    //Obtains a single task ready-to-be-added to the list
    EnlistedObjective obtainTask(LocalDateTime referenceTime);

    //Gets the task source state
    SourceState getState(LocalDateTime referenceTime);
}
