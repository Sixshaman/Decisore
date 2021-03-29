package com.sixshaman.decisore.list;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.archive.ArchiveActivity;
import com.sixshaman.decisore.scheduler.SchedulerActivity;
import com.sixshaman.decisore.utils.LockedReadFile;
import com.sixshaman.decisore.utils.NewObjectiveDialogFragment;
import com.sixshaman.decisore.utils.TransactionDispatcher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

public class ListActivity extends AppCompatActivity implements ListObjectiveCountListener
{
    //Objective list
    private ObjectiveListCache mListCache;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_objective_list);
        Toolbar toolbar = findViewById(R.id.toolbarObjectiveList);
        setSupportActionBar(toolbar);

        FloatingActionButton buttonNewObjective = findViewById(R.id.addNewObjective);
        buttonNewObjective.setOnClickListener(view -> openAddObjectiveDialog());

        setTitle(R.string.title_activity_objective_list);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mListCache = new ObjectiveListCache();

        mListCache.setObjectiveCountListener(this);

        String configFolder = Objects.requireNonNull(getExternalFilesDir("/app")).getAbsolutePath();

        RecyclerView recyclerView = findViewById(R.id.objectiveListView);
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
        inflater.inflate(R.menu.menu_objective_list, menu);

        menu.findItem(R.id.menuOpenArchive).setOnMenuItemClickListener(item ->
        {
            Intent archiveOpenIntent = new Intent(ListActivity.this, ArchiveActivity.class);
            startActivity(archiveOpenIntent);
            return true;
        });

        menu.findItem(R.id.menuOpenScheduler).setOnMenuItemClickListener(item ->
        {
            Intent schedulerOpenIntent = new Intent(ListActivity.this, SchedulerActivity.class);
            startActivity(schedulerOpenIntent);
            return true;
        });

        return true;
    }

    private void openAddObjectiveDialog()
    {
        NewObjectiveDialogFragment newObjectiveDialogFragment = new NewObjectiveDialogFragment();
        newObjectiveDialogFragment.setListCache(mListCache);

        newObjectiveDialogFragment.show(getSupportFragmentManager(), getString(R.string.newObjectiveDialogName));
    }

    @Override
    public void onListObjectiveCountChanged(int newObjectiveCount)
    {
        setTitle(newObjectiveCount + getString(R.string.title_activity_objective_list_append));
    }
}
