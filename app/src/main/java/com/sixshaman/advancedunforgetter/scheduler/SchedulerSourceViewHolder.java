package com.sixshaman.advancedunforgetter.scheduler;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.list.ObjectiveListCache;

public class SchedulerSourceViewHolder extends RecyclerView.ViewHolder
{
    private ObjectiveSchedulerCache mObjectiveSchedulerCache;

    private SchedulerElement mSchedulerElement;

    ImageView mIconView;
    TextView mTextView;

    ConstraintLayout mParentLayout;

    public SchedulerSourceViewHolder(View itemView)
    {
        super(itemView);

        mTextView = itemView.findViewById(R.id.textScheduledSourceName);
        mIconView = itemView.findViewById(R.id.iconScheduledSource);

        mParentLayout = itemView.findViewById(R.id.layoutScheduledSourceView);

        mParentLayout.setOnClickListener(view ->
        {

        });
    }

    void setSourceMetadata(ObjectiveSchedulerCache objectiveSchedulerCache, SchedulerElement schedulerElement)
    {
        mObjectiveSchedulerCache = objectiveSchedulerCache;
        mSchedulerElement        = schedulerElement;
    }
}