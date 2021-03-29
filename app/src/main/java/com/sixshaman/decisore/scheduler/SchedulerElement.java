package com.sixshaman.decisore.scheduler;

import org.json.JSONObject;

public interface SchedulerElement
{
    String getName();

    @SuppressWarnings("unused")
    String getDescription();

    JSONObject toJSON();

    @SuppressWarnings("unused")
    boolean isPaused();

    String getElementName();
}
