package com.sixshaman.decisore.scheduler.chain;

import org.json.JSONObject;

interface ObjectiveChainLoader
{
    @SuppressWarnings("unused")
    ObjectiveChain fromJSON(JSONObject jsonObject);
}