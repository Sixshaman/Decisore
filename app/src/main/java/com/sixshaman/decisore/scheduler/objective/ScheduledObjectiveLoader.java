package com.sixshaman.decisore.scheduler.objective;

import org.json.JSONObject;

public interface ScheduledObjectiveLoader
{
    @SuppressWarnings("unused")
    ScheduledObjective fromJSON(JSONObject jsonObject);
}
