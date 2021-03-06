package com.sixshaman.advancedunforgetter.utils;

import com.sixshaman.advancedunforgetter.archive.ArchivedObjective;
import com.sixshaman.advancedunforgetter.archive.ObjectiveArchiveCache;
import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import com.sixshaman.advancedunforgetter.list.ObjectiveListCache;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveSchedulerCache;

import java.io.FileNotFoundException;
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

    public synchronized void setArchiveCache(ObjectiveArchiveCache archiveCache)
    {
        mArchiveCache = archiveCache;
    }

    public synchronized boolean addPoolTransaction(String configFolder, String poolName, String poolDescription)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);

        try
        {
            LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
            if(mSchedulerCache.addObjectivePool(poolName, poolDescription))
            {
                if(mSchedulerCache.flush(schedulerWriteFile))
                {
                    schedulerWriteFile.close();
                    return true;
                }
            }

            schedulerWriteFile.close();
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }

        invalidateSchedulerCache(schedulerFilePath);
        return false;
    }

    public synchronized boolean addChainTransaction(String configFolder, String chainName, String chainDescription)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);

        try
        {
            LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
            if(mSchedulerCache.addObjectiveChain(chainName, chainDescription))
            {
                if(mSchedulerCache.flush(schedulerWriteFile))
                {
                    schedulerWriteFile.close();
                    return true;
                }
            }

            schedulerWriteFile.close();
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }

        invalidateSchedulerCache(schedulerFilePath);
        return false;
    }

    public synchronized boolean addObjectiveTransaction(String configFolder, LocalDateTime createDateTime, LocalDateTime enlistDateTime,
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

        if(correctTransaction && enlistedObjectiveToAdd != null)
        {
            //1. Lock list file
            try
            {
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
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
                correctTransaction = false;
            }
        }

        if(correctTransaction && scheduledObjectiveToAdd != null)
        {
            //1. Lock scheduler file
            try
            {
                LockedWriteFile schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
                if(!mSchedulerCache.addObjective(null, null, scheduledObjectiveToAdd))
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
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
                correctTransaction = false;
            }
        }

        if(!correctTransaction)
        {
            invalidateSchedulerCache(schedulerFilePath);
            invalidateListCache(listFilePath);
        }

        return correctTransaction;
    }

    public synchronized boolean editObjectiveTransaction(String configFolder, long objectiveId, String objectiveName, String objectiveDescription)
    {
        String listFilePath = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;

        invalidateListCache(listFilePath);

        EnlistedObjective objectiveToEdit = mListCache.getObjective(objectiveId);
        if(objectiveToEdit != null)
        {
            //1. Lock list file
            try
            {
                LockedWriteFile listWriteFile = new LockedWriteFile(listFilePath);
                if(mListCache.editObjectiveName(objectiveId, objectiveName, objectiveDescription))
                {
                    if(mListCache.flush(listWriteFile))
                    {
                        listWriteFile.close();
                        return true;
                    }
                }

                listWriteFile.close();
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }

        invalidateListCache(listFilePath);
        return false;
    }

    public synchronized boolean deleteObjectiveTransaction(String configFolder, EnlistedObjective objective)
    {
        String listFilePath = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;

        invalidateListCache(listFilePath);

        LockedWriteFile listWriteFile = null;
        try
        {
            listWriteFile = new LockedWriteFile(listFilePath);
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }

        if(listWriteFile != null)
        {
            if(mListCache.removeObjective(objective))
            {
                if(mListCache.flush(listWriteFile))
                {
                    listWriteFile.close();
                    return true;
                }
            }

            listWriteFile.close();

            try
            {
                LockedReadFile listReadFile = new LockedReadFile(listFilePath);
                mListCache.invalidate(listReadFile);
                listReadFile.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }

        invalidateListCache(listFilePath);
        return false;
    }

    public synchronized boolean updateObjectiveListTransaction(String configFolder, LocalDateTime enlistDateTime)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;
        String listFilePath      = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;

        invalidateSchedulerCache(schedulerFilePath);
        invalidateListCache(listFilePath);

        //1. Lock scheduler file
        LockedWriteFile schedulerWriteFile = null;
        try
        {
            schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }

        if(schedulerWriteFile != null)
        {
            //2. Lock list file
            LockedWriteFile listWriteFile = null;
            try
            {
                listWriteFile = new LockedWriteFile(listFilePath);
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }

            if(listWriteFile != null)
            {
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
            }

            schedulerWriteFile.close();
            invalidateSchedulerCache(schedulerFilePath);
        }

        return false;
    }

    public synchronized boolean recheduleObjectiveTransaction(String configFolder, EnlistedObjective objective, LocalDateTime nextEnlistDate)
    {
        String listFilePath      = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;

        if(objective.getEnlistDate().isAfter(nextEnlistDate))
        {
            return false;
        }

        invalidateSchedulerCache(schedulerFilePath);
        invalidateListCache(listFilePath);

        //1. Lock list file
        LockedWriteFile listWriteFile = null;
        try
        {
            listWriteFile = new LockedWriteFile(listFilePath);
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }

        if(listWriteFile != null)
        {
            //2. Lock scheduler file
            LockedWriteFile schedulerWriteFile = null;
            try
            {
                schedulerWriteFile = new LockedWriteFile(schedulerFilePath);
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }

            if(schedulerWriteFile != null)
            {
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
            }

            listWriteFile.close();
            invalidateListCache(listFilePath);
        }

        return false;
    }

    public synchronized boolean finishObjectiveTransaction(String configFolder, EnlistedObjective objective, LocalDateTime finishDate)
    {
        String listFilePath    = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;
        String archiveFilePath = configFolder + "/" + ObjectiveArchiveCache.ARCHIVE_FILENAME;

        invalidateListCache(listFilePath);
        invalidateArchiveCache(archiveFilePath);

        //1. Lock list file
        LockedWriteFile listWriteFile = null;
        try
        {
            listWriteFile = new LockedWriteFile(listFilePath);
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }

        if(listWriteFile != null)
        {
            //2. Lock archive file
            LockedWriteFile archiveWriteFile = null;
            try
            {
                archiveWriteFile = new LockedWriteFile(archiveFilePath);
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }

            if(archiveWriteFile != null)
            {
                //3. Remove objective from list
                if(mListCache.removeObjective(objective))
                {
                    //4. Add objective to the archive
                    ArchivedObjective archivedObjective = objective.toArchived(finishDate);
                    if(mArchiveCache.addObjective(archivedObjective))
                    {
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
                }

                archiveWriteFile.close();
                invalidateArchiveCache(archiveFilePath);
            }

            listWriteFile.close();
            invalidateListCache(listFilePath);
        }

        return false;
    }

    private synchronized void invalidateSchedulerCache(String schedulerFilePath)
    {
        if(mSchedulerCache == null)
        {
            mSchedulerCache = new ObjectiveSchedulerCache();

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
    }

    private synchronized void invalidateListCache(String listFilePath)
    {
        if(mListCache == null)
        {
            mListCache = new ObjectiveListCache();

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
    }

    private synchronized void invalidateArchiveCache(String archiveFilePath)
    {
        if(mArchiveCache == null)
        {
            mArchiveCache = new ObjectiveArchiveCache();

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
}
