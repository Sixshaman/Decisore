package com.sixshaman.decisore.list;

import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.decisore.utils.LockedReadFile;
import com.sixshaman.decisore.utils.LockedWriteFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

//Recalculates id based on the fact that chains, pools, and objectives now share a common id s[ace
//Assigns parent id to each objective
public class List10To11Converter
{
    private JSONObject mListJsonObject;
    private final String mListFilePath;

    public List10To11Converter(String listFilePath)
    {
        mListFilePath = listFilePath;

        try
        {
            LockedReadFile listFile = new LockedReadFile(mListFilePath);

            String fileContents = listFile.read();
            mListJsonObject = new JSONObject(fileContents);

            listFile.close();
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
            JSONArray objectivesJsonArray = mListJsonObject.getJSONArray("TASKS");
            for(int i = 0; i < objectivesJsonArray.length(); i++)
            {
                JSONObject objectiveObject = objectivesJsonArray.optJSONObject(i);
                long id = objectiveObject.getLong("Id");
                objectiveIds.add(id);
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        Collections.sort(objectiveIds);
        return objectiveIds;
    }

    @SuppressWarnings("ConstantConditions")
    public boolean patchIds(HashMap<Long, Long> newIdMap)
    {
        try
        {
            JSONArray oldObjectivesJsonArray = mListJsonObject.getJSONArray("TASKS");
            mListJsonObject.remove("TASKS");

            JSONArray newObjectivesJsonArray = new JSONArray();
            for(int i = 0; i < oldObjectivesJsonArray.length(); i++)
            {
                JSONObject objectiveObject = oldObjectivesJsonArray.optJSONObject(i);

                long oldId = objectiveObject.getLong("Id");
                long newId = newIdMap.get(oldId);

                objectiveObject.put("Id", newId);
                newObjectivesJsonArray.put(objectiveObject);
            }

            mListJsonObject.put("OBJECTIVES", newObjectivesJsonArray);
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
            JSONArray oldObjectivesJsonArray = mListJsonObject.getJSONArray("OBJECTIVES");

            JSONArray newObjectivesJsonArray = new JSONArray();
            for(int i = 0; i < oldObjectivesJsonArray.length(); i++)
            {
                JSONObject objectiveObject = oldObjectivesJsonArray.optJSONObject(i);

                long objectiveId = objectiveObject.getLong("Id");
                long parentId    = parentIdMap.get(objectiveId);

                objectiveObject.put("ParentId", parentId);
                newObjectivesJsonArray.put(objectiveObject);
            }

            mListJsonObject.put("OBJECTIVES", newObjectivesJsonArray);
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
            mListJsonObject.put("VERSION", ObjectiveListCache.LIST_VERSION_1_1);
        }
        catch(JSONException e)
        {
            return false;
        }

        return true;
    }

    public void save()
    {
        LockedWriteFile listFile = new LockedWriteFile(mListFilePath);
        listFile.write(mListJsonObject.toString());
        listFile.close();
    }
}
