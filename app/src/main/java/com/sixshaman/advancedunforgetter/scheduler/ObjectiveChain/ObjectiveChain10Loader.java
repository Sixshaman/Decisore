package com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain;

import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjective;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjective10Loader;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjectiveLatestLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ObjectiveChain10Loader implements ObjectiveChainJSONLoader
{
    @Override
    public ObjectiveChain fromJSON(JSONObject jsonObject)
    {
        try
        {
            String name        = jsonObject.optString("Name");
            String description = jsonObject.optString("Description");

            ObjectiveChain objectiveChain = new ObjectiveChain(name, description);

            JSONArray tasksJsonArray = jsonObject.getJSONArray("Tasks");
            if(tasksJsonArray != null)
            {
                ScheduledObjective10Loader objective10Loader = new ScheduledObjective10Loader();

                for(int i = 0; i < tasksJsonArray.length(); i++)
                {
                    JSONObject taskObject = tasksJsonArray.optJSONObject(i);
                    if(taskObject != null)
                    {
                        ScheduledObjective task = objective10Loader.fromJSON(taskObject);
                        if(task != null)
                        {
                            objectiveChain.addTaskToChain(task);
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