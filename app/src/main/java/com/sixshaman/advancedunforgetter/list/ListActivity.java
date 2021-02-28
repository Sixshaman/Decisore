package com.sixshaman.advancedunforgetter.list;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.archive.ArchiveActivity;
import com.sixshaman.advancedunforgetter.archive.*;
import com.sixshaman.advancedunforgetter.scheduler.*;
import com.sixshaman.advancedunforgetter.utils.LockedReadFile;
import com.sixshaman.advancedunforgetter.utils.TransactionDispatcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ListActivity extends AppCompatActivity
{
    //Objective list
    private ObjectiveListCache mListCache;

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

        mListCache = new ObjectiveListCache();

        String configFolder = Objects.requireNonNull(getExternalFilesDir("/app")).getAbsolutePath();

        RecyclerView recyclerView = findViewById(R.id.taskListView);
        mListCache.attachToView(recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        try
        {
            LockedReadFile listFile = new LockedReadFile(configFolder + "/" + ObjectiveListCache.LIST_FILENAME);
            mListCache.invalidate(listFile);
            listFile.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
        transactionDispatcher.setListCache(mListCache);

        transactionDispatcher.updateObjectiveListTransaction(configFolder, LocalDateTime.now());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_task_list, menu);

        menu.findItem(R.id.menuOpenArchive).setOnMenuItemClickListener(item ->
        {
            Intent archiveOpenIntent = new Intent(ListActivity.this, ArchiveActivity.class);
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
                Toast toast = Toast.makeText(ListActivity.this, R.string.invalidTaskName, Toast.LENGTH_SHORT);
                toast.show();
            }
            else
            {
                String descriptionText = editTextNDescription.getEditableText().toString();

                LocalDateTime objectiveCreateDate   = LocalDateTime.now();
                LocalDateTime objectiveScheduleDate = objectiveCreateDate;

                Duration objectiveRepeatDuration = Duration.ZERO;
                float objectiveRepeatProbability = 0.0f;

                int objectiveTypeIndex = taskTypeSpinner.getSelectedItemPosition();
                switch(objectiveTypeIndex)
                {
                    //Immediate task
                    case 0:
                    {
                        objectiveScheduleDate = objectiveCreateDate;
                        break;
                    }

                    //Deferred task
                    case 1:
                    {
                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        try
                        {
                            LocalDate deferDate = LocalDate.parse(deferDateEditText.getEditableText().toString(), dateTimeFormatter);
                            objectiveScheduleDate = deferDate.atTime(LocalTime.of(6, 0)); //Day starts at 6 AM
                        }
                        catch (DateTimeParseException e)
                        {
                            Toast.makeText(ListActivity.this, getString(R.string.dateParseErrorISO8601), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    }

                    //Regular task
                    case 2:
                    {
                        try
                        {
                            int repeatInterval         = Integer.parseInt(repeatIntervalEditText.getEditableText().toString());
                            objectiveRepeatDuration    = Duration.ofDays(repeatInterval);
                            objectiveRepeatProbability = 1.0f;

                            objectiveScheduleDate = objectiveCreateDate.plusDays(1); //Default for regular objectives is +1 day for start
                        }
                        catch(NumberFormatException e)
                        {
                            Toast.makeText(ListActivity.this, getString(R.string.frequencyParseError), Toast.LENGTH_SHORT).show();
                        }

                        break;
                    }

                    //Irregular task
                    case 3:
                    {
                        try
                        {
                            int repeatInterval         = Integer.parseInt(repeatIntervalEditText.getEditableText().toString());
                            objectiveRepeatDuration    = Duration.ofDays(repeatInterval);
                            objectiveRepeatProbability = 0.5f;

                            objectiveScheduleDate = objectiveCreateDate.plusDays(1); //Default for regular objectives is +1 day for start
                        }
                        catch(NumberFormatException e)
                        {
                            Toast.makeText(ListActivity.this, getString(R.string.frequencyParseError), Toast.LENGTH_SHORT).show();
                        }

                        break;
                    }

                    default:
                        break;
                }

                String configFolder = Objects.requireNonNull(getExternalFilesDir("/app")).getAbsolutePath();

                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.setListCache(mListCache);

                transactionDispatcher.addObjectiveTransaction(configFolder, objectiveCreateDate, objectiveScheduleDate,
                                                              objectiveRepeatDuration, objectiveRepeatProbability,
                                                              nameText, descriptionText, new ArrayList<>());
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
