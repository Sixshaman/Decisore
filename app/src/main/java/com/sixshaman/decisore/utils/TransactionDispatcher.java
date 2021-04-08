package com.sixshaman.decisore.utils;

import com.sixshaman.decisore.archive.ArchivedObjective;
import com.sixshaman.decisore.archive.ObjectiveArchiveCache;
import com.sixshaman.decisore.list.EnlistedObjective;
import com.sixshaman.decisore.list.ObjectiveListCache;
import com.sixshaman.decisore.scheduler.chain.ObjectiveChain;
import com.sixshaman.decisore.scheduler.pool.ObjectivePool;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class TransactionDispatcher
{
    private ObjectiveSchedulerCache mSchedulerCache;
    private ObjectiveListCache      mListCache;
    private ObjectiveArchiveCache   mArchiveCache;

    public TransactionDispatcher()
    {
        mSchedulerCache = null;
        mListCache      = null;
        mArchiveCache   = null;
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
    public synchronized boolean addPoolTransaction(String configFolder, String poolName, String poolDescription, Duration produceFrequency)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);

        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
        mSchedulerCache.addObjectivePool(poolName, poolDescription, produceFrequency);

        if(mSchedulerCache.flush(schedulerWriteFile))
        {
            schedulerWriteFile.close();
            return true;
        }

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean editPoolTransaction(String configFolder, long poolId, String poolName, String poolDescription)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);

        //1. Lock list file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
        if(mSchedulerCache.editPool(poolId, poolName, poolDescription))
        {
            if(mSchedulerCache.flush(schedulerWriteFile))
            {
                schedulerWriteFile.close();
                return true;
            }
        }

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        return false;
    }

    public synchronized long addChainTransaction(long poolIdToAddTo, String configFolder, String chainName, String chainDescription, Duration produceFrequency)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;
        invalidateSchedulerCache(schedulerFilePath);

        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);

        long newChainId = mSchedulerCache.addObjectiveChain(poolIdToAddTo, chainName, chainDescription, produceFrequency);
        if(newChainId != -1)
        {
            if(mSchedulerCache.flush(schedulerWriteFile))
            {
                schedulerWriteFile.close();
                invalidateSchedulerCache(schedulerFilePath);

                return newChainId;
            }
        }

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        return newChainId;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean editChainTransaction(String configFolder, long chainId, String chainName, String chainDescription)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;
        invalidateSchedulerCache(schedulerFilePath);

        //1. Lock list file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
        if(mSchedulerCache.editChain(chainId, chainName, chainDescription))
        {
            if(mSchedulerCache.flush(schedulerWriteFile))
            {
                schedulerWriteFile.close();
                return true;
            }
        }

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean deletePoolTransaction(String configFolder, ObjectivePool pool)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;
        invalidateSchedulerCache(schedulerFilePath);

        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
        if(mSchedulerCache.removePool(pool.getId()))
        {
            if(mSchedulerCache.flush(schedulerWriteFile))
            {
                schedulerWriteFile.close();
                return true;
            }
        }

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean deleteChainTransaction(String configFolder, ObjectiveChain chain)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;
        invalidateSchedulerCache(schedulerFilePath);

        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
        if(mSchedulerCache.removeChain(chain.getId()))
        {
            if(mSchedulerCache.flush(schedulerWriteFile))
            {
                schedulerWriteFile.close();
                return true;
            }
        }

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized long addObjectiveTransaction(long poolId, long chainId, boolean addToChainBeginning,
                                                     String configFolder, LocalDateTime createDateTime, LocalDateTime enlistDateTime,
                                                     Duration repeatDuration, float repeatProbability,
                                                     String objectiveName, String objectiveDescription, ArrayList<String> objectiveTags,
                                                     int dayStartHour)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;
        String listFilePath      = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);
        invalidateListCache(listFilePath);

        long maxSchedulerId = mSchedulerCache.getMaxObjectiveId();
        long maxListId      = mListCache.getMaxObjectiveId();

        long objectiveId = Math.max(maxListId, maxSchedulerId) + 1;

        boolean correctTransaction = true;

        EnlistedObjective  enlistedObjectiveToAdd  = null;
        ScheduledObjective scheduledObjectiveToAdd = null;
        if(!createDateTime.isBefore(enlistDateTime) && repeatDuration == Duration.ZERO && repeatProbability < 0.0001f && chainId == -1 && poolId == -1)
        {
            //One-time objective
            enlistedObjectiveToAdd = new EnlistedObjective(objectiveId, createDateTime, enlistDateTime,
                                                           objectiveName, objectiveDescription, objectiveTags);
        }
        else if(!createDateTime.isBefore(enlistDateTime) && chainId == -1 && poolId == -1)
        {
            //Repeated objective which gets added to the list immediately
            scheduledObjectiveToAdd = new ScheduledObjective(objectiveId, objectiveName, objectiveDescription,
                                                             createDateTime, enlistDateTime, objectiveTags,
                                                             repeatDuration, repeatProbability);

            enlistedObjectiveToAdd = scheduledObjectiveToAdd.obtainEnlistedObjective(mListCache.constructBlockingIds(), enlistDateTime, dayStartHour);
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
            LockedWriteFile listWriteFile = new LockedWriteFile(listFilePath);
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
            //1. Lock scheduler file
            LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
            if(!mSchedulerCache.addObjective(poolId, chainId, addToChainBeginning, scheduledObjectiveToAdd))
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
            invalidateSchedulerCache(schedulerFilePath);
            invalidateListCache(listFilePath);

            return -1;
        }

        return objectiveId;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean editObjectiveTransaction(String configFolder, long objectiveId, String objectiveName, String objectiveDescription,
                                                         boolean editInScheduler, boolean editInList)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;
        String listFilePath      = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;

        boolean editSchedulerSucceeded = true;
        boolean editListSucceeded      = true;

        if(editInScheduler)
        {
            invalidateSchedulerCache(schedulerFilePath);

            LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
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
            invalidateListCache(listFilePath);

            EnlistedObjective objectiveToEdit = mListCache.getObjective(objectiveId);
            if(objectiveToEdit != null)
            {
                //1. Lock list file
                LockedWriteFile listWriteFile = new LockedWriteFile(listFilePath);
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

        invalidateSchedulerCache(schedulerFilePath);
        invalidateListCache(listFilePath);
        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean deleteObjectiveFromListTransaction(String configFolder, EnlistedObjective objective)
    {
        String listFilePath = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;

        invalidateListCache(listFilePath);

        LockedWriteFile listWriteFile = new LockedWriteFile(listFilePath);
        if(mListCache.removeObjective(objective))
        {
            if(mListCache.flush(listWriteFile))
            {
                listWriteFile.close();
                return true;
            }
        }

        listWriteFile.close();
        invalidateListCache(listFilePath);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean deleteObjectiveFromSchedulerTransaction(String configFolder, ScheduledObjective objective)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;
        invalidateSchedulerCache(schedulerFilePath);

        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
        if(mSchedulerCache.removeObjective(objective.getId()))
        {
            if(mSchedulerCache.flush(schedulerWriteFile))
            {
                schedulerWriteFile.close();
                return true;
            }
        }

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean updateObjectiveListTransaction(String configFolder, LocalDateTime enlistDateTime, int dayStartHour)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;
        String listFilePath      = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);
        invalidateListCache(listFilePath);

        //1. Lock scheduler and files
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
        LockedWriteFile listWriteFile      = new LockedWriteFile(listFilePath);

        //3. Prepare the list of objectives to add
        ArrayList<EnlistedObjective> enlistedObjectives = mSchedulerCache.dumpReadyObjectives(mListCache.constructBlockingIds(), enlistDateTime, dayStartHour);
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
        invalidateListCache(listFilePath);

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean rescheduleScheduledObjectiveTransaction(String configFolder, long objectiveId, LocalDateTime nextEnlistDate)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);

        //2. Lock scheduler file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);

        ScheduledObjective objective = mSchedulerCache.getObjectiveById(objectiveId);
        objective.rescheduleUnregulated(nextEnlistDate);

        if(mSchedulerCache.flush(schedulerWriteFile))
        {
            schedulerWriteFile.close();
            return true;
        }

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean rescheduleEnlistedObjectiveTransaction(String configFolder, EnlistedObjective objective, LocalDateTime nextEnlistDate)
    {
        String listFilePath      = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        if(objective.getEnlistDate().isAfter(nextEnlistDate))
        {
            return false;
        }

        invalidateSchedulerCache(schedulerFilePath);
        invalidateListCache(listFilePath);

        //1. Lock list and scheduler files
        LockedWriteFile listWriteFile      = new LockedWriteFile(listFilePath);
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);

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
        invalidateSchedulerCache(schedulerFilePath);

        listWriteFile.close();
        invalidateListCache(listFilePath);

        return false;
    }

    public synchronized long rechainEnlistedObjective(String configFolder, EnlistedObjective enlistedObjective)
    {
        String listFilePath      = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);
        invalidateListCache(listFilePath);

        long chainId = mSchedulerCache.findChainOfObjective(enlistedObjective.getId());
        if(chainId == -1)
        {
            String objectiveName = enlistedObjective.getName();
            chainId = addChainTransaction(-1, configFolder, objectiveName, "", Duration.ZERO);
        }

        ObjectiveChain chain = mSchedulerCache.getChainById(chainId);
        if(chain == null)
        {
            return -1;
        }

        //1. Lock list and scheduler files
        LockedWriteFile listWriteFile      = new LockedWriteFile(listFilePath);
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);

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
        invalidateSchedulerCache(schedulerFilePath);

        listWriteFile.close();
        invalidateListCache(listFilePath);

        return -1;
    }

    public synchronized long touchChainWithObjective(String configFolder, long objectiveId)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);

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
                chainId = addChainTransaction(-1, configFolder, objectiveName, "", Duration.ZERO);
            }
            else
            {
                String objectiveName = enlistedObjective.getName();
                chainId = addChainTransaction(-1, configFolder, objectiveName, "", Duration.ZERO);
            }
        }

        ObjectiveChain chain = mSchedulerCache.getChainById(chainId);
        if(chain == null)
        {
            return -1;
        }

        //2. Lock scheduler file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
        chain.bindObjectiveToChain(objectiveId);

        //6. Archive cache flush
        if(mSchedulerCache.flush(schedulerWriteFile))
        {
            schedulerWriteFile.close();
            return chainId;
        }

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        return -1;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean flipPauseObjective(String configFolder, long objectiveId)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);

        //2. Lock scheduler file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);

        ScheduledObjective objective = mSchedulerCache.getObjectiveById(objectiveId);
        objective.setPaused(!objective.isPaused());

        if(mSchedulerCache.flush(schedulerWriteFile))
        {
            schedulerWriteFile.close();
            return true;
        }

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean flipPauseChain(String configFolder, long chainId)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);

        //2. Lock scheduler file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);

        ObjectiveChain chain = mSchedulerCache.getChainById(chainId);
        chain.setPaused(!chain.isPaused());

        if(mSchedulerCache.flush(schedulerWriteFile))
        {
            schedulerWriteFile.close();
            return true;
        }

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean flipPausePool(String configFolder, long poolId)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);

        //2. Lock scheduler file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);

        ObjectivePool pool = mSchedulerCache.getPoolById(poolId);
        pool.setPaused(!pool.isPaused());

        if(mSchedulerCache.flush(schedulerWriteFile))
        {
            schedulerWriteFile.close();
            return true;
        }

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean bindObjectiveToChain(String configFolder, long chainId, long objectiveId)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);

        ObjectiveChain chain = mSchedulerCache.getChainById(chainId);
        if(chain == null)
        {
            return false;
        }

        //2. Lock scheduler file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
        chain.bindObjectiveToChain(objectiveId);

        //6. Archive cache flush
        if(mSchedulerCache.flush(schedulerWriteFile))
        {
            schedulerWriteFile.close();
            return true;
        }

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean finishObjectiveTransaction(String configFolder, EnlistedObjective objective, LocalDateTime finishDate)
    {
        String listFilePath    = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;
        String archiveFilePath = configFolder + "/" + ObjectiveArchiveCache.ARCHIVE_FILENAME;

        invalidateListCache(listFilePath);
        invalidateArchiveCache(archiveFilePath);

        //1. Lock list and archive files
        LockedWriteFile listWriteFile    = new LockedWriteFile(listFilePath);
        LockedWriteFile archiveWriteFile = new LockedWriteFile(archiveFilePath);

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
        invalidateArchiveCache(archiveFilePath);

        listWriteFile.close();
        invalidateListCache(listFilePath);

        return false;
    }

    private synchronized void invalidateSchedulerCache(String schedulerFilePath)
    {
        if(mSchedulerCache == null)
        {
            mSchedulerCache = new ObjectiveSchedulerCache();
        }

        try
        {
            LockedReadFile schedulerFile = new LockedReadFile(schedulerFilePath);
            mSchedulerCache.invalidate(schedulerFile);
            schedulerFile.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private synchronized void invalidateListCache(String listFilePath)
    {
        if(mListCache == null)
        {
            mListCache = new ObjectiveListCache();
        }

        try
        {
            LockedReadFile listFile = new LockedReadFile(listFilePath);
            mListCache.invalidate(listFile);
            listFile.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private synchronized void invalidateArchiveCache(String archiveFilePath)
    {
        if(mArchiveCache == null)
        {
            mArchiveCache = new ObjectiveArchiveCache();
        }

        try
        {
            LockedReadFile archiveFile = new LockedReadFile(archiveFilePath);
            mArchiveCache.invalidate(archiveFile);
            archiveFile.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
