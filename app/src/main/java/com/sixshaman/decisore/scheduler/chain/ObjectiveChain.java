package com.sixshaman.decisore.scheduler.chain;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.list.EnlistedObjective;
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.decisore.scheduler.pool.PoolElement;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ObjectiveChain implements PoolElement
{
    //View holder
    private ChainViewHolder mChainViewHolder;

    //Objective chain id
    private final long mId;

    //Objective chain name
    private String mName;

    //Objective chain description
    private String mDescription;

    //Is this chain active?
    private boolean mIsActive;

    //Does this chain get deleted immediately after finishing every objective?
    private boolean mIsAutoDelete;

    //The minimum frequency at which the chain can provide objectives (0 specifies the "instant" chain)
    private Duration mProduceFrequency;

    //The date-time at which the last objective was provided (valid only for non-instant chains)
    private LocalDateTime mLastUpdate;

    //Used to counter the problem of scheduling the objective for tomorrow and thus shifting the update date
    private int mInstantCount;

    //The objectives that this chain will provide one-by-one. Since Java doesn't have any non-deque Queue implementation, we will use ArrayDeque
    private final LinkedList<ScheduledObjective> mObjectives;

    //The list of ids of all objectives once provided by the chain
    final HashSet<Long> mBoundObjectives;

    //Creates a new objective chain
    public ObjectiveChain(long id, String name, String description)
    {
        mId = id;

        mName        = name;
        mDescription = description;

        mObjectives      = new LinkedList<>();
        mBoundObjectives = new HashSet<>();

        mIsActive = true;

        mIsAutoDelete = false;

        mLastUpdate       = LocalDateTime.MIN; //FAR PAST, A LONG LONG TIME AGO.
        mProduceFrequency = Duration.ZERO;     //Default
        mInstantCount     = 0;
    }

    public void attachToChainView(RecyclerView recyclerView, ObjectiveSchedulerCache schedulerCache)
    {
        mChainViewHolder = new ObjectiveChain.ChainViewHolder(schedulerCache);
        recyclerView.setAdapter(mChainViewHolder);

        mChainViewHolder.notifyDataSetChanged();
    }

    //Adds an objective to the chain
    public void addObjectiveToChain(ScheduledObjective objective)
    {
        mObjectives.addLast(objective);
        mBoundObjectives.add(objective.getId());

        if(mChainViewHolder != null)
        {
            mChainViewHolder.notifyItemInserted(mObjectives.size() - 1);
            mChainViewHolder.notifyItemRangeChanged(mObjectives.size() - 1, mObjectives.size());
        }
    }

    //Adds an objective to the front of the chain
    public void addObjectiveToChainFront(ScheduledObjective objective)
    {
        mObjectives.addFirst(objective);
        mBoundObjectives.add(objective.getId());

        if(mChainViewHolder != null)
        {
            mChainViewHolder.notifyItemInserted(0);
            mChainViewHolder.notifyItemRangeChanged(0, mObjectives.size());
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
    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();

        try
        {
            result.put("Id", mId);

            result.put("Name",        mName);
            result.put("Description", mDescription);

            result.put("IsActive", Boolean.toString(mIsActive));

            result.put("IsAutoDelete", Boolean.toString(mIsAutoDelete));

            result.put("ProduceFrequency", Long.toString(mProduceFrequency.toMinutes()));

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:nnnnnnnnn");
            String lastUpdateString = dateTimeFormatter.format(mLastUpdate);
            result.put("LastUpdate", lastUpdateString);

            result.put("InstantCount", mInstantCount);

            JSONArray objectivesArray = new JSONArray();
            for(ScheduledObjective objective: mObjectives)
            {
                objectivesArray.put(objective.toJSON());
            }

            result.put("Objectives", objectivesArray);

            JSONArray idHistoryArray = new JSONArray();
            for(Long objectiveId: mBoundObjectives)
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

    @Override
    public boolean isAvailable(HashSet<Long> blockingObjectiveIds, LocalDateTime referenceTime)
    {
        if(isPaused())
        {
            return false;
        }

        if(!mProduceFrequency.isZero() && mInstantCount == 0 && !mLastUpdate.plus(mProduceFrequency).isBefore(referenceTime))
        {
            return false;
        }

        for(Long boundId: mBoundObjectives)
        {
            if(blockingObjectiveIds.contains(boundId))
            {
                return false;
            }
        }

        if(mObjectives.isEmpty())
        {
            return false;
        }
        else
        {
            ScheduledObjective firstObjective = mObjectives.getFirst();
            return firstObjective.isAvailable(blockingObjectiveIds, referenceTime);
        }
    }

    @Override
    public EnlistedObjective obtainEnlistedObjective(HashSet<Long> blockingObjectiveIds, LocalDateTime referenceTime, int dayStartHour)
    {
        if(!isAvailable(blockingObjectiveIds, referenceTime))
        {
            return null;
        }

        ScheduledObjective firstChainObjective = mObjectives.getFirst();

        EnlistedObjective enlistedObjective = firstChainObjective.obtainEnlistedObjective(blockingObjectiveIds, referenceTime, dayStartHour);
        if(enlistedObjective == null)
        {
            return null;
        }

        if(!firstChainObjective.isValid())
        {
            mObjectives.removeFirst();
        }

        if(mChainViewHolder != null)
        {
            mChainViewHolder.notifyItemRemoved(0);
            mChainViewHolder.notifyItemRangeChanged(0, mObjectives.size() - 1);
        }

        if(!mProduceFrequency.isZero() && mInstantCount > 0)
        {
            mInstantCount--;
        }
        else if(!mProduceFrequency.isZero())
        {
            mLastUpdate = referenceTime;
        }

        return enlistedObjective;
    }

    @Override
    public void updateDayStart(LocalDateTime referenceTime, int oldStartHour, int newStartHour)
    {
        for(ScheduledObjective objective: mObjectives)
        {
            objective.updateDayStart(referenceTime, oldStartHour, newStartHour);
        }
    }

    public ScheduledObjective getFirstObjective()
    {
        if(mObjectives.isEmpty())
        {
            return null;
        }

        return mObjectives.getFirst();
    }

    public long getMaxObjectiveId()
    {
        long maxId = -1;
        for(ScheduledObjective objective: mObjectives)
        {
            long objectiveId = objective.getId();
            if(objectiveId > maxId)
            {
                maxId = objectiveId;
            }
        }

        for(Long boundId: mBoundObjectives)
        {
            if(boundId > maxId)
            {
                maxId = boundId;
            }
        }

        return maxId;
    }

    public boolean containedObjective(long objectiveId)
    {
        for(Long historicId: mBoundObjectives)
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
        if(!mObjectives.isEmpty())
        {
            ScheduledObjective firstObjective = mObjectives.getFirst();
            if(firstObjective.getId() == objective.getId())
            {
                firstObjective.rescheduleUnregulated(objective.getScheduledEnlistDate());
            }
            else
            {
                mObjectives.addFirst(objective);
                mBoundObjectives.add(objective.getId()); //Just in case
            }

            if(!mProduceFrequency.isZero())
            {
                mInstantCount++;
            }

            return true;
        }

        return false;
    }

    //Marks the objective such that it belongs to this chain (without adding it to chain)
    public void bindObjectiveToChain(long objectiveId)
    {
        mBoundObjectives.add(objectiveId);
    }

    public boolean isNotEmpty()
    {
        return !mObjectives.isEmpty();
    }

    public long getId()
    {
        return mId;
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

    @SuppressWarnings("unused")
    public int getObjectiveCount()
    {
        return mObjectives.size();
    }

    public void setName(String chainName)
    {
        mName = chainName;
    }

    public void setDescription(String chainDescription)
    {
        mDescription = chainDescription;
    }

    public void setProduceFrequency(Duration produceFrequency)
    {
        mProduceFrequency = produceFrequency;
    }

    @Override
    public boolean isPaused()
    {
        return !mIsActive;
    }

    @Override
    public void setPaused(boolean paused)
    {
        mIsActive = !paused;
    }

    public void setAutoDelete(boolean autoDelete)
    {
        mIsAutoDelete = autoDelete;
    }

    @Override
    public boolean isValid()
    {
        return !mIsAutoDelete || (mBoundObjectives.isEmpty() || !mObjectives.isEmpty()); //Don't delete newly created chains (they have 0 bound objectives)
    }

    @Override
    public String getElementName()
    {
        return "ObjectiveChain";
    }

    //Only to be used for JSON loading, consider it private
    void setLastUpdate(LocalDateTime lastUpdate)
    {
        mLastUpdate = lastUpdate;
    }

    //Only to be used for JSON loading, consider it private
    void setInstantCount(int instantCount)
    {
        mInstantCount = instantCount;
    }

    private class ChainViewHolder extends RecyclerView.Adapter<ChainElementViewHolder>
    {
        private final ObjectiveSchedulerCache mSchedulerCache;

        ChainViewHolder(ObjectiveSchedulerCache schedulerCache)
        {
            mSchedulerCache = schedulerCache;
        }

        @NonNull
        @Override
        public ChainElementViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
        {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_scheduled_objective_view, viewGroup, false);
            return new ChainElementViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChainElementViewHolder elementViewHolder, int position)
        {
            ScheduledObjective objective = mObjectives.get(position);

            elementViewHolder.mTextView.setText(objective.getName());

            elementViewHolder.setSourceMetadata(mSchedulerCache, objective);
        }

        @Override
        public int getItemCount()
        {
            return mObjectives.size();
        }
    }
}
