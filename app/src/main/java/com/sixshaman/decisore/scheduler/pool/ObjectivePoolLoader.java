package com.sixshaman.decisore.scheduler.pool;

import org.json.JSONObject;

public interface ObjectivePoolLoader
{
    @SuppressWarnings("unused")
    ObjectivePool fromJSON(JSONObject jsonObject);
}
