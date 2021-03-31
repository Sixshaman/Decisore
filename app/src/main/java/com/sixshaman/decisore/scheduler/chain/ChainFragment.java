package com.sixshaman.decisore.scheduler.chain;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.decisore.scheduler.SchedulerActivity;
import com.sixshaman.decisore.utils.LockedReadFile;
import com.sixshaman.decisore.utils.NewObjectiveDialogFragment;
import com.sixshaman.decisore.utils.TransactionDispatcher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

public class ChainFragment extends Fragment
{
    private View mFragmentView;

    //Scheduler cache model
    private ObjectiveSchedulerCache mSchedulerCache;

    //The id of the chain displayed
    private long mObjectiveChainId;

    public ChainFragment()
    {
        mObjectiveChainId = -1;
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

            TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
            transactionDispatcher.setSchedulerCache(mSchedulerCache);

            transactionDispatcher.updateObjectiveListTransaction(configFolder, LocalDateTime.now());

            RecyclerView recyclerView = mFragmentView.findViewById(R.id.objectiveChainView);
            mSchedulerCache.attachToChainView(recyclerView, mObjectiveChainId);
            recyclerView.setLayoutManager(new LinearLayoutManager(mFragmentView.getContext()));

            ObjectiveChain objectiveChain = mSchedulerCache.getChainById(mObjectiveChainId);
            if(objectiveChain != null)
            {
                SchedulerActivity activity = ((SchedulerActivity) requireActivity());
                Objects.requireNonNull(activity.getSupportActionBar()).setTitle(objectiveChain.getName());
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_chain, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        mFragmentView = view;

        Bundle args = getArguments();
        if(args != null)
        {
            mObjectiveChainId = args.getLong("EyyDee");

            FloatingActionButton buttonNewObjective = mFragmentView.findViewById(R.id.addNewObjectiveToChain);
            buttonNewObjective.setOnClickListener(this::addObjective);
        }
    }

    private void addObjective(View view)
    {
        NewObjectiveDialogFragment newObjectiveDialogFragment = new NewObjectiveDialogFragment();
        newObjectiveDialogFragment.setSchedulerCache(mSchedulerCache);
        newObjectiveDialogFragment.setChainIdToAddTo(mObjectiveChainId);

        newObjectiveDialogFragment.show(getParentFragmentManager(), getString(R.string.newObjectiveDialogName));
    }
}