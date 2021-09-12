package com.sixshaman.decisore.scheduler;

import com.sixshaman.decisore.list.EnlistedObjective;
import com.sixshaman.decisore.scheduler.chain.ObjectiveChain;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.HashSet;

public abstract class SchedulerElement
{
    //The ID of the element
    final long mId;

    //The ID of the parent element
    long mParentId;

    //Element name
    private String mName;

    //Element description
    private String mDescription;

    //Is it active or paused? If paused, the scheduler won't add it to the objective list even after the mScheduledAddDate
    private boolean mIsActive;

    public SchedulerElement(long id, String name, String description)
    {
        mId = id;

        mParentId = -1;

        mName        = name;
        mDescription = description;

        mIsActive = true;
    }

    public long getId()
    {
        return mId;
    }

    public long getParentId()
    {
        return mParentId;
    }

    public String getName()
    {
        return mName;
    }

    public String getDescription()
    {
        return mDescription;
    }

    public abstract JSONObject toJSON();

    public boolean isPaused()
    {
        return !mIsActive;
    }

    public void setPaused(boolean paused)
    {
        mIsActive = !paused;
    }

    public void setParentId(long parentId)
    {
        mParentId = parentId;
    }

    public void setName(String name)
    {
        mName = name;
    }

    public void setDescription(String description)
    {
        mDescription = description;
    }

    //Returns true if it's valid, invalid elements can't be in scheduler
    public abstract boolean isValid();

    //Returns the element name(Objective/Chain/Pool)
    public abstract String getElementName();

    public abstract boolean isAvailable(HashSet<Long> blockingObjectiveIds, LocalDateTime referenceTime, int dayStartHour);

    public abstract EnlistedObjective obtainEnlistedObjective(final HashSet<Long> ignoredObjectiveIds, LocalDateTime referenceTime, int dayStartHour);

    public abstract void updateDayStart(LocalDateTime referenceTime, int oldStartHour, int newStartHour);

    public abstract boolean isRelatedToObjective(long objectiveId);

    public abstract boolean mergeRelatedObjective(ScheduledObjective objective);

    public abstract ScheduledObjective getRelatedObjectiveById(long objectiveId);
    public abstract ObjectiveChain     getRelatedChainById(long chainId);
    public abstract ObjectiveChain     getChainForObjectiveById(long objectiveId);

    //Returns the largest id of any related element (itself, or contained objectives, or contained chains)
    public abstract long getLargestRelatedId();
}
