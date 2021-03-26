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

//The objective archive that contains all the finished objectives
public class ObjectiveArchiveCache
{
    public static final String ARCHIVE_FILENAME = "TaskArchive.json";

    //The list of all finished objectives
    private ArrayList<ArchivedObjective> mFinishedObjectives;

    //The view holder of the cached data
    private ArchiveCacheViewHolder mArchiveViewHolder;

    //Creates a new objective archive
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

            JSONArray objectiveJsonArray = jsonObject.getJSONArray("FINISHED_TASKS");
            for(int i = 0; i < objectiveJsonArray.length(); i++)
            {
                JSONObject objectiveObject = objectiveJsonArray.optJSONObject(i);
                if(objectiveObject != null)
                {
                    ArchivedObjective objective = ArchivedObjective.fromJSON(objectiveObject);
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
            JSONObject jsonObject        = new JSONObject();
            JSONArray objectiveJsonArray = new JSONArray();

            for(ArchivedObjective objective: mFinishedObjectives)
            {
                objectiveJsonArray.put(objective.toJSON());
            }

            jsonObject.put("FINISHED_TASKS", objectiveJsonArray);

            archiveWriteFile.write(jsonObject.toString());
        }
        catch(JSONException e)
        {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    private class ArchiveCacheViewHolder extends RecyclerView.Adapter<ObjectiveArchiveCache.FinishedObjectiveViewHolder>
    {
        private Context mContext;

        @NonNull
        @Override
        public ObjectiveArchiveCache.FinishedObjectiveViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
        {
            mContext = viewGroup.getContext();

            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_finished_objective_view, viewGroup, false);
            return new ObjectiveArchiveCache.FinishedObjectiveViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ObjectiveArchiveCache.FinishedObjectiveViewHolder objectiveViewHolder, int position)
        {
            objectiveViewHolder.mTextView.setText(mFinishedObjectives.get(position).getName());

            objectiveViewHolder.mParentLayout.setOnClickListener(view -> Toast.makeText(mContext, mFinishedObjectives.get(position).getDescription(), Toast.LENGTH_LONG).show());
        }

        @Override
        public int getItemCount() {
            return mFinishedObjectives.size();
        }
    }

    static class FinishedObjectiveViewHolder extends RecyclerView.ViewHolder
    {
        TextView mTextView;

        ConstraintLayout mParentLayout;

        public FinishedObjectiveViewHolder(View itemView)
        {
            super(itemView);

            mTextView = itemView.findViewById(R.id.textFinishedObjectiveName);

            mParentLayout = itemView.findViewById(R.id.layoutFinishedObjectiveView);
        }
    }
}
