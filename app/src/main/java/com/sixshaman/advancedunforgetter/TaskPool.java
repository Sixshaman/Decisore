package com.sixshaman.advancedunforgetter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Random;

//A pool that can randomly choose from several task sources
public class TaskPool
{
    //The name of the pool
    private String mName;

    //The description of the pool
    private String mDescription;

    //The list of all the task sources the pool can choose from
    private ArrayList<TaskSource> mTaskSources;

    //The next date the pool is available
    private LocalDateTime mNextUpdateDate;

    //The period of giving tasks from the pool
    private Duration mRepeatDuration;

    //The id of the task that was most recently provided by this pool.
    private long mLastProvidedTaskId;

    //Constructs a new task pool
    TaskPool(String name, String description)
    {
        mName        = name;
        mDescription = description;

        mTaskSources = new ArrayList<TaskSource>();

        mNextUpdateDate = LocalDateTime.now();
        mNextUpdateDate = mNextUpdateDate.truncatedTo(ChronoUnit.HOURS);

        mRepeatDuration = Duration.ofHours(0); //Default behavior: always update on-demand

        mLastProvidedTaskId = 0;
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
    Task getRandomTask(LocalDateTime referenceTime)
    {
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

        int  randomSourceIndex = (int)RandomUtils.getInstance().getRandomUniform(0, availableSources.size() - 1);
        Task resultTask        = availableSources.get(randomSourceIndex).obtainTask(referenceTime);

        //Reschedule the pool
        mNextUpdateDate = referenceTime.plusHours(mRepeatDuration.toHours());
        mNextUpdateDate = mNextUpdateDate.truncatedTo(ChronoUnit.HOURS);

        mLastProvidedTaskId = resultTask.getId();
        return resultTask;
    }

    //Returns true for a single-source, single-task pool
    boolean isSingleSingleTaskPool()
    {
        return (mTaskSources.size() == 1) && (mTaskSources.get(0) instanceof SingleTaskSource);
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

    //Sets the repeat duration
    public void setRepeatDuration(Duration duration)
    {
        mRepeatDuration = duration;
    }

    //Gets the last provided task id, to check if it has been finished yet
    long getLastProvidedTaskId()
    {
        return mLastProvidedTaskId;
    }
}
