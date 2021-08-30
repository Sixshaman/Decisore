package com.sixshaman.decisore.scheduler.chain;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.scheduler.pool.ObjectivePool;
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.decisore.utils.TransactionDispatcher;
import com.sixshaman.decisore.utils.ValueHolder;

import java.time.Duration;
import java.util.Objects;

public class NewChainDialogFragment extends DialogFragment
{
    private long mPoolIdToAddTo;

    private ObjectiveSchedulerCache mSchedulerCache;

    public NewChainDialogFragment()
    {
        mPoolIdToAddTo = -1;
    }

    public void setSchedulerCache(ObjectiveSchedulerCache schedulerCache)
    {
        mSchedulerCache = schedulerCache;
    }

    public void setPoolIdToAddTo(long poolId)
    {
        mPoolIdToAddTo = poolId;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Activity activity = getActivity();

        final ValueHolder<AlertDialog> resultDialog = new ValueHolder<>(null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = Objects.requireNonNull(activity).getLayoutInflater();

        builder.setView(View.inflate(activity, R.layout.layout_dialog_new_chain, null));
        builder.setTitle(R.string.newChainDialogName);

        builder.setPositiveButton(R.string.createChain, (dialog, id) ->
        {
            final EditText editTextName        = resultDialog.getValue().findViewById(R.id.editNewChainName);
            final EditText editEditDescription = resultDialog.getValue().findViewById(R.id.editNewChainDescription);

            final Spinner  frequencySpinner    = resultDialog.getValue().findViewById(R.id.spinnerChainFrequency);
            final CheckBox autoDeleteCheckbox  = resultDialog.getValue().findViewById(R.id.checkboxAutoDelete);
            final CheckBox unstoppableCheckbox = resultDialog.getValue().findViewById(R.id.checkboxChainUnstoppable);

            String nameText        = editTextName.getEditableText().toString();
            String descriptionText = editEditDescription.getEditableText().toString();

            Duration chainProduceFrequency = Duration.ofNanos(1); //Just to stop Intellij IDEA from whining about already assigned value

            int objectiveIntervalIndex = frequencySpinner.getSelectedItemPosition();
            switch(objectiveIntervalIndex)
            {
                case 0: //Instant
                {
                    chainProduceFrequency = Duration.ZERO;
                    break;
                }
                case 1: //Daily
                {
                    chainProduceFrequency = Duration.ofDays(1);
                    break;
                }
                case 2: //Weekly
                {
                    chainProduceFrequency = Duration.ofDays(7);
                    break;
                }
                case 3: //Monthly
                {
                    chainProduceFrequency = Duration.ofDays(30);
                    break;
                }
            }

            boolean useAutoDelete  = autoDeleteCheckbox.isChecked();
            boolean useUnstoppable = unstoppableCheckbox.isChecked();
            if(nameText.isEmpty())
            {
                Toast toast = Toast.makeText(activity, R.string.invalidChainName, Toast.LENGTH_SHORT);
                toast.show();
            }
            else
            {
                String configFolder = Objects.requireNonNull(activity.getExternalFilesDir("/app")).getAbsolutePath();

                TransactionDispatcher transactionDispatcher = new TransactionDispatcher(configFolder);
                transactionDispatcher.setSchedulerCache(mSchedulerCache);

                transactionDispatcher.addChainTransaction(mPoolIdToAddTo, nameText, descriptionText, chainProduceFrequency, useAutoDelete, useUnstoppable);
            }
        });

        builder.setNegativeButton(R.string.createCancel, (dialog, id) ->
        {
            //Nothing
        });

        resultDialog.setValue(builder.create());
        return resultDialog.getValue();
    }
}