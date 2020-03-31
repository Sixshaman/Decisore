package com.sixshaman.advancedunforgetter.list;

import android.content.Intent;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.archive.ArchiveActivity;
import com.sixshaman.advancedunforgetter.archive.TaskArchive;
import com.sixshaman.advancedunforgetter.scheduler.TaskScheduler;

import java.util.ArrayList;
import java.util.Objects;

public class TaskListActivity extends AppCompatActivity
{
    //Task scheduler to add new tasks there
    private TaskScheduler mTaskScheduler;

    //Task list (the model of this class)
    private TaskList mTaskList;

    //Task archive model
    private TaskArchive mTaskArchive;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);
        Toolbar toolbar = findViewById(R.id.toolbarTaskList);
        setSupportActionBar(toolbar);

        FloatingActionButton buttonNewTask = findViewById(R.id.addNewTask);
        buttonNewTask.setOnClickListener(view -> openAddTaskDialog());
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mTaskArchive   = new TaskArchive();
        mTaskList      = new TaskList();
        mTaskScheduler = new TaskScheduler(mTaskList);

        mTaskList.setArchive(mTaskArchive);

        mTaskList.setConfigFolder(Objects.requireNonNull(getExternalFilesDir("/app")).getAbsolutePath());
        mTaskArchive.setConfigFolder(Objects.requireNonNull(getExternalFilesDir("/app")).getAbsolutePath());

        RecyclerView recyclerView = findViewById(R.id.taskListView);
        recyclerView.setAdapter(mTaskList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mTaskList.loadTasks();
        mTaskArchive.loadFinishedTasks();

        mTaskScheduler.setLastTaskId(mTaskList.getLastTaskId());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_task_list, menu);

        menu.findItem(R.id.menuOpenArchive).setOnMenuItemClickListener(item ->
        {
            mTaskList.setArchive(null); //Stop the archive from updating

            Intent archiveOpenIntent = new Intent(TaskListActivity.this, ArchiveActivity.class);
            startActivity(archiveOpenIntent);
            return true;
        });

        return true;
    }

    private void openAddTaskDialog()
    {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);

        ConstraintLayout taskDialogLayout = findViewById(R.id.layoutDialogNewTask);

        final LayoutInflater inflater = getLayoutInflater();
        alertBuilder.setView(inflater.inflate(R.layout.layout_dialog_new_task, taskDialogLayout));

        alertBuilder.setTitle(R.string.newTaskDialogName);

        alertBuilder.setPositiveButton(R.string.createTask, (dialogInterface, i) ->
        {
            final EditText editTextName         = ((AlertDialog)dialogInterface).findViewById(R.id.editTaskName);
            final EditText editTextNDescription = ((AlertDialog)dialogInterface).findViewById(R.id.editTaskDescription);

            final Spinner taskTypeSpinner = ((AlertDialog)dialogInterface).findViewById(R.id.spinnerTaskType);

            assert editTextName         != null;
            assert editTextNDescription != null;
            assert taskTypeSpinner      != null;

            String nameText = editTextName.getEditableText().toString();
            if(nameText.isEmpty())
            {
                Toast toast = Toast.makeText(TaskListActivity.this, R.string.invalidTaskName, Toast.LENGTH_SHORT);
                toast.show();
            }
            else
            {
                String descriptionText = editTextNDescription.getEditableText().toString();

                int taskTypeIndex = taskTypeSpinner.getSelectedItemPosition();
                switch(taskTypeIndex)
                {
                    //Immediate task
                    case 0:
                        mTaskScheduler.addImmediateTask(nameText, descriptionText, new ArrayList<>());
                        break;

                    //Deferred task
                    case 1:
                        break;

                    //Regular task
                    case 2:
                        break;

                    //Irregular task
                    case 3:
                        break;

                    default:
                        break;
                }
            }
        });

        AlertDialog dialog = alertBuilder.create();
        dialog.show();
    }
}
