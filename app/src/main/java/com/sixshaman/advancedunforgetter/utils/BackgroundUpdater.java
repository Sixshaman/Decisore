package com.sixshaman.advancedunforgetter.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class BackgroundUpdater extends Worker
{
    private static final String TAG = "BackgroundUpdater";

    public BackgroundUpdater(@NonNull Context context, @NonNull WorkerParameters workerParams)
    {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork()
    {
        return Result.success();
    }
}
