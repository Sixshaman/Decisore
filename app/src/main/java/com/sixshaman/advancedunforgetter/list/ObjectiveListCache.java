package com.sixshaman.advancedunforgetter.list;

import android.content.Context;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Toast;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.utils.LockedReadFile;
import com.sixshaman.advancedunforgetter.utils.LockedWriteFile;
import com.sixshaman.advancedunforgetter.utils.TransactionDispatcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public class ObjectiveListCache
{
    public static final String LIST_FILENAME = "TaskList.json";

    //All tasks to be done for today, sorted by ids. Or tomorrow. Or within a year. It's up to the user to decide
    private ArrayList<EnlistedObjective> mEnlistedObjectives;

    //The view holder of the cached data
    private ObjectiveListCache.ListCacheViewHolder mListViewHolder;

    //Constructs a new task list
    public ObjectiveListCache()
    {
        mEnlistedObjectives = new ArrayList<>();
    }

    public void attachToView(RecyclerView recyclerView)
    {
        mListViewHolder = new ObjectiveListCache.ListCacheViewHolder();
        recyclerView.setAdapter(mListViewHolder);
    }

    //Adds an objective to the list
    public boolean addObjective(EnlistedObjective objective)
    {
        if (objective == null)
        {
            return false;
        }

        int addPosition = -1;
        if(mEnlistedObjectives.isEmpty()) //Special case for the empty list
        {
            mEnlistedObjectives.add(objective);
            addPosition = mEnlistedObjectives.size() - 1;
        }
        else if(mEnlistedObjectives.get(mEnlistedObjectives.size() - 1).getId() < objective.getId()) //Special case for the trivial insertion that will keep the list sorted anyway
        {
            mEnlistedObjectives.add(objective);
            addPosition = mEnlistedObjectives.size() - 1;
        }
        else
        {
            int index = Collections.binarySearch(mEnlistedObjectives, objective.getId());
            if(index < 0)
            {
                //Insert at selected position
                int insertIndex = -(index + 1);

                //Shift elements to the right
                mEnlistedObjectives.add(null);
                for(int i = mEnlistedObjectives.size() - 1; i > insertIndex; i++)
                {
                    mEnlistedObjectives.set(i, mEnlistedObjectives.get(i - 1));
                }

                mEnlistedObjectives.set(insertIndex, objective);
                addPosition = insertIndex;
            }
            else
            {
                //OH NO! THE TASK ALREADY EXISTS! WE CAN LOSE THIS TASK! STOP EVERYTHING, DON'T LET IT SAVE
                //Or not, lol
                //throw new RuntimeException("Task already exists");
                return false;
            }
        }

        if (addPosition == -1)
        {
            return false;
        }

        if(mListViewHolder != null)
        {
            mListViewHolder.notifyItemInserted(addPosition);
            mListViewHolder.notifyItemRangeChanged(addPosition, mListViewHolder.getItemCount());
        }

        return true;
    }

    public boolean removeObjective(EnlistedObjective objective)
    {
        int index = Collections.binarySearch(mEnlistedObjectives, objective.getId());
        if(index >= 0)
        {
            mEnlistedObjectives.remove(index);
        }
        else
        {
            return false;
        }

        if(mListViewHolder != null)
        {
            mListViewHolder.notifyItemRemoved(index);
            mListViewHolder.notifyItemRangeChanged(index, mListViewHolder.getItemCount());
        }

        return true;
    }

    public boolean editObjectiveName(long objectiveId, String name, String description)
    {
        int index = Collections.binarySearch(mEnlistedObjectives, objectiveId);
        if(index >= 0)
        {
            mEnlistedObjectives.get(index).setName(name);
            mEnlistedObjectives.get(index).setDescription(description);
        }
        else
        {
            return false;
        }

        if(mListViewHolder != null)
        {
            mListViewHolder.notifyItemChanged(index);
        }

        return true;
    }

    public EnlistedObjective getObjective(long objectiveId)
    {
        //Special case for empty list
        if(mEnlistedObjectives.size() == 0)
        {
            return null;
        }

        //Special case: since mTasks is sorted by id, then last element having lesser id means the task is not in mTasks. This is a pretty common case.
        if(mEnlistedObjectives.get(mEnlistedObjectives.size() - 1).getId() < objectiveId)
        {
            return null;
        }

        //The mTasks list is sorted by id, so find the index with binary search
        int index = (Collections.binarySearch(mEnlistedObjectives, objectiveId));
        if(index < 0)
        {
            return null;
        }

        return mEnlistedObjectives.get(index);
    }

    //Loads objective from JSON config file
    public boolean invalidate(LockedReadFile listReadFile)
    {
        ArrayList<EnlistedObjective> enlistedObjectives = new ArrayList<>();

        try
        {
            String fileContents = listReadFile.read();
            JSONObject jsonObject = new JSONObject(fileContents);

            JSONArray tasksJsonArray = jsonObject.getJSONArray("TASKS");
            for(int i = 0; i < tasksJsonArray.length(); i++)
            {
                JSONObject taskObject = tasksJsonArray.optJSONObject(i);
                if(taskObject != null)
                {
                    EnlistedObjective objective = EnlistedObjective.fromJSON(taskObject);
                    if(objective != null)
                    {
                        enlistedObjectives.add(objective);
                    }
                }
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
            return false;
        }

        mEnlistedObjectives = enlistedObjectives;

        mEnlistedObjectives.sort(Comparator.comparingLong(EnlistedObjective::getId));

        if(mListViewHolder != null)
        {
            mListViewHolder.notifyDataSetChanged();
        }

        return true;
    }

    //Saves objectives to JSON config file
    public boolean flush(LockedWriteFile listWriteFile)
    {
        try
        {
            JSONObject jsonObject    = new JSONObject();
            JSONArray tasksJsonArray = new JSONArray();

            for(EnlistedObjective objective: mEnlistedObjectives)
            {
                tasksJsonArray.put(objective.toJSON());
            }

            jsonObject.put("TASKS", tasksJsonArray);

            listWriteFile.write(jsonObject.toString());
        }
        catch(JSONException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    //Returns the largest id for a stored objective
    //Since objectives are sorted by id, it's gonna be the last objective
    public long getMaxObjectiveId()
    {
        if(mEnlistedObjectives.isEmpty())
        {
            return -1;
        }
        else
        {
            return mEnlistedObjectives.get(mEnlistedObjectives.size() - 1).getId();
        }
    }

    private class ListCacheViewHolder extends RecyclerView.Adapter<ObjectiveViewHolder>
    {
        private Context mContext;

        @NonNull
        @Override
        public ObjectiveViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
        {
            mContext = viewGroup.getContext();

            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_task_view, viewGroup, false);
            return new ObjectiveViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ObjectiveViewHolder taskViewHolder, int position)
        {
            taskViewHolder.mTextView.setText(mEnlistedObjectives.get(position).getName());

            taskViewHolder.mCheckbox.setOnClickListener(view ->
            {
                String configFolder = Objects.requireNonNull(mContext.getExternalFilesDir("/app")).getAbsolutePath();

                EnlistedObjective objectiveToRemove = mEnlistedObjectives.get(position);

                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.setListCache(ObjectiveListCache.this);

                transactionDispatcher.finishObjectiveTransaction(configFolder, objectiveToRemove, LocalDateTime.now());
            });

            taskViewHolder.mParentLayout.setOnClickListener(view -> Toast.makeText(mContext, mEnlistedObjectives.get(position).getDescription(), Toast.LENGTH_LONG).show());

            taskViewHolder.mCheckbox.setChecked(false);

            taskViewHolder.setObjectiveMetadata(ObjectiveListCache.this, ObjectiveListCache.this.mEnlistedObjectives.get(position).getId());
        }

        @Override
        public int getItemCount()
        {
            return mEnlistedObjectives.size();
        }
    }
}
