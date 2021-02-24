package com.sixshaman.advancedunforgetter.archive;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.utils.LockedReadFile;

import java.io.FileNotFoundException;
import java.util.Objects;

public class ArchiveActivity extends AppCompatActivity
{
    //The objective archive, the model of this activity
    private ObjectiveArchiveCache mArchiveCache;

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

        mArchiveCache = new ObjectiveArchiveCache();

        String configFolder = Objects.requireNonNull(getExternalFilesDir("/app")).getAbsolutePath();

        RecyclerView recyclerView = findViewById(R.id.taskArchiveView);
        mArchiveCache.attachToView(recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        try
        {
            LockedReadFile archiveFile = new LockedReadFile(configFolder + "/" + ObjectiveArchiveCache.ARCHIVE_FILENAME);
            mArchiveCache.invalidate(archiveFile);
            archiveFile.close();
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }
}
