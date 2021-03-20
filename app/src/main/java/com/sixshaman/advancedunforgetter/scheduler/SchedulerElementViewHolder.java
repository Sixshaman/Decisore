package com.sixshaman.advancedunforgetter.scheduler;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.list.ObjectiveListCache;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.ObjectivePool;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.PoolElement;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.PoolFragment;

public class SchedulerElementViewHolder extends RecyclerView.ViewHolder
{
    private ObjectiveSchedulerCache mObjectiveSchedulerCache;

    private SchedulerElement mSchedulerElement;

    ImageView mIconView;
    TextView mTextView;

    ConstraintLayout mParentLayout;

    public SchedulerElementViewHolder(View itemView)
    {
        super(itemView);

        mTextView = itemView.findViewById(R.id.textScheduledSourceName);
        mIconView = itemView.findViewById(R.id.iconScheduledSource);

        mParentLayout = itemView.findViewById(R.id.layoutScheduledSourceView);

        mParentLayout.setOnClickListener(view ->
        {
            if(mSchedulerElement instanceof ObjectivePool)
            {
                Context parentContext = view.getContext();
                if(parentContext instanceof FragmentActivity)
                {
                    FragmentManager fragmentManager = (((FragmentActivity) parentContext).getSupportFragmentManager());

                    Bundle bundle = new Bundle();
                    bundle.putLong("EyyDee", ((ObjectivePool)mSchedulerElement).getId());

                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.setReorderingAllowed(true);
                    fragmentTransaction.replace(R.id.scheduler_fragment_container_view, PoolFragment.class, null);
                    fragmentTransaction.commit();
                }
            }
        });
    }

    void setSourceMetadata(ObjectiveSchedulerCache objectiveSchedulerCache, SchedulerElement schedulerElement)
    {
        mObjectiveSchedulerCache = objectiveSchedulerCache;
        mSchedulerElement        = schedulerElement;
    }
}