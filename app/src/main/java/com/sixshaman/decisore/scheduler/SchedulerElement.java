package com.sixshaman.decisore.scheduler;

import com.sixshaman.decisore.list.EnlistedObjective;
import com.sixshaman.decisore.scheduler.chain.ObjectiveChain;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.HashSet;

public interface SchedulerElement
{
    String getName();

    @SuppressWarnings("unused")
    String getDescription();

    JSONObject toJSON();

    boolean isPaused();

    void setPaused(boolean paused);

    //Returns true if it's valid, invalid elements can't be in scheduler
    boolean isValid();

    //Returns the element name(Objective/Chain/Pool)
    String getElementName();

    boolean isAvailable(HashSet<Long> blockingObjectiveIds, LocalDateTime referenceTime);

    EnlistedObjective obtainEnlistedObjective(final HashSet<Long> blockingObjectiveIds, LocalDateTime referenceTime, int dayStartHour);

    void updateDayStart(LocalDateTime referenceTime, int oldStartHour, int newStartHour);

    boolean isRelatedToObjective(long objectiveId);

    boolean mergeRelatedObjective(ScheduledObjective objective);

    ScheduledObjective getRelatedObjectiveById(long objectiveId);
    ObjectiveChain     getRelatedChainById(long chainId);
    ObjectiveChain     getChainForObjectiveById(long objectiveId);

    long getMaxRelatedObjectiveId();
    long getMaxRelatedChainId();
}
