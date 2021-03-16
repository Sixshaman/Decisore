package com.sixshaman.advancedunforgetter.scheduler.ObjectivePool;

import com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain.ObjectiveChain;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain.ObjectiveChainLatestLoader;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjective;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjectiveLatestLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ObjectivePoolLatestLoader implements ObjectivePoolLoader
{
    public ObjectivePool fromJSON(JSONObject jsonObject)
    {
        try
        {
            long id = jsonObject.getLong("Id");

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

            ObjectivePool objectivePool = new ObjectivePool(id, name, description);

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
                            if(sourceType.equals("Chain"))
                            {
                                ObjectiveChainLatestLoader chainLatestLoader = new ObjectiveChainLatestLoader();

                                ObjectiveChain chain = chainLatestLoader.fromJSON(jsonObject);
                                if(chain != null)
                                {
                                    objectivePool.addObjectiveSource(chain);
                                }
                            }
                            else if(sourceType.equals("Objective"))
                            {
                                ScheduledObjectiveLatestLoader objectiveLatestLoader = new ScheduledObjectiveLatestLoader();

                                ScheduledObjective objective = objectiveLatestLoader.fromJSON(sourceData);
                                if(objective != null)
                                {
                                    objectivePool.addObjectiveSource(objective);
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
