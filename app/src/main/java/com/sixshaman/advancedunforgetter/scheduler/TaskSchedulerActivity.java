package com.sixshaman.advancedunforgetter.scheduler;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.list.ObjectiveListCache;
import com.sixshaman.advancedunforgetter.utils.LockedReadFile;
import com.sixshaman.advancedunforgetter.utils.TransactionDispatcher;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.Objects;

public class TaskSchedulerActivity extends AppCompatActivity
{
    //Scheduler cache model
    private ObjectiveSchedulerCache mSchedulerCache;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_scheduler);
        Toolbar toolbar = findViewById(R.id.toolbar_scheduler);
        setSupportActionBar(toolbar);

        setTitle(R.string.title_activity_task_scheduler);

        FloatingActionButton fab = findViewById(R.id.fab_new_scheduled_task);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //COPIED FROM TASKLISTACTIVITY
                openAddTaskDialog();
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mSchedulerCache = new ObjectiveSchedulerCache();

        String configFolder = Objects.requireNonNull(getExternalFilesDir("/app")).getAbsolutePath();

        try
        {
            LockedReadFile listFile = new LockedReadFile(configFolder + "/" + ObjectiveListCache.LIST_FILENAME);
            mSchedulerCache.invalidate(listFile);
            listFile.close();

            TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
            transactionDispatcher.setSchedulerCache(mSchedulerCache);

            transactionDispatcher.updateObjectiveListTransaction(configFolder, LocalDateTime.now());
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
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

            final EditText deferDateEditText      = ((AlertDialog)dialogInterface).findViewById(R.id.editDeferDate);
            final EditText repeatIntervalEditText = ((AlertDialog)dialogInterface).findViewById(R.id.editTextRepeatInterval);

            assert editTextName         != null;
            assert editTextNDescription != null;

            assert taskTypeSpinner != null;

            assert deferDateEditText      != null;
            assert repeatIntervalEditText != null;

            String nameText = editTextName.getEditableText().toString();
            if(nameText.isEmpty())
            {
                Toast toast = Toast.makeText(TaskSchedulerActivity.this, R.string.invalidTaskName, Toast.LENGTH_SHORT);
                toast.show();
            }
            else
            {
                //TODO: get this away from both Scheduler and List activities
                //TODO: remove immediate tasks from Scheduler activity

                //Wait, now? *Sighs*
                //Okay, I'll move it later! But now I want to just project that

                /*
                String descriptionText = editTextNDescription.getEditableText().toString();

                int taskTypeIndex = taskTypeSpinner.getSelectedItemPosition();

                mTaskScheduler.waitLock();
                mTaskList.waitLock();

                try
                {
                    switch(taskTypeIndex)
                    {
                        //Immediate task
                        case 0:
                        {
                            mTaskScheduler.addImmediateTask(nameText, descriptionText, new ArrayList<>());
                            break;
                        }

                        //Deferred task
                        case 1:
                        {
                            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                            try
                            {
                                LocalDate deferDate = LocalDate.parse(deferDateEditText.getEditableText().toString(), dateTimeFormatter);
                                LocalDateTime deferDateTime = deferDate.atTime(LocalTime.of(6, 0)); //Day starts at 6 AM

                                mTaskScheduler.addDeferredTask(deferDateTime, nameText, descriptionText, new ArrayList<>());
                            }
                            catch (DateTimeParseException e)
                            {
                                Toast.makeText(TaskListActivity.this, getString(R.string.dateParseErrorISO8601), Toast.LENGTH_SHORT).show();
                            }
                            break;
                        }

                        //Regular task
                        case 2:
                        {
                            try
                            {
                                int repeatInterval      = Integer.parseInt(repeatIntervalEditText.getEditableText().toString());
                                Duration repeatDuration = Duration.ofDays(repeatInterval);

                                mTaskScheduler.addRepeatedTask(repeatDuration, nameText, descriptionText, new ArrayList<>());
                            }
                            catch(NumberFormatException e)
                            {
                                Toast.makeText(TaskListActivity.this, getString(R.string.frequencyParseError), Toast.LENGTH_SHORT).show();
                            }
                            catch(TaskScheduler.SchedulerFileLockException e)
                            {
                                e.printStackTrace();
                            }

                            break;
                        }

                        //Irregular task
                        case 3:
                        {
                            try
                            {
                                int repeatInterval      = Integer.parseInt(repeatIntervalEditText.getEditableText().toString());
                                Duration repeatDuration = Duration.ofDays(repeatInterval);

                                mTaskScheduler.addTimeToTimeTask(repeatDuration, nameText, descriptionText, new ArrayList<>());
                            }
                            catch(NumberFormatException e)
                            {
                                Toast.makeText(TaskListActivity.this, getString(R.string.frequencyParseError), Toast.LENGTH_SHORT).show();
                            }

                            break;
                        }

                        default:
                            break;
                    }
                }
                catch (BaseFileLockException e)
                {
                    e.printStackTrace();
                }

                mTaskList.unlock();
                mTaskScheduler.unlock(); */
            }
        });

        AlertDialog dialog = alertBuilder.create();
        dialog.setOnShowListener(dialogInterface ->
        {
            Spinner taskTypeSpinner = ((AlertDialog)dialogInterface).findViewById(R.id.spinnerTaskType);

            LinearLayout pickDeferDateLayout      = ((AlertDialog)dialogInterface).findViewById(R.id.layoutSelectDeferDate);
            LinearLayout pickRepeatIntervalLayout = ((AlertDialog)dialogInterface).findViewById(R.id.layoutSelectRepeatInterval);

            TextView repeatIntervalTextView           = ((AlertDialog)dialogInterface).findViewById(R.id.textViewRepeatInterval);
            TextView repeatIntervalTextViewBackground = ((AlertDialog)dialogInterface).findViewById(R.id.textViewBackHintAlwaysShown);
            EditText repeatIntervalEditText           = ((AlertDialog)dialogInterface).findViewById(R.id.editTextRepeatInterval);

            assert taskTypeSpinner     != null;

            assert pickDeferDateLayout      != null;
            assert pickRepeatIntervalLayout != null;

            assert repeatIntervalTextView           != null;
            assert repeatIntervalTextViewBackground != null;
            assert repeatIntervalEditText           != null;

            pickDeferDateLayout.setVisibility(View.GONE);
            pickRepeatIntervalLayout.setVisibility(View.GONE);

            taskTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int index, long l)
                {
                    switch(index)
                    {
                        //Immediate task
                        case 0:
                            pickDeferDateLayout.setVisibility(View.GONE);
                            pickRepeatIntervalLayout.setVisibility(View.GONE);
                            break;

                        //Deferred task
                        case 1:
                            pickDeferDateLayout.setVisibility(View.VISIBLE);
                            pickRepeatIntervalLayout.setVisibility(View.GONE);
                            break;

                        //Regular task
                        case 2:
                            pickDeferDateLayout.setVisibility(View.GONE);
                            pickRepeatIntervalLayout.setVisibility(View.VISIBLE);
                            repeatIntervalTextView.setText(R.string.repeat_interval);
                            repeatIntervalEditText.setText("1");
                            break;

                        //Irregular task
                        case 3:
                            pickDeferDateLayout.setVisibility(View.GONE);
                            pickRepeatIntervalLayout.setVisibility(View.VISIBLE);
                            repeatIntervalTextView.setText(R.string.approx_frequency);
                            repeatIntervalEditText.setText("7");
                            break;

                        default:
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView)
                {
                }
            });

            repeatIntervalEditText.addTextChangedListener(new TextWatcher()
            {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
                {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
                {
                    try
                    {
                        int dayCount = Integer.parseInt(charSequence.toString());
                        repeatIntervalTextViewBackground.setText(getResources().getQuantityString(R.plurals.plural_number_of_days, dayCount, dayCount));
                    }
                    catch(NumberFormatException e)
                    {
                        repeatIntervalTextViewBackground.setText(getString(R.string.placeholder_number_of_days));
                    }
                }

                @Override
                public void afterTextChanged(Editable editable)
                {
                }
            });
        });

        dialog.show();
    }
}
