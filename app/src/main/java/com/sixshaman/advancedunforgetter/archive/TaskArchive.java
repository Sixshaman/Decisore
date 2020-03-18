package com.sixshaman.advancedunforgetter.archive;

import com.sixshaman.advancedunforgetter.utils.Task;

import java.util.ArrayList;

//The task archive that contains all the finished tasks
public class TaskArchive
{
    //The list of all finished task
    private ArrayList<Task> mFinishedTasks;

    //Creates a new task archive
    public TaskArchive()
    {
        mFinishedTasks = new ArrayList<>();
    }

    public void addTask(Task task)
    {
        //Forever
        mFinishedTasks.add(task);
    }
}
