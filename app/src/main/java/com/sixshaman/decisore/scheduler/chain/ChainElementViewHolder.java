package com.sixshaman.decisore.scheduler.chain;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import com.sixshaman.decisore.utils.EditObjectiveDialogFragment;
import com.sixshaman.decisore.utils.TransactionDispatcher;

import java.time.LocalDateTime;
import java.util.Objects;

public class ChainElementViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener
{
    final int MENU_EDIT_ELEMENT         = 0;
    final int MENU_DELETE_ELEMENT       = 1;
    final int MENU_RESCHEDULE_OBJECTIVE = 2;

    private ObjectiveSchedulerCache mObjectiveSchedulerCache;

    private ScheduledObjective mScheduledObjective;

    final TextView mTextView;

    final ConstraintLayout mParentLayout;

    public ChainElementViewHolder(View itemView)
    {
        super(itemView);

        mTextView = itemView.findViewById(R.id.textScheduledObjectiveName);

        mParentLayout = itemView.findViewById(R.id.layoutScheduledObjectiveView);

        mParentLayout.setOnCreateContextMenuListener(this);

        final String onClickMessage = "Scheduled for " + mScheduledObjective.getScheduledEnlistDate().toString();
        mParentLayout.setOnClickListener(view -> Toast.makeText(view.getContext(), onClickMessage, Toast.LENGTH_SHORT).show());
    }

    void setSourceMetadata(ObjectiveSchedulerCache schedulerCache, ScheduledObjective objective)
    {
        mObjectiveSchedulerCache = schedulerCache;
        mScheduledObjective      = objective;
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo)
    {
        contextMenu.setHeaderTitle(mTextView.getText());

        MenuItem rescheduleItem = contextMenu.add(0, MENU_RESCHEDULE_OBJECTIVE, Menu.NONE, R.string.menu_schedule_for_arbitrary);
        MenuItem editItem       = contextMenu.add(1, MENU_EDIT_ELEMENT,         Menu.NONE, R.string.menu_edit_objective);
        MenuItem deleteItem     = contextMenu.add(1, MENU_DELETE_ELEMENT,       Menu.NONE, R.string.menu_delete_objective);

        rescheduleItem.setOnMenuItemClickListener(menuItem ->
        {
            assert mObjectiveSchedulerCache != null;
            assert mScheduledObjective      != null;

            DatePickerDialog datePickerDialog = new DatePickerDialog(view.getContext());
            datePickerDialog.setOnDateSetListener((datePicker, year, month, day) ->
            {
                //Day starts at 6 AM
                //Also Java numerates months from 0, not from 1
                LocalDateTime dateTime = LocalDateTime.of(year, month + 1, day, 6, 0, 0);

                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.setSchedulerCache(mObjectiveSchedulerCache);

                String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();
                transactionDispatcher.rescheduleScheduledObjectiveTransaction(configFolder, mScheduledObjective, dateTime);

                transactionDispatcher.updateObjectiveListTransaction(configFolder, LocalDateTime.now());
            });

            datePickerDialog.show();
            return true;
        });

        editItem.setOnMenuItemClickListener(menuItem ->
        {
            assert mObjectiveSchedulerCache != null;
            assert mScheduledObjective      != null;

            EditObjectiveDialogFragment editObjectiveDialogFragment = new EditObjectiveDialogFragment(mScheduledObjective.getId(), mScheduledObjective.getName(), mScheduledObjective.getDescription());
            editObjectiveDialogFragment.setSchedulerCache(mObjectiveSchedulerCache);
            editObjectiveDialogFragment.setEditInScheduler(true);
            editObjectiveDialogFragment.setEditInList(true);

            FragmentActivity activity = (FragmentActivity)(view.getContext());
            editObjectiveDialogFragment.show(activity.getSupportFragmentManager(), activity.getString(R.string.editObjectiveDialogName));

            return true;
        });

        deleteItem.setOnMenuItemClickListener(menuItem ->
        {
            assert mObjectiveSchedulerCache != null;
            assert mScheduledObjective      != null;

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(view.getContext());
            alertDialogBuilder.setMessage(view.getContext().getString(R.string.deleteObjectiveAreYouSure) + " " + mScheduledObjective.getName() + "?");
            alertDialogBuilder.setPositiveButton("Yes", (dialogInterface, i) ->
            {
                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.setSchedulerCache(mObjectiveSchedulerCache);

                String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();
                transactionDispatcher.deleteObjectiveFromSchedulerTransaction(configFolder, mScheduledObjective);
            });

            alertDialogBuilder.setNegativeButton("No", (dialogInterface, i) ->
            {

            });

            alertDialogBuilder.show();

            return true;
        });

        contextMenu.setGroupDividerEnabled(true);
    }
}