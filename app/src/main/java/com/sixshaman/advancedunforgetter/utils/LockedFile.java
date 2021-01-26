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

    //The stream to read from the file
    InputStreamReader mInputStreamReader;

    //The stream to write to the file
    OutputStreamWriter mOutputStreamWriter;

    public LockedFile(String path)
    {
        mFilePath = path;
        mFileLock = null;

        mInputStreamReader  = null;
        mOutputStreamWriter = null;
    }

    public boolean isLocked()
    {
        return mFileLock != null && mFileLock.isValid();
    }

    //Lock the file, preventing all processes to access this file
    public boolean lock()
    {
        //Do nothing if already locked
        if(mInputStreamReader != null || mOutputStreamWriter != null || mFileLock != null)
        {
            return false;
        }

        try
        {
            File file = new File(mFilePath);
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            mFileLock = randomAccessFile.getChannel().tryLock();
            if(mFileLock == null)
            {
                return false;
            }

            mInputStreamReader  = new InputStreamReader(Channels.newInputStream(randomAccessFile.getChannel()));
            mOutputStreamWriter = new OutputStreamWriter(Channels.newOutputStream(randomAccessFile.getChannel()));
        }
        catch(OverlappingFileLockException | IOException e)
        {
            mInputStreamReader  = null;
            mOutputStreamWriter = null;

            mFileLock = null;

            e.printStackTrace();
            return false;
        }

        Log.i("FILE", "FILE " + mFilePath + " LOCKED!");
        return true;
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
            BufferedReader bufferedReader = new BufferedReader(mInputStreamReader);

            String line;
            StringBuilder fileContentsStringBuilder = new StringBuilder();

            while((line = bufferedReader.readLine()) != null)
            {
                fileContentsStringBuilder.append(line);
            }

            bufferedReader.close();
            return fileContentsStringBuilder.toString();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return "";
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

    //Releases the lock on the file
    public void unlock()
    {
        try
        {
            if(mFileLock != null)
            {
                mFileLock.release();
                mFileLock = null;
            }

            if(mInputStreamReader != null)
            {
                mInputStreamReader.close();
                mInputStreamReader = null;
            }

            if(mOutputStreamWriter != null)
            {
                mOutputStreamWriter.close();
                mOutputStreamWriter = null;
            }

            Log.i("FILE", "FILE " + mFilePath + " LOCKED!");
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
