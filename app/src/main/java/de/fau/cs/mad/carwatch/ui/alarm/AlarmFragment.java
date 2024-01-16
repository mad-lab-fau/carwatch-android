package de.fau.cs.mad.carwatch.ui.alarm;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.List;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.databinding.FragmentAlarmBinding;
import de.fau.cs.mad.carwatch.db.Alarm;

public class AlarmFragment extends Fragment {

    private static final String TAG = AlarmFragment.class.getSimpleName();

    private SharedPreferences sharedPreferences;
    private AlarmViewModel alarmViewModel;
    private AlarmAdapter adapter;
    private CoordinatorLayout coordinatorLayout;
    private Alarm alarm;
    private LinearLayout alarmLayout;
    private TextView timeTextView;
    private SwitchMaterial activeSwitch;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

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
        alarmViewModel.getAlarmLiveData(Constants.INITIAL_ALARM_ID).observe(getViewLifecycleOwner(), alarm -> {
            if (alarm == null) {
                initializeAlarm();
            } else {
                this.alarm = alarm;
            }
            setAlarmView();
        });

        initializeFixedAlarmsAdapter(root);

        return root;
    }

    private void initializeFixedAlarmsAdapter(View root) {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
        adapter = new AlarmAdapter(getResources(), alarmViewModel);
        RecyclerView recyclerView = root.findViewById(R.id.fixed_alarms_list);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // update adapter if alarms change
        alarmViewModel.getAlarms().observe(getViewLifecycleOwner(), alarms -> {
            List<Alarm> salivaAlarms = new ArrayList<>();
            // initial alarm is not shown in list
            for (Alarm alarm : alarms) {
                if (alarm.getId() != Constants.INITIAL_ALARM_ID) {
                    salivaAlarms.add(alarm);
                }
            }
            adapter.setAlarms(salivaAlarms);
            adapter.notifyDataSetChanged();
        });
    }

    private void setAlarmView() {
        final Context context = getContext();

        timeTextView.setText(alarm.getStringTime());
        activeSwitch.setChecked(alarm.isActive());
        setAlarmColor(alarm.isActive());

        // define behavior on activity switch change
        activeSwitch.setOnCheckedChangeListener((compoundButton, checked) -> {
            alarm.setActive(checked);
            setAlarmColor(checked);
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

    private void setAlarmColor(boolean isActive) {
        // Set alarm TextView colors based on alarm's activity state
        int colorId = isActive ? R.color.colorAccent : R.color.colorGrey500;
        timeTextView.setTextColor(getResources().getColor(colorId));
    }

    private void updateAlarm() {
        alarmViewModel.update(alarm);
        if (alarm.isActive()) {
            showAlarmReminderDialog();
        }
    }

    private void scheduleAlarm(Context context) {
        if (alarm.isActive()) {
            AlarmHandler.scheduleWakeUpAlarm(context, alarm, coordinatorLayout);
        } else {
            AlarmHandler.cancelAlarm(context, alarm, coordinatorLayout);
        }
    }

    private void initializeAlarm() {
        alarm = new Alarm();
        DateTime time = DateTime.now().plusSeconds(30);

        timeTextView.setText(time.toString("HH:mm"));
        alarm.setTime(time);
        alarmViewModel.insert(alarm);
        scheduleAlarm(getContext());
        sharedPreferences.edit().putInt(Constants.PREF_CURRENT_ALARM_ID, alarm.getId() + 1).apply();
    }

    private void showAlarmReminderDialog() {
        if (getContext() == null) {
            return;
        }
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_alarm_reminder)
                .setMessage(R.string.message_alarm_reminder)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, which) -> {}).show();
    }
}