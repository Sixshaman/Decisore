package com.sixshaman.decisore.list;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Toast;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.utils.LockedReadFile;
import com.sixshaman.decisore.utils.LockedWriteFile;
import com.sixshaman.decisore.utils.ParseUtils;
import com.sixshaman.decisore.utils.TransactionDispatcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.*;

public class ObjectiveListCache
{
    public enum InvalidateResult
    {
        INVALIDATE_OK,          //Success
        INVALIDATE_VERSION_1_0, //Old version, need to recalculate ids
        INVALIDATE_ERROR        //Unknown error
    }

    static final int LIST_VERSION_1_0 = 1000;
    static final int LIST_VERSION_1_1 = 1001;

    public static final String LIST_FILENAME = "TaskList.json";

    //All objectives to be done for today, sorted by ids. Or tomorrow. Or within a year. It's up to the user to decide
    private ArrayList<EnlistedObjective> mEnlistedObjectives;

    //The view holder of the cached data
    private ObjectiveListCache.ListCacheViewHolder mListViewHolder;

    //The listener for objective count changes
    private ListObjectiveCountListener mObjectiveCountListener;

    //Constructs a new objective list
    public ObjectiveListCache()
    {
        mEnlistedObjectives = new ArrayList<>();

        mListViewHolder         = null;
        mObjectiveCountListener = null;
    }

    public void attachToView(RecyclerView recyclerView)
    {
        mListViewHolder = new ObjectiveListCache.ListCacheViewHolder();
        recyclerView.setAdapter(mListViewHolder);
    }

    public void setObjectiveCountListener(ListObjectiveCountListener listener)
    {
        mObjectiveCountListener = listener;
    }

    //Adds an objective to the list
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean addObjective(EnlistedObjective objective)
    {
        if (objective == null)
        {
            return false;
        }

        int addPosition;
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
                for(int i = mEnlistedObjectives.size() - 1; i > insertIndex; i--)
                {
                    mEnlistedObjectives.set(i, mEnlistedObjectives.get(i - 1));
                }

                mEnlistedObjectives.set(insertIndex, objective);
                addPosition = insertIndex;
            }
            else
            {
                //OH NO! THE OBJECTIVE ALREADY EXISTS! WE CAN LOSE THIS OBJECTIVE! STOP EVERYTHING, DON'T LET IT SAVE
                //Or not, lol. It's oki, the cache will be invalidated back and nothing is gonna be lost
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

        if(mObjectiveCountListener != null)
        {
            mObjectiveCountListener.onListObjectiveCountChanged(mEnlistedObjectives.size());
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

        if(mObjectiveCountListener != null)
        {
            mObjectiveCountListener.onListObjectiveCountChanged(mEnlistedObjectives.size());
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
    public InvalidateResult invalidate(LockedReadFile listReadFile)
    {
        ArrayList<EnlistedObjective> enlistedObjectives = new ArrayList<>();

        try
        {
            String fileContents = listReadFile.read();
            JSONObject jsonObject = new JSONObject(fileContents);

            int version = jsonObject.optInt("VERSION", LIST_VERSION_1_0);
            if(version <= LIST_VERSION_1_0)
            {
                return InvalidateResult.INVALIDATE_VERSION_1_0;
            }

            JSONArray objectivesJsonArray = jsonObject.getJSONArray("TASKS");
            for(int i = 0; i < objectivesJsonArray.length(); i++)
            {
                JSONObject objectiveObject = objectivesJsonArray.optJSONObject(i);
                if(objectiveObject != null)
                {
                    EnlistedObjective objective = EnlistedObjective.fromJSON(objectiveObject);
                    if(objective != null)
                    {
                        enlistedObjectives.add(objective);
                    }
                }
            }

            mEnlistedObjectives = enlistedObjectives;
        }
        catch(JSONException e)
        {
            e.printStackTrace();
            return InvalidateResult.INVALIDATE_ERROR;
        }

        mEnlistedObjectives.sort(Comparator.comparingLong(EnlistedObjective::getId));

        if(mListViewHolder != null)
        {
            mListViewHolder.notifyDataSetChanged();
        }

        if(mObjectiveCountListener != null)
        {
            mObjectiveCountListener.onListObjectiveCountChanged(mEnlistedObjectives.size());
        }

        return InvalidateResult.INVALIDATE_OK;
    }

    //Saves objectives to JSON config file
    public boolean flush(LockedWriteFile listWriteFile)
    {
        try
        {
            JSONObject jsonObject          = new JSONObject();
            JSONArray  objectivesJsonArray = new JSONArray();

            for(EnlistedObjective objective: mEnlistedObjectives)
            {
                objectivesJsonArray.put(objective.toJSON());
            }

            jsonObject.put("TASKS", objectivesJsonArray);

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

    public int getObjectiveCount()
    {
        return mEnlistedObjectives.size();
    }

    public HashSet<Long> constructBlockingIds()
    {
        HashSet<Long> blockingIds = new HashSet<>();
        for(EnlistedObjective objective: mEnlistedObjectives)
        {
            blockingIds.add(objective.getId());
        }

        return blockingIds;
    }

    private class ListCacheViewHolder extends RecyclerView.Adapter<ObjectiveViewHolder>
    {
        private Context mContext;

        @NonNull
        @Override
        public ObjectiveViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
        {
            mContext = viewGroup.getContext();

            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_objectives_view, viewGroup, false);
            return new ObjectiveViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ObjectiveViewHolder objectiveViewHolder, int position)
        {
            objectiveViewHolder.mTextView.setText(mEnlistedObjectives.get(position).getName());

            objectiveViewHolder.mCheckbox.setOnClickListener(view ->
            {
                EnlistedObjective objectiveToRemove = mEnlistedObjectives.get(position);

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(view.getContext()).getApplicationContext());
                String dayStartTimeString = sharedPreferences.getString("day_start_time", "6");
                int dayStartTime = ParseUtils.parseInt(dayStartTimeString, 6);

                String configFolder = Objects.requireNonNull(mContext.getExternalFilesDir("/app")).getAbsolutePath();

                TransactionDispatcher transactionDispatcher = new TransactionDispatcher(configFolder);
                transactionDispatcher.setListCache(ObjectiveListCache.this);

                transactionDispatcher.finishObjectiveTransaction(objectiveToRemove, LocalDateTime.now());
                transactionDispatcher.updateObjectiveListTransaction(LocalDateTime.now(), dayStartTime);
            });

            objectiveViewHolder.mCheckbox.setChecked(false);

            objectiveViewHolder.setObjectiveMetadata(ObjectiveListCache.this, ObjectiveListCache.this.mEnlistedObjectives.get(position).getId());
        }

        @Override
        public int getItemCount()
        {
            return mEnlistedObjectives.size();
        }
    }
}
