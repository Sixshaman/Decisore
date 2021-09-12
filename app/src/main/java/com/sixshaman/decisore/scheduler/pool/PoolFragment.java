package com.sixshaman.decisore.scheduler.pool;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.decisore.scheduler.SchedulerActivity;
import com.sixshaman.decisore.utils.LockedReadFile;
import com.sixshaman.decisore.scheduler.chain.NewChainDialogFragment;
import com.sixshaman.decisore.utils.NewObjectiveDialogFragment;
import com.sixshaman.decisore.utils.ParseUtils;
import com.sixshaman.decisore.utils.TransactionDispatcher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

public class PoolFragment extends Fragment
{
    private View mFragmentView;

    //Scheduler cache model
    private ObjectiveSchedulerCache mSchedulerCache;

    //The id of the pool displayed
    private long mObjectivePoolId;

    //https://stackoverflow.com/a/44508892
    private boolean mFabExpanded;

    //Dynamic offset to speed dial fab elements
    private float mFabChainOffset;
    private float mFabObjectiveOffset;
    private float mFabChainTextOffset;
    private float mFabObjectiveTextOffset;

    public PoolFragment()
    {
        mObjectivePoolId = -1;
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

        try
        {
            LockedReadFile schedulerFile = new LockedReadFile(configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME);
            mSchedulerCache.invalidate(schedulerFile);
            schedulerFile.close();

            RecyclerView recyclerView = mFragmentView.findViewById(R.id.objectivePoolView);
            mSchedulerCache.attachToPoolView(recyclerView, mObjectivePoolId);
            recyclerView.setLayoutManager(new LinearLayoutManager(mFragmentView.getContext()));

            ObjectivePool objectivePool = mSchedulerCache.getPoolById(mObjectivePoolId);
            if(objectivePool != null)
            {
                SchedulerActivity activity = ((SchedulerActivity)requireActivity());
                Objects.requireNonNull(activity.getSupportActionBar()).setTitle(objectivePool.getName());
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext().getApplicationContext());
            String dayStartTimeString = sharedPreferences.getString("day_start_time", "6");
            int dayStartTime = ParseUtils.parseInt(dayStartTimeString, 6);

            TransactionDispatcher transactionDispatcher = new TransactionDispatcher(configFolder);
            transactionDispatcher.setSchedulerCache(mSchedulerCache);

            transactionDispatcher.updateObjectiveListTransaction(LocalDateTime.now(), dayStartTime);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_pool, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        mFragmentView = view;

        Bundle args = getArguments();
        if(args != null)
        {
            mObjectivePoolId = args.getLong("EyyDee");

            final ViewGroup fabContainer = mFragmentView.findViewById(R.id.pool_fab_container);

            final View fabAddChain     = mFragmentView.findViewById(R.id.fab_add_chain_to_pool_layout);
            final View fabAddObjective = mFragmentView.findViewById(R.id.fab_add_objective_to_pool_layout);

            final View fabAddChainText     = mFragmentView.findViewById(R.id.fab_add_chain_to_pool_text);
            final View fabAddObjectiveText = mFragmentView.findViewById(R.id.fab_add_objective_to_pool_text);

            final View fabAddChainButton     = mFragmentView.findViewById(R.id.fab_add_chain_to_pool);
            final View fabAddObjectiveButton = mFragmentView.findViewById(R.id.fab_add_objective_to_pool);

            final ImageButton fabSpeedDial = mFragmentView.findViewById(R.id.poolFabButton);
            fabSpeedDial.setOnClickListener(fabView ->
            {
                mFabExpanded = !mFabExpanded;
                if(mFabExpanded)
                {
                    expandFab(fabSpeedDial, fabAddChain, fabAddObjective, fabAddChainText, fabAddObjectiveText);
                }
                else
                {
                    collapseFab(fabSpeedDial, fabAddChain, fabAddObjective, fabAddChainText, fabAddObjectiveText);
                }
            });

            fabAddChain.setOnClickListener(this::addObjectiveChain);
            fabAddObjective.setOnClickListener(this::addObjective);

            fabAddChainButton.setOnClickListener(this::addObjectiveChain);
            fabAddObjectiveButton.setOnClickListener(this::addObjective);

            fabContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()
            {
                @Override
                public boolean onPreDraw()
                {
                    fabContainer.getViewTreeObserver().removeOnPreDrawListener(this);

                    mFabChainOffset     = fabSpeedDial.getY() - fabAddChain.getY();
                    mFabObjectiveOffset = fabSpeedDial.getY() - fabAddObjective.getY();

                    mFabChainTextOffset     = fabSpeedDial.getX() - fabAddChainText.getX();
                    mFabObjectiveTextOffset = fabSpeedDial.getX() - fabAddObjectiveText.getX();

                    fabAddChain.setTranslationY(mFabChainOffset);
                    fabAddObjective.setTranslationY(mFabObjectiveOffset);

                    fabAddChainText.setTranslationX(mFabChainTextOffset);
                    fabAddObjectiveText.setTranslationX(mFabObjectiveTextOffset);

                    return true;
                }
            });
        }
    }

    private void addObjectiveChain(View view)
    {
        NewChainDialogFragment newChainDialogFragment = new NewChainDialogFragment();
        newChainDialogFragment.setSchedulerCache(mSchedulerCache);
        newChainDialogFragment.setPoolIdToAddTo(mObjectivePoolId);

        newChainDialogFragment.show(getParentFragmentManager(), getString(R.string.newChainDialogName));
    }

    private void addObjective(View view)
    {
        NewObjectiveDialogFragment newObjectiveDialogFragment = new NewObjectiveDialogFragment();
        newObjectiveDialogFragment.setSchedulerCache(mSchedulerCache);
        newObjectiveDialogFragment.setPoolIdToAddTo(mObjectivePoolId);

        newObjectiveDialogFragment.show(getParentFragmentManager(), getString(R.string.newObjectiveDialogName));
    }

    private void collapseFab(final ImageButton fab, final View fabAddChain, final View fabAddObjective, final View fabAddChainText, final View fabAddObjectiveText)
    {
        fab.setImageResource(R.drawable.animated_cross);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(createCollapseAnimator(fabAddChain,             mFabChainOffset),
                                 createCollapseAnimator(fabAddObjective,         mFabObjectiveOffset),
                                 createCollapseTextAnimator(fabAddChainText,     mFabChainTextOffset),
                                 createCollapseTextAnimator(fabAddObjectiveText, mFabObjectiveTextOffset));
        animatorSet.start();

        animateSpeedDialFab(fab);
    }

    private void expandFab(final ImageButton fab, final View fabAddChain, final View fabAddObjective, final View fabAddChainText, final View fabAddObjectiveText)
    {
        fab.setImageResource(R.drawable.animated_plus);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(createExpandAnimator(fabAddChain,             mFabChainOffset),
                                 createExpandAnimator(fabAddObjective,         mFabObjectiveOffset),
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