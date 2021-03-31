package com.sixshaman.decisore.scheduler;

import com.sixshaman.decisore.scheduler.pool.ObjectivePool;
import com.sixshaman.decisore.scheduler.pool.ObjectivePool10Loader;
import com.sixshaman.decisore.scheduler.pool.PoolElement;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SchedulerOldVersionLoader
{
    static ArrayList<SchedulerElement> loadSchedulerElementsOld(ObjectiveSchedulerCache schedulerCache, JSONObject jsonObject, int version)
    {
        if(version == ObjectiveSchedulerCache.SCHEDULER_VERSION_1_0)
        {
            return loadSchedulerElements10(schedulerCache, jsonObject);
        }

        return null;
    }

    private static ArrayList<SchedulerElement> loadSchedulerElements10(ObjectiveSchedulerCache schedulerCache, JSONObject jsonObject)
    {
        ArrayList<SchedulerElement> schedulerElements = new ArrayList<>();

        //Old version used objective pools for everything
        ArrayList<ObjectivePool> objectivePools = new ArrayList<>();
        try
        {
            JSONArray poolsJsonArray = jsonObject.getJSONArray("POOLS");
            for(int i = 0; i < poolsJsonArray.length(); i++)
            {
                JSONObject poolObject = poolsJsonArray.optJSONObject(i);
                if(poolObject != null)
                {
                    ObjectivePool10Loader pool10Loader = new ObjectivePool10Loader(schedulerCache);

                    ObjectivePool pool = pool10Loader.fromJSON(poolObject);
                    if(pool != null)
                    {
                        objectivePools.add(pool);
                    }
                }
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
            return null;
        }

        for(ObjectivePool pool: objectivePools)
        {
            if (pool.getName().equals("")) //Empty pool was either a chain or a regular objective
            {
                if(pool.getSourceCount() != 1)
                {
                    //Implicit pools were always single-element
                    return null;
                }

                PoolElement poolElement = pool.getSource(0);
                schedulerElements.add(poolElement);
            }
            else
            {
                schedulerElements.add(pool);
            }
        }

        return schedulerElements;
    }
}
