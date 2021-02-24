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
public class ObjectiveArchiveCache
{
    public static final String ARCHIVE_FILENAME = "TaskArchive.json";

    //The list of all finished objectives
    private ArrayList<ArchivedObjective> mFinishedObjectives;

    //The view holder of the cached data
    private ArchiveCacheViewHolder mArchiveViewHolder;

    //Creates a new task archive
    public ObjectiveArchiveCache()
    {
        mFinishedObjectives = new ArrayList<>();

        mArchiveViewHolder = null;
    }

    public void attachToView(RecyclerView recyclerView)
    {
        mArchiveViewHolder = new ArchiveCacheViewHolder();
        recyclerView.setAdapter(mArchiveViewHolder);
    }

    //Adds an objective to the archive cache
    public boolean addObjective(ArchivedObjective objective)
    {
        //Forever
        mFinishedObjectives.add(objective);
        if(mArchiveViewHolder != null)
        {
            mArchiveViewHolder.notifyItemInserted(mFinishedObjectives.size() - 1);
            mArchiveViewHolder.notifyItemRangeChanged(mFinishedObjectives.size() - 1, mArchiveViewHolder.getItemCount());
        }

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
        if(mArchiveViewHolder != null)
        {
            mArchiveViewHolder.notifyDataSetChanged();
        }

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

    private class ArchiveCacheViewHolder extends RecyclerView.Adapter<ObjectiveArchiveCache.FinishedTaskViewHolder>
    {
        private Context mContext;

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
        public int getItemCount() {
            return mFinishedObjectives.size();
        }
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
