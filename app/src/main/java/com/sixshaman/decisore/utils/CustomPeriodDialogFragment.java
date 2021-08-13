package com.sixshaman.decisore.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.sixshaman.decisore.R;

public class CustomPeriodDialogFragment extends DialogFragment
{
    private PeriodChosenListener mPeriodChosenListener;

    private final int mInitialDays;

    public CustomPeriodDialogFragment(int initialDays)
    {
        mInitialDays = initialDays;
    }

    void setPeriodChosenListener(PeriodChosenListener periodChosenListener)
    {
        mPeriodChosenListener = periodChosenListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Activity activity = getActivity();

        final ValueHolder<AlertDialog> resultDialog = new ValueHolder<>(null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(View.inflate(activity, R.layout.layout_dialog_custom_period, null));
        builder.setTitle(R.string.choose_period);

        builder.setPositiveButton(R.string.createObjective, (dialog, id) ->
        {
            final EditText editTextCount     = resultDialog.getValue().findViewById(R.id.edit_objective_period);
            final Spinner  spinnerPeriodType = resultDialog.getValue().findViewById(R.id.spinner_period_type);

            int period          = Integer.parseInt(editTextCount.getText().toString());
            int periodTypeIndex = spinnerPeriodType.getSelectedItemPosition();

            int periodMultiply;
            switch(periodTypeIndex)
            {
                case 0: //Days
                {
                    periodMultiply = 1;
                    break;
                }

                case 1: //Weeks
                {
                    periodMultiply = 7;
                    break;
                }

                case 2: //Months
                {
                    periodMultiply = 30;
                    break;
                }

                default:
                {
                    periodMultiply = 1;
                    break;
                }
            }

            int periodDays = period * periodMultiply;
            mPeriodChosenListener.OnPeriodChosen(periodDays);
        });

        builder.setNegativeButton(R.string.createCancel, (dialog, id) ->
        {
            //Nothing
        });

        resultDialog.setValue(builder.create());
        resultDialog.getValue().setOnShowListener(dialogInterface ->
        {
            final EditText editTextCount     = resultDialog.getValue().findViewById(R.id.edit_objective_period);
            final Spinner  spinnerPeriodType = resultDialog.getValue().findViewById(R.id.spinner_period_type);

            int periodInWeeks  = mInitialDays / 7;
            int periodInMonths = mInitialDays / 30;
            if(mInitialDays == periodInMonths * 30) //Exact amount of months
            {
                editTextCount.setText(String.format(editTextCount.getTextLocale(), "%d", periodInMonths));
                spinnerPeriodType.setSelection(2);
            }
            else if(mInitialDays == periodInWeeks * 7) //Exact amount of weeks
            {
                editTextCount.setText(String.format(editTextCount.getTextLocale(), "%d", periodInWeeks));
                spinnerPeriodType.setSelection(1);
            }
            else
            {
                editTextCount.setText(String.format(editTextCount.getTextLocale(), "%d", mInitialDays));
                spinnerPeriodType.setSelection(0);
            }

            String[] spinnerItems = getResources().getStringArray(R.array.objective_period_values);
            spinnerItems[0] = getResources().getQuantityString(R.plurals.plural_days,   mInitialDays);
            spinnerItems[1] = getResources().getQuantityString(R.plurals.plural_weeks,  mInitialDays);
            spinnerItems[2] = getResources().getQuantityString(R.plurals.plural_months, mInitialDays);

            PluralSpinnerAdapter spinnerAdapter = new PluralSpinnerAdapter(getContext());
            spinnerPeriodType.setAdapter(spinnerAdapter);

            final TextWatcherStopper textWatcherStopper = new TextWatcherStopper();
            editTextCount.addTextChangedListener(new PeriodInputListener(resultDialog.getValue(), textWatcherStopper));
            spinnerPeriodType.setOnItemSelectedListener(new PeriodTypeChangedListener(resultDialog.getValue(), textWatcherStopper));
        });

        return resultDialog.getValue();
    }

    private static class TextWatcherStopper //Recursive call fix
    {
        private boolean mStopped;

        TextWatcherStopper()
        {
            mStopped = false;
        }

        boolean isStopped()
        {
            return mStopped;
        }

        void setStopped(boolean stopped)
        {
            mStopped = stopped;
        }
    }

    private class PeriodInputListener implements TextWatcher
    {
        private final AlertDialog mResultDialog;

        private final TextWatcherStopper mStopper;

        PeriodInputListener(final AlertDialog resultDialog, TextWatcherStopper stopper)
        {
            mResultDialog = resultDialog;

            mStopper = stopper;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after)
        {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count)
        {
            if(mStopper.isStopped())
            {
                return;
            }

            final EditText editTextCount     = mResultDialog.findViewById(R.id.edit_objective_period);
            final Spinner  spinnerPeriodType = mResultDialog.findViewById(R.id.spinner_period_type);

            if(!editTextCount.hasFocus())
            {
                //Do not do anything if the text is changed programmatically
                return;
            }

            int period = 0;
            try
            {
                period = Integer.parseInt(s.toString());
            }
            catch(NumberFormatException ignored)
            {
            }

            SpinnerAdapter spinnerAdapter = spinnerPeriodType.getAdapter();
            if(spinnerAdapter instanceof PluralSpinnerAdapter)
            {
                ((PluralSpinnerAdapter)spinnerAdapter).setPeriod(period);
            }

            int periodTypeIndex = spinnerPeriodType.getSelectedItemPosition();

            int periodMultiply;
            switch(periodTypeIndex)
            {
                case 0: //Days
                {
                    periodMultiply = 1;
                    break;
                }

                case 1: //Weeks
                {
                    periodMultiply = 7;
                    break;
                }

                case 2: //Months
                {
                    periodMultiply = 30;
                    break;
                }

                default:
                {
                    periodMultiply = 1;
                    break;
                }
            }

            long periodDays = (long)periodMultiply * period;

            final Button positiveButton = mResultDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(periodDays >= 1 && periodDays <= 1000000000);
        }

        @Override
        public void afterTextChanged(Editable s)
        {

        }
    }

    private static class PeriodTypeChangedListener implements AdapterView.OnItemSelectedListener
    {
        private int mPrevPeriodMultiply;

        private final AlertDialog mResultDialog;

        private final TextWatcherStopper mCountEditTextWatcherStopper;

        PeriodTypeChangedListener(final AlertDialog resultDialog, TextWatcherStopper stopper)
        {
            mResultDialog = resultDialog;

            mCountEditTextWatcherStopper = stopper;

            final Spinner spinnerPeriodType = mResultDialog.findViewById(R.id.spinner_period_type);

            int periodTypeIndex = spinnerPeriodType.getSelectedItemPosition();
            switch(periodTypeIndex)
            {
                case 0: //Days
                {
                    mPrevPeriodMultiply = 1;
                    break;
                }

                case 1: //Weeks
                {
                    mPrevPeriodMultiply = 7;
                    break;
                }

                case 2: //Months
                {
                    mPrevPeriodMultiply = 30;
                    break;
                }

                default:
                {
                    mPrevPeriodMultiply = 1;
                    break;
                }
            }
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            final EditText editTextCount = mResultDialog.findViewById(R.id.edit_objective_period);

            int period = 0;
            try
            {
                period = Integer.parseInt(editTextCount.getText().toString());
            }
            catch(NumberFormatException ignored)
            {
            }

            int periodMultiply;
            switch(position)
            {
                case 0: //Days
                {
                    periodMultiply = 1;
                    break;
                }

                case 1: //Weeks
                {
                    periodMultiply = 7;
                    break;
                }

                case 2: //Months
                {
                    periodMultiply = 30;
                    break;
                }

                default:
                {
                    periodMultiply = 1;
                    break;
                }
            }

            int periodNew = period * mPrevPeriodMultiply;
            periodNew = Math.max(periodNew / periodMultiply, 1);

            PluralSpinnerAdapter spinnerAdapter = (PluralSpinnerAdapter)parent.getAdapter();
            spinnerAdapter.setPeriod(periodNew);

            mCountEditTextWatcherStopper.setStopped(true);
            editTextCount.setText(String.format(editTextCount.getTextLocale(), "%d", periodNew));
            mCountEditTextWatcherStopper.setStopped(false);

            mPrevPeriodMultiply = periodMultiply;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent)
        {
        }
    }

    private class PluralSpinnerAdapter extends ArrayAdapter<String>
    {
        private int mPeriod;

        PluralSpinnerAdapter(Context context)
        {
            super(context, android.R.layout.simple_spinner_item, context.getResources().getStringArray(R.array.objective_period_values));
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            mPeriod = 1;
        }

        void setPeriod(int period)
        {
            mPeriod = period;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
        {
            View view = super.getView(position, convertView, parent);
            TextView textView = view.findViewById(android.R.id.text1);

            if(position == 0) //Day(s)
            {
                textView.setText(getResources().getQuantityString(R.plurals.plural_days, mPeriod));
            }
            else if(position == 1) //Week(s)
            {
                textView.setText(getResources().getQuantityString(R.plurals.plural_weeks, mPeriod));
            }
            else if(position == 2) //Month(s)
            {
                textView.setText(getResources().getQuantityString(R.plurals.plural_months, mPeriod));
            }

            return view;
        }
    }

    public interface PeriodChosenListener
    {
        void OnPeriodChosen(int newPeriodDays);
    }
}
