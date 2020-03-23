package com.sixshaman.advancedunforgetter.ui;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.TaskHolder>
{
    static class TaskHolder extends RecyclerView.ViewHolder
    {
        private String mTaskName;
        private String mTaskDescription;

        private String mTaskId;

        public TaskHolder(View itemView, String name, String description, long id)
        {
            super(itemView);


        }
    }

    @NonNull
    @Override
    public TaskHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
    {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull TaskHolder taskHolder, int i)
    {

    }

    @Override
    public int getItemCount()
    {
        return 0;
    }
}
