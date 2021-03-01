package com.sixshaman.advancedunforgetter.list;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.advancedunforgetter.BuildConfig;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.utils.EditObjectiveDialogFragment;
import com.sixshaman.advancedunforgetter.utils.NewObjectiveDialogFragment;
import com.sixshaman.advancedunforgetter.utils.TransactionDispatcher;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Calendar;
import java.util.Objects;

class ObjectiveViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener
{
    final int MENU_SCHEDULE_FOR_TOMORROW  = 0;
    final int MENU_SCHEDULE_FOR_ARBITRARY = 1;
    final int MENU_ADD_OBJECTIVE_BEFORE   = 2;
    final int MENU_ADD_OBJECTIVE_AFTER    = 3;
    final int MENU_EDIT_OBJECTIVE         = 4;
    final int MENU_DELETE_OBJECTIVE       = 5;

    TextView mTextView;
    CheckBox mCheckbox;

    ConstraintLayout mParentLayout;

    private ObjectiveListCache mObjectiveListCache;

    private long mObjectiveId;

    public ObjectiveViewHolder(View itemView)
    {
        super(itemView);

        mTextView = itemView.findViewById(R.id.textTaskName);
        mCheckbox = itemView.findViewById(R.id.checkBoxTaskDone);

        mParentLayout = itemView.findViewById(R.id.layoutTaskView);

        mParentLayout.setOnCreateContextMenuListener(this);

        mObjectiveListCache = null;

        mObjectiveId = -1;
    }

    void setObjectiveMetadata(ObjectiveListCache objectiveListCache, long objectiveId)
    {
        mObjectiveListCache = objectiveListCache;
        mObjectiveId        = objectiveId;
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo)
    {
        contextMenu.setHeaderTitle(mTextView.getText());

        MenuItem scheduleTomorrowItem = contextMenu.add(0, MENU_SCHEDULE_FOR_TOMORROW,  Menu.NONE, R.string.menu_schedule_for_tomorrow);
        MenuItem scheduleItem         = contextMenu.add(0, MENU_SCHEDULE_FOR_ARBITRARY, Menu.NONE, R.string.menu_schedule_for_arbitrary);
        //MenuItem addBeforeItem        = contextMenu.add(1, MENU_ADD_OBJECTIVE_BEFORE,   Menu.NONE, R.string.menu_add_objective_before);
        //MenuItem addAfterItem         = contextMenu.add(1, MENU_ADD_OBJECTIVE_AFTER,    Menu.NONE, R.string.menu_add_objective_after);
        MenuItem editItem             = contextMenu.add(2, MENU_EDIT_OBJECTIVE,         Menu.NONE, R.string.menu_edit_objective);
        MenuItem deleteItem           = contextMenu.add(2, MENU_DELETE_OBJECTIVE,       Menu.NONE, R.string.menu_delete_objective);

        scheduleTomorrowItem.setOnMenuItemClickListener(menuItem ->
        {
            assert mObjectiveListCache != null;
            assert mObjectiveId        != -1;

            LocalDateTime enlistDateTime = LocalDateTime.now().minusHours(6); //Day starts at 6AM!
            enlistDateTime = enlistDateTime.plusDays(1).truncatedTo(ChronoUnit.DAYS);
            enlistDateTime = enlistDateTime.plusHours(6); //Day starts at 6AM

            TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
            transactionDispatcher.setListCache(mObjectiveListCache);

            String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();
            transactionDispatcher.recheduleObjectiveTransaction(configFolder, mObjectiveListCache.getObjective(mObjectiveId), enlistDateTime);

            return true;
        });

        scheduleItem.setOnMenuItemClickListener(menuItem ->
        {
            assert mObjectiveListCache != null;
            assert mObjectiveId        != -1;

            DatePickerDialog datePickerDialog = new DatePickerDialog(view.getContext());
            datePickerDialog.setOnDateSetListener((datePicker, year, month, day) ->
            {
                //Day starts at 6 AM
                //Also Java numerates months from 0, not from 1
                LocalDateTime dateTime = LocalDateTime.of(year, month + 1, day, 6, 0, 0);

                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.setListCache(mObjectiveListCache);

                String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();
                transactionDispatcher.recheduleObjectiveTransaction(configFolder, mObjectiveListCache.getObjective(mObjectiveId), dateTime);
            });

            datePickerDialog.show();
            return true;
        });

        editItem.setOnMenuItemClickListener(menuItem ->
        {
            assert mObjectiveListCache != null;
            assert mObjectiveId        != -1;

            EnlistedObjective objective = mObjectiveListCache.getObjective(mObjectiveId);

            EditObjectiveDialogFragment editObjectiveDialogFragment = new EditObjectiveDialogFragment(mObjectiveId, objective.getName(), objective.getDescription());
            editObjectiveDialogFragment.setListCache(mObjectiveListCache);

            FragmentActivity activity = (FragmentActivity)(view.getContext());
            editObjectiveDialogFragment.show(activity.getSupportFragmentManager(), activity.getString(R.string.newTaskDialogName));

            return true;
        });

        deleteItem.setOnMenuItemClickListener(menuItem ->
        {
            assert mObjectiveListCache != null;
            assert mObjectiveId        != -1;

            EnlistedObjective objective = mObjectiveListCache.getObjective(mObjectiveId);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(view.getContext());
            alertDialogBuilder.setMessage(view.getContext().getString(R.string.deleteObjectiveAreYouSure) + " " + objective.getName() + "?");
            alertDialogBuilder.setPositiveButton("Yes", (dialogInterface, i) ->
            {
                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.setListCache(mObjectiveListCache);

                String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();
                transactionDispatcher.deleteObjectiveTransaction(configFolder, objective);
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
