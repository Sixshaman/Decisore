package com.sixshaman.advancedunforgetter.scheduler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;

interface SchedulerElement
{
    String getName();

    String getDescription();

    JSONObject toJSON();

    boolean isPaused();
}
