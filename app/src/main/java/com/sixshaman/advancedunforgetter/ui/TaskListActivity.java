package com.sixshaman.advancedunforgetter.ui;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.archive.TaskArchive;
import com.sixshaman.advancedunforgetter.list.TaskList;
import com.sixshaman.advancedunforgetter.scheduler.TaskScheduler;

import java.util.ArrayList;

public class TaskListActivity extends AppCompatActivity
{
    //Task scheduler to add new tasks there
    private TaskScheduler mTaskScheduler;

    //Task list (the model of this class)
    private TaskList mTaskList;

    //Task archive model
    private TaskArchive mTaskArchive;

    //Task list ui
    private RecyclerView mTaskListView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTaskArchive   = new TaskArchive();
        mTaskList      = new TaskList(mTaskArchive);
        mTaskScheduler = new TaskScheduler(mTaskList);

        mTaskList.setUiView(this);

        mTaskListView = findViewById(R.id.taskListView);

        FloatingActionButton buttonNewTask = findViewById(R.id.addNewTask);
        buttonNewTask.setOnClickListener(view ->
        {
            mTaskScheduler.addImmediateTask("LOL", "", new ArrayList<>());
        });
    }

    public void updateTasksUi(ArrayList<String> taskNames)
    {

    }
}
