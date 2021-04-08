package com.sixshaman.decisore.scheduler;

/*

Scheduler update happens:
- After opening the application
- Every hour on schedule. The user can't change the period.

If during the update the scheduler finds an objective that has list add date greater or equal the current date, it adds it to the main list and removes it from the scheduler.

After removing the objective from the scheduler:
- If it's a one-time objective (repeat probability is 0), nothing else is needed.
- If it's a strictly periodic objective (repeat probability is 1), the new objective is added to the scheduler. It has the same creation date, but the list add date is the current one + period.
- If it's not a strictly periodic objective (0 < repeat probability < 1), then ULTRA-RANDOM ALGORITHM decides the next list add date and a new objective with it is added to the scheduler.

*/

import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.list.EnlistedObjective;
import com.sixshaman.decisore.scheduler.chain.ChainElementViewHolder;
import com.sixshaman.decisore.scheduler.chain.ObjectiveChain;
import com.sixshaman.decisore.scheduler.chain.ObjectiveChainLatestLoader;
import com.sixshaman.decisore.scheduler.pool.ObjectivePool;
import com.sixshaman.decisore.scheduler.pool.ObjectivePoolLatestLoader;
import com.sixshaman.decisore.scheduler.pool.PoolElement;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjectiveLatestLoader;
import com.sixshaman.decisore.scheduler.pool.PoolElementViewHolder;
import com.sixshaman.decisore.utils.LockedReadFile;
import com.sixshaman.decisore.utils.LockedWriteFile;
import com.sixshaman.decisore.utils.ValueHolder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

//The class to schedule all deferred objectives. The model for the scheduler UI
public class ObjectiveSchedulerCache
{
    public static final String SCHEDULER_FILENAME = "TaskScheduler.json";

    static final int SCHEDULER_VERSION_1_0 = 1000;
    static final int SCHEDULER_VERSION_1_1 = 1001;

    static final int SCHEDULER_VERSION_CURRENT = SCHEDULER_VERSION_1_1;

    //The list of all the scheduler elements
    private ArrayList<SchedulerElement> mSchedulerElements;

    //The view holder of the cached data
    private SchedulerCacheViewHolder mSchedulerViewHolder;

    //The view holders of the cached pools
    private final LongSparseArray<RecyclerView> mPoolItemViews;

    //The view holders of the cached chains
    private final LongSparseArray<RecyclerView> mChainItemViews;

    //Creates a new objective scheduler
    public ObjectiveSchedulerCache()
    {
        mSchedulerElements = new ArrayList<>();

        mPoolItemViews  = new LongSparseArray<>();
        mChainItemViews = new LongSparseArray<>();
    }

    public void attachToSchedulerView(RecyclerView recyclerView)
    {
        mSchedulerViewHolder = new ObjectiveSchedulerCache.SchedulerCacheViewHolder();
        recyclerView.setAdapter(mSchedulerViewHolder);
    }

    public void attachToChainView(RecyclerView recyclerView, long chainId)
    {
        ObjectiveChain chain = getChainById(chainId);
        if(chain != null)
        {
            chain.attachToChainView(recyclerView, this);
            mChainItemViews.put(chainId, recyclerView);
        }
    }

    public void attachToPoolView(RecyclerView recyclerView, long poolId)
    {
        ObjectivePool pool = getPoolById(poolId);
        if(pool != null)
        {
            pool.attachToPoolView(recyclerView, this);
            mPoolItemViews.put(poolId, recyclerView);
        }
    }

    //Updates the objective scheduler. Returns the list of objectives ready to-do
    public ArrayList<EnlistedObjective> dumpReadyObjectives(final HashSet<Long> blockingObjectiveIds, LocalDateTime enlistDateTime, int dayStartHour)
    {
        ArrayList<EnlistedObjective> result = new ArrayList<>();

        ArrayList<SchedulerElement> changedElements = new ArrayList<>(); //Rebuild objective source list after each update event
        for(SchedulerElement element: mSchedulerElements)
        {
            EnlistedObjective enlistedObjective = element.obtainEnlistedObjective(blockingObjectiveIds, enlistDateTime, dayStartHour);
            if(enlistedObjective != null)
            {
                result.add(enlistedObjective);
            }

            if(element.isValid())
            {
                changedElements.add(element);
            }
        }

        mSchedulerElements = changedElements;
        return result;
    }

    //Loads objectives from JSON config file
    public void invalidate(LockedReadFile schedulerReadFile)
    {
        mSchedulerElements.clear();

        try
        {
            String fileContents = schedulerReadFile.read();
            JSONObject jsonObject = new JSONObject(fileContents);

            int version = jsonObject.optInt("VERSION", SCHEDULER_VERSION_1_0);
            if(version == SCHEDULER_VERSION_CURRENT)
            {
                ScheduledObjectiveLatestLoader objectiveLatestLoader = new ScheduledObjectiveLatestLoader();
                ObjectiveChainLatestLoader     chainLatestLoader     = new ObjectiveChainLatestLoader();
                ObjectivePoolLatestLoader      poolLatestLoader      = new ObjectivePoolLatestLoader();

                JSONArray elementsArray = jsonObject.getJSONArray("ELEMENTS");

                for(int i = 0; i < elementsArray.length(); i++)
                {
                    JSONObject elementObject = elementsArray.getJSONObject(i);

                    String     elementType = elementObject.getString("Type");
                    JSONObject elementData = elementObject.getJSONObject("Data");

                    switch (elementType)
                    {
                        case "ObjectivePool":
                            ObjectivePool pool = poolLatestLoader.fromJSON(elementData);
                            mSchedulerElements.add(pool);

                            RecyclerView poolView = mPoolItemViews.get(pool.getId(), null);
                            if(poolView != null)
                            {
                                pool.attachToPoolView(poolView, this);
                            }

                            pool.attachAllChainViews(mChainItemViews, this);
                            break;
                        case "ObjectiveChain":
                            ObjectiveChain chain = chainLatestLoader.fromJSON(elementData);
                            mSchedulerElements.add(chain);

                            RecyclerView chainView = mChainItemViews.get(chain.getId(), null);
                            if(chainView != null)
                            {
                                chain.attachToChainView(chainView, this);
                            }

                            break;
                        case "ScheduledObjective":
                            ScheduledObjective objective = objectiveLatestLoader.fromJSON(elementData);
                            mSchedulerElements.add(objective);
                            break;
                        default:
                            return;
                    }
                }
            }
            else
            {
                mSchedulerElements = SchedulerOldVersionLoader.loadSchedulerElementsOld(this, jsonObject, version);
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
            return;
        }

        if(mSchedulerViewHolder != null)
        {
            mSchedulerViewHolder.notifyDataSetChanged();
        }
    }

    //Saves scheduled objectives in JSON config file
    public boolean flush(LockedWriteFile schedulerWriteFile)
    {
        try
        {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("VERSION", SCHEDULER_VERSION_CURRENT);

            JSONArray objectivesJsonArray = new JSONArray();

            for(SchedulerElement element: mSchedulerElements)
            {
                JSONObject elementObject = new JSONObject();

                elementObject.put("Type", element.getElementName());
                elementObject.put("Data", element.toJSON());

                objectivesJsonArray.put(elementObject);
            }

            jsonObject.put("ELEMENTS", objectivesJsonArray);

            schedulerWriteFile.write(jsonObject.toString());
        }
        catch(JSONException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    //Creates a new objective chain
    public long addObjectiveChain(long poolIdToAddTo, String name, String description, Duration produceFrequency)
    {
        ObjectivePool poolToAddTo = null;
        if(poolIdToAddTo != -1)
        {
            poolToAddTo = getPoolById(poolIdToAddTo);
            if(poolToAddTo == null)
            {
                return -1;
            }
        }

        long chainId = getMaxChainId() + 1;
        ObjectiveChain chain = new ObjectiveChain(chainId, name, description);
        chain.setProduceFrequency(produceFrequency);

        if(poolToAddTo == null)
        {
            mSchedulerElements.add(chain);
            if(mSchedulerViewHolder != null)
            {
                mSchedulerViewHolder.notifyItemInserted(mSchedulerElements.size() - 1);
                mSchedulerViewHolder.notifyItemRangeChanged(mSchedulerElements.size() - 1, mSchedulerViewHolder.getItemCount());
            }
        }
        else
        {
            poolToAddTo.addObjectiveSource(chain);
            if(mSchedulerViewHolder != null)
            {
                int index = mSchedulerElements.indexOf(poolToAddTo);
                mSchedulerViewHolder.notifyItemChanged(index);
            }
        }

        return chainId;
    }

    //Creates a new objective pool
    public void addObjectivePool(String name, String description, Duration produceFrequency)
    {
        long poolId = getMaxPoolId() + 1;

        ObjectivePool pool = new ObjectivePool(poolId, name, description);
        pool.setProduceFrequency(produceFrequency);

        mSchedulerElements.add(pool);

        if(mSchedulerViewHolder != null)
        {
            mSchedulerViewHolder.notifyItemInserted(mSchedulerElements.size() - 1);
            mSchedulerViewHolder.notifyItemRangeChanged(mSchedulerElements.size() - 1, mSchedulerViewHolder.getItemCount());
        }
    }

    //Adds a general task to task pool pool or task chain chain scheduled to be added at deferTime with repeat duration repeatDuration and repeat probability repeatProbability
    public boolean addObjective(long poolId, long chainId, boolean addToChainBeginning, ScheduledObjective scheduledObjective)
    {
        ObjectivePool poolToAddTo = null;
        if(poolId != -1)
        {
            poolToAddTo = getPoolById(poolId);
            if(poolToAddTo == null)
            {
                return false;
            }
        }

        ObjectiveChain chainToAddTo = null;
        if(chainId != -1)
        {
            chainToAddTo = getChainById(chainId);
            if(chainToAddTo == null)
            {
                return false;
            }
        }

        if(poolToAddTo == null && chainToAddTo == null) //Add a single task source
        {
            //Neither task chain nor pool is provided
            mSchedulerElements.add(scheduledObjective);

            if(mSchedulerViewHolder != null)
            {
                mSchedulerViewHolder.notifyItemInserted(mSchedulerElements.size() - 1);
                mSchedulerViewHolder.notifyItemRangeChanged(mSchedulerElements.size() - 1, mSchedulerViewHolder.getItemCount());
            }

            return true;
        }
        else if(poolToAddTo == null)
        {
            //Chain is provided, add the objective there
            if(addToChainBeginning)
            {
                chainToAddTo.addObjectiveToChainFront(scheduledObjective);
            }
            else
            {
                chainToAddTo.addObjectiveToChain(scheduledObjective);
            }

            if(mSchedulerViewHolder != null)
            {
                for(int i = 0; i < mSchedulerElements.size(); i++)
                {
                    if(mSchedulerElements.get(i) instanceof ObjectivePool && ((ObjectivePool)mSchedulerElements.get(i)).containsSource(chainToAddTo))
                    {
                        mSchedulerViewHolder.notifyItemChanged(i);
                    }
                }

                int plainChainIndex = mSchedulerElements.indexOf(chainToAddTo);
                if(plainChainIndex != -1)
                {
                    mSchedulerViewHolder.notifyItemChanged(plainChainIndex);
                }
            }

            return true;
        }
        else if(chainToAddTo == null)
        {
            //Objective pool provided, add the objective there
            poolToAddTo.addObjectiveSource(scheduledObjective);

            if(mSchedulerViewHolder != null)
            {
                int index = mSchedulerElements.indexOf(poolToAddTo);
                mSchedulerViewHolder.notifyItemChanged(index);
            }

            return true;
        }

        return false;
    }

    public boolean putObjectiveBack(ScheduledObjective objective)
    {
        //Rule: already existing scheduled objective consumes the newly added one if the newly added one was scheduled after existing
        //Example 1: a daily objective was rescheduled for tomorrow. It gets consumed.
        //Example 2: a weekly objective was rescheduled for tomorrow and the next time it gets added is 2 days after. It gets added both tomorrow and 2 days after

        boolean alreadyExisting = false;
        for(SchedulerElement schedulerElement: mSchedulerElements)
        {
            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool objectivePool = (ObjectivePool)schedulerElement;

                PoolElement source = objectivePool.findSourceForObjective(objective.getId());
                if(source != null)
                {
                    if(source instanceof ObjectiveChain)
                    {
                        if(!((ObjectiveChain) source).putBack(objective))
                        {
                            return false;
                        }
                    }
                    else if(source instanceof ScheduledObjective)
                    {
                        ScheduledObjective scheduledObjective = (ScheduledObjective)source;
                        if(objective.getId() == scheduledObjective.getId())
                        {
                            scheduledObjective.rescheduleUnregulated(objective.getScheduledEnlistDate());
                            return true;
                        }
                    }

                    alreadyExisting = true;
                }
            }
            else if(schedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain objectiveChain = (ObjectiveChain)schedulerElement;

                if(objectiveChain.containedObjective(objective.getId()))
                {
                    objectiveChain.putBack(objective);
                    alreadyExisting = true;
                }
            }
            else if(schedulerElement instanceof ScheduledObjective)
            {
                ScheduledObjective scheduledObjective = (ScheduledObjective)schedulerElement;

                if(scheduledObjective.getId() == objective.getId())
                {
                    alreadyExisting = true;
                }
            }
        }

        //Simply add the objective if no source contains it
        if(!alreadyExisting)
        {
            return addObjective(-1, -1, false, objective);
        }

        return true;
    }

    public long getMaxObjectiveId()
    {
        long maxId = 0;
        for(SchedulerElement schedulerElement: mSchedulerElements)
        {
            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool objectivePool = (ObjectivePool)schedulerElement;

                long poolMaxId = objectivePool.getMaxObjectiveId();
                if(poolMaxId > maxId)
                {
                    maxId = poolMaxId;
                }
            }
            else if(schedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain objectiveChain = (ObjectiveChain)schedulerElement;

                long chainMaxId = objectiveChain.getMaxObjectiveId();
                if(chainMaxId > maxId)
                {
                    maxId = chainMaxId;
                }
            }
            else if(schedulerElement instanceof ScheduledObjective)
            {
                ScheduledObjective scheduledObjective = (ScheduledObjective)schedulerElement;

                if(scheduledObjective.getId() > maxId)
                {
                    maxId = scheduledObjective.getId();
                }
            }
        }

        return maxId;
    }

    public long getMaxChainId()
    {
        long maxId = 0;
        for(SchedulerElement schedulerElement: mSchedulerElements)
        {
            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool objectivePool = (ObjectivePool)schedulerElement;

                long poolMaxId = objectivePool.getMaxChainId();
                if(poolMaxId > maxId)
                {
                    maxId = poolMaxId;
                }
            }
            else if(schedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain objectiveChain = (ObjectiveChain)schedulerElement;

                long chainId = objectiveChain.getId();
                if(chainId > maxId)
                {
                    maxId = chainId;
                }
            }
        }

        return maxId;
    }

    public long getMaxPoolId()
    {
        long maxId = 0;
        for(SchedulerElement schedulerElement: mSchedulerElements)
        {
            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool objectivePool = (ObjectivePool)schedulerElement;

                long poolId = objectivePool.getId();
                if(poolId > maxId)
                {
                    maxId = poolId;
                }
            }
        }

        return maxId;
    }

    public ObjectivePool getPoolById(long id)
    {
        for(SchedulerElement schedulerElement: mSchedulerElements)
        {
            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool objectivePool = (ObjectivePool)schedulerElement;
                if (objectivePool.getId() == id)
                {
                    return objectivePool;
                }
            }
        }

        return null;
    }

    public ObjectiveChain getChainById(long id)
    {
        for(SchedulerElement schedulerElement: mSchedulerElements)
        {
            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool  objectivePool  = (ObjectivePool)schedulerElement;
                ObjectiveChain objectiveChain = objectivePool.getChainById(id);

                if(objectiveChain != null)
                {
                    return objectiveChain;
                }
            }
            else if(schedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain objectiveChain = (ObjectiveChain)schedulerElement;
                if(objectiveChain.getId() == id)
                {
                    return objectiveChain;
                }
            }
        }

        return null;
    }

    public ScheduledObjective getObjectiveById(long id)
    {
        for(SchedulerElement schedulerElement: mSchedulerElements)
        {
            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool      objectivePool      = (ObjectivePool)schedulerElement;
                ScheduledObjective scheduledObjective = objectivePool.getObjectiveById(id);

                if(scheduledObjective != null)
                {
                    return scheduledObjective;
                }
            }
            else if(schedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain     objectiveChain     = (ObjectiveChain)schedulerElement;
                ScheduledObjective scheduledObjective = objectiveChain.getObjectiveById(id);

                if(scheduledObjective != null)
                {
                    return scheduledObjective;
                }
            }
            else if(schedulerElement instanceof ScheduledObjective)
            {
                ScheduledObjective scheduledObjective = (ScheduledObjective)schedulerElement;
                if(scheduledObjective.getId() == id)
                {
                    return scheduledObjective;
                }
            }
        }

        return null;
    }

    //Finds the id of a chain that contained an objective with that name
    public long findChainOfObjective(long objectiveId)
    {
        for(SchedulerElement schedulerElement: mSchedulerElements)
        {
            if(schedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain chain = (ObjectiveChain)schedulerElement;
                if(chain.containedObjective(objectiveId))
                {
                    return chain.getId();
                }
            }
            else if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool pool        = ((ObjectivePool)schedulerElement);
                PoolElement   poolElement = pool.findSourceForObjective(objectiveId);
                if(poolElement instanceof ObjectiveChain)
                {
                    return ((ObjectiveChain)poolElement).getId();
                }
            }
        }

        return -1;
    }

    public boolean editObjectiveName(long objectiveId, String objectiveName, String objectiveDescription)
    {
        ScheduledObjective objectiveToEdit = null;
        int                indexToEdit     = -1;

        for(int i = 0; i < mSchedulerElements.size(); i++)
        {
            SchedulerElement schedulerElement = mSchedulerElements.get(i);

            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool      objectivePool      = (ObjectivePool)schedulerElement;
                ScheduledObjective scheduledObjective = objectivePool.getObjectiveById(objectiveId);

                if(scheduledObjective != null)
                {
                    objectiveToEdit = scheduledObjective;
                    indexToEdit     = i;

                    break;
                }
            }
            else if(schedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain     objectiveChain     = (ObjectiveChain)schedulerElement;
                ScheduledObjective scheduledObjective = objectiveChain.getObjectiveById(objectiveId);

                if(scheduledObjective != null)
                {
                    objectiveToEdit = scheduledObjective;
                    indexToEdit     = i;

                    break;
                }
            }
            else if(schedulerElement instanceof ScheduledObjective)
            {
                ScheduledObjective scheduledObjective = (ScheduledObjective)schedulerElement;

                if(scheduledObjective.getId() == objectiveId)
                {
                    objectiveToEdit = scheduledObjective;
                    indexToEdit     = i;
                }
            }
        }

        if(indexToEdit != -1)
        {
            objectiveToEdit.setName(objectiveName);
            objectiveToEdit.setDescription(objectiveDescription);
        }
        else
        {
            return false;
        }

        if(mSchedulerViewHolder != null)
        {
            mSchedulerViewHolder.notifyItemChanged(indexToEdit);
        }

        return true;
    }

    public boolean editChain(long chainId, String chainName, String chainDescription)
    {
        ObjectiveChain chainToEdit = null;
        int            indexToEdit = -1;

        for(int i = 0; i < mSchedulerElements.size(); i++)
        {
            SchedulerElement schedulerElement = mSchedulerElements.get(i);

            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool  objectivePool  = (ObjectivePool)schedulerElement;
                ObjectiveChain objectiveChain = objectivePool.getChainById(chainId);

                if(objectiveChain != null)
                {
                   chainToEdit = objectiveChain;
                   indexToEdit = i;

                   break;
                }
            }
            else if(schedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain objectiveChain = (ObjectiveChain)schedulerElement;
                if(objectiveChain.getId() == chainId)
                {
                    chainToEdit = objectiveChain;
                    indexToEdit = i;

                    break;
                }
            }
        }

        if(indexToEdit != -1)
        {
            chainToEdit.setName(chainName);
            chainToEdit.setDescription(chainDescription);
        }
        else
        {
            return false;
        }

        if(mSchedulerViewHolder != null)
        {
            mSchedulerViewHolder.notifyItemChanged(indexToEdit);
        }

        return true;
    }

    public boolean editPool(long poolId, String poolName, String poolDescription)
    {
        ObjectivePool poolToEdit  = null;
        int           indexToEdit = -1;

        for(int i = 0; i < mSchedulerElements.size(); i++)
        {
            SchedulerElement schedulerElement = mSchedulerElements.get(i);

            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool objectivePool = (ObjectivePool)schedulerElement;
                if(objectivePool.getId() == poolId)
                {
                    poolToEdit  = objectivePool;
                    indexToEdit = i;

                    break;
                }
            }
        }

        if(indexToEdit != -1)
        {
            poolToEdit.setName(poolName);
            poolToEdit.setDescription(poolDescription);
        }
        else
        {
            return false;
        }

        if(mSchedulerViewHolder != null)
        {
            mSchedulerViewHolder.notifyItemChanged(indexToEdit);
        }

        return true;
    }

    public boolean removePool(long poolId)
    {
        int plainIndexToDelete = -1;

        for(int i = 0; i < mSchedulerElements.size(); i++)
        {
            SchedulerElement schedulerElement = mSchedulerElements.get(i);
            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool objectivePool = (ObjectivePool)schedulerElement;
                if(objectivePool.getId() == poolId)
                {
                    plainIndexToDelete = i;
                }
            }
        }

        if(plainIndexToDelete != -1)
        {
            mSchedulerElements.remove(plainIndexToDelete);

            if(mSchedulerViewHolder != null)
            {
                mSchedulerViewHolder.notifyItemRemoved(plainIndexToDelete);
                mSchedulerViewHolder.notifyItemRangeChanged(plainIndexToDelete, mSchedulerViewHolder.getItemCount());
            }
        }

        return (plainIndexToDelete != -1);
    }

    public boolean removeChain(long chainId)
    {
        boolean complexDeleteSucceeded = false;
        int     plainIndexToDelete     = -1;

        for(int i = 0; i < mSchedulerElements.size(); i++)
        {
            SchedulerElement schedulerElement = mSchedulerElements.get(i);

            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool objectivePool = (ObjectivePool)schedulerElement;
                boolean deleteSucceeded = objectivePool.deleteChainById(chainId);

                if(mSchedulerViewHolder != null && deleteSucceeded)
                {
                    mSchedulerViewHolder.notifyItemChanged(i);
                }
                else if(deleteSucceeded)
                {
                    complexDeleteSucceeded = true;
                }
            }
            else if(schedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain objectiveChain = (ObjectiveChain)schedulerElement;

                if(objectiveChain.getId() == chainId)
                {
                    plainIndexToDelete = i;
                }
            }
        }

        if(plainIndexToDelete != -1)
        {
            mSchedulerElements.remove(plainIndexToDelete);

            if(mSchedulerViewHolder != null)
            {
                mSchedulerViewHolder.notifyItemRemoved(plainIndexToDelete);
                mSchedulerViewHolder.notifyItemRangeChanged(plainIndexToDelete, mSchedulerViewHolder.getItemCount());
            }
        }

        return (plainIndexToDelete != -1) || complexDeleteSucceeded;
    }

    public boolean removeObjective(long objectiveId)
    {
        boolean complexDeleteSucceeded = false;
        int     plainIndexToDelete     = -1;

        for(int i = 0; i < mSchedulerElements.size(); i++)
        {
            SchedulerElement schedulerElement = mSchedulerElements.get(i);

            if(schedulerElement instanceof ObjectivePool)
            {
                ObjectivePool objectivePool = (ObjectivePool)schedulerElement;
                boolean deleteSucceeded = objectivePool.deleteObjectiveById(objectiveId);

                if(mSchedulerViewHolder != null && deleteSucceeded)
                {
                    mSchedulerViewHolder.notifyItemChanged(i);
                }
                else if(deleteSucceeded)
                {
                    complexDeleteSucceeded = true;
                }
            }
            else if(schedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain objectiveChain = (ObjectiveChain)schedulerElement;
                boolean deleteSucceeded = objectiveChain.deleteObjectiveById(objectiveId);

                if(mSchedulerViewHolder != null && deleteSucceeded)
                {
                    mSchedulerViewHolder.notifyItemChanged(i);
                }
                else if(deleteSucceeded)
                {
                    complexDeleteSucceeded = true;
                }
            }
            else if(schedulerElement instanceof ScheduledObjective)
            {
                ScheduledObjective scheduledObjective = (ScheduledObjective)schedulerElement;
                if(scheduledObjective.getId() == objectiveId)
                {
                    plainIndexToDelete = i;
                }
            }
        }

        if(plainIndexToDelete != -1)
        {
            mSchedulerElements.remove(plainIndexToDelete);

            if(mSchedulerViewHolder != null)
            {
                mSchedulerViewHolder.notifyItemRemoved(plainIndexToDelete);
                mSchedulerViewHolder.notifyItemRangeChanged(plainIndexToDelete, mSchedulerViewHolder.getItemCount());
            }
        }

        return (plainIndexToDelete != -1) || complexDeleteSucceeded;
    }

    private class SchedulerCacheViewHolder extends RecyclerView.Adapter<SchedulerElementViewHolder>
    {
        @NonNull
        @Override
        public SchedulerElementViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
        {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_objective_source_view, viewGroup, false);
            return new SchedulerElementViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SchedulerElementViewHolder objectiveViewHolder, int position)
        {
            String viewName = "";
            final ValueHolder<String> viewDescription = new ValueHolder<>("");

            int iconId = 0;

            SchedulerElement schedulerElement = mSchedulerElements.get(position);
            if(schedulerElement instanceof ObjectivePool) //Implicit pool
            {
                ObjectivePool pool = (ObjectivePool)schedulerElement;

                viewName = pool.getName();
                viewDescription.setValue(pool.getDescription());

                viewDescription.setValue("Number of sources: " + pool.getSourceCount());

                iconId = R.drawable.ic_scheduler_pool;
            }
            else if(schedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain chain = (ObjectiveChain)schedulerElement;

                viewName = chain.getName();

                ScheduledObjective objective = chain.getFirstObjective();
                if(objective != null)
                {
                    viewDescription.setValue("Next objective: " + objective.getName());
                }

                iconId = R.drawable.ic_scheduler_chain;
            }
            else if(schedulerElement instanceof ScheduledObjective)
            {
                ScheduledObjective objective = (ScheduledObjective)schedulerElement;

                viewName = objective.getName();
                viewDescription.setValue("Scheduled to: " + objective.getScheduledEnlistDate());

                iconId = R.drawable.ic_scheduler_objective;
            }

            objectiveViewHolder.mTextView.setText(viewName);
            objectiveViewHolder.mIconView.setImageResource(iconId);

            objectiveViewHolder.setSourceMetadata(ObjectiveSchedulerCache.this, schedulerElement);
        }

        @Override
        public int getItemCount()
        {
            return mSchedulerElements.size();
        }
    }
}
