package com.sixshaman.decisore.scheduler.pool;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.list.ObjectiveListCache;
import com.sixshaman.decisore.scheduler.chain.ChainFragment;
import com.sixshaman.decisore.scheduler.chain.EditChainDialogFragment;
import com.sixshaman.decisore.scheduler.chain.ObjectiveChain;
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import com.sixshaman.decisore.utils.EditObjectiveDialogFragment;
import com.sixshaman.decisore.utils.LockedReadFile;
import com.sixshaman.decisore.utils.ParseUtils;
import com.sixshaman.decisore.utils.TransactionDispatcher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

public class PoolElementViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener
{
    final int MENU_EDIT_ELEMENT         = 0;
    final int MENU_DELETE_ELEMENT       = 1;
    final int MENU_RESCHEDULE_OBJECTIVE = 2;
    final int MENU_PAUSE_ELEMENT        = 3;

    private ObjectiveSchedulerCache mObjectiveSchedulerCache;

    private PoolElement mPoolElement;

    final ImageView mIconView;
    final TextView  mTextView;

    final ConstraintLayout mParentLayout;

    public PoolElementViewHolder(View itemView)
    {
        super(itemView);

        mTextView = itemView.findViewById(R.id.textPoolElementName);
        mIconView = itemView.findViewById(R.id.iconPoolElement);

        mParentLayout = itemView.findViewById(R.id.layoutPoolElementView);

        mParentLayout.setOnCreateContextMenuListener(this);

        mParentLayout.setOnClickListener(view ->
        {
            if(mPoolElement instanceof ObjectiveChain)
            {
                Context parentContext = view.getContext();
                if(parentContext instanceof FragmentActivity)
                {
                    FragmentManager fragmentManager = (((FragmentActivity) parentContext).getSupportFragmentManager());

                    Bundle bundle = new Bundle();
                    bundle.putLong("EyyDee", ((ObjectiveChain)mPoolElement).getId());

                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.setReorderingAllowed(true);
                    fragmentTransaction.replace(R.id.scheduler_fragment_container_view, ChainFragment.class, bundle);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                }
            }
            else if(mPoolElement instanceof ScheduledObjective)
            {
                Toast.makeText(view.getContext(), "Scheduled for " + ((ScheduledObjective) mPoolElement).getScheduledEnlistDate().toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    void setSourceMetadata(PoolElement poolElement, ObjectiveSchedulerCache schedulerCache)
    {
        mPoolElement             = poolElement;
        mObjectiveSchedulerCache = schedulerCache;
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo)
    {
        contextMenu.setHeaderTitle(mTextView.getText());

        String editString   = "";
        String deleteString = "";

        String pauseString;
        if(mPoolElement.isPaused())
        {
            pauseString = view.getContext().getString(R.string.menu_unpause_element);
        }
        else
        {
            pauseString = view.getContext().getString(R.string.menu_pause_element);
        }

        int menuIndex = 0;

        boolean deleteDisabled = false;
        if(mPoolElement instanceof ScheduledObjective)
        {
            ScheduledObjective scheduledObjective = (ScheduledObjective)mPoolElement;

            TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
            transactionDispatcher.setSchedulerCache(mObjectiveSchedulerCache);

            String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();

            MenuItem rescheduleItem = contextMenu.add(menuIndex++, MENU_RESCHEDULE_OBJECTIVE, Menu.NONE, R.string.menu_schedule_for_arbitrary);
            rescheduleItem.setOnMenuItemClickListener(menuItem ->
            {
                DatePickerDialog datePickerDialog = new DatePickerDialog(view.getContext());
                datePickerDialog.setOnDateSetListener((datePicker, year, month, day) ->
                {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(view.getContext()).getApplicationContext());
                    String dayStartTimeString = sharedPreferences.getString("day_start_time", "6");
                    int dayStartTime = ParseUtils.parseInt(dayStartTimeString, 6);

                    //Java numerates months from 0, not from 1
                    LocalDateTime dateTime = LocalDateTime.of(year, month + 1, day, dayStartTime, 0, 0);

                    transactionDispatcher.rescheduleScheduledObjectiveTransaction(configFolder, scheduledObjective.getId(), dateTime);
                    transactionDispatcher.updateObjectiveListTransaction(configFolder, LocalDateTime.now(), dayStartTime);
                });

                datePickerDialog.show();
                return true;
            });

            if(scheduledObjective.isRepeatable())
            {
                MenuItem pauseItem = contextMenu.add(menuIndex++, MENU_PAUSE_ELEMENT, Menu.NONE, pauseString);
                pauseItem.setOnMenuItemClickListener(menuItem ->
                {
                    transactionDispatcher.flipPauseObjective(configFolder, scheduledObjective.getId());
                    return true;
                });
            }

            editString   = view.getContext().getString(R.string.menu_edit_objective);
            deleteString = view.getContext().getString(R.string.menu_delete_objective);

            deleteDisabled = false;
        }
        else if(mPoolElement instanceof ObjectiveChain)
        {
            ObjectiveChain objectiveChain = (ObjectiveChain)mPoolElement;

            MenuItem pauseItem = contextMenu.add(menuIndex++, MENU_PAUSE_ELEMENT, Menu.NONE, pauseString);
            pauseItem.setOnMenuItemClickListener(menuItem ->
            {
                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.setSchedulerCache(mObjectiveSchedulerCache);

                String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();

                transactionDispatcher.flipPauseChain(configFolder, objectiveChain.getId());
                return true;
            });

            editString   = view.getContext().getString(R.string.menu_edit_chain);
            deleteString = view.getContext().getString(R.string.menu_delete_chain);

            deleteDisabled = objectiveChain.isNotEmpty();
        }

        MenuItem editItem = contextMenu.add(menuIndex++, MENU_EDIT_ELEMENT, Menu.NONE, editString);
        editItem.setOnMenuItemClickListener(menuItem ->
        {
            if(mPoolElement instanceof ObjectiveChain)
            {
                ObjectiveChain objectiveChain = (ObjectiveChain)mPoolElement;

                EditChainDialogFragment editChainDialogFragment = new EditChainDialogFragment(objectiveChain.getId(), objectiveChain.getName(), objectiveChain.getDescription());
                editChainDialogFragment.setSchedulerCache(mObjectiveSchedulerCache);

                FragmentActivity activity = (FragmentActivity)(view.getContext());
                editChainDialogFragment.show(activity.getSupportFragmentManager(), activity.getString(R.string.editChainDialogName));
            }
            else if(mPoolElement instanceof ScheduledObjective)
            {
                ScheduledObjective scheduledObjective = (ScheduledObjective)mPoolElement;

                EditObjectiveDialogFragment editObjectiveDialogFragment = new EditObjectiveDialogFragment(scheduledObjective.getId(), scheduledObjective.getName(), scheduledObjective.getDescription());
                editObjectiveDialogFragment.setSchedulerCache(mObjectiveSchedulerCache);
                editObjectiveDialogFragment.setEditInScheduler(true);
                editObjectiveDialogFragment.setEditInList(true);

                FragmentActivity activity = (FragmentActivity)(view.getContext());
                editObjectiveDialogFragment.show(activity.getSupportFragmentManager(), activity.getString(R.string.editObjectiveDialogName));
            }

            return true;
        });

        MenuItem deleteItem = contextMenu.add(menuIndex, MENU_DELETE_ELEMENT, Menu.NONE, deleteString);
        deleteItem.setOnMenuItemClickListener(menuItem ->
        {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(view.getContext());
            alertDialogBuilder.setMessage(view.getContext().getString(R.string.deleteObjectiveAreYouSure) + " " + mPoolElement.getName() + "?");
            alertDialogBuilder.setPositiveButton("Yes", (dialogInterface, i) ->
            {
                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.setSchedulerCache(mObjectiveSchedulerCache);

                String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();

                if(mPoolElement instanceof ObjectiveChain)
                {
                    ObjectiveChain objectiveChain = (ObjectiveChain)mPoolElement;
                    transactionDispatcher.deleteChainTransaction(configFolder, objectiveChain);
                }
                else if(mPoolElement instanceof ScheduledObjective)
                {
                    ScheduledObjective scheduledObjective = (ScheduledObjective)mPoolElement;
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