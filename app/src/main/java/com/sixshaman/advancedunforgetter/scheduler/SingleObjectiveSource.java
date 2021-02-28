package com.sixshaman.advancedunforgetter.scheduler;

import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;

//A task source that contains a single task
public class SingleObjectiveSource implements ObjectiveSource
{
    //The task that this source can provide
    private ScheduledObjective mObjective;

    //After returning the task the source declares itself finished if the task is not repeatable
    private boolean mIsFinished;

    //Creates a task source from a task
    SingleObjectiveSource(ScheduledObjective task)
    {
        mObjective = task;
        mIsFinished = false;
    }

    @Override
    public JSONObject toJSON()
    {
        //Never save finished task sources
        if(mIsFinished)
        {
            return null;
        }
        else
        {
            JSONObject result = new JSONObject();

            try
            {
                JSONObject taskJsonObject = mObjective.toJSON();
                result.put("Task", taskJsonObject);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }

            return result;
        }
    }

    public static SingleObjectiveSource fromJSON(JSONObject object)
    {
        JSONObject taskJsonObject = object.optJSONObject("Task");
        if(taskJsonObject == null)
        {
            return null;
        }

        ScheduledObjective task = ScheduledObjective.fromJSON(taskJsonObject);
        if(task == null)
        {
            return null;
        }

        return new SingleObjectiveSource(task);
    }

    @Override
    public long getMaxTaskId()
    {
        if(mIsFinished)
        {
            return -1;
        }

        return mObjective.getId();
    }

    @Override
    public boolean containedObjective(long objectiveId)
    {
        return mObjective.getId() == objectiveId;
    }

    @Override
    public boolean putBack(ScheduledObjective objective)
    {
        if(objective.getId() == mObjective.getId())
        {
            mObjective.rescheduleUnregulated(objective.getScheduledEnlistDate());
            return true;
        }

        return false;
    }

    public static String getSourceTypeString()
    {
        return "SingleTaskSource";
    }

    @Override
    public EnlistedObjective obtainTask(LocalDateTime referenceTime)
    {
        if(getState(referenceTime) == SourceState.SOURCE_STATE_REGULAR)
        {
            //Becomes invalid if it's not a repeated task
            if(mObjective.getRepeatProbability() < 0.0001f)
            {
                mIsFinished = true;
            }
            else
            {
                mObjective.reschedule(referenceTime);
            }

            return mObjective.toEnlisted(referenceTime);
        }
        else
        {
            return null;
        }
    }

    @Override
    public SourceState getState(LocalDateTime referenceTime)
    {
        if(mIsFinished)
        {
            return SourceState.SOURCE_STATE_FINISHED;
        }

        //We need to check if the task is currently available
        if(mObjective.isActive() && referenceTime.isAfter(mObjective.getScheduledEnlistDate()))
        {
            return SourceState.SOURCE_STATE_REGULAR;
        }
        else
        {
            return SourceState.SOURCE_STATE_EMPTY;
        }
    }
}
