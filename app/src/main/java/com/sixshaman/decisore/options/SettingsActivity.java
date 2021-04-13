package com.sixshaman.decisore.options;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Toast;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import com.sixshaman.decisore.R;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceFragmentCompat;
import com.sixshaman.decisore.utils.ParseUtils;
import com.sixshaman.decisore.utils.TransactionDispatcher;
import com.sixshaman.decisore.utils.ValueHolder;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        FragmentManager fragmentManager = getSupportFragmentManager();

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.settings, new SettingsFragment());
        fragmentTransaction.commit();

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
        {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            PreferenceManager preferenceManager = getPreferenceManager();

            EditTextPreference dayStartTimePreference = Objects.requireNonNull(preferenceManager.findPreference("day_start_time"));
            dayStartTimePreference.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

            EditTextPreference dayEndTimePreference = Objects.requireNonNull(preferenceManager.findPreference("day_last_today_time"));
            dayEndTimePreference.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

            final ValueHolder<Integer> oldStartHour = new ValueHolder<>(ParseUtils.parseInt(dayStartTimePreference.getText(), 6));
            dayStartTimePreference.setOnPreferenceChangeListener((preference, newValue) ->
            {
                int newHour;

                String newHourStr = newValue.toString();
                try
                {
                    newHour = Integer.parseInt(newHourStr);
                    if(newHour < 0 || newHour > 23)
                    {
                        return false;
                    }
                }
                catch (NumberFormatException e)
                {
                    return false;
                }

                String configFolder = Objects.requireNonNull(requireContext().getExternalFilesDir("/app")).getAbsolutePath();

                TransactionDispatcher transactionDispatcher = new TransactionDispatcher();
                transactionDispatcher.updateNewDayStart(configFolder, oldStartHour.getValue(), newHour);

                oldStartHour.setValue(newHour);
                return true;
            });

            dayEndTimePreference.setOnPreferenceChangeListener((preference, newValue) ->
            {
                int newHour;

                String newHourStr = newValue.toString();
                try
                {
                    newHour = Integer.parseInt(newHourStr);
                    if(newHour < 0 || newHour > 24) //24, not 23 for a reason
                    {
                        return false;
                    }
                }
                catch (NumberFormatException e)
                {
                    return false;
                }

                return true;
            });
        }
    }
}