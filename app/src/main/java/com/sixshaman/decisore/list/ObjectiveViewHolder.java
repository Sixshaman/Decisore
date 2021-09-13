package com.sixshaman.decisore.list;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.archive.ArchiveActivity;
import com.sixshaman.decisore.scheduler.SchedulerActivity;
import com.sixshaman.decisore.scheduler.chain.ObjectiveChain;
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.decisore.utils.*;

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
    final int MENU_GO_TO_PARENT           = 4;
    final int MENU_EDIT_OBJECTIVE         = 5;
    final int MENU_DELETE_OBJECTIVE       = 6;

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

        mParentLayout.setOnClickListener(view -> Toast.makeText(itemView.getContext(), mObjectiveListCache.getObjective(mObjectiveId).getDescription(), Toast.LENGTH_SHORT).show());

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
        }

        TransactionDispatcher transactionDispatcher = new TransactionDispatcher(configFolder);
        transactionDispatcher.setListCache(mObjectiveListCache);
        transactionDispatcher.setSchedulerCache(objectiveSchedulerCache);

        long objectiveParentId = mObjectiveListCache.getObjective(mObjectiveId).getParentId();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(view.getContext()).getApplicationContext());
        String dayStartTimeString = sharedPreferences.getString("day_start_time", "6");
        int dayStartTime = ParseUtils.parseInt(dayStartTimeString, 6);

        MenuItem scheduleTomorrowItem = contextMenu.add(0, MENU_SCHEDULE_FOR_TOMORROW,  Menu.NONE, R.string.menu_schedule_for_tomorrow);
        scheduleTomorrowItem.setOnMenuItemClickListener(menuItem ->
        {
            LocalDateTime enlistDateTime = LocalDateTime.now().minusHours(dayStartTime);
            enlistDateTime = enlistDateTime.plusDays(1).truncatedTo(ChronoUnit.DAYS);
            enlistDateTime = enlistDateTime.plusHours(dayStartTime);

            transactionDispatcher.rescheduleEnlistedObjectiveTransaction(mObjectiveListCache.getObjective(mObjectiveId), enlistDateTime);
            return true;
        });

        MenuItem scheduleItem = contextMenu.add(0, MENU_SCHEDULE_FOR_ARBITRARY, Menu.NONE, R.string.menu_schedule_for_arbitrary);
        scheduleItem.setOnMenuItemClickListener(menuItem ->
        {
            DatePickerDialog datePickerDialog = new DatePickerDialog(view.getContext());
            datePickerDialog.setOnDateSetListener((datePicker, year, month, day) ->
            {
                //Java numerates months from 0, not from 1
                LocalDateTime dateTime = LocalDateTime.of(year, month + 1, day, dayStartTime, 0, 0);
                transactionDispatcher.rescheduleEnlistedObjectiveTransaction(mObjectiveListCache.getObjective(mObjectiveId), dateTime);
            });

            datePickerDialog.show();
            return true;
        });

        MenuItem addBeforeItem = contextMenu.add(1, MENU_ADD_OBJECTIVE_BEFORE, Menu.NONE, R.string.menu_add_objective_before);
        addBeforeItem.setOnMenuItemClickListener(menuItem ->
        {
            //Find the chain the objective belonged to
            //If such chain exists, move it there
            //If not, create a new one with the moved objective name
            final ValueHolder<Long> addedChainIdHolder = new ValueHolder<>((long)-1);

            NewObjectiveDialogFragment newObjectiveDialogFragment = new NewObjectiveDialogFragment();
            newObjectiveDialogFragment.setOnBeforeObjectiveCreatedListener(() ->
            {
                EnlistedObjective oldObjective = mObjectiveListCache.getObjective(mObjectiveId);
                long chainId = transactionDispatcher.rechainEnlistedObjective(oldObjective);

                addedChainIdHolder.setValue(chainId);
            });

            newObjectiveDialogFragment.setOnAfterObjectiveCreatedListener(objectiveId ->
            {
                if(addedChainIdHolder.getValue() != -1)
                {
                    transactionDispatcher.bindObjectiveToChain(addedChainIdHolder.getValue(), objectiveId);
                }

                transactionDispatcher.updateObjectiveListTransaction(LocalDateTime.now(), dayStartTime);
            });

            newObjectiveDialogFragment.setSchedulerCache(objectiveSchedulerCache);
            newObjectiveDialogFragment.setListCache(mObjectiveListCache);

            FragmentActivity fragmentActivity = (FragmentActivity)view.getContext();
            newObjectiveDialogFragment.show(fragmentActivity.getSupportFragmentManager(), fragmentActivity.getString(R.string.newObjectiveDialogName));

            return true;
        });

        MenuItem addAfterItem = contextMenu.add(1, MENU_ADD_OBJECTIVE_AFTER, Menu.NONE, R.string.menu_add_objective_after);
        addAfterItem.setOnMenuItemClickListener(menuItem ->
        {
            //Find a chain the objective belonged to
            //If such chain exists, add the new objective there
            //If not, create a new one, make it as if it contained the given objective and add the new objective there
            NewObjectiveDialogFragment newObjectiveDialogFragment = new NewObjectiveDialogFragment();

            newObjectiveDialogFragment.setSchedulerCache(objectiveSchedulerCache);
            newObjectiveDialogFragment.setListCache(mObjectiveListCache);

            FragmentActivity fragmentActivity = (FragmentActivity)view.getContext();
            newObjectiveDialogFragment.show(fragmentActivity.getSupportFragmentManager(), fragmentActivity.getString(R.string.newObjectiveDialogName));

            //IS ALREADY NON-ACTIVE AT THIS POINT
            newObjectiveDialogFragment.setOnBeforeObjectiveCreatedListener(() ->
            {
                long touchedChainId = transactionDispatcher.touchChainWithObjective(mObjectiveId);
                newObjectiveDialogFragment.setChainIdToAddTo(touchedChainId, true);
            });

            return true;
        });

        if(objectiveParentId != -1)
        {
            String elementType = objectiveSchedulerCache.getElementTypeById(objectiveParentId);

            String elementName = "";
            if(elementType.equals("ObjectiveChain"))
            {
                elementName = "chain";
            }
            else if(elementType.equals("ObjectivePool"))
            {
                elementName = "pool";
            }

            String openParentString = view.getResources().getString(R.string.menu_open_parent) + " " + elementName;
            MenuItem goToParentItem = contextMenu.add(2, MENU_GO_TO_PARENT, Menu.NONE, openParentString);
            goToParentItem.setOnMenuItemClickListener(menuItem ->
            {
                Intent parentOpenIntent = new Intent(view.getContext(), SchedulerActivity.class);
                parentOpenIntent.putExtra("ElementId", objectiveParentId);
                parentOpenIntent.putExtra("ElementType", elementType);
                view.getContext().startActivity(parentOpenIntent);
                return true;
            });
        }

        MenuItem editItem = contextMenu.add(3, MENU_EDIT_OBJECTIVE, Menu.NONE, R.string.menu_edit_objective);
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

        MenuItem deleteItem = contextMenu.add(3, MENU_DELETE_OBJECTIVE, Menu.NONE, R.string.menu_delete_objective);
        deleteItem.setOnMenuItemClickListener(menuItem ->
        {
            EnlistedObjective objective = mObjectiveListCache.getObjective(mObjectiveId);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(view.getContext());
            alertDialogBuilder.setMessage(view.getContext().getString(R.string.deleteObjectiveAreYouSure) + " " + objective.getName() + "?");
            alertDialogBuilder.setPositiveButton("Yes", (dialogInterface, i) ->
            {
                transactionDispatcher.deleteObjectiveFromListTransaction(objective);
                transactionDispatcher.updateObjectiveListTransaction(LocalDateTime.now(), dayStartTime);
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
