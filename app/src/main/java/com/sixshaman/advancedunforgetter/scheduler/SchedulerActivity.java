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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.utils.LockedReadFile;
import com.sixshaman.advancedunforgetter.utils.TransactionDispatcher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

public class SchedulerActivity extends AppCompatActivity
{
    private static final String TAG = "SchedulerActivity";

    //Scheduler cache model
    private ObjectiveSchedulerCache mSchedulerCache;

    //https://stackoverflow.com/a/44508892
    private boolean mFabExpanded;

    //Dynamic offset to speed dial fab elements
    private float mFabPoolOffset;
    private float mFabChainOffset;
    private float mFabObjectiveOffset;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_scheduler);
        Toolbar toolbar = findViewById(R.id.toolbar_scheduler);
        setSupportActionBar(toolbar);

        setTitle(R.string.title_activity_task_scheduler);

        mFabExpanded = false;

        final ViewGroup fabContainer = findViewById(R.id.fab_container);

        final View fabAddPool      = findViewById(R.id.fab_add_pool);
        final View fabAddChain     = findViewById(R.id.fab_add_chain);
        final View fabAddObjective = findViewById(R.id.fab_add_objective);

        final ImageButton fabSpeedDial = findViewById(R.id.mainFabButton);
        fabSpeedDial.setOnClickListener(view ->
        {
            mFabExpanded = !mFabExpanded;
            if(mFabExpanded)
            {
                expandFab(fabSpeedDial, fabAddPool, fabAddChain, fabAddObjective);
            }
            else
            {
                collapseFab(fabSpeedDial, fabAddPool, fabAddChain, fabAddObjective);
            }
        });

        fabAddPool.setOnClickListener(this::addObjectivePool);
        fabAddChain.setOnClickListener(this::addObjectiveChain);
        fabAddObjective.setOnClickListener(this::addObjective);

        fabContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()
        {
            @Override
            public boolean onPreDraw()
            {
                fabContainer.getViewTreeObserver().removeOnPreDrawListener(this);

                mFabPoolOffset      = fabSpeedDial.getY() - fabAddPool.getY();
                mFabChainOffset     = fabSpeedDial.getY() - fabAddChain.getY();
                mFabObjectiveOffset = fabSpeedDial.getY() - fabAddObjective.getY();

                fabAddPool.setTranslationY(mFabPoolOffset);
                fabAddChain.setTranslationY(mFabChainOffset);
                fabAddObjective.setTranslationY(mFabObjectiveOffset);

                return true;
            }
        });
    }

    private void collapseFab(final ImageButton fab, final View fabAddPool, final View fabAddChain, final View fabAddObjective)
    {
        fab.setImageResource(R.drawable.animated_cross);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(createCollapseAnimator(fabAddPool,      mFabPoolOffset),
                                 createCollapseAnimator(fabAddChain,     mFabChainOffset),
                                 createCollapseAnimator(fabAddObjective, mFabObjectiveOffset));
        animatorSet.start();

        animateSpeedDialFab(fab);
    }

    private void expandFab(final ImageButton fab, final View fabAddPool, final View fabAddChain, final View fabAddObjective)
    {
        fab.setImageResource(R.drawable.animated_plus);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(createExpandAnimator(fabAddPool,      mFabPoolOffset),
                                 createExpandAnimator(fabAddChain,     mFabChainOffset),
                                 createExpandAnimator(fabAddObjective, mFabObjectiveOffset));
        animatorSet.start();

        animateSpeedDialFab(fab);
    }

    private Animator createCollapseAnimator(View view, float offset)
    {
        return ObjectAnimator.ofFloat(view, "translationY", 0, offset).setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
    }

    private Animator createExpandAnimator(View view, float offset)
    {
        return ObjectAnimator.ofFloat(view, "translationY", offset, 0).setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
    }

    private void animateSpeedDialFab(ImageButton fab)
    {
        Drawable drawable = fab.getDrawable();
        if(drawable instanceof Animatable)
        {
            ((Animatable)drawable).start();
        }
    }

    public void addObjectivePool(View view)
    {
        Toast.makeText(this, "Add objective pool", Toast.LENGTH_SHORT).show();
    }

    public void addObjectiveChain(View view)
    {
        Toast.makeText(this, "Add objective chain", Toast.LENGTH_SHORT).show();
    }

    public void addObjective(View view)
    {
        Toast.makeText(this, "Add objective", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mSchedulerCache = new ObjectiveSchedulerCache();

        String configFolder = Objects.requireNonNull(getExternalFilesDir("/app")).getAbsolutePath();

        RecyclerView recyclerView = findViewById(R.id.objectiveSchedulerView);
        mSchedulerCache.attachToView(recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        try
        {
            LockedReadFile schedulerFile = new LockedReadFile(configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME);
            mSchedulerCache.invalidate(schedulerFile);
            schedulerFile.close();

            TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
            transactionDispatcher.setSchedulerCache(mSchedulerCache);

            transactionDispatcher.updateObjectiveListTransaction(configFolder, LocalDateTime.now());
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private void openAddObjectiveDialog()
    {
        Toast.makeText(this, R.string.not_implemented, Toast.LENGTH_SHORT).show();
    }
}
