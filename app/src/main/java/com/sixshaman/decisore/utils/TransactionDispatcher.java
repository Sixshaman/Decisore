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
    public synchronized boolean addPoolTransaction(String configFolder, String poolName, String poolDescription)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);

        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
        mSchedulerCache.addObjectivePool(poolName, poolDescription);

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

    public synchronized ObjectiveChain addChainTransaction(ObjectivePool poolToAddTo, String configFolder, String chainName, String chainDescription)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;
        invalidateSchedulerCache(schedulerFilePath);

        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);

        ObjectiveChain newChain = mSchedulerCache.addObjectiveChain(poolToAddTo, chainName, chainDescription);
        if(newChain != null)
        {
            if(mSchedulerCache.flush(schedulerWriteFile))
            {
                schedulerWriteFile.close();
                return newChain;
            }
        }

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        return null;
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
    public synchronized boolean addObjectiveTransaction(ObjectivePool poolToAddTo, ObjectiveChain chainToAddTo,
                                                        String configFolder, LocalDateTime createDateTime, LocalDateTime enlistDateTime,
                                                        Duration repeatDuration, float repeatProbability,
                                                        String objectiveName, String objectiveDescription, ArrayList<String> objectiveTags)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;
        String listFilePath      = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);
        invalidateListCache(listFilePath);

        long maxSchedulerId = mSchedulerCache.getMaxObjectiveId();
        long maxListId      = mListCache.getMaxObjectiveId();

        long objectiveId = Math.max(maxListId, maxSchedulerId) + 1;

        EnlistedObjective  enlistedObjectiveToAdd  = null;
        ScheduledObjective scheduledObjectiveToAdd = null;
        if(!createDateTime.isBefore(enlistDateTime))
        {
            if(repeatDuration == Duration.ZERO && repeatProbability < 0.0001f)
            {
                //One-time objective
                enlistedObjectiveToAdd = new EnlistedObjective(objectiveId, createDateTime, enlistDateTime,
                                                               objectiveName, objectiveDescription, objectiveTags);

                if(chainToAddTo != null)
                {
                    chainToAddTo.bindObjectiveToChain(enlistedObjectiveToAdd.getId());
                }
            }
            else
            {
                //Repeated objective which gets added to the list immediately
                scheduledObjectiveToAdd = new ScheduledObjective(objectiveId, objectiveName, objectiveDescription,
                                                                 createDateTime, enlistDateTime, objectiveTags,
                                                                 repeatDuration, repeatProbability);

                enlistedObjectiveToAdd = scheduledObjectiveToAdd.toEnlisted(enlistDateTime);
                scheduledObjectiveToAdd.reschedule(enlistDateTime);
            }
        }
        else
        {
            //Add a new truly scheduled objective
            scheduledObjectiveToAdd = new ScheduledObjective(objectiveId, objectiveName, objectiveDescription,
                                                             createDateTime, enlistDateTime, objectiveTags,
                                                             repeatDuration, repeatProbability);
        }

        boolean correctTransaction = true;

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
            if(!mSchedulerCache.addObjective(poolToAddTo, chainToAddTo, scheduledObjectiveToAdd))
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
        }

        return correctTransaction;
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
    public synchronized boolean updateObjectiveListTransaction(String configFolder, LocalDateTime enlistDateTime)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;
        String listFilePath      = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);
        invalidateListCache(listFilePath);

        //1. Lock scheduler and files
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
        LockedWriteFile listWriteFile      = new LockedWriteFile(listFilePath);

        //3. Prepare the list of objectives to add
        ArrayList<EnlistedObjective> enlistedObjectives = mSchedulerCache.dumpReadyObjectives(mListCache, enlistDateTime);
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
    public synchronized boolean rescheduleScheduledObjectiveTransaction(String configFolder, ScheduledObjective objective, LocalDateTime nextEnlistDate)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);

        //2. Lock scheduler file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
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

    public synchronized ObjectiveChain rechainEnlistedObjective(String configFolder, EnlistedObjective enlistedObjective)
    {
        String listFilePath      = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);
        invalidateListCache(listFilePath);

        ObjectiveChain chain = mSchedulerCache.findChainOfObjective(enlistedObjective.getId());
        if(chain == null)
        {
            String objectiveName = enlistedObjective.getName();
            chain = addChainTransaction(null, configFolder, objectiveName, "");
        }

        if(chain == null)
        {
            return null;
        }

        //1. Lock list and scheduler files
        LockedWriteFile listWriteFile      = new LockedWriteFile(listFilePath);
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);

        //3. Remove objective from list
        if(mListCache.removeObjective(enlistedObjective))
        {
            //4. Add objective to the archive
            ScheduledObjective scheduledObjective = enlistedObjective.toScheduled(enlistedObjective.getEnlistDate());
            chain.addObjectiveToChain(scheduledObjective);

            //5. List cache flush
            if(mListCache.flush(listWriteFile))
            {
                //6. Archive cache flush
                if(mSchedulerCache.flush(schedulerWriteFile))
                {
                    schedulerWriteFile.close();
                    listWriteFile.close();

                    return chain;
                }
            }
        }

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        listWriteFile.close();
        invalidateListCache(listFilePath);

        return null;
    }

    public synchronized ObjectiveChain touchChainWithObjective(String configFolder, EnlistedObjective enlistedObjective)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);

        ObjectiveChain chain = mSchedulerCache.findChainOfObjective(enlistedObjective.getId());
        if(chain == null)
        {
            String objectiveName = enlistedObjective.getName();
            chain = addChainTransaction(null, configFolder, objectiveName, "");
        }

        if(chain == null)
        {
            return null;
        }

        //2. Lock scheduler file
        LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);

        chain.bindObjectiveToChain(enlistedObjective.getId());

        //6. Archive cache flush
        if(mSchedulerCache.flush(schedulerWriteFile))
        {
            schedulerWriteFile.close();
            return chain;
        }

        schedulerWriteFile.close();
        invalidateSchedulerCache(schedulerFilePath);

        return null;
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
