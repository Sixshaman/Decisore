package com.sixshaman.advancedunforgetter.scheduler.ObjectivePool;

import com.sixshaman.advancedunforgetter.scheduler.SchedulerElement;

import java.time.LocalDateTime;

public interface PoolElement extends SchedulerElement
{
    boolean isAvailable(LocalDateTime referenceTime);
}
