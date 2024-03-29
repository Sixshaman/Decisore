package com.sixshaman.decisore.utils;

import com.sixshaman.decisore.archive.ArchivedObjective;
import com.sixshaman.decisore.archive.ObjectiveArchiveCache;
import com.sixshaman.decisore.list.EnlistedObjective;
import com.sixshaman.decisore.list.List10To11Converter;
import com.sixshaman.decisore.list.ObjectiveListCache;
import com.sixshaman.decisore.scheduler.Scheduler11To12Converter;
import com.sixshaman.decisore.scheduler.chain.ObjectiveChain;
import com.sixshaman.decisore.scheduler.pool.ObjectivePool;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class TransactionDispatcher
{
    private ObjectiveSchedulerCache mSchedulerCache;
    private ObjectiveListCache      mListCache;
    private ObjectiveArchiveCache   mArchiveCache;

    private final String mSchedulerFilePath;
    private final String mListFilePath;
    private final String mArchiveFilePath;

    public TransactionDispatcher(String configFolder)
    {
        mSchedulerCache = null;
        mListCache      = null;
        mArchiveCache   = null;

        mSchedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;
        mListFilePath      = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;
        mArchiveFilePath   = configFolder + "/" + ObjectiveArchiveCache.ARCHIVE_FILENAME;
    }

    public synchronized void setSchedulerCache(ObjectiveSchedulerCache schedulerCache)
    {
        mSchedulerCache = schedulerCache;
    }

    public synchronized void setListCache(ObjectiveListCache listCache)
    {
        mListCache = listCache;
    }

    @SuppressWarnings("unused")
    public synchronized void setArchiveCache(ObjectiveArchiveCache archiveCache)
    {
        mArchiveCache = archiveCache;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean addPoolTransaction(String poolName, String poolDescription, Duration produceFrequency, boolean autoDelete, boolean unstoppable)
    {
        invalidateCaches(true, true, false);

        long maxSchedulerId = mSchedulerCache.getLargestUsedId();
        long maxListId      = mListCache.getLargestUsedId();

        long poolId = Math.max(maxSchedulerId, maxListId) + 1;

        ObjectivePool pool = new ObjectivePool(poolId, poolName, poolDescription);
        pool.setProduceFrequency(produceFrequency);
        pool.setAutoDelete(autoDelete);
        pool.setUnstoppable(unstoppable);

        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);
        if(mSchedulerCache.addObjectivePool(pool))
        {
            if (mSchedulerCache.flush(schedulerWriteFile))
            {
                schedulerWriteFile.close();
                return true;
            }
        }

        schedulerWriteFile.close();
        invalidateCaches(true, false, false);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean editPoolTransaction(long poolId, String poolName, String poolDescription)
    {
        invalidateCaches(true, false, false);

        //1. Lock list file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);
        if(mSchedulerCache.editPool(poolId, poolName, poolDescription))
        {
            if(mSchedulerCache.flush(schedulerWriteFile))
            {
                schedulerWriteFile.close();
                return true;
            }
        }

        schedulerWriteFile.close();
        invalidateCaches(true, false, false);

        return false;
    }

    public synchronized long addChainTransaction(long poolIdToAddTo, String chainName, String chainDescription, Duration produceFrequency, boolean useAutoDelete, boolean useUnstoppable)
    {
        invalidateCaches(true, true, false);

        long maxSchedulerId = mSchedulerCache.getLargestUsedId();
        long maxListId      = mListCache.getLargestUsedId();

        long chainId = Math.max(maxSchedulerId, maxListId) + 1;
        ObjectiveChain chain = new ObjectiveChain(chainId, chainName, chainDescription);
        chain.setParentId(poolIdToAddTo);
        chain.setProduceFrequency(produceFrequency);
        chain.setAutoDelete(useAutoDelete);
        chain.setUnstoppable(useUnstoppable);

        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);

        boolean addResult = mSchedulerCache.addObjectiveChain(poolIdToAddTo, chain);
        if(addResult)
        {
            if(mSchedulerCache.flush(schedulerWriteFile))
            {
                schedulerWriteFile.close();
                invalidateCaches(true, false, false);

                return chainId;
            }
        }

        schedulerWriteFile.close();
        invalidateCaches(true, false, false);

        return chainId;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean editChainTransaction(long chainId, String chainName, String chainDescription)
    {
        invalidateCaches(true, false, false);

        //1. Lock list file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);
        if(mSchedulerCache.editChain(chainId, chainName, chainDescription))
        {
            if(mSchedulerCache.flush(schedulerWriteFile))
            {
                schedulerWriteFile.close();
                return true;
            }
        }

        schedulerWriteFile.close();
        invalidateCaches(true, false, false);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean deletePoolTransaction(ObjectivePool pool)
    {
        invalidateCaches(true, false, false);

        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);
        if(mSchedulerCache.removePool(pool.getId()))
        {
            if(mSchedulerCache.flush(schedulerWriteFile))
            {
                schedulerWriteFile.close();
                return true;
            }
        }

        schedulerWriteFile.close();
        invalidateCaches(true, false, false);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean deleteChainTransaction(ObjectiveChain chain)
    {
        invalidateCaches(true, false, false);

        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);
        if(mSchedulerCache.removeChain(chain.getId()))
        {
            if(mSchedulerCache.flush(schedulerWriteFile))
            {
                schedulerWriteFile.close();
                return true;
            }
        }

        schedulerWriteFile.close();
        invalidateCaches(true, false, false);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized long addObjectiveTransaction(long parentId, boolean addToChainBeginning,
                                                     LocalDateTime createDateTime, LocalDateTime enlistDateTime,
                                                     Duration repeatDuration, float repeatProbability,
                                                     String objectiveName, String objectiveDescription, ArrayList<String> objectiveTags,
                                                     int dayStartHour)
    {
        invalidateCaches(true, true, false);

        long maxSchedulerId = mSchedulerCache.getLargestUsedId();
        long maxListId      = mListCache.getLargestUsedId();

        long objectiveId = Math.max(maxListId, maxSchedulerId) + 1;

        boolean correctTransaction = true;

        EnlistedObjective  enlistedObjectiveToAdd  = null;
        ScheduledObjective scheduledObjectiveToAdd = null;
        if(!createDateTime.isBefore(enlistDateTime) && repeatDuration == Duration.ZERO && repeatProbability < 0.0001f && parentId == -1)
        {
            //One-time objective
            enlistedObjectiveToAdd = new EnlistedObjective(objectiveId, parentId, createDateTime, enlistDateTime,
                                                           objectiveName, objectiveDescription, objectiveTags);
        }
        else if(!createDateTime.isBefore(enlistDateTime) && parentId == -1)
        {
            //Repeated objective which gets added to the list immediately
            scheduledObjectiveToAdd = new ScheduledObjective(objectiveId, objectiveName, objectiveDescription,
                                                             createDateTime, enlistDateTime, objectiveTags,
                                                             repeatDuration, repeatProbability);

            enlistedObjectiveToAdd = scheduledObjectiveToAdd.obtainEnlistedObjective(mListCache.constructIdSet(), enlistDateTime, dayStartHour);
        }
        else
        {
            //If a chain or a pool is provided or it's not the time to add the objective, always create a scheduled objective only
            scheduledObjectiveToAdd = new ScheduledObjective(objectiveId, objectiveName, objectiveDescription,
                                                             createDateTime, enlistDateTime, objectiveTags,
                                                             repeatDuration, repeatProbability);
        }

        if(enlistedObjectiveToAdd != null)
        {
            //1. Lock list file
            LockedWriteFile listWriteFile = new LockedWriteFile(mListFilePath);
            if(!mListCache.addObjective(enlistedObjectiveToAdd))
            {
                correctTransaction = false;
            }
            else
            {
                if(!mListCache.flush(listWriteFile))
                {
                    correctTransaction = false;
                }

                listWriteFile.close();
            }
        }

        if(correctTransaction && scheduledObjectiveToAdd != null)
        {
            scheduledObjectiveToAdd.setParentId(parentId);

            //1. Lock scheduler file
            LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);
            if(!mSchedulerCache.addObjective(scheduledObjectiveToAdd, addToChainBeginning))
            {
                correctTransaction = false;
            }
            else
            {
                if(!mSchedulerCache.flush(schedulerWriteFile))
                {
                    correctTransaction = false;
                }
            }

            schedulerWriteFile.close();
        }

        if(!correctTransaction)
        {
            invalidateCaches(true, true, false);
            return -1;
        }

        return objectiveId;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean editObjectiveTransaction(long objectiveId, String objectiveName, String objectiveDescription,
                                                         boolean editInScheduler, boolean editInList)
    {
        boolean editSchedulerSucceeded = true;
        boolean editListSucceeded      = true;

        if(editInScheduler)
        {
            invalidateCaches(true, false, false);

            LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);
            if(mSchedulerCache.editObjectiveName(objectiveId, objectiveName, objectiveDescription))
            {
                if(!mSchedulerCache.flush(schedulerWriteFile))
                {
                    editSchedulerSucceeded = false;
                }
            }
            else
            {
                editSchedulerSucceeded = false;
            }

            schedulerWriteFile.close();
        }

        if(editInList)
        {
            invalidateCaches(false, true, false);

            EnlistedObjective objectiveToEdit = mListCache.getObjective(objectiveId);
            if(objectiveToEdit != null)
            {
                //1. Lock list file
                LockedWriteFile listWriteFile = new LockedWriteFile(mListFilePath);
                if(mListCache.editObjectiveName(objectiveId, objectiveName, objectiveDescription))
                {
                    if(!mListCache.flush(listWriteFile))
                    {
                        editListSucceeded = false;
                    }
                }
                else
                {
                    editListSucceeded = false;
                }

                listWriteFile.close();
            }
            else
            {
                editListSucceeded = false;
            }
        }

        if(editSchedulerSucceeded && editListSucceeded)
        {
            return true;
        }

        invalidateCaches(true, true, false);
        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean deleteObjectiveFromListTransaction(EnlistedObjective objective)
    {
        invalidateCaches(false, true, false);

        LockedWriteFile listWriteFile = new LockedWriteFile(mListFilePath);
        if(mListCache.removeObjective(objective))
        {
            if(mListCache.flush(listWriteFile))
            {
                listWriteFile.close();
                return true;
            }
        }

        listWriteFile.close();
        invalidateCaches(false, true, false);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean deleteObjectiveFromSchedulerTransaction(ScheduledObjective objective)
    {
        invalidateCaches(true, false, false);

        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);
        if(mSchedulerCache.removeObjective(objective.getId()))
        {
            if(mSchedulerCache.flush(schedulerWriteFile))
            {
                schedulerWriteFile.close();
                return true;
            }
        }

        schedulerWriteFile.close();
        invalidateCaches(true, false, false);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean updateNewDayStart(int oldStartHour, int newStartHour)
    {
        invalidateCaches(true, false, false);

        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);
        mSchedulerCache.updateDayStart(oldStartHour, newStartHour);

        if(mSchedulerCache.flush(schedulerWriteFile))
        {
            schedulerWriteFile.close();
            return true;
        }

        schedulerWriteFile.close();
        invalidateCaches(true, false, false);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean updateObjectiveListTransaction(LocalDateTime enlistDateTime, int dayStartHour)
    {
        invalidateCaches(true, true, false);

        //1. Lock scheduler and files
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);
        LockedWriteFile listWriteFile      = new LockedWriteFile(mListFilePath);

        //3. Prepare the list of objectives to add
        ArrayList<EnlistedObjective> enlistedObjectives = mSchedulerCache.dumpReadyObjectives(mListCache.constructIdSet(), enlistDateTime, dayStartHour);
        if(enlistedObjectives != null)
        {
            //4. Add objectives
            boolean allAdded = true;
            for(EnlistedObjective objective: enlistedObjectives)
            {
                if(!mListCache.addObjective(objective))
                {
                    allAdded = false;
                    break;
                }
            }

            if(allAdded)
            {
                //5. List cache flush
                if(mListCache.flush(listWriteFile))
                {
                    //6. Scheduler cache flush
                    if(mSchedulerCache.flush(schedulerWriteFile))
                    {
                        schedulerWriteFile.close();
                        listWriteFile.close();

                        return true;
                    }
                }
            }
        }

        listWriteFile.close();
        schedulerWriteFile.close();

        invalidateCaches(true, true, false);
        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean rescheduleScheduledObjectiveTransaction(long objectiveId, LocalDateTime nextEnlistDate)
    {
        invalidateCaches(true, false, false);

        //2. Lock scheduler file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);

        ScheduledObjective objective = mSchedulerCache.getObjectiveById(objectiveId);
        objective.rescheduleUnregulated(nextEnlistDate);

        if(mSchedulerCache.flush(schedulerWriteFile))
        {
            schedulerWriteFile.close();
            return true;
        }

        schedulerWriteFile.close();
        invalidateCaches(true, false, false);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean rescheduleEnlistedObjectiveTransaction(EnlistedObjective objective, LocalDateTime nextEnlistDate)
    {
        if(objective.getEnlistDate().isAfter(nextEnlistDate))
        {
            return false;
        }

        invalidateCaches(true, true, false);

        //1. Lock list and scheduler files
        LockedWriteFile listWriteFile      = new LockedWriteFile(mListFilePath);
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);

        //3. Remove objective from list
        if(mListCache.removeObjective(objective))
        {
            //4. Add objective to the archive
            ScheduledObjective scheduledObjective = objective.toScheduled(nextEnlistDate);
            if(mSchedulerCache.putObjectiveBack(scheduledObjective))
            {
                //5. List cache flush
                if(mListCache.flush(listWriteFile))
                {
                    //6. Archive cache flush
                    if(mSchedulerCache.flush(schedulerWriteFile))
                    {
                        schedulerWriteFile.close();
                        listWriteFile.close();

                        return true;
                    }
                }
            }
        }

        schedulerWriteFile.close();
        listWriteFile.close();

        invalidateCaches(true, true, false);
        return false;
    }

    public synchronized long rechainEnlistedObjective(EnlistedObjective enlistedObjective)
    {
        invalidateCaches(true, true, false);

        long chainId = mSchedulerCache.findChainOfObjective(enlistedObjective.getId());
        if(chainId == -1)
        {
            String objectiveName = enlistedObjective.getName();
            chainId = addChainTransaction(-1, objectiveName, "", Duration.ZERO, true, false); //Implicitly created chains are auto-delete
        }

        ObjectiveChain chain = mSchedulerCache.getChainById(chainId);
        if(chain == null)
        {
            return -1;
        }

        //1. Lock list and scheduler files
        LockedWriteFile listWriteFile      = new LockedWriteFile(mListFilePath);
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);

        //3. Remove objective from list
        if(mListCache.removeObjective(enlistedObjective))
        {
            //4. Add objective to the archive
            ScheduledObjective scheduledObjective = enlistedObjective.toScheduled(enlistedObjective.getEnlistDate());
            chain.addObjectiveToChainFront(scheduledObjective);

            //5. List cache flush
            if(mListCache.flush(listWriteFile))
            {
                //6. Archive cache flush
                if(mSchedulerCache.flush(schedulerWriteFile))
                {
                    schedulerWriteFile.close();
                    listWriteFile.close();

                    return chainId;
                }
            }
        }

        schedulerWriteFile.close();
        listWriteFile.close();

        invalidateCaches(true, true, false);
        return -1;
    }

    public synchronized long touchChainWithObjective(long objectiveId)
    {
        invalidateCaches(true, false, false);

        long chainId = mSchedulerCache.findChainOfObjective(objectiveId);
        if(chainId == -1)
        {
            EnlistedObjective enlistedObjective = mListCache.getObjective(objectiveId);
            if(enlistedObjective == null)
            {
                ScheduledObjective scheduledObjective = mSchedulerCache.getObjectiveById(objectiveId);
                if(scheduledObjective == null)
                {
                    return -1;
                }

                String objectiveName = scheduledObjective.getName();
                chainId = addChainTransaction(-1, objectiveName, "", Duration.ZERO, true, false);
            }
            else
            {
                String objectiveName = enlistedObjective.getName();
                chainId = addChainTransaction(-1, objectiveName, "", Duration.ZERO, true, false);
            }
        }

        ObjectiveChain chain = mSchedulerCache.getChainById(chainId);
        if(chain == null)
        {
            return -1;
        }

        //2. Lock scheduler file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);
        chain.bindObjectiveToChain(objectiveId);

        //6. Archive cache flush
        if(mSchedulerCache.flush(schedulerWriteFile))
        {
            schedulerWriteFile.close();
            return chainId;
        }

        schedulerWriteFile.close();
        invalidateCaches(true, false, false);

        return -1;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean flipPauseObjective(long objectiveId)
    {
        invalidateCaches(true, false, false);

        //2. Lock scheduler file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);

        ScheduledObjective objective = mSchedulerCache.getObjectiveById(objectiveId);
        objective.setPaused(!objective.isPaused());

        if(mSchedulerCache.flush(schedulerWriteFile))
        {
            schedulerWriteFile.close();
            return true;
        }

        schedulerWriteFile.close();
        invalidateCaches(true, false, false);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean flipPauseChain(long chainId)
    {
        invalidateCaches(true, false, false);

        //2. Lock scheduler file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);

        ObjectiveChain chain = mSchedulerCache.getChainById(chainId);
        chain.setPaused(!chain.isPaused());

        if(mSchedulerCache.flush(schedulerWriteFile))
        {
            schedulerWriteFile.close();
            return true;
        }

        schedulerWriteFile.close();
        invalidateCaches(true, false, false);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean flipPausePool(long poolId)
    {
        invalidateCaches(true, false, false);

        //2. Lock scheduler file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);

        ObjectivePool pool = mSchedulerCache.getPoolById(poolId);
        pool.setPaused(!pool.isPaused());

        if(mSchedulerCache.flush(schedulerWriteFile))
        {
            schedulerWriteFile.close();
            return true;
        }

        schedulerWriteFile.close();
        invalidateCaches(true, false, false);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean bindObjectiveToChain(long chainId, long objectiveId)
    {
        invalidateCaches(true, false, false);

        ObjectiveChain chain = mSchedulerCache.getChainById(chainId);
        if(chain == null)
        {
            return false;
        }

        //2. Lock scheduler file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(mSchedulerFilePath);
        chain.bindObjectiveToChain(objectiveId);

        //6. Archive cache flush
        if(mSchedulerCache.flush(schedulerWriteFile))
        {
            schedulerWriteFile.close();
            return true;
        }

        schedulerWriteFile.close();
        invalidateCaches(true, false, false);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean finishObjectiveTransaction(EnlistedObjective objective, LocalDateTime finishDate)
    {
        invalidateCaches(false, true, true);

        //1. Lock list and archive files
        LockedWriteFile listWriteFile    = new LockedWriteFile(mListFilePath);
        LockedWriteFile archiveWriteFile = new LockedWriteFile(mArchiveFilePath);

        //3. Remove objective from list
        if(mListCache.removeObjective(objective))
        {
            //4. Add objective to the archive
            ArchivedObjective archivedObjective = objective.toArchived(finishDate);
            mArchiveCache.addObjective(archivedObjective);

            //5. List cache flush
            if(mListCache.flush(listWriteFile))
            {
                //6. Archive cache flush
                if(mArchiveCache.flush(archiveWriteFile))
                {
                    archiveWriteFile.close();
                    listWriteFile.close();

                    return true;
                }
            }
        }

        archiveWriteFile.close();
        listWriteFile.close();

        invalidateCaches(false, true, true);
        return false;
    }

    private synchronized void invalidateCaches(boolean invalidateScheduler, boolean invalidateList, boolean invalidateArchive)
    {
        boolean needToValidateOldIds = false;

        if(invalidateScheduler)
        {
            if(mSchedulerCache == null)
            {
                mSchedulerCache = new ObjectiveSchedulerCache();
            }

            ObjectiveSchedulerCache.InvalidateResult invalidateResult = ObjectiveSchedulerCache.InvalidateResult.INVALIDATE_ERROR;
            try
            {
                LockedReadFile schedulerFile = new LockedReadFile(mSchedulerFilePath);
                invalidateResult = mSchedulerCache.invalidate(schedulerFile);
                schedulerFile.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            if(invalidateResult == ObjectiveSchedulerCache.InvalidateResult.INVALIDATE_VERSION_1_1)
            {
                //Version 1.2 unifies ids for pools, chains, and objectives
                needToValidateOldIds = true;
            }
        }

        if(invalidateList || needToValidateOldIds)
        {
            if(mListCache == null)
            {
                mListCache = new ObjectiveListCache();
            }

            ObjectiveListCache.InvalidateResult invalidateResult = ObjectiveListCache.InvalidateResult.INVALIDATE_ERROR;
            try
            {
                LockedReadFile listFile = new LockedReadFile(mListFilePath);
                invalidateResult = mListCache.invalidate(listFile);
                listFile.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            if(invalidateResult == ObjectiveListCache.InvalidateResult.INVALIDATE_VERSION_1_0)
            {
                //Version 1.1 unifies ids for pools, chains, and objectives
                needToValidateOldIds = true;
            }
        }

        if(invalidateArchive)
        {
            if(mArchiveCache == null)
            {
                mArchiveCache = new ObjectiveArchiveCache();
            }

            try
            {
                LockedReadFile archiveFile = new LockedReadFile(mArchiveFilePath);
                mArchiveCache.invalidate(archiveFile);
                archiveFile.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }

        if(needToValidateOldIds)
        {
            revalidateIds();
        }
    }

    private synchronized void revalidateIds()
    {
        List10To11Converter      listConverter      = new List10To11Converter(mListFilePath);
        Scheduler11To12Converter schedulerConverter = new Scheduler11To12Converter(mSchedulerFilePath);

        ArrayList<Long> poolIds               = schedulerConverter.gatherPoolIds();
        ArrayList<Long> chainIds              = schedulerConverter.gatherChainIds();
        ArrayList<Long> scheduledObjectiveIds = schedulerConverter.gatherObjectiveIds();
        ArrayList<Long> enlistedObjectiveIds  = listConverter.gatherObjectiveIds();

        HashMap<Long, Long> poolPatchedIdMap      = new HashMap<>();
        HashMap<Long, Long> chainPatchedIdMap     = new HashMap<>();
        HashMap<Long, Long> objectivePatchedIdMap = new HashMap<>();

        long newIdCounter = 0;

        for(long oldPoolId: poolIds)
        {
            poolPatchedIdMap.put(oldPoolId, newIdCounter);
            newIdCounter++;
        }

        for(long oldChainId: chainIds)
        {
            chainPatchedIdMap.put(oldChainId, newIdCounter);
            newIdCounter++;
        }

        for(long oldObjectiveId: scheduledObjectiveIds)
        {
            objectivePatchedIdMap.put(oldObjectiveId, newIdCounter);
            newIdCounter++;
        }

        for(long oldObjectiveId: enlistedObjectiveIds)
        {
            if(!objectivePatchedIdMap.containsKey(oldObjectiveId))
            {
                objectivePatchedIdMap.put(oldObjectiveId, newIdCounter);
                newIdCounter++;
            }
        }

        if(schedulerConverter.patchIds(objectivePatchedIdMap, chainPatchedIdMap, poolPatchedIdMap) && listConverter.patchIds(objectivePatchedIdMap))
        {
            HashMap<Long, Long> parentIdMap = schedulerConverter.gatherParentIds();
            if(schedulerConverter.patchParentIds(parentIdMap) && listConverter.patchParentIds(parentIdMap))
            {
                if(schedulerConverter.updateVersion() && listConverter.updateVersion())
                {
                    listConverter.save();
                    schedulerConverter.save();

                    invalidateCaches(true, true, false);
                }
            }
        }
    }
}
