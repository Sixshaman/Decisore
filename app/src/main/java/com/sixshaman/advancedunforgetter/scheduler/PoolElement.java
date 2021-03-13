package com.sixshaman.advancedunforgetter.scheduler;

import java.time.LocalDateTime;

interface PoolElement extends SchedulerElement
{
    boolean isAvailable(LocalDateTime referenceTime);
}
