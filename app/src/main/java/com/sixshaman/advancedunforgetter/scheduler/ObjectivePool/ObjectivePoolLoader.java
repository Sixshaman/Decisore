package com.sixshaman.advancedunforgetter.scheduler.ObjectivePool;

import org.json.JSONObject;

public interface ObjectivePoolLoader
{
    ObjectivePool fromJSON(JSONObject jsonObject);
}
