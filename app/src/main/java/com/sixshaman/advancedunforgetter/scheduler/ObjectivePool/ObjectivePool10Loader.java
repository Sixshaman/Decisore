package com.sixshaman.advancedunforgetter.scheduler.ObjectivePool;

import com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain.ObjectiveChain;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain.ObjectiveChain10Loader;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjective;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.SingleObjectiveSource10Loader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ObjectivePool10Loader implements ObjectivePoolLoader
{
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

            ObjectivePool objectivePool = new ObjectivePool(name, description);

            String isActiveString = jsonObject.optString("IsActive");

            boolean isActive = true;
            if(isActiveString != null && !isActiveString.isEmpty())
            {
                if(isActiveString.equalsIgnoreCase("false"))
                {
                    isActive = false;
                }
            }

            if(!isActive)
            {
                objectivePool.pause();
            }

            JSONArray sourcesJsonArray = jsonObject.getJSONArray("Sources");
            if(sourcesJsonArray != null)
            {
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
                                ObjectiveChain10Loader chain10Loader = new ObjectiveChain10Loader();

                                ObjectiveChain taskChain = chain10Loader.fromJSON(sourceData);
                                if(taskChain != null)
                                {
                                    objectivePool.addObjectiveSource(taskChain);
                                }
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
