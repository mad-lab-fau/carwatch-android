package de.fau.cs.mad.carwatch.ui.wakeup;

import android.app.Activity;
import android.content.Context;
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
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.alarmmanager.TimerHandler;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.AlertActivity;
import de.fau.cs.mad.carwatch.ui.BarcodeActivity;
import de.fau.cs.mad.carwatch.ui.MainActivity;
import de.fau.cs.mad.carwatch.userpresent.UserPresentService;
import de.fau.cs.mad.carwatch.util.AlarmRepository;

public class WakeupFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = WakeupFragment.class.getSimpleName();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_wakeup, container, false);

        Button yesButton = root.findViewById(R.id.button_yes);
        Button noButton = root.findViewById(R.id.button_no);

        yesButton.setOnClickListener(this);
        noButton.setOnClickListener(this);

        return root;
    }

    @Override
    public void onClick(View v) {
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
                    json.put(Constants.LOGGER_EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_INITIAL);
                    LoggerUtil.log(Constants.LOGGER_ACTION_SPONTANEOUS_AWAKENING, json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
                DateTime date = new DateTime(sp.getLong(Constants.PREF_CURRENT_DATE, 0));
                if (date.equals(LocalTime.MIDNIGHT.toDateTimeToday())) {
//                    showWakeupWarningDialog();
                    if (getActivity() != null)
                        Snackbar.make(getActivity().findViewById(R.id.coordinator), getString(R.string.warning_already_report_wakeup), Snackbar.LENGTH_SHORT).show();
                } else {
                    // no morning procedure ongoing yet
                    showWakeupDialog();
                }
                break;
        }
    }

    private void showWakeupDialog() {
        if (getContext() == null) {
            return;
        }
        Drawable icon = getResources().getDrawable(R.drawable.ic_wakeup_24dp);
        icon.setTint(getResources().getColor(R.color.colorPrimary));

        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.wakeup_title))
                .setCancelable(false)
                .setIcon(icon)
                .setMessage(getString(R.string.wakeup_text))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    if (UserPresentService.serviceRunning) {
                        UserPresentService.stopService(getContext());
                    }

                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
                    initializeDay();

                    // disable night mode
                    if (getActivity() != null) {
                        sp.edit().putBoolean(Constants.PREF_NIGHT_MODE_ENABLED, false).apply();
                        AppCompatDelegate delegate = ((AppCompatActivity) getActivity()).getDelegate();
                        delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        delegate.applyDayNight();
                    }

                    if (sp.getString(Constants.PREF_SALIVA_DISTANCES, "").startsWith("0")) {
                        TimerHandler.scheduleSpontaneousAwakeningTimer(getContext());
                        Intent intent = new Intent(getContext(), BarcodeActivity.class);
                        intent.putExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_INITIAL);
                        intent.putExtra(Constants.EXTRA_SALIVA_ID, Constants.EXTRA_SALIVA_ID_INITIAL);
                        startActivityForResult(intent, Constants.REQUEST_CODE_SCAN);
                    } else {
                        AlarmHandler.showMessageSalivaAlarmsScheduled(getContext(), getActivity().findViewById(R.id.coordinator));
                    }
                })
                .show();
    }

    private void initializeDay() {
        Context context = requireContext();
        AlarmHandler.rescheduleSalivaAlarms(context);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int dayCounter = sp.getInt(Constants.PREF_DAY_COUNTER, -1) + 1;

        sp.edit()
                .putLong(Constants.PREF_CURRENT_DATE, LocalTime.MIDNIGHT.toDateTimeToday().getMillis())
                .putInt(Constants.PREF_DAY_COUNTER, dayCounter)
                .putInt(Constants.PREF_ID_ONGOING_ALARM, Constants.EXTRA_ALARM_ID_INITIAL)
                .apply();


        if (getActivity() == null)
            return;

        AlarmRepository repository = AlarmRepository.getInstance(getActivity().getApplication());
        try {
            Alarm alarm = repository.getAlarmById(Constants.EXTRA_ALARM_ID_INITIAL);
            if (alarm == null)
                return;

            View view = getActivity() == null ? null : getActivity().findViewById(R.id.coordinator);

            AlarmHandler.cancelAlarm(context, alarm, view);
            alarm.setActive(false);
            repository.update(alarm);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}