package com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain;

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
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.ObjectivePool;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.advancedunforgetter.utils.TransactionDispatcher;
import com.sixshaman.advancedunforgetter.utils.ValueHolder;

import java.util.Objects;

public class EditChainDialogFragment extends DialogFragment
{
    private long mChainId;

    private String mCurrentName;
    private String mCurrentDescription;

    private ObjectiveSchedulerCache mSchedulerCache;

    public EditChainDialogFragment(long chainId, String chainName, String chainDescription)
    {
        mChainId = chainId;

        mCurrentName        = chainName;
        mCurrentDescription = chainDescription;
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

        builder.setView(inflater.inflate(R.layout.layout_dialog_edit_chain, null));
        builder.setTitle(R.string.editChainDialogName);

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

                transactionDispatcher.editСhainTransaction(configFolder, mChainId, nameText, descriptionText);
            }
        });

        builder.setNegativeButton(R.string.createCancel, (dialog, id) ->
        {
            //Nothing
        });

        resultDialog.setValue(builder.create());
        resultDialog.getValue().setOnShowListener(dialogInterface ->
        {
            EditText editTextName        = resultDialog.getValue().findViewById(R.id.editEditChainName);
            EditText editTextDescription = resultDialog.getValue().findViewById(R.id.editEditChainDescription);

            editTextName.setText(mCurrentName);
            editTextDescription.setText(mCurrentDescription);
        });

        return resultDialog.getValue();
    }
}