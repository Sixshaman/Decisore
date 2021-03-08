package com.sixshaman.advancedunforgetter.scheduler;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.*;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.utils.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

public class SchedulerActivity extends AppCompatActivity
{
    private static final String TAG = "SchedulerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_scheduler);
        Toolbar toolbar = findViewById(R.id.toolbar_scheduler);
        setSupportActionBar(toolbar);

        setTitle(R.string.title_activity_task_scheduler);

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
