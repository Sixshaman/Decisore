package com.sixshaman.decisore.scheduler.pool;

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
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.decisore.utils.TransactionDispatcher;
import com.sixshaman.decisore.utils.ValueHolder;

import java.time.Duration;
import java.util.Objects;

public class NewPoolDialogFragment extends DialogFragment
{
    private ObjectiveSchedulerCache mSchedulerCache;

    public NewPoolDialogFragment()
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
        Activity activity = requireActivity();

        final ValueHolder<AlertDialog> resultDialog = new ValueHolder<>(null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setView(View.inflate(activity, R.layout.layout_dialog_new_pool, null));
        builder.setTitle(R.string.newPoolDialogName);

        builder.setPositiveButton(R.string.createPool, (dialog, id) ->
        {
            final EditText editTextName        = resultDialog.getValue().findViewById(R.id.editNewPoolName);
            final EditText editEditDescription = resultDialog.getValue().findViewById(R.id.editNewPoolDescription);

            final Spinner  frequencySpinner    = resultDialog.getValue().findViewById(R.id.spinnerPoolFrequency);
            final CheckBox autoDeleteCheckbox  = resultDialog.getValue().findViewById(R.id.checkboxAutoDeletePool);
            final CheckBox unstoppableCheckbox = resultDialog.getValue().findViewById(R.id.checkboxPoolUnstoppable);

            String nameText        = editTextName.getEditableText().toString();
            String descriptionText = editEditDescription.getEditableText().toString();

            Duration poolProduceFrequency = Duration.ofNanos(1); //Just to stop Intellij IDEA from whining about already assigned value

            int objectiveIntervalIndex = frequencySpinner.getSelectedItemPosition();
            switch(objectiveIntervalIndex)
            {
                case 0: //Instant
                {
                    poolProduceFrequency = Duration.ZERO;
                    break;
                }
                case 1: //Daily
                {
                    poolProduceFrequency = Duration.ofDays(1);
                    break;
                }
                case 2: //Weekly
                {
                    poolProduceFrequency = Duration.ofDays(7);
                    break;
                }
                case 3: //Monthly
                {
                    poolProduceFrequency = Duration.ofDays(30);
                    break;
                }
            }

            boolean isAutoDelete  = autoDeleteCheckbox.isChecked();
            boolean isUnstoppable = unstoppableCheckbox.isChecked();
            if(nameText.isEmpty())
            {
                Toast toast = Toast.makeText(activity, R.string.invalidPoolName, Toast.LENGTH_SHORT);
                toast.show();
            }
            else
            {
                String configFolder = Objects.requireNonNull(activity.getExternalFilesDir("/app")).getAbsolutePath();

                TransactionDispatcher transactionDispatcher = new TransactionDispatcher(configFolder);
                transactionDispatcher.setSchedulerCache(mSchedulerCache);

                transactionDispatcher.addPoolTransaction(nameText, descriptionText, poolProduceFrequency, isAutoDelete, isUnstoppable);
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
