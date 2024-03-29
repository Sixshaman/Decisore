package com.sixshaman.decisore.utils;

import android.util.Log;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

//The file with exclusive access only (no shared access possible)
public class LockedReadFile
{
    //The path to the file
    private final String mFilePath;

    //The locking mechanism of the file
    private FileLock mFileLock;

    //File contents reader
    private InputStreamReader mInputStreamReader;

    public LockedReadFile(String path) throws IOException
    {
        mFilePath = path;
        mFileLock = null;

        while(mFileLock == null)
        {
            try
            {
                File file = new File(mFilePath);
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

                FileChannel channel = randomAccessFile.getChannel();

                mFileLock = channel.tryLock();
                if(mFileLock != null)
                {
                    Log.i("FILE", "FILE " + mFilePath + " LOCKED!");
                    mInputStreamReader = new InputStreamReader(Channels.newInputStream(channel));
                }
            }
            catch(OverlappingFileLockException e)
            {
                e.printStackTrace();
            }
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

    //Reads the contents of the file
    public String read()
    {
        if(mInputStreamReader == null || mFileLock == null)
        {
            return "";
        }

        try
        {
            //Do not call close() on bufferedReader since it automatically releases the lock (which we don't need)
            BufferedReader bufferedReader = new BufferedReader(mInputStreamReader);

            String line;
            StringBuilder fileContentsStringBuilder = new StringBuilder();

            while((line = bufferedReader.readLine()) != null)
            {
                fileContentsStringBuilder.append(line);
            }

            return fileContentsStringBuilder.toString();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return "";
    }
}
