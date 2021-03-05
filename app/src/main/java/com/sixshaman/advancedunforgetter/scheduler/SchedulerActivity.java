package com.sixshaman.advancedunforgetter.scheduler;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.list.ObjectiveListCache;
import com.sixshaman.advancedunforgetter.utils.LockedReadFile;
import com.sixshaman.advancedunforgetter.utils.NewObjectiveDialogFragment;
import com.sixshaman.advancedunforgetter.utils.TransactionDispatcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

public class SchedulerActivity extends AppCompatActivity
{
    private static final String TAG = "SchedulerActivity";

    //Scheduler cache model
    private ObjectiveSchedulerCache mSchedulerCache;

    //FAB speed dial data (non-refined)
    //https://stackoverflow.com/questions/35375153/android-floatingactionbutton-speed-dial
    private static final String TRANSLATION_Y = "translationY";
    private ImageButton fab;
    private boolean expanded = false;
    private View fabAction1;
    private View fabAction2;
    private View fabAction3;
    private float offset1;
    private float offset2;
    private float offset3;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_scheduler);
        Toolbar toolbar = findViewById(R.id.toolbar_scheduler);
        setSupportActionBar(toolbar);

        setTitle(R.string.title_activity_task_scheduler);

        final ViewGroup fabContainer = (ViewGroup) findViewById(R.id.fab_container);

        fabAction1 = findViewById(R.id.fab_action_1);
        fabAction1.setOnClickListener(this::fabAction1);

        fabAction2 = findViewById(R.id.fab_action_2);
        fabAction2.setOnClickListener(this::fabAction2);

        fabAction3 = findViewById(R.id.fab_action_3);
        fabAction2.setOnClickListener(this::fabAction2);

        fab = (ImageButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                expanded = !expanded;
                if (expanded)
                {
                    expandFab();
                }
                else
                {
                    collapseFab();
                }
            }
        });

        fabContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()
        {
            @Override
            public boolean onPreDraw()
            {
                fabContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                offset1 = fab.getY() - fabAction1.getY();
                fabAction1.setTranslationY(offset1);
                offset2 = fab.getY() - fabAction2.getY();
                fabAction2.setTranslationY(offset2);
                offset3 = fab.getY() - fabAction3.getY();
                fabAction3.setTranslationY(offset3);
                return true;
            }
        });
    }

    private void collapseFab()
    {
        fab.setImageResource(R.drawable.animated_minus);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(createCollapseAnimator(fabAction1, offset1),
                                 createCollapseAnimator(fabAction2, offset2),
                                 createCollapseAnimator(fabAction3, offset3));
        animatorSet.start();
        animateFab();
    }

    private void expandFab()
    {
        fab.setImageResource(R.drawable.animated_plus);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(createExpandAnimator(fabAction1, offset1),
                                 createExpandAnimator(fabAction2, offset2),
                                 createExpandAnimator(fabAction3, offset3));
        animatorSet.start();
        animateFab();
    }

    private Animator createCollapseAnimator(View view, float offset)
    {
        return ObjectAnimator.ofFloat(view, TRANSLATION_Y, 0, offset)
                .setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
    }

    private Animator createExpandAnimator(View view, float offset)
    {
        return ObjectAnimator.ofFloat(view, TRANSLATION_Y, offset, 0)
                .setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
    }

    private void animateFab()
    {
        Drawable drawable = fab.getDrawable();
        if(drawable instanceof Animatable)
        {
            ((Animatable) drawable).start();
        }
    }

    public void fabAction1(View view)
    {
        Log.d(TAG, "Action 1");
        Toast.makeText(this, "Go shopping!", Toast.LENGTH_SHORT).show();
    }

    public void fabAction2(View view)
    {
        Log.d(TAG, "Action 2");
        Toast.makeText(this, "Gimme money!", Toast.LENGTH_SHORT).show();
    }

    public void fabAction3(View view)
    {
        Log.d(TAG, "Action 3");
        Toast.makeText(this, "Turn it up!", Toast.LENGTH_SHORT).show();
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
