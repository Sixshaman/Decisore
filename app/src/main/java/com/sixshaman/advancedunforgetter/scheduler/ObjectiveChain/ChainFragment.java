package com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.advancedunforgetter.utils.LockedReadFile;
import com.sixshaman.advancedunforgetter.utils.NewObjectiveDialogFragment;
import com.sixshaman.advancedunforgetter.utils.TransactionDispatcher;

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

    //The pool to display
    private ObjectiveChain mObjectiveChain;

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

        mObjectiveChain = mSchedulerCache.getChainById(mObjectiveChainId);

        RecyclerView recyclerView = mFragmentView.findViewById(R.id.objectiveSchedulerView);
        mObjectiveChain.attachToChainView(recyclerView);
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
        return inflater.inflate(R.layout.fragment_chain, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        mObjectiveChainId = savedInstanceState.getLong("EyyDee");

        FloatingActionButton buttonNewObjective = mFragmentView.findViewById(R.id.addNewObjectiveToChain);
        buttonNewObjective.setOnClickListener(this::addObjective);
    }

    private void addObjective(View view)
    {
        NewObjectiveDialogFragment newObjectiveDialogFragment = new NewObjectiveDialogFragment();
        newObjectiveDialogFragment.setSchedulerCache(mSchedulerCache);
        newObjectiveDialogFragment.setChainToAddTo(mObjectiveChain);

        newObjectiveDialogFragment.show(getParentFragmentManager(), getString(R.string.newObjectiveDialogName));
    }
}