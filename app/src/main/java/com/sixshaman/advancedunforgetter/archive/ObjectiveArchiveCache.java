package com.sixshaman.advancedunforgetter.archive;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.utils.LockedReadFile;
import com.sixshaman.advancedunforgetter.utils.LockedWriteFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

//The task archive that contains all the finished tasks
public class ObjectiveArchiveCache extends RecyclerView.Adapter<ObjectiveArchiveCache.FinishedTaskViewHolder>
{
    public static final String ARCHIVE_FILENAME = "TaskArchive.json";

    //The list of all finished objectives
    private ArrayList<ArchivedObjective> mFinishedObjectives;

    //The context for displaying the finished objective list
    private Context mContext;

    //Creates a new task archive
    public ObjectiveArchiveCache()
    {
        mFinishedObjectives = new ArrayList<>();

        mContext = null;
    }

    //Adds an objective to the archive cache
    public boolean addObjective(ArchivedObjective objective)
    {
        //Forever
        mFinishedObjectives.add(objective);
        notifyItemInserted(mFinishedObjectives.size() - 1);
        notifyItemRangeChanged(mFinishedObjectives.size() - 1, getItemCount());

        return true;
    }

    //Loads finished tasks from JSON config file
    public boolean invalidate(LockedReadFile archiveReadFile)
    {
        ArrayList<ArchivedObjective> finishedObjectives = new ArrayList<>();

        try
        {
            String fileContents = archiveReadFile.read();
            JSONObject jsonObject = new JSONObject(fileContents);

            JSONArray tasksJsonArray = jsonObject.getJSONArray("FINISHED_TASKS");
            for(int i = 0; i < tasksJsonArray.length(); i++)
            {
                JSONObject taskObject = tasksJsonArray.optJSONObject(i);
                if(taskObject != null)
                {
                    ArchivedObjective objective = ArchivedObjective.fromJSON(taskObject);
                    if(objective != null)
                    {
                        finishedObjectives.add(objective);
                    }
                }
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
            return false;
        }

        mFinishedObjectives = finishedObjectives;
        notifyDataSetChanged();

        return true;
    }

    //Saves all objectives to the archive file (cache flush)
    public boolean flush(LockedWriteFile archiveWriteFile)
    {
        try
        {
            JSONObject jsonObject    = new JSONObject();
            JSONArray tasksJsonArray = new JSONArray();

            for(ArchivedObjective task: mFinishedObjectives)
            {
                tasksJsonArray.put(task.toJSON());
            }

            jsonObject.put("FINISHED_TASKS", tasksJsonArray);

            archiveWriteFile.write(jsonObject.toString());
        }
        catch(JSONException e)
        {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    @NonNull
    @Override
    public ObjectiveArchiveCache.FinishedTaskViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
    {
        mContext = viewGroup.getContext();

        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_finished_task_view, viewGroup, false);
        return new ObjectiveArchiveCache.FinishedTaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ObjectiveArchiveCache.FinishedTaskViewHolder taskViewHolder, int position)
    {
        taskViewHolder.mTextView.setText(mFinishedObjectives.get(position).getName());

        taskViewHolder.mParentLayout.setOnClickListener(view -> Toast.makeText(mContext, mFinishedObjectives.get(position).getDescription(), Toast.LENGTH_LONG).show());
    }

    @Override
    public int getItemCount()
    {
        return mFinishedObjectives.size();
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
