package com.sixshaman.advancedunforgetter.scheduler;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.dummy.DummyContent;
import com.sixshaman.advancedunforgetter.utils.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

public class SchedulerFragment extends Fragment
{
    private View mFragmentView;

    //Scheduler cache model
    private ObjectiveSchedulerCache mSchedulerCache;

    //https://stackoverflow.com/a/44508892
    private boolean mFabExpanded;

    //Dynamic offset to speed dial fab elements
    private float mFabPoolOffset;
    private float mFabChainOffset;
    private float mFabObjectiveOffset;

    public SchedulerFragment()
    {
    }

    public static SchedulerFragment newInstance(int columnCount)
    {
        SchedulerFragment fragment = new SchedulerFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        mSchedulerCache = new ObjectiveSchedulerCache();

        String configFolder = Objects.requireNonNull(mFragmentView.getContext().getExternalFilesDir("/app")).getAbsolutePath();

        RecyclerView recyclerView = mFragmentView.findViewById(R.id.objectiveSchedulerView);
        mSchedulerCache.attachToView(recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(mFragmentView.getContext()));

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mFragmentView = inflater.inflate(R.layout.fragment_scheduler, container, false);

        mFabExpanded = false;

        final ViewGroup fabContainer = mFragmentView.findViewById(R.id.fab_container);

        final View fabAddPool      = mFragmentView.findViewById(R.id.fab_add_pool);
        final View fabAddChain     = mFragmentView.findViewById(R.id.fab_add_chain);
        final View fabAddObjective = mFragmentView.findViewById(R.id.fab_add_objective);

        final ImageButton fabSpeedDial = mFragmentView.findViewById(R.id.mainFabButton);
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

        return mFragmentView;
    }

    private void addObjectivePool(View view)
    {
        NewPoolDialogFragment newPoolDialogFragment = new NewPoolDialogFragment();
        newPoolDialogFragment.setSchedulerCache(mSchedulerCache);

        newPoolDialogFragment.show(getParentFragmentManager(), getString(R.string.newPoolDialogName));
    }

    private void addObjectiveChain(View view)
    {
        NewChainDialogFragment newChainDialogFragment = new NewChainDialogFragment();
        newChainDialogFragment.setSchedulerCache(mSchedulerCache);

        newChainDialogFragment.show(getParentFragmentManager(), getString(R.string.newChainDialogName));
    }

    private void addObjective(View view)
    {
        NewObjectiveDialogFragment newObjectiveDialogFragment = new NewObjectiveDialogFragment();
        newObjectiveDialogFragment.setSchedulerCache(mSchedulerCache);

        newObjectiveDialogFragment.show(getParentFragmentManager(), getString(R.string.newTaskDialogName));
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
}