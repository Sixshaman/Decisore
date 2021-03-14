package com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective;

import org.json.JSONObject;

public interface ScheduledObjectiveLoader
{
    ScheduledObjective fromJSON(JSONObject jsonObject);
}
