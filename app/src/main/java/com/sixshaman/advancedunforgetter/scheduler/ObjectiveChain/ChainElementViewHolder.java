package com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjective;

public class ChainElementViewHolder extends RecyclerView.ViewHolder
{
    private ObjectiveSchedulerCache mObjectiveSchedulerCache;

    private ScheduledObjective mScheduledObjective;

    TextView mTextView;

    ConstraintLayout mParentLayout;

    public ChainElementViewHolder(View itemView)
    {
        super(itemView);

        mTextView = itemView.findViewById(R.id.textScheduledObjectiveName);

        mParentLayout = itemView.findViewById(R.id.layoutScheduledObjectiveView);

        mParentLayout.setOnClickListener(view ->
        {
            Toast.makeText(view.getContext(), "Scheduled for " + mScheduledObjective.getScheduledEnlistDate().toString(), Toast.LENGTH_SHORT).show();
        });
    }

    void setSourceMetadata(ScheduledObjective objective)
    {
        mScheduledObjective = objective;
    }
}