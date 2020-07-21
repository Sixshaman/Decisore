package com.sixshaman.advancedunforgetter.archive;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.utils.BaseFileLockException;
import com.sixshaman.advancedunforgetter.utils.LockedFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.channels.FileLock;
import java.util.ArrayList;

//The task archive that contains all the finished tasks
public class TaskArchive extends RecyclerView.Adapter<TaskArchive.FinishedTaskViewHolder>
{
    public static class ArchiveFileLockException extends BaseFileLockException
    {
    }

    private static final String ARCHIVE_FILENAME = "TaskArchive.json";

    //The list of all finished task
    private ArrayList<ArchivedTask> mFinishedTasks;

    //The file to store the scheduler data
    private LockedFile mConfigFile;

    //The context for displaying the task list
    private Context mContext;

    //Creates a new task archive
    public TaskArchive()
    {
        mFinishedTasks = new ArrayList<>();

        mContext = null;
    }

    //Sets the folder to store the JSON config file
    public void setConfigFolder(String folder)
    {
        mConfigFile = new LockedFile(folder + "/" + ARCHIVE_FILENAME);
    }

    //Adds a task to the archive
    public void addTask(ArchivedTask task) throws ArchiveFileLockException
    {
        //Forever
        mFinishedTasks.add(task);
        notifyItemInserted(mFinishedTasks.size() - 1);
        notifyItemRangeChanged(mFinishedTasks.size() - 1, getItemCount());
        saveFinishedTasks();
    }

    public void waitLock()
    {
        while(!mConfigFile.lock())
        {
            Log.d("LOCK", "Can't lock the list config file!");
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    public boolean tryLock()
    {
        return mConfigFile.lock();
    }

    public void unlock()
    {
        mConfigFile.unlock();
    }

    //Loads finished tasks from JSON config file
    public void loadFinishedTasks() throws ArchiveFileLockException
    {
        if(!mConfigFile.isLocked())
        {
            throw new ArchiveFileLockException();
        }

        mFinishedTasks.clear();

        try
        {
            String fileContents = mConfigFile.read();
            JSONObject jsonObject = new JSONObject(fileContents);

            JSONArray tasksJsonArray = jsonObject.getJSONArray("FINISHED_TASKS");
            for(int i = 0; i < tasksJsonArray.length(); i++)
            {
                JSONObject taskObject = tasksJsonArray.optJSONObject(i);
                if(taskObject != null)
                {
                    ArchivedTask task = ArchivedTask.fromJSON(taskObject);
                    if(task != null)
                    {
                        mFinishedTasks.add(task);
                    }
                }
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        notifyDataSetChanged();
    }

    //Saves finished tasks in JSON config file
    public void saveFinishedTasks() throws ArchiveFileLockException
    {
        if(!mConfigFile.isLocked())
        {
            throw new ArchiveFileLockException();
        }

        try
        {
            JSONObject jsonObject    = new JSONObject();
            JSONArray tasksJsonArray = new JSONArray();

            for(ArchivedTask task: mFinishedTasks)
            {
                tasksJsonArray.put(task.toJSON());
            }

            jsonObject.put("FINISHED_TASKS", tasksJsonArray);

            mConfigFile.write(jsonObject.toString());
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public TaskArchive.FinishedTaskViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
    {
        mContext = viewGroup.getContext();

        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_finished_task_view, viewGroup, false);
        return new TaskArchive.FinishedTaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskArchive.FinishedTaskViewHolder taskViewHolder, int position)
    {
        taskViewHolder.mTextView.setText(mFinishedTasks.get(position).getName());

        taskViewHolder.mParentLayout.setOnClickListener(view -> Toast.makeText(mContext, mFinishedTasks.get(position).getDescription(), Toast.LENGTH_LONG).show());
    }

    @Override
    public int getItemCount()
    {
        return mFinishedTasks.size();
    }

    static class FinishedTaskViewHolder extends RecyclerView.ViewHolder
    {
        TextView mTextView;

        ConstraintLayout mParentLayout;

        public FinishedTaskViewHolder(View itemView)
        {
            super(itemView);

            mTextView = itemView.findViewById(R.id.textFinishedTaskName);

            mParentLayout = itemView.findViewById(R.id.layoutFinishedTaskView);
        }
    }
}
