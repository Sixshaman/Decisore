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
import java.util.HashMap;

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
                            JSONObject objectiveJson = objectivesJsonArray.optJSONObject(j);
                            long id = objectiveJson.getLong("Id");
                            objectiveIds.add(id);
                        }

                        JSONArray idHistoryArray = chainJson.getJSONArray("ObjectiveHistory");
                        for (int j = 0; j < idHistoryArray.length(); j++)
                        {
                            long objectiveId = idHistoryArray.optLong(j, -1);
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

                            String sourceType = sourceObject.optString("Type");
                            if(sourceType.equals("ScheduledObjective"))
                            {
                                JSONObject sourceData = sourceObject.getJSONObject("Data");
                                long id = sourceData.getLong("Id");
                                objectiveIds.add(id);
                            }
                            else if (sourceType.equals("ObjectiveChain"))
                            {
                                JSONObject chainJson = elementObject.getJSONObject("Data");

                                JSONArray objectivesJsonArray = chainJson.getJSONArray("Objectives");
                                for(int k = 0; k < objectivesJsonArray.length(); k++)
                                {
                                    JSONObject objectiveObject = objectivesJsonArray.optJSONObject(k);
                                    long id = objectiveObject.getLong("Id");
                                    objectiveIds.add(id);
                                }

                                JSONArray idHistoryArray = chainJson.getJSONArray("ObjectiveHistory");
                                for(int k = 0; k < idHistoryArray.length(); k++)
                                {
                                    long objectiveId = idHistoryArray.optLong(k, -1);
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

                        String sourceType = sourceObject.optString("Type");
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

        return poolIds;
    }

    public void patchIds(HashMap<Long, Long> newObjectiveIdMap, HashMap<Long, Long> newChainIdMap, HashMap<Long, Long> newPoolIdMap)
    {
        try
        {
            mShedulerJsonObject.put("VERSION", ObjectiveSchedulerCache.SCHEDULER_VERSION_1_2);

            JSONArray elementsArray = mShedulerJsonObject.getJSONArray("ELEMENTS");
            mShedulerJsonObject.remove("ELEMENTS");

            JSONArray elementsJsonArray = new JSONArray();
            for(int i = 0; i < elementsArray.length(); i++)
            {
                JSONObject elementObject = new JSONObject();

                String     elementType = elementObject.getString("Type");
                JSONObject elementData = elementObject.getJSONObject("Data");

                switch(elementType)
                {
                    case "ScheduledObjective":
                    {
                        long oldId = elementData.getLong("Id");
                        elementData.put("Id", newObjectiveIdMap.get(oldId));
                        break;
                    }
                    case "ObjectiveChain":
                    {
                        long oldId = elementData.getLong("Id");
                        elementData.put("Id", newChainIdMap.get(oldId));
                        break;
                    }
                    case "ObjectivePool":
                    {
                        long oldId = elementData.getLong("Id");
                        elementData.put("Id", newPoolIdMap.get(oldId));
                        break;
                    }
                }

                elementsJsonArray.put(elementObject);
            }

            mShedulerJsonObject.put("ELEMENTS", elementsJsonArray);
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        LockedWriteFile schedulerFile = new LockedWriteFile(mSchedulerFilePath);
        schedulerFile.write(mShedulerJsonObject.toString());
        schedulerFile.close();
    }
}
