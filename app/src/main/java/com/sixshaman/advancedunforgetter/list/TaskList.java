package com.sixshaman.advancedunforgetter.list;

import com.sixshaman.advancedunforgetter.utils.Task;

import java.util.ArrayList;

public class TaskList
{
    //All tasks to be done for today, sorted by ids. Or tomorrow. Or within a year. It's up to the user to decide
    private ArrayList<Task> mTasks;

    //Constructs a new task list
    public TaskList()
    {
        mTasks = new ArrayList<>();
    }

    //Adds a task to the list
    public void addTask(Task task)
    {
        //TODO move binary search

        //Binary search for the new task index. This is to keep task list sorted by id.
        int leftBound  = 0;
        int rightBound = mTasks.size();
        while(rightBound != leftBound)
        {
            int emplacePoint = leftBound + (rightBound - leftBound) / 2;
            if(mTasks.get(emplacePoint).getId() < task.getId())
            {
                leftBound = emplacePoint;
            }
            else if(mTasks.get(emplacePoint).getId() > task.getId())
            {
                rightBound = emplacePoint;
            }
            else
            {
                break;
            }
        }

        int addIndex = leftBound + (rightBound - leftBound) / 2;

        mTasks.add(null);
        for(int i = addIndex; i < mTasks.size() - 1; i++)
        {
            mTasks.set(i + 1, mTasks.get(i));
        }

        mTasks.set(addIndex, task);
    }

    public boolean isTaskInList(long taskId)
    {
        //TODO move binary search
        return false;
    }
}
