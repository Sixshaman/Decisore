package com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain;

import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjective;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjectiveLatestLoader;
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

            ObjectiveChain objectiveChain = new ObjectiveChain(id, name, description);

            JSONArray objectivesJsonArray = jsonObject.getJSONArray("Objectives");
            if(objectivesJsonArray != null)
            {
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
            }

            JSONArray idHistoryArray = jsonObject.getJSONArray("ObjectiveHistory");
            if(idHistoryArray != null)
            {
                for(int i = 0; i < idHistoryArray.length(); i++)
                {
                    long objectiveId = idHistoryArray.optLong(i, -1);
                    if(objectiveId != -1)
                    {
                        objectiveChain.mObjectiveIdHistory.add(objectiveId);
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