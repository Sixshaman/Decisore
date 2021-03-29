package com.sixshaman.decisore.list;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.scheduler.chain.ObjectiveChain;
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.decisore.utils.EditObjectiveDialogFragment;
import com.sixshaman.decisore.utils.LockedReadFile;
import com.sixshaman.decisore.utils.NewObjectiveDialogFragment;
import com.sixshaman.decisore.utils.TransactionDispatcher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

class ObjectiveViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener
{
    final int MENU_SCHEDULE_FOR_TOMORROW  = 0;
    final int MENU_SCHEDULE_FOR_ARBITRARY = 1;
    final int MENU_ADD_OBJECTIVE_BEFORE   = 2;
    final int MENU_ADD_OBJECTIVE_AFTER    = 3;
    final int MENU_EDIT_OBJECTIVE         = 4;
    final int MENU_DELETE_OBJECTIVE       = 5;

    final TextView mTextView;
    final CheckBox mCheckbox;

    final ConstraintLayout mParentLayout;

    private ObjectiveListCache mObjectiveListCache;

    private long mObjectiveId;

    public ObjectiveViewHolder(View itemView)
    {
        super(itemView);

        mTextView = itemView.findViewById(R.id.textObjectiveName);
        mCheckbox = itemView.findViewById(R.id.checkBoxObjectiveDone);

        mParentLayout = itemView.findViewById(R.id.layoutObjectiveView);

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
        MenuItem addBeforeItem        = contextMenu.add(1, MENU_ADD_OBJECTIVE_BEFORE,   Menu.NONE, R.string.menu_add_objective_before);
        MenuItem addAfterItem         = contextMenu.add(1, MENU_ADD_OBJECTIVE_AFTER,    Menu.NONE, R.string.menu_add_objective_after);
        MenuItem editItem             = contextMenu.add(2, MENU_EDIT_OBJECTIVE,         Menu.NONE, R.string.menu_edit_objective);
        MenuItem deleteItem           = contextMenu.add(2, MENU_DELETE_OBJECTIVE,       Menu.NONE, R.string.menu_delete_objective);

        scheduleTomorrowItem.setOnMenuItemClickListener(menuItem ->
        {
            LocalDateTime enlistDateTime = LocalDateTime.now().minusHours(6); //Day starts at 6AM!
            enlistDateTime = enlistDateTime.plusDays(1).truncatedTo(ChronoUnit.DAYS);
            enlistDateTime = enlistDateTime.plusHours(6); //Day starts at 6AM

            TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
            transactionDispatcher.setListCache(mObjectiveListCache);

            String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();
            transactionDispatcher.rescheduleEnlistedObjectiveTransaction(configFolder, mObjectiveListCache.getObjective(mObjectiveId), enlistDateTime);

            return true;
        });

        scheduleItem.setOnMenuItemClickListener(menuItem ->
        {
            DatePickerDialog datePickerDialog = new DatePickerDialog(view.getContext());
            datePickerDialog.setOnDateSetListener((datePicker, year, month, day) ->
            {
                //Day starts at 6 AM
                //Also Java numerates months from 0, not from 1
                LocalDateTime dateTime = LocalDateTime.of(year, month + 1, day, 6, 0, 0);

                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.setListCache(mObjectiveListCache);

                String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();
                transactionDispatcher.rescheduleEnlistedObjectiveTransaction(configFolder, mObjectiveListCache.getObjective(mObjectiveId), dateTime);
            });

            datePickerDialog.show();
            return true;
        });

        addBeforeItem.setOnMenuItemClickListener(menuItem ->
        {
            //Find a chain the objective belonged to
            //If such chain exists, move it there
            //If not, create a new one with the moved objective name
            String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();

            ObjectiveSchedulerCache objectiveSchedulerCache = new ObjectiveSchedulerCache();
            try
            {
                LockedReadFile schedulerFile = new LockedReadFile(configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME);
                objectiveSchedulerCache.invalidate(schedulerFile);
                schedulerFile.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return false;
            }

            TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
            transactionDispatcher.setSchedulerCache(objectiveSchedulerCache);
            transactionDispatcher.setListCache(mObjectiveListCache);

            EnlistedObjective oldObjective = mObjectiveListCache.getObjective(mObjectiveId);
            ObjectiveChain addedChain = transactionDispatcher.rechainEnlistedObjective(configFolder, oldObjective);

            NewObjectiveDialogFragment newObjectiveDialogFragment = new NewObjectiveDialogFragment();
            newObjectiveDialogFragment.setChainToAddTo(addedChain);

            newObjectiveDialogFragment.setSchedulerCache(objectiveSchedulerCache);
            newObjectiveDialogFragment.setListCache(mObjectiveListCache);

            FragmentActivity fragmentActivity = (FragmentActivity)view.getContext();
            newObjectiveDialogFragment.show(fragmentActivity.getSupportFragmentManager(), fragmentActivity.getString(R.string.newObjectiveDialogName));

            return true;
        });

        addAfterItem.setOnMenuItemClickListener(menuItem ->
        {
            //Find a chain the objective belonged to
            //If such chain exists, add the new objective there
            //If not, create a new one, make it as if it contained the given objective and add the new objective there

            String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();

            ObjectiveSchedulerCache objectiveSchedulerCache = new ObjectiveSchedulerCache();
            try
            {
                LockedReadFile schedulerFile = new LockedReadFile(configFolder + "/" + ObjectiveSchedulerCache.SCHEDULER_FILENAME);
                objectiveSchedulerCache.invalidate(schedulerFile);
                schedulerFile.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return false;
            }

            TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
            transactionDispatcher.setSchedulerCache(objectiveSchedulerCache);
            transactionDispatcher.setListCache(mObjectiveListCache);

            EnlistedObjective currentObjective = mObjectiveListCache.getObjective(mObjectiveId);
            ObjectiveChain addedChain = transactionDispatcher.touchChainWithObjective(configFolder, currentObjective);

            NewObjectiveDialogFragment newObjectiveDialogFragment = new NewObjectiveDialogFragment();
            newObjectiveDialogFragment.setChainToAddTo(addedChain);

            newObjectiveDialogFragment.setSchedulerCache(objectiveSchedulerCache);
            newObjectiveDialogFragment.setListCache(mObjectiveListCache);

            FragmentActivity fragmentActivity = (FragmentActivity)view.getContext();
            newObjectiveDialogFragment.show(fragmentActivity.getSupportFragmentManager(), fragmentActivity.getString(R.string.newObjectiveDialogName));

            return true;
        });

        editItem.setOnMenuItemClickListener(menuItem ->
        {
            EnlistedObjective objective = mObjectiveListCache.getObjective(mObjectiveId);

            EditObjectiveDialogFragment editObjectiveDialogFragment = new EditObjectiveDialogFragment(mObjectiveId, objective.getName(), objective.getDescription());
            editObjectiveDialogFragment.setListCache(mObjectiveListCache);
            editObjectiveDialogFragment.setEditInList(true);

            FragmentActivity activity = (FragmentActivity)(view.getContext());
            editObjectiveDialogFragment.show(activity.getSupportFragmentManager(), activity.getString(R.string.newObjectiveDialogName));

            return true;
        });

        deleteItem.setOnMenuItemClickListener(menuItem ->
        {
            EnlistedObjective objective = mObjectiveListCache.getObjective(mObjectiveId);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(view.getContext());
            alertDialogBuilder.setMessage(view.getContext().getString(R.string.deleteObjectiveAreYouSure) + " " + objective.getName() + "?");
            alertDialogBuilder.setPositiveButton("Yes", (dialogInterface, i) ->
            {
                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.setListCache(mObjectiveListCache);

                String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();
                transactionDispatcher.deleteObjectiveFromListTransaction(configFolder, objective);
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
