package com.sixshaman.decisore.scheduler;

import com.sixshaman.decisore.list.ObjectiveListCache;
import com.sixshaman.decisore.scheduler.chain.ObjectiveChain;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import com.sixshaman.decisore.utils.LockedReadFile;
import com.sixshaman.decisore.utils.LockedWriteFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

//Recalculates id based on the fact that chains, pools, and objectives now share a common id s[ace
//Assigns parent id to each objective
public class Scheduler11To12Converter
{
    private JSONObject mShedulerJsonObject;
    private final String mSchedulerFilePath;

    public Scheduler11To12Converter(String schedulerFilePath)
    {
        mSchedulerFilePath = schedulerFilePath;

        try
        {
            LockedReadFile schedulerFile = new LockedReadFile(mSchedulerFilePath);

            String fileContents = schedulerFile.read();
            mShedulerJsonObject = new JSONObject(fileContents);

            schedulerFile.close();
        }
        catch(IOException | JSONException e)
        {
            e.printStackTrace();
        }
    }

    public ArrayList<Long> gatherObjectiveIds()
    {
        ArrayList<Long> objectiveIds = new ArrayList<>();
        try
        {
            JSONArray elementsArray = mShedulerJsonObject.getJSONArray("ELEMENTS");
            for(int i = 0; i < elementsArray.length(); i++)
            {
                JSONObject elementObject = elementsArray.getJSONObject(i);

                String elementType = elementObject.getString("Type");
                switch (elementType)
                {
                    case "ScheduledObjective":
                    {
                        JSONObject objectiveJson = elementObject.getJSONObject("Data");
                        long id = objectiveJson.getLong("Id");
                        objectiveIds.add(id);
                        break;
                    }
                    case "ObjectiveChain":
                    {
                        JSONObject chainJson = elementObject.getJSONObject("Data");

                        JSONArray objectivesJsonArray = chainJson.getJSONArray("Objectives");
                        for (int j = 0; j < objectivesJsonArray.length(); j++)
                        {
                            JSONObject objectiveJson = objectivesJsonArray.getJSONObject(j);
                            long id = objectiveJson.getLong("Id");
                            objectiveIds.add(id);
                        }

                        JSONArray idHistoryArray = chainJson.getJSONArray("ObjectiveHistory");
                        for (int j = 0; j < idHistoryArray.length(); j++)
                        {
                            long objectiveId = idHistoryArray.getLong(j);
                            if (objectiveId != -1)
                            {
                                objectiveIds.add(objectiveId);
                            }
                        }

                        break;
                    }
                    case "ObjectivePool":
                    {
                        JSONObject poolJson = elementObject.getJSONObject("Data");

                        JSONArray sourcesJsonArray = poolJson.getJSONArray("Sources");
                        for (int j = 0; j < sourcesJsonArray.length(); j++)
                        {
                            JSONObject sourceObject = sourcesJsonArray.getJSONObject(j);

                            String sourceType = sourceObject.getString("Type");
                            if(sourceType.equals("ScheduledObjective"))
                            {
                                JSONObject sourceData = sourceObject.getJSONObject("Data");
                                long id = sourceData.getLong("Id");
                                objectiveIds.add(id);
                            }
                            else if (sourceType.equals("ObjectiveChain"))
                            {
                                JSONObject chainJson = sourceObject.getJSONObject("Data");

                                JSONArray objectivesJsonArray = chainJson.getJSONArray("Objectives");
                                for(int k = 0; k < objectivesJsonArray.length(); k++)
                                {
                                    JSONObject objectiveObject = objectivesJsonArray.getJSONObject(k);
                                    long id = objectiveObject.getLong("Id");
                                    objectiveIds.add(id);
                                }

                                JSONArray idHistoryArray = chainJson.getJSONArray("ObjectiveHistory");
                                for(int k = 0; k < idHistoryArray.length(); k++)
                                {
                                    long objectiveId = idHistoryArray.getLong(k);
                                    if(objectiveId != -1)
                                    {
                                        objectiveIds.add(objectiveId);
                                    }
                                }
                            }
                        }

                        break;
                    }
                }
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        Collections.sort(objectiveIds);
        return objectiveIds;
    }

    public ArrayList<Long> gatherChainIds()
    {
        ArrayList<Long> chainIds = new ArrayList<>();
        try
        {
            JSONArray elementsArray = mShedulerJsonObject.getJSONArray("ELEMENTS");
            for(int i = 0; i < elementsArray.length(); i++)
            {
                JSONObject elementObject = elementsArray.getJSONObject(i);

                String elementType = elementObject.getString("Type");
                if(elementType.equals("ObjectiveChain"))
                {
                    JSONObject chainJson = elementObject.getJSONObject("Data");
                    long id = chainJson.getLong("Id");
                    chainIds.add(id);
                }
                else if(elementType.equals("ObjectivePool"))
                {
                    JSONObject poolJson = elementObject.getJSONObject("Data");

                    JSONArray sourcesJsonArray = poolJson.getJSONArray("Sources");
                    for(int j = 0; j < sourcesJsonArray.length(); j++)
                    {
                        JSONObject sourceObject = sourcesJsonArray.getJSONObject(j);

                        String sourceType = sourceObject.getString("Type");
                        if(sourceType.equals("ObjectiveChain"))
                        {
                            JSONObject sourceData = sourceObject.getJSONObject("Data");
                            long id = sourceData.getLong("Id");
                            chainIds.add(id);
                        }
                    }
                }
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        Collections.sort(chainIds);
        return chainIds;
    }

    public ArrayList<Long> gatherPoolIds()
    {
        ArrayList<Long> poolIds = new ArrayList<>();
        try
        {
            JSONArray elementsArray = mShedulerJsonObject.getJSONArray("ELEMENTS");
            for(int i = 0; i < elementsArray.length(); i++)
            {
                JSONObject elementObject = elementsArray.getJSONObject(i);

                String elementType = elementObject.getString("Type");
                if(elementType.equals("ObjectivePool"))
                {
                    JSONObject poolJson = elementObject.getJSONObject("Data");
                    long id = poolJson.getLong("Id");
                    poolIds.add(id);
                }
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        Collections.sort(poolIds);
        return poolIds;
    }

    public HashMap<Long, Long> gatherParentIds()
    {
        HashMap<Long, Long> parentIdMap = new HashMap<>();

        try
        {
            JSONArray elementsArray = mShedulerJsonObject.getJSONArray("ELEMENTS");
            for(int i = 0; i < elementsArray.length(); i++)
            {
                JSONObject elementObject = elementsArray.getJSONObject(i);

                String elementType = elementObject.getString("Type");
                switch (elementType)
                {
                    case "ScheduledObjective":
                    {
                        JSONObject objectiveJson = elementObject.getJSONObject("Data");
                        long objectiveId = objectiveJson.getLong("Id");
                        parentIdMap.put(objectiveId, (long)-1);
                        break;
                    }
                    case "ObjectiveChain":
                    {
                        JSONObject chainJson = elementObject.getJSONObject("Data");
                        long chainId = chainJson.getLong("Id");
                        parentIdMap.put(chainId, (long)-1);

                        JSONArray objectivesJsonArray = chainJson.getJSONArray("Objectives");
                        for (int j = 0; j < objectivesJsonArray.length(); j++)
                        {
                            JSONObject objectiveJson = objectivesJsonArray.getJSONObject(j);
                            long objectiveId = objectiveJson.getLong("Id");
                            parentIdMap.put(objectiveId, chainId);
                        }

                        JSONArray idHistoryArray = chainJson.getJSONArray("ObjectiveHistory");
                        for (int j = 0; j < idHistoryArray.length(); j++)
                        {
                            long objectiveId = idHistoryArray.getLong(j);
                            if(objectiveId != -1)
                            {
                                parentIdMap.put(objectiveId, chainId);
                            }
                        }

                        break;
                    }
                    case "ObjectivePool":
                    {
                        JSONObject poolJson = elementObject.getJSONObject("Data");
                        long poolId = poolJson.getLong("Id");
                        parentIdMap.put(poolId, (long)-1);

                        JSONArray sourcesJsonArray = poolJson.getJSONArray("Sources");
                        for (int j = 0; j < sourcesJsonArray.length(); j++)
                        {
                            JSONObject sourceObject = sourcesJsonArray.getJSONObject(j);

                            String sourceType = sourceObject.getString("Type");
                            if(sourceType.equals("ScheduledObjective"))
                            {
                                JSONObject sourceData = sourceObject.getJSONObject("Data");
                                long objectiveId = sourceData.getLong("Id");
                                parentIdMap.put(objectiveId, poolId);
                            }
                            else if (sourceType.equals("ObjectiveChain"))
                            {
                                JSONObject chainJson = sourceObject.getJSONObject("Data");
                                long chainId = chainJson.getLong("Id");
                                parentIdMap.put(chainId, poolId);

                                JSONArray objectivesJsonArray = chainJson.getJSONArray("Objectives");
                                for(int k = 0; k < objectivesJsonArray.length(); k++)
                                {
                                    JSONObject objectiveObject = objectivesJsonArray.getJSONObject(k);
                                    long objectiveId = objectiveObject.getLong("Id");
                                    parentIdMap.put(objectiveId, chainId);
                                }

                                JSONArray idHistoryArray = chainJson.getJSONArray("ObjectiveHistory");
                                for(int k = 0; k < idHistoryArray.length(); k++)
                                {
                                    long objectiveId = idHistoryArray.getLong(k);
                                    if(objectiveId != -1)
                                    {
                                        parentIdMap.put(objectiveId, chainId);
                                    }
                                }
                            }
                        }

                        break;
                    }
                }
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        return parentIdMap;
    }

    @SuppressWarnings("ConstantConditions")
    public boolean patchIds(HashMap<Long, Long> newObjectiveIdMap, HashMap<Long, Long> newChainIdMap, HashMap<Long, Long> newPoolIdMap)
    {
        try
        {
            mShedulerJsonObject.put("VERSION", ObjectiveSchedulerCache.SCHEDULER_VERSION_1_2);

            JSONArray elementsArray = mShedulerJsonObject.getJSONArray("ELEMENTS");
            for(int i = 0; i < elementsArray.length(); i++)
            {
                JSONObject elementObject = elementsArray.getJSONObject(i);

                String     elementType = elementObject.getString("Type");
                JSONObject elementData = elementObject.getJSONObject("Data");
                switch(elementType)
                {
                    case "ScheduledObjective":
                    {
                        long oldObjectiveId = elementData.getLong("Id");
                        long newObjectiveId = newObjectiveIdMap.get(oldObjectiveId);

                        elementData.put("Id", newObjectiveId);
                        break;
                    }
                    case "ObjectiveChain":
                    {
                        long oldChainId = elementData.getLong("Id");
                        long newChainId = newChainIdMap.get(oldChainId);
                        elementData.put("Id", newChainId);

                        JSONArray chainObjectives = elementData.getJSONArray("Objectives");
                        for (int j = 0; j < chainObjectives.length(); j++)
                        {
                            JSONObject objectiveJson = chainObjectives.getJSONObject(j);

                            long oldObjectiveId = objectiveJson.getLong("Id");
                            long newObjectiveId = newObjectiveIdMap.get(oldObjectiveId);

                            objectiveJson.put("Id", newObjectiveId);
                            chainObjectives.put(j, objectiveJson);
                        }

                        JSONArray chainHistory = elementData.getJSONArray("ObjectiveHistory");
                        for(int j = 0; j < chainHistory.length(); j++)
                        {
                            long oldObjectiveId = chainHistory.getLong(j);
                            if(oldObjectiveId != -1)
                            {
                                long newObjectiveId = newObjectiveIdMap.get(oldObjectiveId);
                                chainHistory.put(j, newObjectiveId);
                            }
                        }

                        elementData.put("Objectives", chainObjectives);
                        elementData.put("ObjectiveHistory", chainHistory);

                        break;
                    }
                    case "ObjectivePool":
                    {
                        long oldPoolId = elementData.getLong("Id");
                        long newPoolId = newPoolIdMap.get(oldPoolId);
                        elementData.put("Id", newPoolId);

                        JSONArray poolSources = elementData.getJSONArray("Sources");
                        for(int j = 0; j < poolSources.length(); j++)
                        {
                            JSONObject sourceJson = poolSources.getJSONObject(j);

                            String     sourceType = sourceJson.getString("Type");
                            JSONObject sourceData = sourceJson.getJSONObject("Data");

                            if(sourceType.equals("ScheduledObjective"))
                            {
                                long oldObjectiveId = sourceData.getLong("Id");
                                long newObjectiveId = newObjectiveIdMap.get(oldObjectiveId);
                                sourceData.put("Id", newObjectiveId);
                            }
                            else if(sourceType.equals("ObjectiveChain"))
                            {
                                long oldChainId = sourceData.getLong("Id");
                                long newChainId = newChainIdMap.get(oldChainId);
                                sourceData.put("Id", newChainId);

                                JSONArray chainObjectives = sourceData.getJSONArray("Objectives");
                                for (int k = 0; k < chainObjectives.length(); k++)
                                {
                                    JSONObject objectiveJson = chainObjectives.getJSONObject(k);

                                    long oldObjectiveId = objectiveJson.getLong("Id");
                                    long newObjectiveId = newObjectiveIdMap.get(oldObjectiveId);
                                    objectiveJson.put("Id", newObjectiveId);

                                    chainObjectives.put(k, objectiveJson);
                                }

                                JSONArray chainHistory = sourceData.getJSONArray("ObjectiveHistory");
                                for(int k = 0; k < chainHistory.length(); k++)
                                {
                                    long oldObjectiveId = chainHistory.getLong(k);
                                    if(oldObjectiveId != -1)
                                    {
                                        long newObjectiveId = newObjectiveIdMap.get(oldObjectiveId);
                                        chainHistory.put(k, newObjectiveId);
                                    }
                                }

                                sourceData.put("Objectives", chainObjectives);
                                sourceData.put("ObjectiveHistory", chainHistory);
                            }

                            sourceJson.put("Data", sourceData);
                            poolSources.put(j, sourceJson);
                        }

                        elementData.put("Sources", poolSources);

                        break;
                    }
                }

                elementObject.put("Data", elementData);
                elementsArray.put(i, elementObject);
            }

            mShedulerJsonObject.put("ELEMENTS", elementsArray);
        }
        catch(JSONException | NullPointerException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @SuppressWarnings("ConstantConditions")
    public boolean patchParentIds(HashMap<Long, Long> parentIdMap)
    {
        try
        {
            JSONArray elementsArray = mShedulerJsonObject.getJSONArray("ELEMENTS");
            for(int i = 0; i < elementsArray.length(); i++)
            {
                JSONObject elementObject = elementsArray.getJSONObject(i);

                String     elementType = elementObject.getString("Type");
                JSONObject elementData = elementObject.getJSONObject("Data");
                switch(elementType)
                {
                    case "ScheduledObjective":
                    {
                        long objectiveId       = elementData.getLong("Id");
                        long objectiveParentId = parentIdMap.get(objectiveId);

                        elementData.put("ParentId", objectiveParentId);
                        break;
                    }
                    case "ObjectiveChain":
                    {
                        long chainId       = elementData.getLong("Id");
                        long chainParentId = parentIdMap.get(chainId);
                        elementData.put("ParentId", chainParentId);

                        JSONArray chainObjectives = elementData.getJSONArray("Objectives");
                        for (int j = 0; j < chainObjectives.length(); j++)
                        {
                            JSONObject objectiveJson = chainObjectives.getJSONObject(j);

                            long objectiveId       = objectiveJson.getLong("Id");
                            long objectiveParentId = parentIdMap.get(objectiveId);

                            objectiveJson.put("ParentId", objectiveParentId);
                            chainObjectives.put(j, objectiveJson);
                        }

                        elementData.put("Objectives", chainObjectives);

                        break;
                    }
                    case "ObjectivePool":
                    {
                        long poolId       = elementData.getLong("Id");
                        long poolParentId = parentIdMap.get(poolId);
                        elementData.put("ParentId", poolParentId);

                        JSONArray poolSources = elementData.getJSONArray("Sources");
                        for(int j = 0; j < poolSources.length(); j++)
                        {
                            JSONObject sourceJson = poolSources.getJSONObject(j);

                            String     sourceType = sourceJson.getString("Type");
                            JSONObject sourceData = sourceJson.getJSONObject("Data");

                            if(sourceType.equals("ScheduledObjective"))
                            {
                                long objectiveId       = sourceData.getLong("Id");
                                long objectiveParentId = parentIdMap.get(objectiveId);
                                sourceData.put("ParentId", objectiveParentId);
                            }
                            else if(sourceType.equals("ObjectiveChain"))
                            {
                                long chainId       = sourceData.getLong("Id");
                                long chainParentId = parentIdMap.get(chainId);
                                sourceData.put("ParentId", chainParentId);

                                JSONArray chainObjectives = sourceData.getJSONArray("Objectives");
                                for (int k = 0; k < chainObjectives.length(); k++)
                                {
                                    JSONObject objectiveJson = chainObjectives.getJSONObject(k);

                                    long objectiveId       = objectiveJson.getLong("Id");
                                    long objectiveParentId = parentIdMap.get(objectiveId);
                                    objectiveJson.put("ParentId", objectiveParentId);

                                    chainObjectives.put(k, objectiveJson);
                                }

                                sourceData.put("Objectives", chainObjectives);
                            }

                            sourceJson.put("Data", sourceData);
                            poolSources.put(j, sourceJson);
                        }

                        elementData.put("Sources", poolSources);

                        break;
                    }
                }

                elementObject.put("Data", elementData);
                elementsArray.put(i, elementObject);
            }

            mShedulerJsonObject.put("ELEMENTS", elementsArray);
        }
        catch(JSONException | NullPointerException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean updateVersion()
    {
        try
        {
            mShedulerJsonObject.put("VERSION", ObjectiveSchedulerCache.SCHEDULER_VERSION_1_2);
        }
        catch(JSONException e)
        {
            return false;
        }

        return true;
    }

    public void save()
    {
        LockedWriteFile schedulerFile = new LockedWriteFile(mSchedulerFilePath);
        schedulerFile.write(mShedulerJsonObject.toString());
        schedulerFile.close();
    }
}
