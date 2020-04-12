package com.sixshaman.advancedunforgetter.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.sixshaman.advancedunforgetter.list.TaskList;
import com.sixshaman.advancedunforgetter.scheduler.TaskScheduler;

//The class to do background updates
public class BackgroundUpdater extends Worker
{
    private static final String TAG = "BackgroundUpdater";

    public static final String DATA_CONFIG_FOLDER = "ConfigFolder";

    public BackgroundUpdater(@NonNull Context context, @NonNull WorkerParameters workerParams)
    {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork()
    {
        Data workParamsData = getInputData();
        String configFolder = workParamsData.getString(DATA_CONFIG_FOLDER);

        TaskScheduler scheduler = new TaskScheduler();
        TaskList      list      = new TaskList();

        scheduler.setTaskList(list);

        scheduler.setConfigFolder(configFolder);
        list.setConfigFolder(configFolder);

        scheduler.loadScheduledTasks();
        list.loadTasks();

        scheduler.update();
        return Result.success();
    }
}
