package com.sixshaman.advancedunforgetter.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.fragment.app.DialogFragment;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.list.ObjectiveListCache;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain.ObjectiveChain;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.ObjectivePool;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveSchedulerCache;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Objects;

public class NewObjectiveDialogFragment extends DialogFragment
{
    private ObjectivePool  mPoolToAddTo;
    private ObjectiveChain mChainToAddTo;

    private ObjectiveSchedulerCache mSchedulerCache;
    private ObjectiveListCache      mListCache;

    public NewObjectiveDialogFragment()
    {
        mPoolToAddTo  = null;
        mChainToAddTo = null;
    }

    public void setSchedulerCache(ObjectiveSchedulerCache schedulerCache)
    {
        mSchedulerCache = schedulerCache;
    }

    public void setListCache(ObjectiveListCache listCache)
    {
        mListCache = listCache;
    }

    public void setPoolToAddTo(ObjectivePool pool)
    {
        mPoolToAddTo = pool;
    }

    public void setChainToAddTo(ObjectiveChain chain)
    {
        mChainToAddTo = chain;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Activity activity = getActivity();

        final ValueHolder<AlertDialog> resultDialog = new ValueHolder<>(null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.layout_dialog_new_objective, null));
        builder.setTitle(R.string.newObjectiveDialogName);

        LocalDateTime objectiveCreateDate = LocalDateTime.now();

        //I blamed Java for necessity to make hacks like ValueHolder... Then I learned about RefCell in Rust. Lol. Apparently good languages have this too
        final ValueHolder<LocalDateTime> objectiveScheduleDate = new ValueHolder<>(objectiveCreateDate);

        builder.setPositiveButton(R.string.createObjective, (dialog, id) ->
        {
            final EditText editTextName         = resultDialog.getValue().findViewById(R.id.editObjectiveName);
            final EditText editTextNDescription = resultDialog.getValue().findViewById(R.id.editObjectiveDescription);

            final Spinner  scheduleSpinner    = resultDialog.getValue().findViewById(R.id.spinnerObjectiveSchedule);
            final Spinner  repeatSpinner      = resultDialog.getValue().findViewById(R.id.spinnerObjectiveRepeats);
            final Spinner  intervalSpinner    = resultDialog.getValue().findViewById(R.id.spinnerObjectiveInterval);
            final CheckBox occasionalCheckbox = resultDialog.getValue().findViewById(R.id.checkboxOccasional);

            String nameText = editTextName.getEditableText().toString();
            if(nameText.isEmpty())
            {
                Toast toast = Toast.makeText(activity, R.string.invalidObjectiveName, Toast.LENGTH_SHORT);
                toast.show();
            }
            else
            {
                String descriptionText = editTextNDescription.getEditableText().toString();

                Duration objectiveRepeatDuration = Duration.ZERO;
                float objectiveRepeatProbability = 0.0f;

                int objectiveScheduleIndex = scheduleSpinner.getSelectedItemPosition();
                switch(objectiveScheduleIndex)
                {
                    case 0: //Now
                    {
                        objectiveScheduleDate.setValue(objectiveCreateDate);
                        break;
                    }
                    case 1: //Tomorrow
                    {
                        //Day starts at 6 AM
                        objectiveScheduleDate.setValue(objectiveCreateDate.minusHours(6).plusDays(1).truncatedTo(ChronoUnit.DAYS).plusHours(6));
                        break;
                    }
                    case 2: //In a week
                    {
                        //Day starts at 6 AM
                        objectiveScheduleDate.setValue(objectiveCreateDate.minusHours(6).plusDays(7).truncatedTo(ChronoUnit.DAYS).plusHours(6));
                        break;
                    }
                    case 3: //Custom
                    {
                        //Set in setOnItemClickListener
                        break;
                    }
                    default:
                        break;
                }

                boolean objectiveRepeatable = false;

                int objectiveRepeatIndex = repeatSpinner.getSelectedItemPosition();
                switch(objectiveRepeatIndex)
                {
                    case 0: //1
                    {
                        objectiveRepeatable = false;
                        break;
                    }
                    case 1: //Infinity
                    {
                        objectiveRepeatable = true;
                        break;
                    }
                }

                int objectiveIntervalIndex = intervalSpinner.getSelectedItemPosition();
                switch(objectiveIntervalIndex)
                {
                    case 0: //Day
                    {
                        objectiveRepeatDuration = Duration.ofDays(1);
                        break;
                    }
                    case 1: //Week
                    {
                        objectiveRepeatDuration = Duration.ofDays(7);
                        break;
                    }
                    case 2: //Month
                    {
                        objectiveRepeatDuration = Duration.ofDays(30);
                        break;
                    }
                }

                if(objectiveRepeatable)
                {
                    if(occasionalCheckbox.isChecked())
                    {
                        objectiveRepeatProbability = 0.5f;
                    }
                    else
                    {
                        objectiveRepeatProbability = 1.0f;
                    }
                }
                else
                {
                    //No repetition!
                    objectiveRepeatDuration = Duration.ZERO;
                    objectiveRepeatProbability = 0.0f;
                }

                String configFolder = Objects.requireNonNull(activity.getExternalFilesDir("/app")).getAbsolutePath();

                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.setSchedulerCache(mSchedulerCache);
                transactionDispatcher.setListCache(mListCache);

                transactionDispatcher.addObjectiveTransaction(mPoolToAddTo, mChainToAddTo,
                                                              configFolder, objectiveCreateDate, objectiveScheduleDate.getValue(),
                                                              objectiveRepeatDuration, objectiveRepeatProbability,
                                                              nameText, descriptionText, new ArrayList<>());
            }
        });

        builder.setNegativeButton(R.string.createCancel, (dialog, id) ->
        {
            //Nothing
        });

        resultDialog.setValue(builder.create());
        resultDialog.getValue().setOnShowListener(dialogInterface ->
        {
            final Spinner scheduleSpinner = resultDialog.getValue().findViewById(R.id.spinnerObjectiveSchedule);

            scheduleSpinner.setOnItemClickListener((adapterView, view, index, l) ->
            {
                if(index == 3) //Custom date
                {
                    DatePickerDialog datePickerDialog = new DatePickerDialog(activity);
                    datePickerDialog.setOnDateSetListener((datePicker, year, month, day) ->
                    {
                        //Day starts at 6 AM
                        //Also Java numerates months from 0, not from 1
                        LocalDateTime dateTime = LocalDateTime.of(year, month + 1, day, 6, 0, 0);
                        objectiveScheduleDate.setValue(dateTime);
                    });

                    datePickerDialog.show();
                }
            });
        });

        return resultDialog.getValue();
    }
}
