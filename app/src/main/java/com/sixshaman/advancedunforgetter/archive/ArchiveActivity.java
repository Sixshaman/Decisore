package com.sixshaman.advancedunforgetter.archive;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import com.sixshaman.advancedunforgetter.R;

import java.util.Objects;

public class ArchiveActivity extends AppCompatActivity
{
    //The task archive, the model of this activity
    private TaskArchive mArchive;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);
        Toolbar toolbar = findViewById(R.id.toolbarTaskArchive);
        setSupportActionBar(toolbar);

        setTitle(R.string.title_activity_task_archive);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mArchive = new TaskArchive();

        mArchive.setConfigFolder(Objects.requireNonNull(getExternalFilesDir("/app")).getAbsolutePath());

        RecyclerView recyclerView = findViewById(R.id.taskArchiveView);
        recyclerView.setAdapter(mArchive);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mArchive.loadFinishedTasks();
    }
}