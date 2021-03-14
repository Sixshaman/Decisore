package com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain;

import org.json.JSONObject;

interface ObjectiveChainJSONLoader
{
    ObjectiveChain fromJSON(JSONObject jsonObject);
}