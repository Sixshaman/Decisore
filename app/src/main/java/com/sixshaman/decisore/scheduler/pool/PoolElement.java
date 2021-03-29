package com.sixshaman.decisore.scheduler.pool;

import com.sixshaman.decisore.scheduler.SchedulerElement;

import java.time.LocalDateTime;

public interface PoolElement extends SchedulerElement
{
    boolean isAvailable(LocalDateTime referenceTime);
}
