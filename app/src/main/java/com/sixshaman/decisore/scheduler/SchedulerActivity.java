package com.sixshaman.decisore.scheduler;

import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.sixshaman.decisore.R;

public class SchedulerActivity extends AppCompatActivity
{
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
