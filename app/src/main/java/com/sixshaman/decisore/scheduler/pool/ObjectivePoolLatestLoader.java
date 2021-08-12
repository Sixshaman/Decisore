package com.sixshaman.decisore.scheduler.pool;

import com.sixshaman.decisore.scheduler.chain.ObjectiveChain;
import com.sixshaman.decisore.scheduler.chain.ObjectiveChainLatestLoader;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjectiveLatestLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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

            String produceFrequencyString = jsonObject.optString("ProduceFrequency");
            String lastProducedDateString = jsonObject.optString("LastUpdate");

            String isAutoDeleteString  = jsonObject.optString("IsAutoDelete");
            String isUnstoppableString = jsonObject.optString("IsUnstoppable");

            ObjectivePool objectivePool = new ObjectivePool(id, name, description);
            objectivePool.setLastProvidedObjectiveId(lastId);

            String isActiveString = jsonObject.optString("IsActive");

            objectivePool.setPaused(!isActiveString.isEmpty()           && isActiveString.equalsIgnoreCase("false"));
            objectivePool.setAutoDelete(!isAutoDeleteString.isEmpty()   && isAutoDeleteString.equalsIgnoreCase("true"));
            objectivePool.setUnstoppable(!isUnstoppableString.isEmpty() && isUnstoppableString.equalsIgnoreCase("true"));

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

            if(!produceFrequencyString.isEmpty())
            {
                try
                {
                    long produceFrequencyMinutes = Long.parseLong(produceFrequencyString);
                    Duration produceFrequency = Duration.ofMinutes(produceFrequencyMinutes);

                    objectivePool.setProduceFrequency(produceFrequency);

                    if(!lastProducedDateString.isEmpty())
                    {
                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:nnnnnnnnn");
                        LocalDateTime lastProducedDate = LocalDateTime.parse(lastProducedDateString, dateTimeFormatter);

                        objectivePool.setLastUpdate(lastProducedDate);
                    }
                }
                catch(NumberFormatException | DateTimeParseException e)
                {
                    e.printStackTrace();
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
