package de.fau.cs.mad.carwatch.ui.bedtime;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import de.fau.cs.mad.carwatch.ui.CarwatchSnackbar;
import de.fau.cs.mad.carwatch.ui.CarwatchDialogBuilder;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.json.JSONException;
import org.json.JSONObject;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.TimerHandler;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.BarcodeActivity;
import de.fau.cs.mad.carwatch.userpresent.UserPresentService;

public class BedtimeFragment extends Fragment implements View.OnClickListener {

    private BedtimeViewModel bedtimeViewModel;
    private final ActivityResultLauncher<Intent> barcodeScannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> handleBarcodeScannerResult(result.getResultCode())
    );

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
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean hasEveningSalivette = sp.getBoolean(Constants.PREF_HAS_EVENING, false);

        int viewId = v.getId();
        if (viewId == R.id.button_no) {
            if (getActivity() != null) {
                CarwatchSnackbar.show(getActivity().findViewById(R.id.coordinator), R.string.feedback_bedtime_no, CarwatchSnackbar.LENGTH_SHORT);
            }
        } else if (viewId == R.id.button_yes) {
            // create Json object and log information
            try {
                JSONObject json = new JSONObject();
                json.put(Constants.LOGGER_EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_EVENING);
                LoggerUtil.log(Constants.LOGGER_ACTION_EVENING_SALIVETTE, json);
            } catch (JSONException e) {
                Log.e(BedtimeFragment.class.getSimpleName(), "Could not log evening salivette event", e);
            }


            DateTime date = new DateTime(sp.getLong(Constants.PREF_EVENING_TAKEN, 0));
            if (!hasEveningSalivette) {
                completeBedtimeWithoutEveningSample();
            } else if (date.equals(LocalTime.MIDNIGHT.toDateTimeToday())) {
                showBedtimeWarningDialog();
            } else {
                showBedtimeDialog();
            }

        } else if (viewId == R.id.button_toggle_night_mode) {
            if (getActivity() == null) {
                return;
            }

            boolean enableDarkMode = !sp.getBoolean(Constants.PREF_NIGHT_MODE_ENABLED, false);
            sp.edit().putBoolean(Constants.PREF_NIGHT_MODE_ENABLED, enableDarkMode).apply();
            AppCompatDelegate delegate = ((AppCompatActivity) getActivity()).getDelegate();
            delegate.setLocalNightMode(enableDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            delegate.applyDayNight();
            LoggerUtil.log(enableDarkMode ? Constants.LOGGER_ACTION_LIGHTS_OUT : Constants.LOGGER_ACTION_LIGHTS_ON, new JSONObject());
        }
    }

    private void handleBarcodeScannerResult(int resultCode) {
        if (getActivity() == null) {
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            bedtimeViewModel.setSalivaTaken(true);
            if (!UserPresentService.serviceRunning) {
                UserPresentService.startService(getContext());
            }
        }
    }


    private void completeBedtimeWithoutEveningSample() {
        bedtimeViewModel.setSalivaTaken(true);
        if (!UserPresentService.serviceRunning) {
            UserPresentService.startService(getContext());
        }
        showAttentionTrackingStartedWithoutEveningSampleDialog();
    }

    private void showAttentionTrackingStartedWithoutEveningSampleDialog() {
        if (getContext() == null) {
            return;
        }

        Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_info_24dp);
        if (icon != null) {
            icon.setTint(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        }

        new CarwatchDialogBuilder(getContext())
                .setIcon(icon)
                .setTitle(R.string.title_attention_tracking_started)
                .setMessage(R.string.message_attention_tracking_started_no_evening_sample)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void showBedtimeDialog() {
        if (getContext() == null) {
            return;
        }

        Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_bedtime_24dp);
        if (icon != null) {
            icon.setTint(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        int eveningSalivaId = sp.getInt(Constants.PREF_EVENING_SALIVA_ID, 1);

        new CarwatchDialogBuilder(getContext())
                .setTitle(getString(R.string.bedtime_title))
                .setCancelable(false)
                .setIcon(icon)
                .setMessage(getString(R.string.bedtime_text))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    TimerHandler.scheduleSalivaCountdown(getContext(), Constants.EXTRA_ALARM_ID_EVENING, eveningSalivaId);

                    Intent intent = new Intent(getContext(), BarcodeActivity.class);
                    intent.putExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_EVENING);
                    intent.putExtra(Constants.EXTRA_SALIVA_ID, eveningSalivaId);
                    barcodeScannerLauncher.launch(intent);
                })
                .show();
    }

    private void showBedtimeWarningDialog() {
        if (getContext() == null) {
            return;
        }
        Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_warning_24dp);
        if (icon != null) {
            icon.setTint(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        }

        new CarwatchDialogBuilder(getContext())
                .setTitle(getString(R.string.warning_title))
                .setCancelable(false)
                .setIcon(icon)
                .setMessage(getString(R.string.warning_already_taken_bedtime))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                })
                .show();
    }
}
