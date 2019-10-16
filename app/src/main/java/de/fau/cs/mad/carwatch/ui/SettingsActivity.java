package de.fau.cs.mad.carwatch.ui;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.subject.SubjectIdCheck;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        getSupportFragmentManager().beginTransaction().replace(R.id.settings_container, new SettingsFragment()).commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            EditTextPreference subjectIdPref = getPreferenceScreen().findPreference(Constants.PREF_SUBJECT_ID);
            if (subjectIdPref == null) {
                return;
            }

            subjectIdPref.setOnPreferenceChangeListener((preference, newValue) -> {
                if (!(newValue instanceof String)) {
                    return false;
                }

                boolean isValid = SubjectIdCheck.isValidSubjectId((String) newValue);

                if (!isValid) {
                    if (getContext() == null) {
                        return false;
                    }
                    new AlertDialog.Builder(getContext())
                            .setCancelable(false)
                            .setTitle(getString(R.string.title_invalid_id))
                            .setMessage(getString(R.string.message_invalid_id))
                            .setPositiveButton(R.string.ok, (dialog, which) -> {
                            })
                            .show();
                }
                return isValid;
            });
        }


        @Override
        public void onStart() {
            super.onStart();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onStop() {
            super.onStop();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        }
    }
}