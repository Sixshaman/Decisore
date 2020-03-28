package com.sixshaman.advancedunforgetter.list;

import android.util.JsonWriter;
import com.sixshaman.advancedunforgetter.archive.TaskArchive;
import com.sixshaman.advancedunforgetter.ui.TaskListAdapter;
import com.sixshaman.advancedunforgetter.utils.Task;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class TaskList
{
    private static final String LIST_FILENAME = "TaskList.json";

    //All tasks to be done for today, sorted by ids. Or tomorrow. Or within a year. It's up to the user to decide
    private ArrayList<Task> mTasks;

    //The archive to move finished tasks into
    private TaskArchive mArchive;

    //Ui controller for this class
    private TaskListAdapter mAdapter;

    //The folder to store the config files
    private String mConfigFolder;

    //Constructs a new task list
    public TaskList(TaskArchive archive)
    {
        mTasks = new ArrayList<>();

        mArchive = archive;
    }

    public void setUiAdapter(TaskListAdapter taskListController)
    {
        mAdapter = taskListController;
    }

    public void setConfigFolder(String folder)
    {
        mConfigFolder = folder;
    }

    //Adds a task to the list
    public void addTask(Task task)
    {
        int addPosition = -1;

        if(mTasks.isEmpty()) //Special case for the empty list
        {
            mTasks.add(task);
            addPosition = mTasks.size() - 1;
        }
        else if(mTasks.get(mTasks.size() - 1).getId() < task.getId()) //Special case for the trivial insertion that will keep the list sorted anyway
        {
            mTasks.add(task);
            addPosition = mTasks.size() - 1;
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
                addPosition = insertIndex;
            }
            else
            {
                //OH NOOOOOOOOO! THE TASK ALREADY EXISTS! WE CAN LOSE THIS TASK! STOP EVERYTHING, DON'T LET IT SAVE
                throw new RuntimeException("Task already exists");
            }
        }

        if(addPosition != -1)
        {
            mAdapter.addTaskData(addPosition, task.getName(), task.getDescription(), task.getId());
            saveTasks();
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

        mAdapter.removeTaskData(index);
        mAdapter.updateOnRemoved(index);
        saveTasks();
    }

    public void loadTasks()
    {
        mTasks.clear();

        try
        {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(mConfigFolder + "/" + LIST_FILENAME));

            String line = "";
            StringBuilder fileContentsStringBuilder = new StringBuilder();

            while((line = bufferedReader.readLine()) != null)
            {
                fileContentsStringBuilder.append(line);
            }

            String fileContents = fileContentsStringBuilder.toString();
            JSONObject jsonObject = new JSONObject(fileContents);

            JSONArray tasksJsonArray = jsonObject.getJSONArray("TASKS");
            for(int i = 0; i < tasksJsonArray.length(); i++)
            {
                JSONObject taskObject = tasksJsonArray.optJSONObject(i);
                if(taskObject != null)
                {
                    Task task = Task.fromJSON(taskObject);
                    mTasks.add(task);
                }
            }
        }
        catch(JSONException | IOException e)
        {
            e.printStackTrace();
        }

        mTasks.sort(Comparator.comparingLong(Task::getId));

        for(int i = 0; i < mTasks.size(); i++)
        {
            mAdapter.addTaskData(i, mTasks.get(i).getName(), mTasks.get(i).getDescription(), mTasks.get(i).getId());
        }

        mAdapter.updateAll();
    }

    public void saveTasks()
    {
        try
        {
            JSONObject jsonObject    = new JSONObject();
            JSONArray tasksJsonArray = new JSONArray();

            for(Task task: mTasks)
            {
                tasksJsonArray.put(task.toJSON());
            }

            jsonObject.put("TASKS", tasksJsonArray);

            FileWriter fileWriter = new FileWriter(mConfigFolder + "/" + LIST_FILENAME);
            fileWriter.write(jsonObject.toString());
            fileWriter.close();
        }
        catch(JSONException | IOException e)
        {
            e.printStackTrace();
        }
    }
}
