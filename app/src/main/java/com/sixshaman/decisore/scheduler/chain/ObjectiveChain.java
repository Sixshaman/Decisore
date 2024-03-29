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
import com.sixshaman.decisore.utils.TwoSidedArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ObjectiveChain extends PoolElement
{
    //View holder
    private ChainViewHolder mChainViewHolder;

    //Does this chain get deleted immediately after finishing every objective?
    private boolean mIsAutoDelete;

    //Can this chain produce objectives that are supposed to be finished yesterday and earlier?
    private boolean mIsUnstoppable;

    //The minimum frequency at which the chain can provide objectives (0 specifies the "instant" chain)
    private Duration mProduceFrequency;

    //The date-time at which the last objective was provided (valid only for non-instant chains)
    private LocalDateTime mLastUpdate;

    //Used to counter the problem of scheduling the objective for tomorrow and thus shifting the update date
    private int mInstantCount;

    //The objectives that this chain will provide one-by-one. Since Java doesn't have any non-deque Queue implementation, we will use ArrayDeque
    private final TwoSidedArrayList<ScheduledObjective> mObjectives;

    //The list of ids of all objectives once provided by the chain
    final HashSet<Long> mBoundObjectives;

    //Creates a new objective chain
    public ObjectiveChain(long id, String name, String description)
    {
        super(id, name, description);

        mObjectives      = new TwoSidedArrayList<>();
        mBoundObjectives = new HashSet<>();

        mIsAutoDelete  = false;
        mIsUnstoppable = false;

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
        mObjectives.addBack(objective);
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
        mObjectives.addFront(objective);
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
            result.put("Id",       getId());
            result.put("ParentId", getParentId());

            result.put("Name",        getName());
            result.put("Description", getDescription());

            result.put("IsActive", Boolean.toString(!isPaused()));

            result.put("IsAutoDelete",  Boolean.toString(mIsAutoDelete));
            result.put("IsUnstoppable", Boolean.toString(mIsUnstoppable));

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

    public static ObjectiveChain fromJSON(JSONObject jsonObject)
    {
        try
        {
            long id       = jsonObject.getLong("Id");
            long parentId = jsonObject.getLong("ParentId");

            String name        = jsonObject.getString("Name");
            String description = jsonObject.getString("Description");

            String isActiveString = jsonObject.getString("IsActive");

            String isAutoDeleteString  = jsonObject.optString("IsAutoDelete");
            String isUnstoppableString = jsonObject.optString("IsUnstoppable");

            String produceFrequencyString = jsonObject.optString("ProduceFrequency");
            String lastProducedDateString = jsonObject.optString("LastUpdate");

            int instantCount = jsonObject.optInt("InstantCount", 0);

            ObjectiveChain objectiveChain = new ObjectiveChain(id, name, description);
            objectiveChain.setParentId(parentId);

            JSONArray objectivesJsonArray = jsonObject.getJSONArray("Objectives");
            for(int i = 0; i < objectivesJsonArray.length(); i++)
            {
                JSONObject objectiveObject = objectivesJsonArray.optJSONObject(i);
                if(objectiveObject != null)
                {
                    ScheduledObjective objective = ScheduledObjective.fromJSON(objectiveObject);
                    if(objective != null)
                    {
                        objectiveChain.addObjectiveToChain(objective);
                    }
                }
            }

            JSONArray idHistoryArray = jsonObject.getJSONArray("ObjectiveHistory");
            for(int i = 0; i < idHistoryArray.length(); i++)
            {
                long objectiveId = idHistoryArray.optLong(i, -1);
                if(objectiveId != -1)
                {
                    objectiveChain.mBoundObjectives.add(objectiveId);
                }
            }

            objectiveChain.setPaused(!isActiveString.isEmpty()           && isActiveString.equalsIgnoreCase("false"));
            objectiveChain.setAutoDelete(!isAutoDeleteString.isEmpty()   && isAutoDeleteString.equalsIgnoreCase("true"));
            objectiveChain.setUnstoppable(!isUnstoppableString.isEmpty() && isUnstoppableString.equalsIgnoreCase("true"));

            if(!produceFrequencyString.isEmpty())
            {
                try
                {
                    long produceFrequencyMinutes = Long.parseLong(produceFrequencyString);
                    Duration produceFrequency = Duration.ofMinutes(produceFrequencyMinutes);

                    objectiveChain.setProduceFrequency(produceFrequency);

                    if(!lastProducedDateString.isEmpty())
                    {
                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:nnnnnnnnn");
                        LocalDateTime lastProducedDate = LocalDateTime.parse(lastProducedDateString, dateTimeFormatter);

                        objectiveChain.setLastUpdate(lastProducedDate);
                    }

                    objectiveChain.setInstantCount(instantCount);
                }
                catch(NumberFormatException | DateTimeParseException e)
                {
                    e.printStackTrace();
                }
            }

            return objectiveChain;
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean isAvailable(HashSet<Long> blockingObjectiveIds, LocalDateTime referenceTime, int dayStartHour)
    {
        if(isPaused())
        {
            return false;
        }

        if(!mLastUpdate.equals(LocalDateTime.MIN))
        {
            LocalDateTime nextUpdate = mLastUpdate.minusHours(dayStartHour).truncatedTo(ChronoUnit.DAYS).plusHours(dayStartHour).plus(mProduceFrequency).minusHours(dayStartHour).truncatedTo(ChronoUnit.DAYS).plusHours(dayStartHour);
            if(!mProduceFrequency.isZero() && mInstantCount == 0 && !nextUpdate.isBefore(referenceTime))
            {
                return false;
            }
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
            ScheduledObjective firstObjective = mObjectives.getFront();
            return firstObjective.isAvailable(blockingObjectiveIds, referenceTime, dayStartHour);
        }
    }

    @Override
    public EnlistedObjective obtainEnlistedObjective(HashSet<Long> ignoredObjectiveIds, LocalDateTime referenceTime, int dayStartHour)
    {
        if(!isAvailable(ignoredObjectiveIds, referenceTime, dayStartHour))
        {
            return null;
        }

        ScheduledObjective firstChainObjective = mObjectives.getFront();

        EnlistedObjective enlistedObjective = firstChainObjective.obtainEnlistedObjective(ignoredObjectiveIds, referenceTime, dayStartHour);
        if(enlistedObjective == null)
        {
            return null;
        }

        if(!firstChainObjective.isValid())
        {
            mObjectives.removeFront();
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
            if(mIsUnstoppable)
            {
                if(mLastUpdate.equals(LocalDateTime.MIN)) //This is the first objective produced by the chain (after making it periodic)
                {
                    mLastUpdate = enlistedObjective.getCreatedDate();
                }
                else
                {
                    mLastUpdate = mLastUpdate.minusHours(dayStartHour).truncatedTo(ChronoUnit.DAYS).plusHours(dayStartHour).plusHours(mProduceFrequency.toHours()).minusHours(dayStartHour).truncatedTo(ChronoUnit.DAYS).plusHours(dayStartHour);
                }
            }
            else
            {
                mLastUpdate = referenceTime;
            }
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

    @Override
    public boolean isRelatedToObjective(long objectiveId)
    {
        return containedObjective(objectiveId);
    }

    @Override
    public boolean mergeRelatedObjective(ScheduledObjective objective)
    {
        if(!isRelatedToObjective(objective.getId()))
        {
            return false;
        }

        putBack(objective);
        return true;
    }

    @Override
    public ObjectiveChain getRelatedChainById(long chainId)
    {
        if(chainId == getId())
        {
            return this;
        }

        return null;
    }

    @Override
    public ObjectiveChain getChainForObjectiveById(long objectiveId)
    {
        if(containedObjective(objectiveId))
        {
            return this;
        }

        return null;
    }

    public ScheduledObjective getFirstObjective()
    {
        if(mObjectives.isEmpty())
        {
            return null;
        }

        return mObjectives.getFront();
    }

    @Override
    public long getLargestRelatedId()
    {
        long maxId = getId();
        for(ScheduledObjective objective: mObjectives)
        {
            long objectiveId = objective.getLargestRelatedId();
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

    @Override
    public ScheduledObjective getRelatedObjectiveById(long objectiveId)
    {
        for(ScheduledObjective objective: mObjectives)
        {
            ScheduledObjective relatedObjective = objective.getRelatedObjectiveById(objectiveId);
            if(relatedObjective != null)
            {
                return relatedObjective;
            }
        }

        return null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean putBack(ScheduledObjective objective)
    {
        if(!mObjectives.isEmpty())
        {
            ScheduledObjective firstObjective = mObjectives.getFront();
            if(firstObjective.getId() == objective.getId())
            {
                firstObjective.rescheduleUnregulated(objective.getScheduledEnlistDate());
            }
            else
            {
                mObjectives.addFront(objective);
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

    @SuppressWarnings("unused")
    public int getObjectiveCount()
    {
        return mObjectives.size();
    }

    public void setProduceFrequency(Duration produceFrequency)
    {
        mProduceFrequency = produceFrequency;
    }

    public void setAutoDelete(boolean autoDelete)
    {
        mIsAutoDelete = autoDelete;
    }

    public void setUnstoppable(boolean unstoppable)
    {
        mIsUnstoppable = unstoppable;
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
