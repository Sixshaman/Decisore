package com.sixshaman.advancedunforgetter.archive;

import java.util.ArrayList;

//The task archive that contains all the finished tasks
public class TaskArchive
{
    //The list of all finished task
    private ArrayList<ArchivedTask> mFinishedTasks;

    //Creates a new task archive
    public TaskArchive()
    {
        mFinishedTasks = new ArrayList<>();
    }

    //Adds a task to the archive
    public void addTask(ArchivedTask task)
    {
        //Forever
        mFinishedTasks.add(task);
    }
}
