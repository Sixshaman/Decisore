package com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.ObjectivePool;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.PoolElement;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.PoolElementViewHolder;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjective;
import com.sixshaman.advancedunforgetter.utils.ValueHolder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.*;

public class ObjectiveChain implements PoolElement
{
    //View holder
    private ChainViewHolder mChainViewHolder;

    //Objective chain id
    private long mId;

    //Objective chain name
    private String mName;

    //Objective chain description
    private String mDescription;

    //The objectives that this chain will provide one-by-one. Since Java doesn't have any non-deque Queue implementation, we will use ArrayDeque
    private LinkedList<ScheduledObjective> mObjectives;

    //The list of ids of all objectives once provided by the chain
    HashSet<Long> mObjectiveIdHistory;

    //Creates a new objective chain
    public ObjectiveChain(long id, String name, String description)
    {
        mId = id;

        mName        = name;
        mDescription = description;

        mObjectives         = new LinkedList<>();
        mObjectiveIdHistory = new HashSet<>();
    }

    public void attachToChainView(RecyclerView recyclerView)
    {
        mChainViewHolder = new ObjectiveChain.ChainViewHolder();
        recyclerView.setAdapter(mChainViewHolder);
    }

    //Adds an objective to the chain
    public void addObjectiveToChain(ScheduledObjective objective)
    {
        mObjectives.addLast(objective);
        mObjectiveIdHistory.add(objective.getId());

        if(mChainViewHolder != null)
        {
            mChainViewHolder.notifyItemInserted(mObjectives.size() - 1);
            mChainViewHolder.notifyItemRangeChanged(mObjectives.size() - 1, mObjectives.size());
        }
    }

    public boolean deleteObjectiveById(long objectiveId)
    {
        int indexToDelete = -1;
        for(int i = 0; i < mObjectives.size(); i++)
        {
            if(mObjectives.get(i).getId() == objectiveId)
            {
                indexToDelete = i;
                break;
            }
        }

        if(indexToDelete != -1)
        {
            mObjectives.remove(indexToDelete);

            if(mChainViewHolder != null)
            {
                mChainViewHolder.notifyItemRemoved(indexToDelete);
                mChainViewHolder.notifyItemRangeChanged(indexToDelete, mChainViewHolder.getItemCount());
            }

            return true;
        }

        return false;
    }

    @Override
    public String getName()
    {
        return mName;
    }

    @Override
    public String getDescription()
    {
        return mDescription;
    }

    public void setName(String chainName)
    {
        mName = chainName;
    }

    public void setDescription(String chainDescription)
    {
        mDescription = chainDescription;
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();

        try
        {
            result.put("Id", mId);

            result.put("Name",        mName);
            result.put("Description", mDescription);

            JSONArray objectivesArray = new JSONArray();
            for(ScheduledObjective objective: mObjectives)
            {
                objectivesArray.put(objective.toJSON());
            }

            result.put("Objectives", objectivesArray);

            JSONArray idHistoryArray = new JSONArray();
            for(Long objectiveId: mObjectiveIdHistory)
            {
                idHistoryArray.put(objectiveId.longValue());
            }

            result.put("ObjectiveHistory", idHistoryArray);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public int getObjectiveCount()
    {
        return mObjectives.size();
    }

    public ScheduledObjective getFirstObjective()
    {
        return mObjectives.getFirst();
    }

    public long getMaxObjectiveId()
    {
        if(mObjectives == null)
        {
            return 0;
        }

        long maxId = -1;
        for(ScheduledObjective objective: mObjectives)
        {
            long objectiveId = objective.getId();
            if(objectiveId > maxId)
            {
                maxId = objectiveId;
            }
        }

        return maxId;
    }

    public boolean containedObjective(long objectiveId)
    {
        for(Long historicId: mObjectiveIdHistory)
        {
            if(historicId == objectiveId)
            {
                return true;
            }
        }

        return false;
    }

    public ScheduledObjective getObjectiveById(long objectiveId)
    {
        for(ScheduledObjective objective: mObjectives)
        {
            if(objective.getId() == objectiveId)
            {
                return objective;
            }
        }

        return null;
    }

    public boolean putBack(ScheduledObjective objective)
    {
        if(mObjectives != null && !mObjectives.isEmpty())
        {
            ScheduledObjective firstObjective = mObjectives.getFirst();
            if(firstObjective.getId() == objective.getId())
            {
                firstObjective.rescheduleUnregulated(objective.getScheduledEnlistDate());
            }
            else
            {
                mObjectives.addFirst(objective);
                mObjectiveIdHistory.add(objective.getId()); //Just in case
            }

            return true;
        }

        return false;
    }

    public EnlistedObjective obtainObjective(LocalDateTime referenceTime)
    {
        if(mObjectives != null && !mObjectives.isEmpty())
        {
            if(mObjectives.getFirst().isAvailable(referenceTime))
            {
                return mObjectives.removeFirst().toEnlisted(referenceTime);
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    public boolean isEmpty()
    {
        return mObjectives.isEmpty();
    }

    public long getId()
    {
        return mId;
    }

    @Override
    public boolean isPaused()
    {
        return false;
    }

    @Override
    public boolean isAvailable(LocalDateTime referenceTime)
    {
        if(isPaused())
        {
            return false;
        }

        if(mObjectives.isEmpty())
        {
            return true;
        }
        else
        {
            ScheduledObjective firstObjective = mObjectives.getFirst();
            return firstObjective.isAvailable(referenceTime);
        }
    }

    @Override
    public String getElementName()
    {
        return "ObjectiveChain";
    }

    private class ChainViewHolder extends RecyclerView.Adapter<ChainElementViewHolder>
    {
        private Context mContext;

        @NonNull
        @Override
        public ChainElementViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
        {
            mContext = viewGroup.getContext();

            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_pool_element_view, viewGroup, false);
            return new ChainElementViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChainElementViewHolder elementViewHolder, int position)
        {
            ScheduledObjective objective = mObjectives.get(position);

            elementViewHolder.mTextView.setText(objective.getName());

            elementViewHolder.setSourceMetadata(objective);
        }

        @Override
        public int getItemCount()
        {
            return mObjectives.size();
        }
    }
}
