package com.sixshaman.advancedunforgetter.scheduler;

import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.sixshaman.advancedunforgetter.R;

public class SchedulerActivity extends AppCompatActivity
{
    private static final String TAG = "SchedulerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_objective_scheduler);
        Toolbar toolbar = findViewById(R.id.toolbar_scheduler);
        setSupportActionBar(toolbar);

        setTitle(R.string.title_activity_objective_scheduler);

        if (savedInstanceState == null)
        {
            FragmentManager fragmentManager = getSupportFragmentManager();

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.setReorderingAllowed(true);
            fragmentTransaction.replace(R.id.scheduler_fragment_container_view, SchedulerFragment.class, null);
            fragmentTransaction.commit();
        }
    }
}
