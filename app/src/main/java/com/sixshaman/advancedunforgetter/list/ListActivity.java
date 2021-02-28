package com.sixshaman.advancedunforgetter.list;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.archive.ArchiveActivity;
import com.sixshaman.advancedunforgetter.utils.LockedReadFile;
import com.sixshaman.advancedunforgetter.utils.NewObjectiveDialogFragment;
import com.sixshaman.advancedunforgetter.utils.TransactionDispatcher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

public class ListActivity extends AppCompatActivity
{
    //Objective list
    private ObjectiveListCache mListCache;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);
        Toolbar toolbar = findViewById(R.id.toolbarTaskList);
        setSupportActionBar(toolbar);

        //The only way to get the name for the application right
        setTitle(R.string.title_activity_task_list);

        FloatingActionButton buttonNewTask = findViewById(R.id.addNewTask);
        buttonNewTask.setOnClickListener(view -> openAddTaskDialog());
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mListCache = new ObjectiveListCache();

        String configFolder = Objects.requireNonNull(getExternalFilesDir("/app")).getAbsolutePath();

        RecyclerView recyclerView = findViewById(R.id.taskListView);
        mListCache.attachToView(recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        try
        {
            LockedReadFile listFile = new LockedReadFile(configFolder + "/" + ObjectiveListCache.LIST_FILENAME);
            mListCache.invalidate(listFile);
            listFile.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
        transactionDispatcher.setListCache(mListCache);

        transactionDispatcher.updateObjectiveListTransaction(configFolder, LocalDateTime.now());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_task_list, menu);

        menu.findItem(R.id.menuOpenArchive).setOnMenuItemClickListener(item ->
        {
            Intent archiveOpenIntent = new Intent(ListActivity.this, ArchiveActivity.class);
            startActivity(archiveOpenIntent);
            return true;
        });

        return true;
    }

    private void openAddTaskDialog()
    {
        NewObjectiveDialogFragment newObjectiveDialogFragment = new NewObjectiveDialogFragment();
        newObjectiveDialogFragment.setListCache(mListCache);

        newObjectiveDialogFragment.show(getSupportFragmentManager(), getString(R.string.newTaskDialogName));
    }
}
