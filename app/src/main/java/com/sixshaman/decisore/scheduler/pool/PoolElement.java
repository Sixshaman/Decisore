package com.sixshaman.decisore.scheduler.pool;

import com.sixshaman.decisore.scheduler.SchedulerElement;

import java.time.LocalDateTime;
import java.util.HashSet;

public abstract class PoolElement extends SchedulerElement
{
    public PoolElement(long id, String name, String description)
    {
        super(id, name, description);
    }
}
