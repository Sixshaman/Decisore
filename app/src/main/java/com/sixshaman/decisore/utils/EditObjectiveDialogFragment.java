package com.sixshaman.decisore.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.list.ObjectiveListCache;
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;

import java.util.Objects;

public class EditObjectiveDialogFragment extends DialogFragment
{
    private final long mObjectiveId;

    private final String mCurrentName;
    private final String mCurrentDescription;

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
        mEditInScheduler = doEdit;
    }

    public void setEditInList(boolean doEdit)
    {
        mEditInList = doEdit;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Activity activity = getActivity();

        final ValueHolder<AlertDialog> resultDialog = new ValueHolder<>(null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = Objects.requireNonNull(activity).getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.layout_dialog_edit_objective, null));
        builder.setTitle(R.string.editObjectiveDialogName);

        builder.setPositiveButton(R.string.createObjective, (dialog, id) ->
        {
            final EditText editTextName         = resultDialog.getValue().findViewById(R.id.editEditObjectiveName);
            final EditText editTextNDescription = resultDialog.getValue().findViewById(R.id.editEditObjectiveDescription);

            String nameText = editTextName.getEditableText().toString();
            if(nameText.isEmpty())
            {
                Toast toast = Toast.makeText(activity, R.string.invalidObjectiveName, Toast.LENGTH_SHORT);
                toast.show();
            }
            else
            {
                String descriptionText = editTextNDescription.getEditableText().toString();

                String configFolder = Objects.requireNonNull(activity.getExternalFilesDir("/app")).getAbsolutePath();

                TransactionDispatcher transactionDispatcher = new TransactionDispatcher(configFolder);
                transactionDispatcher.setSchedulerCache(mSchedulerCache);
                transactionDispatcher.setListCache(mListCache);

                transactionDispatcher.editObjectiveTransaction(mObjectiveId, nameText, descriptionText, mEditInScheduler, mEditInList);
            }
        });

        builder.setNegativeButton(R.string.createCancel, (dialog, id) ->
        {
            //Nothing
        });

        resultDialog.setValue(builder.create());
        resultDialog.getValue().setOnShowListener(dialogInterface ->
        {
            EditText editTextName        = resultDialog.getValue().findViewById(R.id.editEditObjectiveName);
            EditText editTextDescription = resultDialog.getValue().findViewById(R.id.editEditObjectiveDescription);

            editTextName.setText(mCurrentName);
            editTextDescription.setText(mCurrentDescription);
        });

        return resultDialog.getValue();
    }
}
