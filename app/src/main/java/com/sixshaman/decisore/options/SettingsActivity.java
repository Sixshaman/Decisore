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

            dayStartTimePreference.setOnPreferenceChangeListener((preference, newValue) ->
            {
                String newHourStr = newValue.toString();
                try
                {
                    int newHour = Integer.parseInt(newHourStr);
                    if(newHour < 0 || newHour > 23)
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