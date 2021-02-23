package com.sixshaman.advancedunforgetter.utils;

import com.sixshaman.advancedunforgetter.archive.ArchivedObjective;
import com.sixshaman.advancedunforgetter.archive.ObjectiveArchiveCache;
import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import com.sixshaman.advancedunforgetter.list.ObjectiveListCache;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.Objects;

public class TransactionDispatcher
{
    //Caches
    private ObjectiveListCache    mObjectiveListCache;
    private ObjectiveArchiveCache mObjectiveArchiveCache;

    public boolean finishObjectiveTransaction(String configFolder, EnlistedObjective objective, LocalDateTime finishDate)
    {
        String listFilePath    = configFolder + "/" + ObjectiveListCache.LIST_FILENAME;
        String archiveFilePath = configFolder + "/" + ObjectiveArchiveCache.ARCHIVE_FILENAME;

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
                archiveWriteFile = new LockedWriteFile(listFilePath);
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }

            if(archiveWriteFile != null)
            {
                //3. Remove objective from list
                if(mObjectiveListCache.removeObjective(objective))
                {
                    //4. Add objective to the archive
                    ArchivedObjective archivedObjective = objective.toArchived(finishDate);
                    if(mObjectiveArchiveCache.addObjective(archivedObjective))
                    {
                        //5. List cache flush
                        if(mObjectiveListCache.flush(listWriteFile))
                        {
                            //6. Archive cache flush
                            if(mObjectiveArchiveCache.flush(archiveWriteFile))
                            {
                                archiveWriteFile.close();
                                listWriteFile.close();

                                return true;
                            }
                        }

                        try
                        {
                            LockedReadFile archiveReadFile = new LockedReadFile(archiveFilePath);
                            mObjectiveArchiveCache.invalidate(archiveReadFile);
                            archiveReadFile.close();
                        }
                        catch(FileNotFoundException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    try
                    {
                        LockedReadFile listReadFile = new LockedReadFile(listFilePath);
                        mObjectiveListCache.invalidate(listReadFile);
                        listReadFile.close();
                    }
                    catch(FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                }

                archiveWriteFile.close();
            }

            listWriteFile.close();
        }

        return false;
    }
}
