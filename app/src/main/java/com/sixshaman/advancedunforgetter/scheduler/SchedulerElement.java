package com.sixshaman.advancedunforgetter.scheduler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;

public interface SchedulerElement
{
    String getName();

    String getDescription();

    JSONObject toJSON();

    boolean isPaused();

    String getElementName();
}
