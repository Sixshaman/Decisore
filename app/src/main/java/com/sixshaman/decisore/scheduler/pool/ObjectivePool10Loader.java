package com.sixshaman.decisore.scheduler.pool;

import com.sixshaman.decisore.scheduler.chain.ObjectiveChain;
import com.sixshaman.decisore.scheduler.chain.ObjectiveChain10Loader;
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import com.sixshaman.decisore.scheduler.objective.SingleObjectiveSource10Loader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ObjectivePool10Loader implements ObjectivePoolLoader
{
    private final ObjectiveSchedulerCache mSchedulerCache;

    public ObjectivePool10Loader(ObjectiveSchedulerCache schedulerCache)
    {
        mSchedulerCache = schedulerCache;
    }

    @Override
    public ObjectivePool fromJSON(JSONObject jsonObject)
    {
        try
        {
            String name        = jsonObject.optString("Name");
            String description = jsonObject.optString("Description");

            String lastIdString = jsonObject.optString("LastId");
            Long lastId = null;
            try
            {
                lastId = Long.parseLong(lastIdString);
            }
            catch(NumberFormatException e)
            {
                e.printStackTrace();
            }

            if(lastId == null)
            {
                return null;
            }

            long poolId = mSchedulerCache.getLargestUsedId() + 1;
            ObjectivePool objectivePool = new ObjectivePool(poolId, name, description);

            String isActiveString = jsonObject.optString("IsActive");
            objectivePool.setPaused(!isActiveString.isEmpty() && isActiveString.equalsIgnoreCase("false"));

            JSONArray sourcesJsonArray = jsonObject.getJSONArray("Sources");
            for(int i = 0; i < sourcesJsonArray.length(); i++)
            {
                JSONObject sourceObject = sourcesJsonArray.optJSONObject(i);
                if(sourceObject != null)
                {
                    String     sourceType = sourceObject.optString("Type");
                    JSONObject sourceData = sourceObject.optJSONObject("Data");

                    if(sourceData != null)
                    {
                        if(sourceType.equals("SingleTaskSource"))
                        {
                            SingleObjectiveSource10Loader singleObjectiveSource10Loader = new SingleObjectiveSource10Loader();

                            ScheduledObjective objective = singleObjectiveSource10Loader.fromJSON(sourceData);
                            if(objective != null)
                            {
                                objectivePool.addObjectiveSource(objective);
                            }
                        }
                        else if(sourceType.equals("TaskChain"))
                        {
                            ObjectiveChain10Loader chain10Loader = new ObjectiveChain10Loader(mSchedulerCache);

                            ObjectiveChain objectiveChain = chain10Loader.fromJSON(sourceData);
                            if(objectiveChain != null)
                            {
                                objectivePool.addObjectiveSource(objectiveChain);
                            }
                        }
                    }
                }
            }

            return objectivePool;
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
