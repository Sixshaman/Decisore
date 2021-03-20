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
            String name        = jsonObject.optString("Name");
            String description = jsonObject.optString("Description");

            ObjectiveChain objectiveChain = new ObjectiveChain(name, description);

            JSONArray tasksJsonArray = jsonObject.getJSONArray("Objectives");
            if(tasksJsonArray != null)
            {
                for(int i = 0; i < tasksJsonArray.length(); i++)
                {
                    JSONObject taskObject = tasksJsonArray.optJSONObject(i);
                    if(taskObject != null)
                    {
                        ScheduledObjectiveLatestLoader objectiveLatestLoader = new ScheduledObjectiveLatestLoader();

                        ScheduledObjective objective = objectiveLatestLoader.fromJSON(taskObject);
                        if(objective != null)
                        {
                            objectiveChain.addTaskToChain(objective);
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