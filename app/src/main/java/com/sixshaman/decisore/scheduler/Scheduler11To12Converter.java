package com.sixshaman.decisore.scheduler;

import com.sixshaman.decisore.list.ObjectiveListCache;
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
                if(elementType.equals("ScheduledObjective"))
                {
                    JSONObject objectiveJson = elementObject.getJSONObject("Data");
                    long id = objectiveJson.getLong("Id");
                    objectiveIds.add(id);
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
                    JSONObject chainJson = elementObject.getJSONObject("Data");
                    long id = chainJson.getLong("Id");
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
