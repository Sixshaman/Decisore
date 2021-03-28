package com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain;

import com.sixshaman.advancedunforgetter.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjective;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjective10Loader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ObjectiveChain10Loader implements ObjectiveChainLoader
{
    private ObjectiveSchedulerCache mSchedulerCache;

    public ObjectiveChain10Loader(ObjectiveSchedulerCache schedulerCache)
    {
        mSchedulerCache = schedulerCache;
    }

    @Override
    public ObjectiveChain fromJSON(JSONObject jsonObject)
    {
        try
        {
            String name        = jsonObject.optString("Name");
            String description = jsonObject.optString("Description");

            long chainId = mSchedulerCache.getMaxChainId() + 1;
            ObjectiveChain objectiveChain = new ObjectiveChain(chainId, name, description);

            JSONArray objectivesJsonArray = jsonObject.getJSONArray("Tasks");
            if(objectivesJsonArray != null)
            {
                ScheduledObjective10Loader objective10Loader = new ScheduledObjective10Loader();

                for(int i = 0; i < objectivesJsonArray.length(); i++)
                {
                    JSONObject objectiveObject = objectivesJsonArray.optJSONObject(i);
                    if(objectiveObject != null)
                    {
                        ScheduledObjective objective = objective10Loader.fromJSON(objectiveObject);
                        if(objective != null)
                        {
                            objectiveChain.addObjectiveToChain(objective);
                        }
                    }
                }
            }

            JSONArray idHistoryArray = jsonObject.getJSONArray("TasksHistory");
            if(idHistoryArray != null)
            {
                for(int i = 0; i < idHistoryArray.length(); i++)
                {
                    long objectiveId = idHistoryArray.optLong(i, -1);
                    if(objectiveId != -1)
                    {
                        objectiveChain.mBoundObjectives.add(objectiveId);
                    }
                }
            }

            return objectiveChain;
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}