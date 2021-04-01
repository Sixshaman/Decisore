package com.sixshaman.decisore.scheduler.chain;

import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjectiveLatestLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ObjectiveChainLatestLoader implements ObjectiveChainLoader
{
    @Override
    public ObjectiveChain fromJSON(JSONObject jsonObject)
    {
        try
        {
            long id = jsonObject.getLong("Id");

            String name        = jsonObject.optString("Name");
            String description = jsonObject.optString("Description");

            String isActiveString = jsonObject.optString("IsActive");

            ObjectiveChain objectiveChain = new ObjectiveChain(id, name, description);

            JSONArray objectivesJsonArray = jsonObject.getJSONArray("Objectives");
            for(int i = 0; i < objectivesJsonArray.length(); i++)
            {
                JSONObject objectiveObject = objectivesJsonArray.optJSONObject(i);
                if(objectiveObject != null)
                {
                    ScheduledObjectiveLatestLoader objectiveLatestLoader = new ScheduledObjectiveLatestLoader();

                    ScheduledObjective objective = objectiveLatestLoader.fromJSON(objectiveObject);
                    if(objective != null)
                    {
                        objectiveChain.addObjectiveToChain(objective);
                    }
                }
            }

            JSONArray idHistoryArray = jsonObject.getJSONArray("ObjectiveHistory");
            for(int i = 0; i < idHistoryArray.length(); i++)
            {
                long objectiveId = idHistoryArray.optLong(i, -1);
                if(objectiveId != -1)
                {
                    objectiveChain.mBoundObjectives.add(objectiveId);
                }
            }

            objectiveChain.setPaused(!isActiveString.isEmpty() && isActiveString.equalsIgnoreCase("false"));

            return objectiveChain;
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}