package com.sixshaman.advancedunforgetter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Random;

//A pool that can randomly choose from several task sources
public class TaskPool
{
    //The list of all the task sources the pool can choose from
    private ArrayList<TaskSource> mTaskSources;

    //The period of pool giving tasks
    private Duration mRepeatDuration;

    //Constructs a new task pool
    public TaskPool()
    {
    }

    Task getRandomTask()
    {
        if(mTaskSources.size() == 0)
        {
            return null;
        }

        int randomSourceIndex = (int)RandomUtils.getInstance().getRandomUniform(0, mTaskSources.size());
        return mTaskSources.get(randomSourceIndex).obtainTask().getTask();
    }

    //Adds a new task source to choose from
    void addTaskSource(TaskSource source)
    {
        mTaskSources.add(source);
    }
}
