package com.sixshaman.decisore.scheduler;

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
import android.widget.Toast;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.list.ObjectiveListCache;
import com.sixshaman.decisore.scheduler.chain.ChainFragment;
import com.sixshaman.decisore.scheduler.chain.EditChainDialogFragment;
import com.sixshaman.decisore.scheduler.pool.EditPoolDialogFragment;
import com.sixshaman.decisore.scheduler.chain.ObjectiveChain;
import com.sixshaman.decisore.scheduler.pool.ObjectivePool;
import com.sixshaman.decisore.scheduler.pool.PoolFragment;
import com.sixshaman.decisore.scheduler.objective.ScheduledObjective;
import com.sixshaman.decisore.utils.EditObjectiveDialogFragment;
import com.sixshaman.decisore.utils.LockedReadFile;
import com.sixshaman.decisore.utils.TransactionDispatcher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

public class SchedulerElementViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener
{
    final int MENU_EDIT_ELEMENT         = 0;
    final int MENU_DELETE_ELEMENT       = 1;
    final int MENU_RESCHEDULE_OBJECTIVE = 2;
    final int MENU_PAUSE_ELEMENT        = 3;

    private ObjectiveSchedulerCache mObjectiveSchedulerCache;

    private SchedulerElement mSchedulerElement;

    final ImageView mIconView;
    final TextView mTextView;

    final ConstraintLayout mParentLayout;

    public SchedulerElementViewHolder(View itemView)
    {
        super(itemView);

        mTextView = itemView.findViewById(R.id.textScheduledSourceName);
        mIconView = itemView.findViewById(R.id.iconScheduledSource);

        mParentLayout = itemView.findViewById(R.id.layoutScheduledSourceView);

        mParentLayout.setOnCreateContextMenuListener(this);

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
                    fragmentTransaction.replace(R.id.scheduler_fragment_container_view, PoolFragment.class, bundle);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                }
            }
            else if(mSchedulerElement instanceof ObjectiveChain)
            {
                Context parentContext = view.getContext();
                if(parentContext instanceof FragmentActivity)
                {
                    FragmentManager fragmentManager = (((FragmentActivity) parentContext).getSupportFragmentManager());

                    Bundle bundle = new Bundle();
                    bundle.putLong("EyyDee", ((ObjectiveChain)mSchedulerElement).getId());

                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.setReorderingAllowed(true);
                    fragmentTransaction.replace(R.id.scheduler_fragment_container_view, ChainFragment.class, bundle);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                }
            }
            else if(mSchedulerElement instanceof ScheduledObjective)
            {
                final String onClickMessage = "Scheduled for " + ((ScheduledObjective) mSchedulerElement).getScheduledEnlistDate().toString();
                mParentLayout.setOnClickListener(parentView -> Toast.makeText(parentView.getContext(), onClickMessage, Toast.LENGTH_SHORT).show());
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

        String pauseString;
        if(mSchedulerElement.isPaused())
        {
            pauseString = view.getContext().getString(R.string.menu_unpause_element);
        }
        else
        {
            pauseString = view.getContext().getString(R.string.menu_pause_element);
        }

        int menuIndex = 0;

        boolean deleteDisabled = false;
        if(mSchedulerElement instanceof ScheduledObjective)
        {
            ScheduledObjective scheduledObjective = (ScheduledObjective)mSchedulerElement;

            TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
            transactionDispatcher.setSchedulerCache(mObjectiveSchedulerCache);

            String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();

            MenuItem rescheduleItem = contextMenu.add(menuIndex++, MENU_RESCHEDULE_OBJECTIVE, Menu.NONE, R.string.menu_schedule_for_arbitrary);
            rescheduleItem.setOnMenuItemClickListener(menuItem ->
            {
                DatePickerDialog datePickerDialog = new DatePickerDialog(view.getContext());
                datePickerDialog.setOnDateSetListener((datePicker, year, month, day) ->
                {
                    //Day starts at 6 AM
                    //Also Java numerates months from 0, not from 1
                    LocalDateTime dateTime = LocalDateTime.of(year, month + 1, day, 6, 0, 0);

                    transactionDispatcher.rescheduleScheduledObjectiveTransaction(configFolder, scheduledObjective.getId(), dateTime);
                    transactionDispatcher.updateObjectiveListTransaction(configFolder, LocalDateTime.now());
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
        else if(mSchedulerElement instanceof ObjectiveChain)
        {
            ObjectiveChain objectiveChain = (ObjectiveChain)mSchedulerElement;

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
        else if(mSchedulerElement instanceof ObjectivePool)
        {
            ObjectivePool objectivePool = (ObjectivePool)mSchedulerElement;

            MenuItem pauseItem = contextMenu.add(menuIndex++, MENU_PAUSE_ELEMENT, Menu.NONE, pauseString);
            pauseItem.setOnMenuItemClickListener(menuItem ->
            {
                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.setSchedulerCache(mObjectiveSchedulerCache);

                String configFolder = Objects.requireNonNull(view.getContext().getExternalFilesDir("/app")).getAbsolutePath();

                transactionDispatcher.flipPausePool(configFolder, objectivePool.getId());
                return true;
            });

            editString   = view.getContext().getString(R.string.menu_edit_pool);
            deleteString = view.getContext().getString(R.string.menu_delete_pool);

            deleteDisabled = !objectivePool.isEmpty();
        }

        MenuItem editItem = contextMenu.add(menuIndex++, MENU_EDIT_ELEMENT, Menu.NONE, editString);
        editItem.setOnMenuItemClickListener(menuItem ->
        {
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

        MenuItem deleteItem = contextMenu.add(menuIndex, MENU_DELETE_ELEMENT, Menu.NONE, deleteString);
        deleteItem.setOnMenuItemClickListener(menuItem ->
        {
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