package com.sixshaman.advancedunforgetter.scheduler;

import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;

//A task source that contains a single task
public class SingleTaskSource implements TaskSource
{
    //The task that this source can provide
    private ScheduledObjective mTask;

    //After returning the task the source declares itself finished if the task is not repeatable
    private boolean mIsFinished;

    //Creates a task source from a task
    SingleTaskSource(ScheduledObjective task)
    {
        mTask       = task;
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
                JSONObject taskJsonObject = mTask.toJSON();
                result.put("Task", taskJsonObject);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }

            return result;
        }
    }

    public static SingleTaskSource fromJSON(JSONObject object)
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

        return new SingleTaskSource(task);
    }

    @Override
    public long getMaxTaskId()
    {
        if(mIsFinished)
        {
            return -1;
        }

        return mTask.getId();
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
            if(mTask.getRepeatProbability() < 0.0001f)
            {
                mIsFinished = true;
            }
            else
            {
                mTask.reschedule(referenceTime);
            }

            return mTask.toEnlisted(referenceTime);
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
        if(mTask.isActive() && referenceTime.isAfter(mTask.getScheduledEnlistDate()))
        {
            return SourceState.SOURCE_STATE_REGULAR;
        }
        else
        {
            return SourceState.SOURCE_STATE_EMPTY;
        }
    }
}
