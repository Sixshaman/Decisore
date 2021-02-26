package com.sixshaman.advancedunforgetter.scheduler;

import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import com.sixshaman.advancedunforgetter.utils.RandomUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayList;

//A pool that can randomly choose from several task sources
public class ObjectivePool
{
    //The name of the pool
    private String mName;

    //The description of the pool
    private String mDescription;

    //The list of all the task sources the pool can choose from
    private ArrayList<TaskSource> mObjectiveSources;

    //The id of the task that was most recently provided by this pool.
    private long mLastProvidedObjectiveId;

    //The flag that shows that the pool is active (i.e. not paused)
    boolean mIsActive;

    //Constructs a new task pool
    ObjectivePool(String name, String description)
    {
        mName        = name;
        mDescription = description;

        mObjectiveSources = new ArrayList<>();

        setLastProvidedObjectiveId(0);

        mIsActive = true;
    }

    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();

        try
        {
            result.put("Name",        mName);
            result.put("Description", mDescription);

            result.put("LastId", Long.toString(mLastProvidedObjectiveId));

            result.put("IsActive", Boolean.toString(mIsActive));

            JSONArray sourcesArray = new JSONArray();
            for(TaskSource source: mObjectiveSources)
            {
                JSONObject taskSourceObject = new JSONObject();

                //Another poke at Java! It can't into static methods in interfaces
                if(source instanceof SingleTaskSource)
                {
                    taskSourceObject.put("Type", SingleTaskSource.getSourceTypeString());
                }
                else if(source instanceof TaskChain)
                {
                    taskSourceObject.put("Type", TaskChain.getSourceTypeString());
                }
                else
                {
                    continue; //Unknown source type????
                }

                taskSourceObject.put("Data", source.toJSON());

                sourcesArray.put(taskSourceObject);
            }

            result.put("Sources", sourcesArray);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static ObjectivePool fromJSON(JSONObject jsonObject)
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
                            if(sourceType.equals(SingleTaskSource.getSourceTypeString()))
                            {
                                SingleTaskSource singleTaskSource = SingleTaskSource.fromJSON(sourceData);
                                if(singleTaskSource != null)
                                {
                                    objectivePool.addTaskSource(singleTaskSource);
                                }
                            }
                            else if(sourceType.equals(TaskChain.getSourceTypeString()))
                            {
                                TaskChain taskChain = TaskChain.fromJSON(sourceData);
                                if(taskChain != null)
                                {
                                    objectivePool.addTaskSource(taskChain);
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

    //Updates all the task sources, removing the finished ones
    void updateObjectiveSources(LocalDateTime referenceTime)
    {
        ArrayList<TaskSource> updatedSourceList = new ArrayList<>();
        for(TaskSource source: mObjectiveSources)
        {
            //Throw away finished task sources
            if(source.getState(referenceTime) != TaskSource.SourceState.SOURCE_STATE_FINISHED)
            {
                updatedSourceList.add(source);
            }
        }

        mObjectiveSources = updatedSourceList;
    }

    //Gets a task from a random source
    EnlistedObjective getRandomObjective(LocalDateTime referenceTime)
    {
        if(!mIsActive)
        {
            return null;
        }

        //Check non-empty sources only
        ArrayList<TaskSource> availableSources = new ArrayList<>();
        for(TaskSource source: mObjectiveSources)
        {
            if(source.getState(referenceTime) == TaskSource.SourceState.SOURCE_STATE_REGULAR)
            {
                availableSources.add(source);
            }
        }

        if(availableSources.size() == 0)
        {
            return null;
        }

        int  randomSourceIndex  = (int) RandomUtils.getInstance().getRandomUniform(0, availableSources.size() - 1);
        EnlistedObjective resultObjective = availableSources.get(randomSourceIndex).obtainTask(referenceTime);

        if(resultObjective != null)
        {
            setLastProvidedObjectiveId(resultObjective.getId());
        }

        return resultObjective;
    }

    //Returns true for a single-source, single-task pool
    boolean isSingleSingleTaskPool()
    {
        return (mObjectiveSources.size() == 1) && (mObjectiveSources.get(0) instanceof SingleTaskSource);
    }

    public long getMaxTaskId()
    {
        long maxId = -1;
        for(TaskSource source: mObjectiveSources)
        {
            long sourceMaxId = source.getMaxTaskId();
            if(sourceMaxId > maxId)
            {
                maxId = sourceMaxId;
            }
        }

        return maxId;
    }

    public String getName()
    {
        return mName;
    }

    public String getDescription()
    {
        return mDescription;
    }

    int getTaskSourceCount()
    {
        return mObjectiveSources.size();
    }

    //Adds a new task source to choose from
    void addTaskSource(TaskSource source)
    {
        mObjectiveSources.add(source);
    }

    //Pauses the pool
    void pause()
    {
        mIsActive = false;
    }

    //Unpauses the pool
    void unpause()
    {
        mIsActive = true;
    }

    //Gets the last provided task id, to check if it has been finished yet
    long getLastProvidedObjectiveId()
    {
        return mLastProvidedObjectiveId;
    }

    private void setLastProvidedObjectiveId(long objectiveId)
    {
        mLastProvidedObjectiveId = objectiveId;
    }
}
