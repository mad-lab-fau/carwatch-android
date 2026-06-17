package de.fau.cs.mad.carwatch.ui.wakeup;

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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import de.fau.cs.mad.carwatch.ui.CarwatchSnackbar;
import de.fau.cs.mad.carwatch.ui.CarwatchDialogBuilder;

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
        int viewId = v.getId();
        if (viewId == R.id.button_no) {
            if (getActivity() != null) {
                CarwatchSnackbar.show(getActivity().findViewById(R.id.coordinator), R.string.feedback_wakeup_no, CarwatchSnackbar.LENGTH_SHORT);
            }
        } else if (viewId == R.id.button_yes) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
            if (isWakeupInitializedToday(sp)) {
                showWakeupAlreadyRecordedMessage();
                return;
            }

            int dayCounter = getWakeupStudyDay(sp);
            int numDays = sp.getInt(Constants.PREF_NUM_DAYS, Integer.MAX_VALUE);
            if (dayCounter > numDays) {
                if (getActivity() != null) {
                    CarwatchSnackbar.show(getActivity().findViewById(R.id.coordinator), getString(R.string.warning_study_finished), CarwatchSnackbar.LENGTH_SHORT);
                }
                return;
            }

            WakeupAlert wakeupAlert = createWakeupAlert(sp);
            if (isOverdueSampleAlert(wakeupAlert)) {
                startWakeupSampling(wakeupAlert);
                return;
            }

            String salivaDistances = sp.getString(Constants.PREF_SALIVA_DISTANCES, "");
            boolean delayedOnlyWakeupSample = wakeupAlert != null
                    && wakeupAlert.type.equals(Constants.WAKEUP_ALERT_DELAYED_SAMPLE)
                    && !AlarmHandler.requiresImmediateWakeupSample(salivaDistances);
            if (delayedOnlyWakeupSample) {
                startWakeupSampling(wakeupAlert);
                return;
            }

            showWakeupDialog();
        }
    }

    private void showWakeupAlreadyRecordedMessage() {
        if (getActivity() != null) {
            CarwatchSnackbar.show(
                    getActivity().findViewById(R.id.coordinator),
                    getString(R.string.warning_already_report_wakeup),
                    CarwatchSnackbar.LENGTH_SHORT
            );
        }
    }

    private void showWakeupDialog() {
        if (getContext() == null) {
            return;
        }
        Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_wakeup_24dp);
        if (icon != null) {
            icon.setTint(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        }

        new CarwatchDialogBuilder(getContext())
                .setTitle(getString(R.string.wakeup_title))
                .setCancelable(false)
                .setIcon(icon)
                .setMessage(getString(R.string.wakeup_text))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
                    WakeupAlert wakeupAlert = createWakeupAlert(sp);
                    startWakeupSampling(wakeupAlert);
                })
                .show();
    }

    private void startWakeupSampling(WakeupAlert wakeupAlert) {
        if (getContext() == null || getActivity() == null) {
            return;
        }

        if (UserPresentService.serviceRunning) {
            UserPresentService.stopService(getContext());
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());

        String salivaDistances = sp.getString(Constants.PREF_SALIVA_DISTANCES, "");
        if (isOverdueSampleAlert(wakeupAlert)) {
            initializeDay(true);
            saveWakeupAlert(sp, null);
            showWakeupAlert(wakeupAlert);
            return;
        }

        if (AlarmHandler.requiresImmediateWakeupSample(salivaDistances)) {
            initializeDay(false);
            saveWakeupAlert(sp, wakeupAlert);
            TimerHandler.scheduleSpontaneousAwakeningTimer(getContext());
            Intent intent = new Intent(getContext(), BarcodeActivity.class);
            intent.putExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_INITIAL);
            intent.putExtra(Constants.EXTRA_SALIVA_ID, Constants.EXTRA_SALIVA_ID_INITIAL);
            startActivity(intent);
        } else if (wakeupAlert != null) {
            initializeDay(true);
            showWakeupAlert(wakeupAlert);
        } else {
            initializeDay(true);
            AlarmHandler.showMessageSalivaAlarmsScheduled(getContext(), getActivity().findViewById(R.id.coordinator));
        }
    }

    private boolean isOverdueSampleAlert(WakeupAlert wakeupAlert) {
        return wakeupAlert != null && wakeupAlert.type.equals(Constants.WAKEUP_ALERT_OVERDUE_SAMPLE);
    }

    private WakeupAlert createWakeupAlert(SharedPreferences sp) {
        if (hasOverdueFixedSample(sp)) {
            return new WakeupAlert(Constants.WAKEUP_ALERT_OVERDUE_SAMPLE, 0);
        }

        String timeDistancesString = sp.getString(Constants.PREF_SALIVA_DISTANCES, "");
        if (AlarmHandler.requiresImmediateWakeupSample(timeDistancesString)) {
            return null;
        }

        int delayedSampleMinutes = getNextDelayedSampleMinutes(sp);
        if (delayedSampleMinutes > 0) {
            return new WakeupAlert(Constants.WAKEUP_ALERT_DELAYED_SAMPLE, delayedSampleMinutes);
        }

        return null;
    }

    private boolean hasOverdueFixedSample(SharedPreferences sp) {
        String fixedTimesString = sp.getString(Constants.PREF_SALIVA_TIMES, "");
        DateTime now = DateTime.now();

        for (String timeRaw : fixedTimesString.split(",")) {
            if (timeRaw.isEmpty()) {
                continue;
            }

            String time = timeRaw.substring(0, 2) + ":" + timeRaw.substring(2);
            DateTime sampleTime = now.withTime(LocalTime.parse(time));
            if (sampleTime.isBefore(now)) {
                return true;
            }
        }

        return false;
    }

    private int getNextDelayedSampleMinutes(SharedPreferences sp) {
        String timeDistancesString = sp.getString(Constants.PREF_SALIVA_DISTANCES, "");

        for (String distanceString : timeDistancesString.split(",")) {
            if (distanceString.isEmpty() || distanceString.equals("0")) {
                continue;
            }

            return Integer.parseInt(distanceString);
        }

        return 0;
    }

    private void saveWakeupAlert(SharedPreferences sp, WakeupAlert wakeupAlert) {
        if (wakeupAlert == null) {
            sp.edit()
                    .remove(Constants.PREF_WAKEUP_ALERT_TYPE)
                    .remove(Constants.PREF_WAKEUP_DELAYED_SAMPLE_MINUTES)
                    .apply();
            return;
        }

        sp.edit()
                .putString(Constants.PREF_WAKEUP_ALERT_TYPE, wakeupAlert.type)
                .putInt(Constants.PREF_WAKEUP_DELAYED_SAMPLE_MINUTES, wakeupAlert.minutes)
                .apply();
    }

    private void showWakeupAlert(WakeupAlert wakeupAlert) {
        if (getContext() == null || getActivity() == null) {
            return;
        }

        int titleId = wakeupAlert.type.equals(Constants.WAKEUP_ALERT_OVERDUE_SAMPLE)
                ? R.string.title_overdue_sample_pending
                : R.string.title_delayed_sample_planned;
        String message = wakeupAlert.type.equals(Constants.WAKEUP_ALERT_OVERDUE_SAMPLE)
                ? getString(R.string.message_overdue_sample_pending)
                : getString(R.string.message_delayed_sample_planned, wakeupAlert.minutes);

        new CarwatchDialogBuilder(getContext())
                .setIcon(R.drawable.ic_info_24dp)
                .setTitle(titleId)
                .setMessage(message)
                .setPositiveButton(R.string.ok, (dialog, which) -> ((MainActivity) getActivity()).navigate(R.id.navigation_alarm))
                .show();
    }

    private void initializeDay(boolean wakeupRecorded) {
        Context context = requireContext();
        AlarmHandler.rescheduleSalivaAlarms(context);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int dayCounter = getWakeupStudyDay(sp);

        SharedPreferences.Editor editor = sp.edit()
                .putInt(Constants.PREF_DAY_COUNTER, dayCounter)
                .putInt(Constants.PREF_ID_ONGOING_ALARM, Constants.EXTRA_ALARM_ID_INITIAL)
                .putBoolean(Constants.PREF_STUDY_DAY_MANUALLY_ADVANCED, false);

        if (wakeupRecorded) {
            logWakeup();
            editor.putLong(Constants.PREF_LAST_WAKE_UP_ALARM_RING_TIME, DateTime.now().getMillis())
                    .putBoolean(Constants.PREF_WAKEUP_SCAN_PENDING, false);
        } else {
            editor.putBoolean(Constants.PREF_WAKEUP_SCAN_PENDING, true);
        }
        editor.apply();

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
            alarm.setWasSampleTaken(false);
            repository.update(alarm);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private int getWakeupStudyDay(SharedPreferences sp) {
        int currentDayCounter = sp.getInt(Constants.PREF_DAY_COUNTER, 0);
        if (sp.getBoolean(Constants.PREF_STUDY_DAY_MANUALLY_ADVANCED, false)
                || sp.getBoolean(Constants.PREF_WAKEUP_SCAN_PENDING, false)
                || isWakeupInitializedToday(sp)) {
            return Math.max(currentDayCounter, 1);
        }

        return currentDayCounter + 1;
    }

    private boolean isWakeupInitializedToday(SharedPreferences sp) {
        if (!sp.contains(Constants.PREF_LAST_WAKE_UP_ALARM_RING_TIME)) {
            return false;
        }

        DateTime lastWakeUpAlarmRingTime = new DateTime(sp.getLong(Constants.PREF_LAST_WAKE_UP_ALARM_RING_TIME, 0));
        DateTime dayCurrentSalivaAlarmsWereScheduled = lastWakeUpAlarmRingTime.withTime(LocalTime.MIDNIGHT);
        return dayCurrentSalivaAlarmsWereScheduled.equals(LocalTime.MIDNIGHT.toDateTimeToday());
    }

    private void logWakeup() {
        try {
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_INITIAL);
            LoggerUtil.log(Constants.LOGGER_ACTION_SPONTANEOUS_AWAKENING, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static class WakeupAlert {
        private final String type;
        private final int minutes;

        private WakeupAlert(String type, int minutes) {
            this.type = type;
            this.minutes = minutes;
        }
    }
}
