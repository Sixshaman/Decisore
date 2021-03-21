package com.sixshaman.advancedunforgetter.scheduler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.advancedunforgetter.R;
import com.sixshaman.advancedunforgetter.list.EnlistedObjective;
import com.sixshaman.advancedunforgetter.list.ObjectiveListCache;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain.EditChainDialogFragment;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.EditPoolDialogFragment;
import com.sixshaman.advancedunforgetter.scheduler.ObjectiveChain.ObjectiveChain;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.ObjectivePool;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.PoolElement;
import com.sixshaman.advancedunforgetter.scheduler.ObjectivePool.PoolFragment;
import com.sixshaman.advancedunforgetter.scheduler.ScheduledObjective.ScheduledObjective;
import com.sixshaman.advancedunforgetter.utils.EditObjectiveDialogFragment;
import com.sixshaman.advancedunforgetter.utils.TransactionDispatcher;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class SchedulerElementViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener
{
    final int MENU_EDIT_ELEMENT         = 0;
    final int MENU_DELETE_ELEMENT       = 1;
    final int MENU_RESCHEDULE_OBJECTIVE = 2;

    private ObjectiveSchedulerCache mObjectiveSchedulerCache;

    private SchedulerElement mSchedulerElement;

    ImageView mIconView;
    TextView mTextView;

    ConstraintLayout mParentLayout;

    public SchedulerElementViewHolder(View itemView)
    {
        super(itemView);

        mTextView = itemView.findViewById(R.id.textScheduledSourceName);
        mIconView = itemView.findViewById(R.id.iconScheduledSource);

        mParentLayout = itemView.findViewById(R.id.layoutScheduledSourceView);

        mParentLayout.setOnClickListener(view ->
        {
            if(mSchedulerElement instanceof ObjectivePool)
            {
                Context parentContext = view.getContext();
                if(parentContext instanceof FragmentActivity)
                {
                    FragmentManager fragmentManager = (((FragmentActivity) parentContext).getSupportFragmentManager());

                    Bundle bundle = new Bundle();
                    bundle.putLong("EyyDee", ((ObjectivePool)mSchedulerElement).getId());

                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.setReorderingAllowed(true);
                    fragmentTransaction.replace(R.id.scheduler_fragment_container_view, PoolFragment.class, null);
                    fragmentTransaction.commit();
                }
            }
        });
    }

    void setSourceMetadata(ObjectiveSchedulerCache objectiveSchedulerCache, SchedulerElement schedulerElement)
    {
        mObjectiveSchedulerCache = objectiveSchedulerCache;
        mSchedulerElement        = schedulerElement;
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo)
    {
        contextMenu.setHeaderTitle(mTextView.getText());

        String editString   = "";
        String deleteString = "";

        boolean deleteDisabled = false;
        if(mSchedulerElement instanceof ScheduledObjective)
        {
            MenuItem rescheduleItem = contextMenu.add(0, MENU_RESCHEDULE_OBJECTIVE, Menu.NONE, R.string.menu_schedule_for_arbitrary);

            ScheduledObjective scheduledObjective = (ScheduledObjective)mSchedulerElement;
            rescheduleItem.setOnMenuItemClickListener(menuItem ->
            {
                assert mObjectiveSchedulerCache != null;
                assert mSchedulerElement        != null;

                DatePickerDialog datePickerDialog = new DatePickerDialog(view.getContext());
                datePickerDialog.setOnDateSetListener((datePicker, year, month, day) ->
                {
                    //Day starts at 6 AM
                    //Also Java numerates months from 0, not from 1
                    LocalDateTime dateTime = LocalDateTime.of(year, month + 1, day, 6, 0, 0);

                    TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                    transactionDispatcher.setSchedulerCache(mObjectiveSchedulerCache);

                    String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();
                    transactionDispatcher.recheduleScheduledObjectiveTransaction(configFolder, scheduledObjective, dateTime);

                    transactionDispatcher.updateObjectiveListTransaction(configFolder, LocalDateTime.now());
                });

                datePickerDialog.show();
                return true;
            });

            editString   = view.getContext().getString(R.string.menu_edit_objective);
            deleteString = view.getContext().getString(R.string.menu_delete_objective);

            deleteDisabled = false;
        }
        else if(mSchedulerElement instanceof ObjectiveChain)
        {
            ObjectiveChain objectiveChain = (ObjectiveChain)mSchedulerElement;

            editString   = view.getContext().getString(R.string.menu_edit_chain);
            deleteString = view.getContext().getString(R.string.menu_delete_chain);

            deleteDisabled = !objectiveChain.isEmpty();
        }
        else if(mSchedulerElement instanceof ObjectivePool)
        {
            ObjectivePool objectivePool = (ObjectivePool)mSchedulerElement;

            editString   = view.getContext().getString(R.string.menu_edit_pool);
            deleteString = view.getContext().getString(R.string.menu_delete_pool);

            deleteDisabled = !objectivePool.isEmpty();
        }

        MenuItem editItem   = contextMenu.add(1, MENU_EDIT_ELEMENT,   Menu.NONE, editString);
        MenuItem deleteItem = contextMenu.add(1, MENU_DELETE_ELEMENT, Menu.NONE, deleteString);

        editItem.setOnMenuItemClickListener(menuItem ->
        {
            assert mObjectiveSchedulerCache != null;
            assert mSchedulerElement        != null;

            if(mSchedulerElement instanceof ObjectivePool)
            {
                ObjectivePool objectivePool = (ObjectivePool)mSchedulerElement;

                EditPoolDialogFragment editPoolDialogFragment = new EditPoolDialogFragment(objectivePool.getId(), objectivePool.getName(), objectivePool.getDescription());
                editPoolDialogFragment.setSchedulerCache(mObjectiveSchedulerCache);

                FragmentActivity activity = (FragmentActivity)(view.getContext());
                editPoolDialogFragment.show(activity.getSupportFragmentManager(), activity.getString(R.string.editPoolDialogName));
            }
            else if(mSchedulerElement instanceof ObjectiveChain)
            {
                ObjectiveChain objectiveChain = (ObjectiveChain)mSchedulerElement;

                EditChainDialogFragment editChainDialogFragment = new EditChainDialogFragment(objectiveChain.getId(), objectiveChain.getName(), objectiveChain.getDescription());
                editChainDialogFragment.setSchedulerCache(mObjectiveSchedulerCache);

                FragmentActivity activity = (FragmentActivity)(view.getContext());
                editChainDialogFragment.show(activity.getSupportFragmentManager(), activity.getString(R.string.editChainDialogName));
            }
            else if(mSchedulerElement instanceof ScheduledObjective)
            {
                ScheduledObjective scheduledObjective = (ScheduledObjective)mSchedulerElement;

                EditObjectiveDialogFragment editObjectiveDialogFragment = new EditObjectiveDialogFragment(scheduledObjective.getId(), scheduledObjective.getName(), scheduledObjective.getDescription());
                editObjectiveDialogFragment.setSchedulerCache(mObjectiveSchedulerCache);
                editObjectiveDialogFragment.setEditInScheduler(true);
                editObjectiveDialogFragment.setEditInList(true);

                FragmentActivity activity = (FragmentActivity)(view.getContext());
                editObjectiveDialogFragment.show(activity.getSupportFragmentManager(), activity.getString(R.string.editObjectiveDialogName));
            }

            return true;
        });

        deleteItem.setOnMenuItemClickListener(menuItem ->
        {
            assert mObjectiveSchedulerCache != null;
            assert mSchedulerElement        != null;

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(view.getContext());
            alertDialogBuilder.setMessage(view.getContext().getString(R.string.deleteObjectiveAreYouSure) + " " + mSchedulerElement.getName() + "?");
            alertDialogBuilder.setPositiveButton("Yes", (dialogInterface, i) ->
            {
                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.setSchedulerCache(mObjectiveSchedulerCache);

                String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();

                if(mSchedulerElement instanceof ObjectivePool)
                {
                    ObjectivePool objectivePool = (ObjectivePool)mSchedulerElement;
                    transactionDispatcher.deletePoolTransaction(configFolder, objectivePool);
                }
                else if(mSchedulerElement instanceof ObjectiveChain)
                {
                    ObjectiveChain objectiveChain = (ObjectiveChain)mSchedulerElement;
                    transactionDispatcher.deleteChainTransaction(configFolder, objectiveChain);
                }
                else if(mSchedulerElement instanceof ScheduledObjective)
                {
                    ScheduledObjective scheduledObjective = (ScheduledObjective)mSchedulerElement;
                    transactionDispatcher.deleteObjectiveFromSchedulerTransaction(configFolder, scheduledObjective);
                }
            });

            alertDialogBuilder.setNegativeButton("No", (dialogInterface, i) ->
            {

            });

            alertDialogBuilder.show();

            return true;
        });

        deleteItem.setEnabled(!deleteDisabled);

        contextMenu.setGroupDividerEnabled(true);
    }
}