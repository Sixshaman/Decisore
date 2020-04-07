package com.sixshaman.advancedunforgetter.list;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.archive.TaskArchive;
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

public class TaskList extends RecyclerView.Adapter<TaskList.TaskViewHolder>
{
    private static final String LIST_FILENAME = "TaskList.json";

    //All tasks to be done for today, sorted by ids. Or tomorrow. Or within a year. It's up to the user to decide
    private ArrayList<EnlistedTask> mTasks;

    //The archive to move finished tasks into
    private TaskArchive mArchive;

    //The folder to store the config files
    private String mConfigFolder;

    //The context for displaying the task list
    private Context mContext;

    //Constructs a new task list
    public TaskList()
    {
        mTasks = new ArrayList<>();

        mContext = null;
    }

    public void setArchive(TaskArchive archive)
    {
        mArchive = archive;
    }

    //Sets the folder to store the JSON config file
    public void setConfigFolder(String folder)
    {
        mConfigFolder = folder;
    }

    //Adds a task to the list
    public void addTask(EnlistedTask task)
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
                //OH NO! THE TASK ALREADY EXISTS! WE CAN LOSE THIS TASK! STOP EVERYTHING, DON'T LET IT SAVE
                //Or not, lol
                //throw new RuntimeException("Task already exists");
                Toast.makeText(mContext, "Error adding new task", Toast.LENGTH_SHORT).show();
            }
        }

        if(addPosition != -1)
        {
            notifyItemInserted(addPosition);
            notifyItemRangeChanged(addPosition, getItemCount());
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
    public void moveTaskToArchive(EnlistedTask task)
    {
        if(mArchive == null)
        {
            return;
        }

        int index = Collections.binarySearch(mTasks, task.getId());
        if(index >= 0)
        {
            mTasks.remove(index);
        }

        mArchive.addTask(task.toArchived(LocalDateTime.now()));

        notifyItemRemoved(index);
        notifyItemRangeChanged(index, getItemCount());
        saveTasks();
    }

    //Loads tasks from JSON config file
    public void loadTasks()
    {
        mTasks.clear();

        try
        {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(mConfigFolder + "/" + LIST_FILENAME));

            String line;
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
                    EnlistedTask task = EnlistedTask.fromJSON(taskObject);
                    if(task != null)
                    {
                        mTasks.add(task);
                    }
                }
            }
        }
        catch(JSONException | IOException e)
        {
            e.printStackTrace();
        }

        mTasks.sort(Comparator.comparingLong(EnlistedTask::getId));
        notifyDataSetChanged();
    }

    //Saves tasks in JSON config file
    public void saveTasks()
    {
        try
        {
            JSONObject jsonObject    = new JSONObject();
            JSONArray tasksJsonArray = new JSONArray();

            for(EnlistedTask task: mTasks)
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

    //Returns the largest id for a stored task
    public long getLastTaskId()
    {
        if(mTasks.isEmpty())
        {
            return -1;
        }
        else
        {
            return mTasks.get(mTasks.size() - 1).getId();
        }
    }

    @NonNull
    @Override
    public TaskList.TaskViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
    {
        mContext = viewGroup.getContext();

        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_task_view, viewGroup, false);
        return new TaskList.TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskList.TaskViewHolder taskViewHolder, int position)
    {
        taskViewHolder.mTextView.setText(mTasks.get(position).getName());

        taskViewHolder.mCheckbox.setOnClickListener(view -> moveTaskToArchive(mTasks.get(position)));

        taskViewHolder.mParentLayout.setOnClickListener(view -> Toast.makeText(mContext, mTasks.get(position).getDescription(), Toast.LENGTH_LONG).show());

        taskViewHolder.mCheckbox.setChecked(false);
    }

    @Override
    public int getItemCount()
    {
        return mTasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder
    {
        TextView mTextView;
        CheckBox mCheckbox;

        ConstraintLayout mParentLayout;

        public TaskViewHolder(View itemView)
        {
            super(itemView);

            mTextView = itemView.findViewById(R.id.textTaskName);
            mCheckbox = itemView.findViewById(R.id.checkBoxTaskDone);

            mParentLayout = itemView.findViewById(R.id.layoutTaskView);
        }
    }
}
