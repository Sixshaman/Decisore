package com.sixshaman.advancedunforgetter.scheduler;

import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import com.sixshaman.advancedunforgetter.utils.RandomUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayList;

//A pool that can randomly choose from several task sources
public class TaskPool
{
    //The name of the pool
    private String mName;

    //The description of the pool
    private String mDescription;

    //The list of all the task sources the pool can choose from
    private ArrayList<TaskSource> mTaskSources;

    //The id of the task that was most recently provided by this pool.
    private long mLastProvidedTaskId;

    //The flag that shows that the pool is active (i.e. not paused)
    boolean mIsActive;

    //Constructs a new task pool
    TaskPool(String name, String description)
    {
        mName        = name;
        mDescription = description;

        mTaskSources = new ArrayList<>();

        setLastProvidedTaskId(0);

        mIsActive = true;
    }

    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();

        try
        {
            result.put("Name",        mName);
            result.put("Description", mDescription);

            result.put("LastId", Long.toString(mLastProvidedTaskId));

            result.put("IsActive", Boolean.toString(mIsActive));

            JSONArray sourcesArray = new JSONArray();
            for(TaskSource source: mTaskSources)
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

    public static TaskPool fromJSON(JSONObject jsonObject)
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

            TaskPool taskPool = new TaskPool(name, description);

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
                taskPool.pause();
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
                                    taskPool.addTaskSource(singleTaskSource);
                                }
                            }
                            else if(sourceType.equals(TaskChain.getSourceTypeString()))
                            {
                                TaskChain taskChain = TaskChain.fromJSON(sourceData);
                                if(taskChain != null)
                                {
                                    taskPool.addTaskSource(taskChain);
                                }
                            }
                        }
                    }
                }
            }

            return taskPool;
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    //Updates all the task sources, removing the finished ones
    void updateTaskSources(LocalDateTime referenceTime)
    {
        ArrayList<TaskSource> updatedSourceList = new ArrayList<>();
        for(TaskSource source: mTaskSources)
        {
            //Throw away finished task sources
            if(source.getState(referenceTime) != TaskSource.SourceState.SOURCE_STATE_FINISHED)
            {
                updatedSourceList.add(source);
            }
        }

        mTaskSources = updatedSourceList;
    }

    //Gets a task from a random source
    EnlistedObjective getRandomTask(LocalDateTime referenceTime)
    {
        if(!mIsActive)
        {
            return null;
        }

        //Check non-empty sources only
        ArrayList<TaskSource> availableSources = new ArrayList<>();
        for(TaskSource source: mTaskSources)
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
        EnlistedObjective resultTask = availableSources.get(randomSourceIndex).obtainTask(referenceTime);

        setLastProvidedTaskId(resultTask.getId());
        return resultTask;
    }

    //Returns true for a single-source, single-task pool
    boolean isSingleSingleTaskPool()
    {
        return (mTaskSources.size() == 1) && (mTaskSources.get(0) instanceof SingleTaskSource);
    }

    public long getMaxTaskId()
    {
        long maxId = -1;
        for(TaskSource source: mTaskSources)
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
        return mTaskSources.size();
    }

    //Adds a new task source to choose from
    void addTaskSource(TaskSource source)
    {
        mTaskSources.add(source);
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
    long getLastProvidedTaskId()
    {
        return mLastProvidedTaskId;
    }

    private void setLastProvidedTaskId(long taskId)
    {
        mLastProvidedTaskId = taskId;
    }
}
