package com.sixshaman.advancedunforgetter.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.list.ObjectiveListCache;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveSchedulerCache;

import java.util.Objects;

public class NewChainDialogFragment extends DialogFragment
{
    private ObjectiveSchedulerCache mSchedulerCache;

    public NewChainDialogFragment()
    {
    }

    public void setSchedulerCache(ObjectiveSchedulerCache schedulerCache)
    {
        mSchedulerCache = schedulerCache;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Activity activity = getActivity();

        final ValueHolder<AlertDialog> resultDialog = new ValueHolder<>(null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.layout_dialog_new_chain, null));
        builder.setTitle(R.string.newChainDialogName);

        builder.setPositiveButton(R.string.createChain, (dialog, id) ->
        {
            final EditText editTextName        = resultDialog.getValue().findViewById(R.id.editNewChainName);
            final EditText editEditDescription = resultDialog.getValue().findViewById(R.id.editNewChainDescription);

            String nameText = editTextName.getEditableText().toString();
            if(nameText.isEmpty())
            {
                Toast toast = Toast.makeText(activity, R.string.invalidChainName, Toast.LENGTH_SHORT);
                toast.show();
            }
            else
            {
                String descriptionText = editEditDescription.getEditableText().toString();

                String configFolder = Objects.requireNonNull(activity.getExternalFilesDir("/app")).getAbsolutePath();

                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.setSchedulerCache(mSchedulerCache);

                transactionDispatcher.addChainTransaction(configFolder, nameText, descriptionText);
            }
        });

        builder.setNegativeButton(R.string.createCancel, (dialog, id) ->
        {
            //Nothing
        });

        return resultDialog.getValue();
    }
}