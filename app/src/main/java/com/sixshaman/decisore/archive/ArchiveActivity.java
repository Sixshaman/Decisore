package com.sixshaman.decisore.archive;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.utils.LockedReadFile;

import java.io.IOException;
import java.util.Objects;

public class ArchiveActivity extends AppCompatActivity
{
    //The objective archive, the model of this activity
    @SuppressWarnings("FieldCanBeLocal")
    private ObjectiveArchiveCache mArchiveCache;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);
        Toolbar toolbar = findViewById(R.id.toolbarObjectiveArchive);
        setSupportActionBar(toolbar);

        setTitle(R.string.title_activity_objective_archive);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mArchiveCache = new ObjectiveArchiveCache();

        String configFolder = Objects.requireNonNull(getExternalFilesDir("/app")).getAbsolutePath();

        RecyclerView recyclerView = findViewById(R.id.objectiveArchiveView);
        mArchiveCache.attachToView(recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        try
        {
            LockedReadFile archiveFile = new LockedReadFile(configFolder + "/" + ObjectiveArchiveCache.ARCHIVE_FILENAME);
            mArchiveCache.invalidate(archiveFile);
            archiveFile.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
