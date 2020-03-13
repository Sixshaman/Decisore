package com.sixshaman.advancedunforgetter;

import java.util.Queue;

public class TaskChain implements TaskSource
{
    private Queue<Task> mTasks;

    //Creates a new task chain
    TaskChain()
    {
    }

    //Adds a task to the chain
    void AddTaskToChain(Task task)
    {
        mTasks.add(task);
    }

    @Override
    public Task obtainTask()
    {
        return mTasks.remove();
    }
}
