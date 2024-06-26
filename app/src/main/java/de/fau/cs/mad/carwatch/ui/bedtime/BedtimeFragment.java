package de.fau.cs.mad.carwatch.ui.bedtime;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.json.JSONException;
import org.json.JSONObject;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.TimerHandler;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.BarcodeActivity;
import de.fau.cs.mad.carwatch.ui.MainActivity;
import de.fau.cs.mad.carwatch.userpresent.UserPresentService;

public class BedtimeFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = BedtimeFragment.class.getSimpleName();

    private BedtimeViewModel bedtimeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_bedtime, container, false);

        bedtimeViewModel = new ViewModelProvider(this).get(BedtimeViewModel.class);

        Button yesButton = root.findViewById(R.id.button_yes);
        Button noButton = root.findViewById(R.id.button_no);
        yesButton.setOnClickListener(this);
        noButton.setOnClickListener(this);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean isDarkModeActive = sp.getBoolean(Constants.PREF_NIGHT_MODE_ENABLED, false);

        Button lightsOutButton = root.findViewById(R.id.button_toggle_night_mode);
        lightsOutButton.setOnClickListener(this);
        lightsOutButton.setText(isDarkModeActive ? R.string.lights_on : R.string.lights_out);

        return root;
    }

    @Override
    public void onClick(View v) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean hasEveningSalivette = sp.getBoolean(Constants.PREF_HAS_EVENING, false);

        switch (v.getId()) {
            case R.id.button_no:
                if (getActivity() != null) {
                    Snackbar.make(getActivity().findViewById(R.id.coordinator), getString(R.string.feedback_thanks), Snackbar.LENGTH_SHORT).show();
                    ((MainActivity) getActivity()).navigate(R.id.navigation_alarm);
                }
                break;
            case R.id.button_yes:
                // create Json object and log information
                try {
                    JSONObject json = new JSONObject();
                    json.put(Constants.LOGGER_EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_EVENING);
                    LoggerUtil.log(Constants.LOGGER_ACTION_EVENING_SALIVETTE, json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                DateTime date = new DateTime(sp.getLong(Constants.PREF_EVENING_TAKEN, 0));
                if (date.equals(LocalTime.MIDNIGHT.toDateTimeToday())) {
                    showBedtimeWarningDialog();
                } else {
                    bedtimeViewModel.setSalivaTaken(true);
                    if (!UserPresentService.serviceRunning) {
                        UserPresentService.startService(getContext());
                    }
                    showBedtimeDialog(hasEveningSalivette);
                }

                break;
            case R.id.button_toggle_night_mode:
                if (getActivity() == null) {
                    break;
                }

                boolean enableDarkMode = !sp.getBoolean(Constants.PREF_NIGHT_MODE_ENABLED, false);
                sp.edit().putBoolean(Constants.PREF_NIGHT_MODE_ENABLED, enableDarkMode).apply();
                AppCompatDelegate delegate = ((AppCompatActivity) getActivity()).getDelegate();
                delegate.setLocalNightMode(enableDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                delegate.applyDayNight();
                LoggerUtil.log(enableDarkMode ? Constants.LOGGER_ACTION_LIGHTS_OUT : Constants.LOGGER_ACTION_LIGHTS_ON, new JSONObject());
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (getActivity() == null) {
            return;
        }

        if (requestCode == Constants.REQUEST_CODE_SCAN) {
            if (resultCode == Activity.RESULT_OK) {
                bedtimeViewModel.setSalivaTaken(true);
                if (!UserPresentService.serviceRunning) {
                    UserPresentService.startService(getContext());
                }
            }
        }
    }


    private void showBedtimeDialog(boolean hasEveningSalivette) {
        if (getContext() == null) {
            return;
        }

        Drawable icon = getResources().getDrawable(R.drawable.ic_bedtime_24dp);
        icon.setTint(getResources().getColor(R.color.colorPrimary));

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        int eveningSalivaId = sp.getInt(Constants.PREF_EVENING_SALIVA_ID, 1);

        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.bedtime_title))
                .setCancelable(false)
                .setIcon(icon)
                .setMessage(getString(R.string.bedtime_text))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    if (hasEveningSalivette) {
                        TimerHandler.scheduleSalivaCountdown(getContext(), Constants.EXTRA_ALARM_ID_EVENING, eveningSalivaId);

                        Intent intent = new Intent(getContext(), BarcodeActivity.class);
                        intent.putExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_EVENING);
                        intent.putExtra(Constants.EXTRA_SALIVA_ID, eveningSalivaId);
                        startActivityForResult(intent, Constants.REQUEST_CODE_SCAN);
                    } else {
                        bedtimeViewModel.setSalivaTaken(true);
                        if (!UserPresentService.serviceRunning) {
                            UserPresentService.startService(getContext());
                        }
                    }
                })
                .show();
    }

    private void showBedtimeWarningDialog() {
        if (getContext() == null) {
            return;
        }
        Drawable icon = getResources().getDrawable(R.drawable.ic_warning_24dp);
        icon.setTint(getResources().getColor(R.color.colorPrimary));

        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.warning_title))
                .setCancelable(false)
                .setIcon(icon)
                .setMessage(getString(R.string.warning_already_taken_bedtime))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                })
                .show();
    }
}