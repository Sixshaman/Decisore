package com.sixshaman.decisore.scheduler.pool;

import com.sixshaman.decisore.scheduler.chain.ObjectiveChain;
import com.sixshaman.decisore.scheduler.chain.ObjectiveChainLatestLoader;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjectiveLatestLoader;
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
            objectivePool.setLastProvidedObjectiveId(lastId);

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
                        if(sourceType.equals("ObjectiveChain"))
                        {
                            ObjectiveChainLatestLoader chainLatestLoader = new ObjectiveChainLatestLoader();

                            ObjectiveChain chain = chainLatestLoader.fromJSON(sourceData);
                            if(chain != null)
                            {
                                objectivePool.addObjectiveSource(chain);
                            }
                        }
                        else if(sourceType.equals("ScheduledObjective"))
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

            return objectivePool;
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
