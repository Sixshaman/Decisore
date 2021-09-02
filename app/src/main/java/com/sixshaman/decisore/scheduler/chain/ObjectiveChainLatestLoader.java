package com.sixshaman.decisore.scheduler.chain;

import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjectiveLatestLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ObjectiveChainLatestLoader implements ObjectiveChainLoader
{
    @Override
    public ObjectiveChain fromJSON(JSONObject jsonObject)
    {
        try
        {
            long id       = jsonObject.getLong("Id");
            long parentId = jsonObject.optLong("ParentId", -1);

            String name        = jsonObject.optString("Name");
            String description = jsonObject.optString("Description");

            String isActiveString = jsonObject.optString("IsActive");

            String isAutoDeleteString  = jsonObject.optString("IsAutoDelete");
            String isUnstoppableString = jsonObject.optString("IsUnstoppable");

            String produceFrequencyString = jsonObject.optString("ProduceFrequency");
            String lastProducedDateString = jsonObject.optString("LastUpdate");

            int instantCount = jsonObject.optInt("InstantCount", 0);

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

            objectiveChain.setPaused(!isActiveString.isEmpty()           && isActiveString.equalsIgnoreCase("false"));
            objectiveChain.setAutoDelete(!isAutoDeleteString.isEmpty()   && isAutoDeleteString.equalsIgnoreCase("true"));
            objectiveChain.setUnstoppable(!isUnstoppableString.isEmpty() && isUnstoppableString.equalsIgnoreCase("true"));

            if(!produceFrequencyString.isEmpty())
            {
                try
                {
                    long produceFrequencyMinutes = Long.parseLong(produceFrequencyString);
                    Duration produceFrequency = Duration.ofMinutes(produceFrequencyMinutes);

                    objectiveChain.setProduceFrequency(produceFrequency);

                    if(!lastProducedDateString.isEmpty())
                    {
                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:nnnnnnnnn");
                        LocalDateTime lastProducedDate = LocalDateTime.parse(lastProducedDateString, dateTimeFormatter);

                        objectiveChain.setLastUpdate(lastProducedDate);
                    }

                    objectiveChain.setInstantCount(instantCount);
                }
                catch(NumberFormatException | DateTimeParseException e)
                {
                    e.printStackTrace();
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