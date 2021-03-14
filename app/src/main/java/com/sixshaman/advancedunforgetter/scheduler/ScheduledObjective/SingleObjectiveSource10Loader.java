package com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective;

import org.json.JSONObject;

//Old version's SingleObjectivePoolSource is equivalent to this version's SchedulerObjective
public class SingleObjectiveSource10Loader implements ScheduledObjectiveLoader
{
    public ScheduledObjective fromJSON(JSONObject jsonObject)
    {
        JSONObject taskJsonObject = jsonObject.optJSONObject("Task");
        if(taskJsonObject == null)
        {
            return null;
        }

        ScheduledObjective10Loader objective10Loader = new ScheduledObjective10Loader();
        return objective10Loader.fromJSON(jsonObject);
    }
}
