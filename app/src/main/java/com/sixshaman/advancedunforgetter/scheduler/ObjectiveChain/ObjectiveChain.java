package com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain;

import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.PoolElement;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjective;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;

public class ObjectiveChain implements PoolElement
{
    //Objective chain name
    private String mName;

    //Objective chain description
    private String mDescription;

    //The objectives that this chain will provide one-by-one. Since Java doesn't have any non-deque Queue implementation, we will use ArrayDeque
    private ArrayDeque<ScheduledObjective> mObjectives;

    //The list of ids of all objectives once provided by the chain
    HashSet<Long> mObjectiveIdHistory;

    //Creates a new task chain
    public ObjectiveChain(String name, String description)
    {
        mName        = name;
        mDescription = description;

        mObjectives         = new ArrayDeque<>();
        mObjectiveIdHistory = new HashSet<>();
    }

    //Adds a task to the chain
    public void addTaskToChain(ScheduledObjective objective)
    {
        if(mObjectives != null) //mObjectives can be null if the chain is finished
        {
            mObjectives.addLast(objective);
            mObjectiveIdHistory.add(objective.getId());
        }
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

    @Override
    public JSONObject toJSON()
    {
        //Never save finished task sources
        if(mObjectives == null)
        {
            return null;
        }
        else
        {
            JSONObject result = new JSONObject();

            try
            {
                result.put("Name",        mName);
                result.put("Description", mDescription);

                JSONArray objectivesArray = new JSONArray();
                for(ScheduledObjective objective: mObjectives)
                {
                    objectivesArray.put(objective.toJSON());
                }

                result.put("Objectives", objectivesArray);

                JSONArray idHistoryArray = new JSONArray();
                for(Long objectiveId: mObjectiveIdHistory)
                {
                    idHistoryArray.put(objectiveId.longValue());
                }

                result.put("ObjectiveHistory", idHistoryArray);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }

            return result;
        }
    }

    public int getObjectiveCount()
    {
        return mObjectives.size();
    }

    public ScheduledObjective getFirstObjective()
    {
        return mObjectives.getFirst();
    }

    public long getMaxObjectiveId()
    {
        if(mObjectives == null)
        {
            return 0;
        }

        long maxId = -1;
        for(ScheduledObjective objective: mObjectives)
        {
            long objectiveId = objective.getId();
            if(objectiveId > maxId)
            {
                maxId = objectiveId;
            }
        }

        return maxId;
    }

    public boolean containedObjective(long objectiveId)
    {
        for(Long historicId: mObjectiveIdHistory)
        {
            if(historicId == objectiveId)
            {
                return true;
            }
        }

        return false;
    }

    public boolean putBack(ScheduledObjective objective)
    {
        if(mObjectives != null && !mObjectives.isEmpty())
        {
            ScheduledObjective firstObjective = mObjectives.getFirst();
            if(firstObjective.getId() == objective.getId())
            {
                firstObjective.rescheduleUnregulated(objective.getScheduledEnlistDate());
            }
            else
            {
                mObjectives.addFirst(objective);
                mObjectiveIdHistory.add(objective.getId()); //Just in case
            }

            return true;
        }

        return false;
    }

    public EnlistedObjective obtainObjective(LocalDateTime referenceTime)
    {
        if(mObjectives != null && !mObjectives.isEmpty())
        {
            if(mObjectives.getFirst().isAvailable(referenceTime))
            {
                return mObjectives.removeFirst().toEnlisted(referenceTime);
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    @Override
    public boolean isPaused()
    {
        return false;
    }

    @Override
    public boolean isAvailable(LocalDateTime referenceTime)
    {
        if(isPaused())
        {
            return false;
        }

        if(mObjectives.isEmpty())
        {
            return true;
        }
        else
        {
            ScheduledObjective firstObjective = mObjectives.getFirst();
            return firstObjective.isAvailable(referenceTime);
        }
    }

    @Override
    public String getElementName()
    {
        return "ObjectiveChain";
    }
}
