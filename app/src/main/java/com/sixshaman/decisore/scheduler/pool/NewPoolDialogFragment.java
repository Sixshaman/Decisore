package com.sixshaman.decisore.scheduler.pool;

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
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.decisore.utils.TransactionDispatcher;
import com.sixshaman.decisore.utils.ValueHolder;

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
        Activity activity = getActivity();

        final ValueHolder<AlertDialog> resultDialog = new ValueHolder<>(null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = Objects.requireNonNull(activity).getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.layout_dialog_new_pool, null));
        builder.setTitle(R.string.newPoolDialogName);

        builder.setPositiveButton(R.string.createPool, (dialog, id) ->
        {
            final EditText editTextName        = resultDialog.getValue().findViewById(R.id.editNewPoolName);
            final EditText editEditDescription = resultDialog.getValue().findViewById(R.id.editNewPoolDescription);

            String nameText = editTextName.getEditableText().toString();
            if(nameText.isEmpty())
            {
                Toast toast = Toast.makeText(activity, R.string.invalidPoolName, Toast.LENGTH_SHORT);
                toast.show();
            }
            else
            {
                String descriptionText = editEditDescription.getEditableText().toString();

                String configFolder = Objects.requireNonNull(activity.getExternalFilesDir("/app")).getAbsolutePath();

                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.setSchedulerCache(mSchedulerCache);

                transactionDispatcher.addPoolTransaction(configFolder, nameText, descriptionText);
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
