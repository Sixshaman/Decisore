package com.sixshaman.advancedunforgetter.list;

import com.sixshaman.advancedunforgetter.archive.TaskArchive;
import com.sixshaman.advancedunforgetter.utils.Task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class TaskList
{
    //All tasks to be done for today, sorted by ids. Or tomorrow. Or within a year. It's up to the user to decide
    private ArrayList<Task> mTasks;

    //The archive to move finished tasks into
    private TaskArchive mArchive;

    //Constructs a new task list
    public TaskList(TaskArchive archive)
    {
        mTasks = new ArrayList<>();

        mArchive = archive;
    }

    //Adds a task to the list
    public void addTask(Task task)
    {
        if(mTasks.isEmpty()) //Special case for the empty list
        {
            mTasks.add(task);
        }
        else if(mTasks.get(mTasks.size() - 1).getId() < task.getId()) //Special case for the trivial insertion that will keep the list sorted anyway
        {
            mTasks.add(task);
        }
        else
        {
            int index = Collections.binarySearch(mTasks, task.getId());
            if(index < 0)
            {
                //Insert at selected position
                int insertIndex = -(index + 1);

                mTasks.add(null);
                for(int i = insertIndex; i < mTasks.size() - 1; i++)
                {
                    mTasks.set(i + 1, mTasks.get(i));
                }

                mTasks.set(insertIndex, task);
            }
            else
            {
                //OH NOOOOOOOOO! THE TASK ALREADY EXISTS! WE CAN LOSE THIS TASK! STOP EVERYTHING, DON'T LET IT SAVE
                throw new RuntimeException("Task already exists");
            }
        }
    }

    //Checks if the task with specified id is in the list
    public boolean isTaskInList(long taskId)
    {
        //Special case for empty list
        if(mTasks.size() == 0)
        {
            return false;
        }

        //Special case: since mTasks is sorted by id, then last element having lesser id means the task is not in mTasks. This is a pretty common case.
        if(mTasks.get(mTasks.size() - 1).getId() < taskId)
        {
            return false;
        }

        //The mTasks list is sorted by id, so find the index with binary search
        return (Collections.binarySearch(mTasks, taskId) >= 0);
    }

    //Removes the task from the list
    public void moveTaskToArchive(Task task)
    {
        int index = Collections.binarySearch(mTasks, task.getId());
        if(index >= 0)
        {
            mTasks.remove(index);
        }

        task.setFinishedDate(LocalDateTime.now());
        mArchive.addTask(task);
    }
}
