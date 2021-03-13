package com.sixshaman.advancedunforgetter.scheduler;

import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import com.sixshaman.advancedunforgetter.utils.RandomUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayList;

//A pool that can randomly choose from several task sources
public class ObjectivePool implements SchedulerElement
{
    //The name of the pool
    private String mName;

    //The description of the pool
    private String mDescription;

    //The list of all the task sources the pool can choose from
    private ArrayList<PoolElement> mObjectiveSources;

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

    @Override
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
            for(PoolElement source: mObjectiveSources)
            {
                JSONObject taskSourceObject = new JSONObject();

                //Another poke at Java! It can't into static methods in interfaces
                if(source instanceof ScheduledObjective)
                {
                    taskSourceObject.put("Type", SingleObjectivePoolSource.getSourceTypeString());
                }
                else if(source instanceof ObjectiveChain)
                {
                    taskSourceObject.put("Type", ObjectiveChain.getSourceTypeString());
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
                            if(sourceType.equals(SingleObjectivePoolSource.getSourceTypeString()))
                            {
                                SingleObjectivePoolSource singleTaskSource = SingleObjectivePoolSource.fromJSON(sourceData);
                                if(singleTaskSource != null)
                                {
                                    objectivePool.addTaskSource(singleTaskSource);
                                }
                            }
                            else if(sourceType.equals(ObjectiveChain.getSourceTypeString()))
                            {
                                ObjectiveChain taskChain = ObjectiveChain.fromJSON(sourceData);
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

    //Gets a task from a random source
    EnlistedObjective getRandomObjective(LocalDateTime referenceTime)
    {
        if(!mIsActive)
        {
            return null;
        }

        //Check non-empty sources only
        ArrayList<PoolElement> availableSources = new ArrayList<>();
        for(PoolElement source: mObjectiveSources)
        {
            if(source.isAvailable(referenceTime))
            {
                availableSources.add(source);
            }
        }

        if(availableSources.size() == 0)
        {
            return null;
        }

        int  randomSourceIndex  = (int) RandomUtils.getInstance().getRandomUniform(0, availableSources.size() - 1);
        PoolElement randomPoolElement = availableSources.get(randomSourceIndex);

        EnlistedObjective resultObjective = null;
        if(randomPoolElement instanceof ObjectiveChain)
        {
            resultObjective = ((ObjectiveChain)randomPoolElement).obtainObjective(referenceTime);
        }
        else if(randomPoolElement instanceof ScheduledObjective)
        {
            ScheduledObjective scheduledObjective = (ScheduledObjective)randomPoolElement;
            resultObjective = scheduledObjective.toEnlisted(referenceTime);

            if(scheduledObjective.isRepeatable())
            {
                scheduledObjective.reschedule(referenceTime);
            }
            else
            {
                //Delete finished objectives
                mObjectiveSources.remove(randomSourceIndex);
            }
        }

        if(resultObjective != null)
        {
            setLastProvidedObjectiveId(resultObjective.getId());
        }

        return resultObjective;
    }

    //Returns the number of objective sources in pool
    int getSourceCount()
    {
        return mObjectiveSources.size();
    }

    //Returns the source with given index
    PoolElement getSource(int position)
    {
        return mObjectiveSources.get(position);
    }

    //Returns true if the pool contains the source
    boolean containsSource(PoolElement source)
    {
        return mObjectiveSources.contains(source);
    }

    public long getMaxObjectiveId()
    {
        long maxId = -1;
        for(PoolElement source: mObjectiveSources)
        {
            long sourceMaxId = 0;
            if(source instanceof ObjectiveChain)
            {
                sourceMaxId = ((ObjectiveChain) source).getMaxObjectiveId();
            }
            else if(source instanceof ScheduledObjective)
            {
                sourceMaxId = ((ScheduledObjective)source).getId();
            }

            if(sourceMaxId > maxId)
            {
                maxId = sourceMaxId;
            }
        }

        return maxId;
    }

    public PoolElement findSourceForObjective(long objectiveId)
    {
        for(PoolElement source: mObjectiveSources)
        {
            if(source instanceof ObjectiveChain)
            {
                if(((ObjectiveChain)source).containedObjective(objectiveId))
                {
                    return source;
                }
            }
            else if(source instanceof ScheduledObjective)
            {
                if(((ScheduledObjective) source).getId() == objectiveId)
                {
                    return source;
                }
            }
        }

        return null;
    }

    @Override
    public boolean isPaused()
    {
        return !mIsActive;
    }

    @Override
    public String getName()
    {
        return mName;
    }

    @Override
    public String getDescription()
    {
        return mDescription;
    }

    int getTaskSourceCount()
    {
        return mObjectiveSources.size();
    }

    //Adds a new task source to choose from
    void addTaskSource(PoolElement source)
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
