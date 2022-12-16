package de.fau.cs.mad.carwatch.ui.alarm;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.databinding.FragmentAlarmBinding;
import de.fau.cs.mad.carwatch.db.Alarm;

public class AlarmFragment extends Fragment {

    private static final String TAG = AlarmFragment.class.getSimpleName();

    private AlarmViewModel alarmViewModel;
    private CoordinatorLayout coordinatorLayout;

    Alarm alarm;

    private LinearLayout alarmLayout;
    private TextView timeTextView;
    private SwitchMaterial activeSwitch;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        alarmViewModel = ViewModelProviders.of(this).get(AlarmViewModel.class);

        FragmentAlarmBinding dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_alarm, container, false);
        View root = dataBinding.getRoot();
        dataBinding.setViewmodel(alarmViewModel);

        if (getActivity() != null) {
            coordinatorLayout = getActivity().findViewById(R.id.coordinator);
        }

        alarmLayout = root.findViewById(R.id.alarm);
        timeTextView = root.findViewById(R.id.alarm_time_text);
        activeSwitch = root.findViewById(R.id.alarm_active_switch);

        // Add an observer on the LiveData returned by getAlarm
        alarmViewModel.getAlarm().observe(getViewLifecycleOwner(), alarm -> {
            this.alarm = alarm;
            // alarm was not created yet
            if (this.alarm == null) {
                createInitialAlarm();
            }
            setAlarmView(this.alarm);
        });
        return root;
    }

    private void setAlarmView(Alarm alarm) {
        final Context context = getContext();
        final Resources resources = getResources();

        // set alarm time
        timeTextView.setText(alarm.getStringTime());
        // set alarm activity
        activeSwitch.setChecked(alarm.isActive());
        setAlarmColor();

        // define behavior on activity switch change
        activeSwitch.setOnCheckedChangeListener((compoundButton, checked) -> {
            alarm.setActive(checked);
            setAlarmColor();
            if (checked) {
                timeTextView.setTextColor(resources.getColor(R.color.colorAccent));
            } else {
                timeTextView.setTextColor(resources.getColor(R.color.colorGrey500));
            }
            scheduleAlarm(context);
            updateAlarm();
        });

        // define behavior on time update
        alarmLayout.setOnClickListener(view -> {
            DateTime time;
            if (alarm.getTime() == null) {
                time = DateTime.now();
            } else {
                time = alarm.getTime();
            }
            TimePickerDialog timePicker = new TimePickerDialog(context, (timePicker1, selectedHour, selectedMinute) -> {
                LocalTime selectedTime = new LocalTime(selectedHour, selectedMinute);
                alarm.setTime(selectedTime.toDateTimeToday());
                timeTextView.setText(selectedTime.toString("HH:mm"));
                alarm.setActive(true);
                scheduleAlarm(context);
                updateAlarm();
            }, time.getHourOfDay(), time.getMinuteOfHour(), true);
            timePicker.show();
        });
    }

    private void setAlarmColor() {
        // Set alarm TextView colors based on alarm's activity state
        if (alarm.isActive()) {
            timeTextView.setTextColor(getResources().getColor(R.color.colorAccent));
        } else {
            timeTextView.setTextColor(getResources().getColor(R.color.colorGrey500));
        }
    }

    private void updateAlarm() {
        alarmViewModel.update(alarm);
        if (alarm.isActive()) {
            showAlarmReminderDialog();
        }
    }

    private void scheduleAlarm(Context context) {
        if (alarm.isActive()) {
            AlarmHandler.scheduleAlarm(context, alarm, coordinatorLayout);
        } else {
            AlarmHandler.cancelAlarm(context, alarm, coordinatorLayout);
        }
    }

    private void createInitialAlarm() {
        alarm = new Alarm();
        setInitialAlarmTime();
        alarmViewModel.insert(alarm);
        scheduleAlarm(getContext());
    }

    /**
     * Initialize TimeTextView and Alarm's time with current time
     */
    private void setInitialAlarmTime() {
        // Get time and set it to alarm time TextView
        DateTime time = DateTime.now();

        String currentTime = time.toString("HH:mm");
        timeTextView.setText(currentTime);
        alarm.setTime(time);
    }

    private void showAlarmReminderDialog() {
        if (getContext() == null) {
            return;
        }
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_alarm_reminder)
                .setMessage(R.string.message_alarm_reminder)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, which) -> {

                }).show();
    }
}