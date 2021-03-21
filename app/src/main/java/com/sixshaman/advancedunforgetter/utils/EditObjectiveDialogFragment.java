package com.sixshaman.advancedunforgetter.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.fragment.app.DialogFragment;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import com.sixshaman.advancedunforgetter.list.ObjectiveListCache;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveSchedulerCache;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Objects;

public class EditObjectiveDialogFragment extends DialogFragment
{
    private long mObjectiveId;

    private String mCurrentName;
    private String mCurrentDescription;

    private boolean mEditInScheduler;
    private boolean mEditInList;

    private ObjectiveSchedulerCache mSchedulerCache;
    private ObjectiveListCache      mListCache;

    public EditObjectiveDialogFragment(long objectiveId, String objectiveName, String objectiveDescription)
    {
        mObjectiveId = objectiveId;

        mCurrentName        = objectiveName;
        mCurrentDescription = objectiveDescription;

        mEditInList      = false;
        mEditInScheduler = false;
    }

    public void setSchedulerCache(ObjectiveSchedulerCache schedulerCache)
    {
        mSchedulerCache = schedulerCache;
    }

    public void setListCache(ObjectiveListCache listCache)
    {
        mListCache = listCache;
    }

    public void setEditInScheduler(boolean doEdit)
    {
        mEditInScheduler = true;
    }

    public void setEditInList(boolean doEdit)
    {
        mEditInList = true;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Activity activity = getActivity();

        final ValueHolder<AlertDialog> resultDialog = new ValueHolder<>(null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.layout_dialog_edit_objective, null));
        builder.setTitle(R.string.editObjectiveDialogName);

        builder.setPositiveButton(R.string.createTask, (dialog, id) ->
        {
            final EditText editTextName         = resultDialog.getValue().findViewById(R.id.editEditTaskName);
            final EditText editTextNDescription = resultDialog.getValue().findViewById(R.id.editEditTaskDescription);

            String nameText = editTextName.getEditableText().toString();
            if(nameText.isEmpty())
            {
                Toast toast = Toast.makeText(activity, R.string.invalidTaskName, Toast.LENGTH_SHORT);
                toast.show();
            }
            else
            {
                String descriptionText = editTextNDescription.getEditableText().toString();

                String configFolder = Objects.requireNonNull(activity.getExternalFilesDir("/app")).getAbsolutePath();

                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.setSchedulerCache(mSchedulerCache);
                transactionDispatcher.setListCache(mListCache);

                transactionDispatcher.editObjectiveTransaction(configFolder, mObjectiveId, nameText, descriptionText, mEditInScheduler, mEditInList);
            }
        });

        builder.setNegativeButton(R.string.createCancel, (dialog, id) ->
        {
            //Nothing
        });

        resultDialog.setValue(builder.create());
        resultDialog.getValue().setOnShowListener(dialogInterface ->
        {
            EditText editTextName        = resultDialog.getValue().findViewById(R.id.editEditTaskName);
            EditText editTextDescription = resultDialog.getValue().findViewById(R.id.editEditTaskDescription);

            editTextName.setText(mCurrentName);
            editTextDescription.setText(mCurrentDescription);
        });

        return resultDialog.getValue();
    }
}
