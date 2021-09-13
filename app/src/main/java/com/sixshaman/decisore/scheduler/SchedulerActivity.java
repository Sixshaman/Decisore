package com.sixshaman.decisore.scheduler;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.scheduler.chain.ChainFragment;
import com.sixshaman.decisore.scheduler.pool.PoolFragment;

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

        Intent intent = getIntent();
        long elementId = intent.getLongExtra("ElementId", -1);
        String elementType = intent.getStringExtra("ElementType");

        Bundle bundle = new Bundle();
        bundle.putLong("EyyDee", elementId);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setReorderingAllowed(true);

        if(elementId != -1 && elementType.equals("ObjectivePool"))
        {
            fragmentTransaction.replace(R.id.scheduler_fragment_container_view, PoolFragment.class, bundle);
        }
        else if(elementId != -1 && elementType.equals("ObjectiveChain"))
        {
            fragmentTransaction.replace(R.id.scheduler_fragment_container_view, ChainFragment.class, bundle);
        }
        else
        {
            fragmentTransaction.replace(R.id.scheduler_fragment_container_view, SchedulerFragment.class, null);
        }

        fragmentTransaction.commit();
    }
}
