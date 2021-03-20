package com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain;

import org.json.JSONObject;

interface ObjectiveChainLoader
{
    ObjectiveChain fromJSON(JSONObject jsonObject);
}