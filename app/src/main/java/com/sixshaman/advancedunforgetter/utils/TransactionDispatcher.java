package com.sixshaman.advancedunforgetter.utils;

import com.sixshaman.advancedunforgetter.archive.ArchivedObjective;
import com.sixshaman.advancedunforgetter.archive.ObjectiveArchiveCache;
import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import com.sixshaman.advancedunforgetter.list.ObjectiveListCache;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveSchedulerCache;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;

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

    public synchronized boolean addTransaction(String configFolder)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;
        String listFilePath      = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;
    }

    public synchronized boolean updateTransaction(String configFolder, LocalDateTime enlistDateTime)
    {
        String schedulerFilePath = configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME;
        String listFilePath      = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;

        if(mSchedulerCache == null)
        {
            mSchedulerCache = new ObjectiveSchedulerCache();

            try
            {
                LockedReadFile schedulerFile = new LockedReadFile(schedulerFilePath);
                mSchedulerCache.invalidate(schedulerFile);
                schedulerFile.close();
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }

        if(mListCache == null)
        {
            mListCache = new ObjectiveListCache();

            try
            {
                LockedReadFile listFile = new LockedReadFile(listFilePath);
                mListCache.invalidate(listFile);
                listFile.close();
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }

        //1. Lock list file
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
            //2. Lock archive file
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

                try
                {
                    LockedReadFile listReadFile = new LockedReadFile(listFilePath);
                    mListCache.invalidate(listReadFile);
                    listReadFile.close();
                }
                catch(FileNotFoundException e)
                {
                    e.printStackTrace();
                }
            }

            schedulerWriteFile.close();

            try
            {
                LockedReadFile schedulerReadFile = new LockedReadFile(schedulerFilePath);
                mSchedulerCache.invalidate(schedulerReadFile);
                schedulerReadFile.close();
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }

        return false;
    }

    public synchronized boolean finishObjectiveTransaction(String configFolder, EnlistedObjective objective, LocalDateTime finishDate)
    {
        String listFilePath    = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;
        String archiveFilePath = configFolder + "/" + ObjectiveArchiveCache.ARCHIVE_FILENAME;

        if(mListCache == null)
        {
            mListCache = new ObjectiveListCache();

            try
            {
                LockedReadFile listFile = new LockedReadFile(listFilePath);
                mListCache.invalidate(listFile);
                listFile.close();
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }

        if(mArchiveCache == null)
        {
            mArchiveCache = new ObjectiveArchiveCache();

            try
            {
                LockedReadFile archiveFile = new LockedReadFile(archiveFilePath);
                mArchiveCache.invalidate(archiveFile);
                archiveFile.close();
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }

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

                try
                {
                    LockedReadFile archiveReadFile = new LockedReadFile(archiveFilePath);
                    mArchiveCache.invalidate(archiveReadFile);
                    archiveReadFile.close();
                }
                catch(FileNotFoundException e)
                {
                    e.printStackTrace();
                }
            }

            listWriteFile.close();

            try
            {
                LockedReadFile listReadFile = new LockedReadFile(listFilePath);
                mListCache.invalidate(listReadFile);
                listReadFile.close();
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }

        return false;
    }
}
