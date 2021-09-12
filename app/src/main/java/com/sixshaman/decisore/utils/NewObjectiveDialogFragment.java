package com.sixshaman.decisore.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;
import com.sixshaman.decisore.R;
import com.sixshaman.decisore.list.ObjectiveListCache;
import com.sixshaman.decisore.scheduler.ObjectiveSchedulerCache;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Objects;

public class NewObjectiveDialogFragment extends DialogFragment
{
    private long    mParentIdToAddTo;
    private boolean mAddToChainBeginning;
    private boolean mTomorrowDefault;

    private ObjectiveSchedulerCache mSchedulerCache;
    private ObjectiveListCache      mListCache;

    private BeforeObjectiveCreatedListener mBeforeObjectiveCreatedListener;
    private AfterObjectiveCreatedListener  mAfterObjectiveCreatedListener;

    public NewObjectiveDialogFragment()
    {
        mParentIdToAddTo = -1;

        mBeforeObjectiveCreatedListener = ()          -> {};
        mAfterObjectiveCreatedListener  = objectiveId -> {};

        mTomorrowDefault = false;
    }

    public void setSchedulerCache(ObjectiveSchedulerCache schedulerCache)
    {
        mSchedulerCache = schedulerCache;
    }

    public void setListCache(ObjectiveListCache listCache)
    {
        mListCache = listCache;
    }

    public void setPoolIdToAddTo(long poolId)
    {
        mParentIdToAddTo = poolId;
    }

    public void setChainIdToAddTo(long chainId, boolean addToBeginning)
    {
        mParentIdToAddTo     = chainId;
        mAddToChainBeginning = addToBeginning;
    }

    public void setOnBeforeObjectiveCreatedListener(BeforeObjectiveCreatedListener listener)
    {
        mBeforeObjectiveCreatedListener = listener;
    }

    public void setOnAfterObjectiveCreatedListener(AfterObjectiveCreatedListener listener)
    {
        mAfterObjectiveCreatedListener = listener;
    }

    public void setTomorrowDefault(boolean tomorrowDefault)
    {
        mTomorrowDefault = tomorrowDefault;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Activity activity = getActivity();

        final ValueHolder<AlertDialog> resultDialog = new ValueHolder<>(null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = Objects.requireNonNull(activity).getLayoutInflater();

        builder.setView(View.inflate(activity, R.layout.layout_dialog_new_objective, null));
        builder.setTitle(R.string.newObjectiveDialogName);

        LocalDateTime objectiveCreateDate = LocalDateTime.now();

        //I blamed Java for necessity to make hacks like ValueHolder... Then I learned about RefCell in Rust. Lol. Apparently good languages have this too
        final ValueHolder<LocalDateTime> objectiveScheduleDate   = new ValueHolder<>(objectiveCreateDate);
        final ValueHolder<Duration>      objectiveRepeatDuration = new ValueHolder<>(Duration.ofDays(1));

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(getContext()).getApplicationContext());
        String dayStartTimeString  = sharedPreferences.getString("day_start_time", "6");
        String dayEndNowTimeString = sharedPreferences.getString("day_last_today_time", "16");

        int dayStartTime = ParseUtils.parseInt(dayStartTimeString, 6);
        int dayEndTime   = ParseUtils.parseInt(dayEndNowTimeString, 16);

        builder.setPositiveButton(R.string.createObjective, (dialog, id) ->
        {
            final EditText editTextName         = resultDialog.getValue().findViewById(R.id.edit_objective_name);
            final EditText editTextNDescription = resultDialog.getValue().findViewById(R.id.edit_objective_description);

            final Spinner  repeatSpinner      = resultDialog.getValue().findViewById(R.id.spinner_objective_repeats);
            final CheckBox occasionalCheckbox = resultDialog.getValue().findViewById(R.id.checkbox_occasional);

            String nameText = editTextName.getEditableText().toString();
            if(nameText.isEmpty())
            {
                Toast toast = Toast.makeText(activity, R.string.invalidObjectiveName, Toast.LENGTH_SHORT);
                toast.show();
            }
            else
            {
                String descriptionText = editTextNDescription.getEditableText().toString();
                float objectiveRepeatProbability;

                boolean objectiveRepeatable = false;

                int objectiveRepeatIndex = repeatSpinner.getSelectedItemPosition();
                switch(objectiveRepeatIndex)
                {
                    case 0: //1
                    {
                        objectiveRepeatable = false;
                        break;
                    }
                    case 1: //Infinity
                    {
                        objectiveRepeatable = true;
                        break;
                    }
                }

                if(objectiveRepeatable)
                {
                    if(occasionalCheckbox.isChecked())
                    {
                        objectiveRepeatProbability = 0.5f;
                    }
                    else
                    {
                        objectiveRepeatProbability = 1.0f;
                    }
                }
                else
                {
                    //No repetition!
                    objectiveRepeatDuration.setValue(Duration.ZERO);
                    objectiveRepeatProbability = 0.0f;
                }

                String configFolder = Objects.requireNonNull(activity.getExternalFilesDir("/app")).getAbsolutePath();

                TransactionDispatcher transactionDispatcher = new TransactionDispatcher(configFolder);
                transactionDispatcher.setSchedulerCache(mSchedulerCache);
                transactionDispatcher.setListCache(mListCache);

                mBeforeObjectiveCreatedListener.beforeObjectiveCreated();

                long newObjectiveId = transactionDispatcher.addObjectiveTransaction(mParentIdToAddTo, mAddToChainBeginning,
                                                                                    objectiveCreateDate, objectiveScheduleDate.getValue(),
                                                                                    objectiveRepeatDuration.getValue(), objectiveRepeatProbability,
                                                                                    nameText, descriptionText, new ArrayList<>(), dayStartTime);

                mAfterObjectiveCreatedListener.afterObjectiveCreated(newObjectiveId);
            }
        });

        builder.setNegativeButton(R.string.createCancel, (dialog, id) ->
        {
            //Nothing
        });

        resultDialog.setValue(builder.create());
        resultDialog.getValue().setOnShowListener(dialogInterface ->
        {
            final Spinner scheduleSpinner = resultDialog.getValue().findViewById(R.id.spinner_objective_schedule);
            final Spinner intervalSpinner = resultDialog.getValue().findViewById(R.id.spinner_objective_interval);

            SpinnerCustomTextAdapter dateCustomTextAdapter = new SpinnerCustomTextAdapter(scheduleSpinner.getContext(), R.array.objective_schedule_types);
            scheduleSpinner.setAdapter(dateCustomTextAdapter);

            SpinnerCustomTextAdapter intervalCustomTextAdapter = new SpinnerCustomTextAdapter(intervalSpinner.getContext(), R.array.objective_interval_types);
            intervalSpinner.setAdapter(intervalCustomTextAdapter);

            //Default selection
            if(objectiveCreateDate.getHour() >= dayEndTime || mTomorrowDefault)
            {
                scheduleSpinner.setSelection(1);
            }
            else
            {
                scheduleSpinner.setSelection(0);
            }

            DateSelectListener dateSelectListener = new DateSelectListener(objectiveScheduleDate, objectiveCreateDate);
            dateSelectListener.setDayStartTime(dayStartTime);
            dateSelectListener.setDayEndTime(dayEndTime);

            scheduleSpinner.setOnItemSelectedListener(dateSelectListener);
            intervalSpinner.setOnItemSelectedListener(new IntervalSelectListener(objectiveRepeatDuration));
        });

        return resultDialog.getValue();
    }

    private static class DateSelectListener implements AdapterView.OnItemSelectedListener
    {
        private final ValueHolder<LocalDateTime> mScheduleDate;
        private final LocalDateTime              mCreateDate;

        private int mDayStartTime;
        private int mDayEndTime;

        DateSelectListener(final ValueHolder<LocalDateTime> scheduleDate, LocalDateTime createDate)
        {
            mScheduleDate = scheduleDate;
            mCreateDate   = createDate;

            mDayStartTime = 0;
            mDayEndTime   = 24;
        }

        void setDayStartTime(int dayStartTime)
        {
            mDayStartTime = dayStartTime;
        }

        void setDayEndTime(int dayEndTime)
        {
            mDayEndTime = dayEndTime;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            switch(position)
            {
                case 0: //Now
                {
                    if(mCreateDate.getHour() >= mDayEndTime)
                    {
                        parent.setSelection(1);
                        Toast.makeText(view.getContext(), R.string.do_it_tomorrow_principle, Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        mScheduleDate.setValue(mCreateDate);
                    }

                    break;
                }
                case 1: //Tomorrow
                {
                    //Day starts at 6 AM
                    mScheduleDate.setValue(mCreateDate.minusHours(mDayStartTime).plusDays(1).truncatedTo(ChronoUnit.DAYS).plusHours(mDayStartTime));
                    break;
                }
                case 2: //In a week
                {
                    //Day starts at 6 AM
                    mScheduleDate.setValue(mCreateDate.minusHours(mDayStartTime).plusDays(7).truncatedTo(ChronoUnit.DAYS).plusHours(mDayStartTime));
                    break;
                }
                case 3: //Custom
                {
                    DatePickerDialog datePickerDialog = new DatePickerDialog(view.getContext());
                    datePickerDialog.setOnDateSetListener((datePicker, year, month, day) ->
                    {
                        //Day starts at 6 AM
                        //Also Java numerates months from 0, not from 1
                        LocalDateTime dateTime = LocalDateTime.of(year, month + 1, day, mDayStartTime, 0, 0);
                        mScheduleDate.setValue(dateTime);

                        ((SpinnerCustomTextAdapter)parent.getAdapter()).setCustomText(dateTime.toLocalDate().toString());
                    });

                    datePickerDialog.show();
                    break;
                }
                default:
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent)
        {
        }
    }

    private class IntervalSelectListener implements AdapterView.OnItemSelectedListener
    {
        private final ValueHolder<Duration> mRepeatDuration;

        IntervalSelectListener(final ValueHolder<Duration> repeatDuration)
        {
            mRepeatDuration = repeatDuration;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            switch(position)
            {
                case 0: //Day
                {
                    mRepeatDuration.setValue(Duration.ofDays(1));
                    break;
                }

                case 1: //Week
                {
                    mRepeatDuration.setValue(Duration.ofDays(7));
                    break;
                }

                case 2: //Month
                {
                    mRepeatDuration.setValue(Duration.ofDays(30));
                    break;
                }

                case 3: //Custom
                {
                    CustomPeriodDialogFragment customPeriodDialog = new CustomPeriodDialogFragment((int)mRepeatDuration.getValue().toDays());
                    customPeriodDialog.setPeriodChosenListener(newPeriodDays ->
                    {
                        mRepeatDuration.setValue(Duration.ofDays(newPeriodDays));

                        SpinnerCustomTextAdapter customTextAdapter = ((SpinnerCustomTextAdapter)parent.getAdapter());

                        int periodInWeeks  = newPeriodDays / 7;
                        int periodInMonths = newPeriodDays / 30;
                        if(newPeriodDays == periodInMonths * 30) //Exact amount of months
                        {
                            customTextAdapter.setCustomText(periodInMonths + " " + getResources().getQuantityString(R.plurals.plural_months, periodInMonths));
                        }
                        else if(newPeriodDays == periodInWeeks * 7) //Exact amount of weeks
                        {
                            customTextAdapter.setCustomText(periodInWeeks + " " + getResources().getQuantityString(R.plurals.plural_weeks, periodInWeeks));
                        }
                        else
                        {
                            customTextAdapter.setCustomText(newPeriodDays + " " + getResources().getQuantityString(R.plurals.plural_days, newPeriodDays));
                        }
                    });

                    customPeriodDialog.show(getParentFragmentManager(), getString(R.string.choose_period));
                    break;
                }

                default:
                {
                    break;
                }
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent)
        {
        }
    }

    private static class SpinnerCustomTextAdapter extends ArrayAdapter<String>
    {
        //https://stackoverflow.com/a/25234785/2857541

        private String mCustomText = "";

        SpinnerCustomTextAdapter(Context context, int arrayId)
        {
            super(context, android.R.layout.simple_spinner_item, context.getResources().getStringArray(arrayId));
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        void setCustomText(String text)
        {
            mCustomText = text;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
        {
            View view = super.getView(position, convertView, parent);
            if(position == 3) //Custom
            {
                TextView textView = view.findViewById(android.R.id.text1); //Looks kinda hacky, but apparently there's no other way
                textView.setText(mCustomText);
            }

            return view;
        }
    }
}
