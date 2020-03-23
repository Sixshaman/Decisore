package com.sixshaman.advancedunforgetter.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import com.sixshaman.advancedunforgetter.R;

import java.util.ArrayList;

public class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.TaskViewHolder>
{
    private ArrayList<String> mTaskNames;
    private ArrayList<String> mTaskDecriptions;

    private ArrayList<Long> mTaskIds;

    private Context mContext;

    public TaskListAdapter(Context context)
    {
        mTaskNames       = new ArrayList<>();
        mTaskDecriptions = new ArrayList<>();

        mTaskIds = new ArrayList<>();

        mContext = context;
    }

    public void addTaskData(String taskName, String taskDescription, long taskId)
    {
        mTaskNames.add(taskName);
        mTaskDecriptions.add(taskDescription);
        mTaskIds.add(taskId);

        notifyItemInserted(mTaskIds.size() - 1);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
    {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_task_view, viewGroup, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder taskViewHolder, int position)
    {
        taskViewHolder.mTextView.setText(mTaskNames.get(position));

        taskViewHolder.mParentLayout.setOnClickListener(view ->
        {
            Toast.makeText(mContext, mTaskDecriptions.get(position), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public int getItemCount()
    {
        return mTaskIds.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder
    {
        TextView mTextView;
        CheckBox mCheckbox;

        ConstraintLayout mParentLayout;

        public TaskViewHolder(View itemView)
        {
            super(itemView);

            mTextView = (TextView)itemView.findViewById(R.id.textTaskName);
            mCheckbox = (CheckBox)itemView.findViewById(R.id.checkBoxTaskDone);

            mParentLayout = (ConstraintLayout)itemView.findViewById(R.id.layoutTaskView);
        }
    }
}
