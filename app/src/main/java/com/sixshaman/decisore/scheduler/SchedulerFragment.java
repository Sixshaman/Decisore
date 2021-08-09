package com.sixshaman.decisore.scheduler;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.scheduler.chain.NewChainDialogFragment;
import com.sixshaman.decisore.scheduler.pool.NewPoolDialogFragment;
import com.sixshaman.decisore.utils.*;

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
    private float mFabPoolTextOffset;
    private float mFabChainTextOffset;
    private float mFabObjectiveTextOffset;

    public SchedulerFragment()
    {
        mFabExpanded = false;

        mFabPoolOffset      = 0.0f;
        mFabChainOffset     = 0.0f;
        mFabObjectiveOffset = 0.0f;
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

        SchedulerActivity activity = ((SchedulerActivity) requireActivity());
        Objects.requireNonNull(activity.getSupportActionBar()).setTitle(R.string.title_activity_objective_scheduler);

        mSchedulerCache = new ObjectiveSchedulerCache();

        String configFolder = Objects.requireNonNull(mFragmentView.getContext().getExternalFilesDir("/app")).getAbsolutePath();

        RecyclerView recyclerView = mFragmentView.findViewById(R.id.objectiveSchedulerView);
        mSchedulerCache.attachToSchedulerView(recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(mFragmentView.getContext()));

        try
        {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext().getApplicationContext());
            String dayStartTimeString = sharedPreferences.getString("day_start_time", "6");
            int dayStartTime = ParseUtils.parseInt(dayStartTimeString, 6);

            LockedReadFile schedulerFile = new LockedReadFile(configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME);
            mSchedulerCache.invalidate(schedulerFile);
            schedulerFile.close();

            TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
            transactionDispatcher.setSchedulerCache(mSchedulerCache);

            transactionDispatcher.updateObjectiveListTransaction(configFolder, LocalDateTime.now(), dayStartTime);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_scheduler, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        mFabExpanded = false;

        mFragmentView = view;

        final ViewGroup fabContainer = mFragmentView.findViewById(R.id.fab_container);

        final View fabAddPool      = mFragmentView.findViewById(R.id.fab_add_pool_layout);
        final View fabAddChain     = mFragmentView.findViewById(R.id.fab_add_chain_layout);
        final View fabAddObjective = mFragmentView.findViewById(R.id.fab_add_objective_layout);

        final View fabAddPoolText      = mFragmentView.findViewById(R.id.fab_add_pool_text);
        final View fabAddChainText     = mFragmentView.findViewById(R.id.fab_add_chain_text);
        final View fabAddObjectiveText = mFragmentView.findViewById(R.id.fab_add_objective_text);

        final ImageButton fabSpeedDial = mFragmentView.findViewById(R.id.mainFabButton);
        fabSpeedDial.setOnClickListener(fabView ->
        {
            mFabExpanded = !mFabExpanded;
            if(mFabExpanded)
            {
                expandFab(fabSpeedDial, fabAddPool, fabAddChain, fabAddObjective, fabAddPoolText, fabAddChainText, fabAddObjectiveText);
            }
            else
            {
                collapseFab(fabSpeedDial, fabAddPool, fabAddChain, fabAddObjective, fabAddPoolText, fabAddChainText, fabAddObjectiveText);
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

                mFabPoolTextOffset      = fabSpeedDial.getX() - fabAddPoolText.getX();
                mFabChainTextOffset     = fabSpeedDial.getX() - fabAddChainText.getX();
                mFabObjectiveTextOffset = fabSpeedDial.getX() - fabAddObjectiveText.getX();

                fabAddPool.setTranslationY(mFabPoolOffset);
                fabAddChain.setTranslationY(mFabChainOffset);
                fabAddObjective.setTranslationY(mFabObjectiveOffset);

                fabAddPoolText.setTranslationX(mFabPoolTextOffset);
                fabAddChainText.setTranslationX(mFabChainTextOffset);
                fabAddObjectiveText.setTranslationX(mFabObjectiveTextOffset);

                return true;
            }
        });
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
        newObjectiveDialogFragment.setTomorrowDefault(true);
        newObjectiveDialogFragment.setSchedulerCache(mSchedulerCache);

        newObjectiveDialogFragment.setOnAfterObjectiveCreatedListener(objectiveId ->
        {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext().getApplicationContext());
            String dayStartTimeString = sharedPreferences.getString("day_start_time", "6");
            int dayStartTime = ParseUtils.parseInt(dayStartTimeString, 6);

            TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
            transactionDispatcher.setSchedulerCache(mSchedulerCache);

            String configFolder = Objects.requireNonNull(mFragmentView.getContext().getExternalFilesDir("/app")).getAbsolutePath();
            transactionDispatcher.updateObjectiveListTransaction(configFolder, LocalDateTime.now(), dayStartTime);
        });

        newObjectiveDialogFragment.show(getParentFragmentManager(), getString(R.string.newObjectiveDialogName));
    }

    private void collapseFab(final ImageButton fab, final View fabAddPool, final View fabAddChain, final View fabAddObjective, final View fabAddPoolText, final View fabAddChainText, final View fabAddObjectiveText)
    {
        fab.setImageResource(R.drawable.animated_cross);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(createCollapseAnimator(fabAddPool,              mFabPoolOffset),
                                 createCollapseAnimator(fabAddChain,             mFabChainOffset),
                                 createCollapseAnimator(fabAddObjective,         mFabObjectiveOffset),
                                 createCollapseTextAnimator(fabAddPoolText,      mFabPoolTextOffset),
                                 createCollapseTextAnimator(fabAddChainText,     mFabChainTextOffset),
                                 createCollapseTextAnimator(fabAddObjectiveText, mFabObjectiveTextOffset));
        animatorSet.start();

        animateSpeedDialFab(fab);
    }

    private void expandFab(final ImageButton fab, final View fabAddPool, final View fabAddChain, final View fabAddObjective, final View fabAddPoolText, final View fabAddChainText, final View fabAddObjectiveText)
    {
        fab.setImageResource(R.drawable.animated_plus);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(createExpandAnimator(fabAddPool,              mFabPoolOffset),
                                 createExpandAnimator(fabAddChain,             mFabChainOffset),
                                 createExpandAnimator(fabAddObjective,         mFabObjectiveOffset),
                                 createExpandTextAnimator(fabAddPoolText,      mFabPoolTextOffset),
                                 createExpandTextAnimator(fabAddChainText,     mFabChainTextOffset),
                                 createExpandTextAnimator(fabAddObjectiveText, mFabObjectiveTextOffset));
        animatorSet.start();

        animateSpeedDialFab(fab);
    }

    private Animator createCollapseAnimator(View view, float offset)
    {
        return ObjectAnimator.ofFloat(view, "translationY", 0, offset).setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
    }

    private Animator createCollapseTextAnimator(View view, float offset)
    {
        return ObjectAnimator.ofFloat(view, "translationX", 0, offset).setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
    }

    private Animator createExpandAnimator(View view, float offset)
    {
        return ObjectAnimator.ofFloat(view, "translationY", offset, 0).setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
    }

    private Animator createExpandTextAnimator(View view, float offset)
    {
        return ObjectAnimator.ofFloat(view, "translationX", offset, 0).setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
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