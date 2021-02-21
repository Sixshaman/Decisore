package com.sixshaman.advancedunforgetter.utils;

import android.util.Log;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

//The file with exclusive access only (no shared access possible)
public class LockedFile
{
    //The path to the file
    private final String mFilePath;

    //The locking mechanism of the file
    private FileLock mFileLock;

    public LockedFile(String path)
    {
        mFilePath = path;
        mFileLock = null;
    }

    public InputStreamReader openRead()
    {
        try
        {
            File file = new File(mFilePath);
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            mFileLock = randomAccessFile.getChannel().tryLock();
            if(mFileLock == null)
            {
                return null;
            }

            Log.i("FILE", "FILE " + mFilePath + " LOCKED!");
            return new InputStreamReader(Channels.newInputStream(randomAccessFile.getChannel()));
        }
        catch(OverlappingFileLockException | IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public OutputStreamWriter openWrite()
    {
        try
        {
            File file = new File(mFilePath);
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            mFileLock = randomAccessFile.getChannel().tryLock();
            if(mFileLock == null)
            {
                return null;
            }

            Log.i("FILE", "FILE " + mFilePath + " LOCKED!");
            return new OutputStreamWriter(Channels.newOutputStream(randomAccessFile.getChannel()));
        }
        catch(OverlappingFileLockException | IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    //Releases the lock on the file
    public void close()
    {
        try
        {
            if(mFileLock != null)
            {
                mFileLock.release();
                mFileLock = null;
            }

            Log.i("FILE", "FILE " + mFilePath + " UNLOCKED!");
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
