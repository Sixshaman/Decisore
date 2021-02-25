package com.sixshaman.advancedunforgetter.utils;

import android.util.Log;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

public class LockedWriteFile
{
    //The path to the file
    private String mFilePath;

    //The locking mechanism of the file
    private FileLock mFileLock;

    //File contents writer
    private OutputStreamWriter mOutputStreamWriter;

    public LockedWriteFile(String path) throws FileNotFoundException
    {
        mFilePath = path;
        mFileLock = null;

        while(mFileLock == null)
        {
            try
            {
                File file = new File(mFilePath);
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "w");

                FileChannel channel = randomAccessFile.getChannel();

                mFileLock = channel.tryLock();
                if(mFileLock != null)
                {
                    Log.i("FILE", "FILE " + mFilePath + " LOCKED!");
                    mOutputStreamWriter = new OutputStreamWriter(Channels.newOutputStream(channel));
                }
            }
            catch(OverlappingFileLockException | IOException e)
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

    //Writes the contents to the file
    public void write(String contents)
    {
        if(mOutputStreamWriter == null || mFileLock == null)
        {
            return;
        }

        try
        {
            BufferedWriter bufferedWriter = new BufferedWriter(mOutputStreamWriter);
            bufferedWriter.write(contents);
            bufferedWriter.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
