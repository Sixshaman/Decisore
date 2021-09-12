package com.sixshaman.decisore.scheduler.chain;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.list.ObjectiveListCache;
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import com.sixshaman.decisore.utils.EditObjectiveDialogFragment;
import com.sixshaman.decisore.utils.LockedReadFile;
import com.sixshaman.decisore.utils.ParseUtils;
import com.sixshaman.decisore.utils.TransactionDispatcher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

public class ChainElementViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener
{
    final int MENU_EDIT_ELEMENT             = 0;
    final int MENU_DELETE_ELEMENT           = 1;
    final int MENU_RESCHEDULE_OBJECTIVE_NOW = 2;
    final int MENU_RESCHEDULE_OBJECTIVE     = 3;
    final int MENU_PAUSE_OBJECTIVE          = 4;

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

        mParentLayout.setOnClickListener(view ->
        {
            final String onClickMessage = "Scheduled for " + mScheduledObjective.getScheduledEnlistDate().toString();
            Toast.makeText(view.getContext(), onClickMessage, Toast.LENGTH_SHORT).show();
        });
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

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(view.getContext()).getApplicationContext());
        String dayStartTimeString = sharedPreferences.getString("day_start_time", "6");
        int dayStartTime = ParseUtils.parseInt(dayStartTimeString, 6);

        String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();

        TransactionDispatcher transactionDispatcher = new TransactionDispatcher(configFolder);
        transactionDispatcher.setSchedulerCache(mObjectiveSchedulerCache);

        String pauseString;
        if(mScheduledObjective.isPaused())
        {
            pauseString = view.getContext().getString(R.string.menu_unpause_element);
        }
        else
        {
            pauseString = view.getContext().getString(R.string.menu_pause_element);
        }

        int menuIndex = 0;

        MenuItem rescheduleNowItem = contextMenu.add(menuIndex, MENU_RESCHEDULE_OBJECTIVE_NOW, Menu.NONE, R.string.menu_schedule_for_now);
        rescheduleNowItem.setOnMenuItemClickListener(menuItem ->
        {
            LocalDateTime enlistDateTime = LocalDateTime.now();

            transactionDispatcher.rescheduleScheduledObjectiveTransaction(mScheduledObjective.getId(), enlistDateTime);
            return true;
        });

        MenuItem rescheduleItem = contextMenu.add(menuIndex++, MENU_RESCHEDULE_OBJECTIVE, Menu.NONE, R.string.menu_schedule_for_arbitrary);
        rescheduleItem.setOnMenuItemClickListener(menuItem ->
        {
            DatePickerDialog datePickerDialog = new DatePickerDialog(view.getContext());
            datePickerDialog.setOnDateSetListener((datePicker, year, month, day) ->
            {
                //Java numerates months from 0, not from 1
                LocalDateTime dateTime = LocalDateTime.of(year, month + 1, day, dayStartTime, 0, 0);

                transactionDispatcher.rescheduleScheduledObjectiveTransaction(mScheduledObjective.getId(), dateTime);
                transactionDispatcher.updateObjectiveListTransaction(LocalDateTime.now(), dayStartTime);
            });

            datePickerDialog.show();
            return true;
        });

        if(mScheduledObjective.isRepeatable())
        {
            MenuItem pauseItem = contextMenu.add(menuIndex++, MENU_PAUSE_OBJECTIVE, Menu.NONE, pauseString);
            pauseItem.setOnMenuItemClickListener(menuItem ->
            {
                transactionDispatcher.flipPauseObjective(mScheduledObjective.getId());
                return true;
            });
        }

        MenuItem editItem = contextMenu.add(menuIndex++, MENU_EDIT_ELEMENT, Menu.NONE, R.string.menu_edit_objective);
        editItem.setOnMenuItemClickListener(menuItem ->
        {
            EditObjectiveDialogFragment editObjectiveDialogFragment = new EditObjectiveDialogFragment(mScheduledObjective.getId(), mScheduledObjective.getName(), mScheduledObjective.getDescription());
            editObjectiveDialogFragment.setSchedulerCache(mObjectiveSchedulerCache);
            editObjectiveDialogFragment.setEditInScheduler(true);
            editObjectiveDialogFragment.setEditInList(true);

            FragmentActivity activity = (FragmentActivity)(view.getContext());
            editObjectiveDialogFragment.show(activity.getSupportFragmentManager(), activity.getString(R.string.editObjectiveDialogName));

            return true;
        });

        MenuItem deleteItem = contextMenu.add(menuIndex, MENU_DELETE_ELEMENT, Menu.NONE, R.string.menu_delete_objective);
        deleteItem.setOnMenuItemClickListener(menuItem ->
        {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(view.getContext());
            alertDialogBuilder.setMessage(view.getContext().getString(R.string.deleteObjectiveAreYouSure) + " " + mScheduledObjective.getName() + "?");
            alertDialogBuilder.setPositiveButton("Yes", (dialogInterface, i) -> transactionDispatcher.deleteObjectiveFromSchedulerTransaction(mScheduledObjective));
            alertDialogBuilder.setNegativeButton("No", (dialogInterface, i) -> {});

            alertDialogBuilder.show();

            return true;
        });

        contextMenu.setGroupDividerEnabled(true);
    }
}