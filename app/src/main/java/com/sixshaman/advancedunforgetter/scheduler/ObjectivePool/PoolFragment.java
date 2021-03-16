package com.sixshaman.advancedunforgetter.scheduler.ObjectivePool;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.advancedunforgetter.utils.LockedReadFile;
import com.sixshaman.advancedunforgetter.utils.TransactionDispatcher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A fragment representing a list of Items.
 */
public class PoolFragment extends Fragment {

    private View mFragmentView;

    //Scheduler cache model
    private ObjectiveSchedulerCache mSchedulerCache;

    //The id of the pool displayed
    private long mObjectivePoolId;

    //The pool to display
    private ObjectivePool mObjectivePool;

    public PoolFragment()
    {
        mObjectivePoolId = -1;
    }

    public static PoolFragment newInstance(long poolId)
    {
        PoolFragment fragment = new PoolFragment();
        Bundle args = new Bundle();
        args.putLong("EyyDee", poolId);
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
        mSchedulerCache.attachToPoolView(recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(mFragmentView.getContext()));

        mObjectivePool = mSchedulerCache.getPoolById(mObjectivePoolId);

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
        mFragmentView = inflater.inflate(R.layout.fragment_pool, container, false);

        return mFragmentView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        mObjectivePoolId = savedInstanceState.getLong("EyyDee");
    }
}