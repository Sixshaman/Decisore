package com.sixshaman.advancedunforgetter.scheduler.ObjectivePool;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.list.ObjectiveListCache;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain.ObjectiveChain;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.ObjectivePool;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.PoolElement;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.PoolFragment;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjective;

public class PoolElementViewHolder extends RecyclerView.ViewHolder
{
    private ObjectiveSchedulerCache mObjectiveSchedulerCache;

    private PoolElement mPoolElement;

    ImageView mIconView;
    TextView  mTextView;

    ConstraintLayout mParentLayout;

    public PoolElementViewHolder(View itemView)
    {
        super(itemView);

        mTextView = itemView.findViewById(R.id.textPoolElementName);
        mIconView = itemView.findViewById(R.id.iconPoolElement);

        mParentLayout = itemView.findViewById(R.id.layoutPoolElementView);

        mParentLayout.setOnClickListener(view ->
        {
            if(mPoolElement instanceof ObjectiveChain)
            {
                Toast.makeText(view.getContext(), ((ObjectiveChain) mPoolElement).getFirstObjective().getName(), Toast.LENGTH_SHORT).show();
            }
            else if(mPoolElement instanceof ScheduledObjective)
            {
                Toast.makeText(view.getContext(), "Scheduled for " + ((ScheduledObjective) mPoolElement).getScheduledEnlistDate().toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    void setSourceMetadata(PoolElement poolElement)
    {
        mPoolElement = poolElement;
    }
}