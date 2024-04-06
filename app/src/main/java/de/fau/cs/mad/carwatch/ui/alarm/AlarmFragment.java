package de.fau.cs.mad.carwatch.ui.alarm;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    private TextView timeTextView;
    private TextView salivaAlarmsHeader;
    private TextView sampleNameTextView;
    private SwitchMaterial activeSwitch;
    private ImageView checkIcon;
    private ImageView scannerIcon;
    private ImageView samplePendingIcon;

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

        timeTextView = root.findViewById(R.id.alarm_time_text);
        sampleNameTextView = root.findViewById(R.id.tv_sample_name);
        activeSwitch = root.findViewById(R.id.alarm_active_switch);
        salivaAlarmsHeader = root.findViewById(R.id.tv_saliva_alarms);
        checkIcon = root.findViewById(R.id.iv_check_icon);
        scannerIcon = root.findViewById(R.id.iv_scanner_icon);
        samplePendingIcon = root.findViewById(R.id.iv_sample_pending_icon);

        // Add an observer on the LiveData returned by getAlarm
        alarmViewModel.getAlarmLiveData(Constants.EXTRA_ALARM_ID_INITIAL).observe(getViewLifecycleOwner(), alarm -> {
            if (alarm == null) {
                initializeAlarm();
            } else {
                this.alarm = alarm;
            }
            setAlarmView();
        });

        initializeSalivaAlarmsAdapter(root);

        return root;
    }

    private void initializeSalivaAlarmsAdapter(View root) {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
        adapter = new AlarmAdapter(getResources(), alarmViewModel, getSampleIdPrefix(), getStartSampleId());
        RecyclerView recyclerView = root.findViewById(R.id.saliva_alarms_list);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        setSalivaAlarmAdapterItems(alarmViewModel.getAlarms().getValue());

        // update adapter if alarms change
        alarmViewModel.getAlarms().observe(getViewLifecycleOwner(), this::setSalivaAlarmAdapterItems);
    }

    private void setSalivaAlarmAdapterItems(List<Alarm> alarms) {
        if (alarms == null)
            return;

        List<Alarm> salivaAlarms = new ArrayList<>();
        List<Alarm> sampleAlarms = new ArrayList<>();
        // initial alarm is not shown in list
        for (Alarm alarm : alarms) {
            if (alarm.getId() != Constants.EXTRA_ALARM_ID_INITIAL) {
                salivaAlarms.add(alarm);
            }
        }
        adapter.setAlarms(salivaAlarms);
        adapter.notifyDataSetChanged();
        salivaAlarmsHeader.setVisibility(salivaAlarms.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setAlarmView() {
        final Context context = getContext();

        initializeSampleIdTextView();
        timeTextView.setText(alarm.getStringTime());
        activeSwitch.setChecked(alarm.isActive());
        setAlarmColor(alarm.isActive());
        setSuffixIconProps();
        initializeScannerIconOnclick();

        // define behavior on activity switch change
        activeSwitch.setOnClickListener(view -> {
            alarm.setActive(activeSwitch.isChecked());
            setInitialSalivaId();
            setAlarmColor(activeSwitch.isChecked());
            scheduleAlarm(context);
            updateAlarm();
        });

        // define behavior on time update
        timeTextView.setOnClickListener(view -> {
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
                setInitialSalivaId();
                scheduleAlarm(context);
                updateAlarm();
            }, time.getHourOfDay(), time.getMinuteOfHour(), true);
            timePicker.show();
        });
    }

    private void initializeSampleIdTextView() {
        boolean firstAlarmHasSample = sharedPreferences.getString(Constants.PREF_SALIVA_DISTANCES, "").startsWith("0");
        int adjustedSalivaId = alarm.getSalivaId() + getStartSampleId();
        String sampleName = getSampleIdPrefix() + adjustedSalivaId + ":";

        sampleNameTextView.setVisibility(firstAlarmHasSample ? View.VISIBLE : View.GONE);
        sampleNameTextView.setText(sampleName);

    }

    private void setInitialSalivaId() {
        String salivaDistances = sharedPreferences.getString(Constants.PREF_SALIVA_DISTANCES, "");
        boolean requestSaliva = salivaDistances.startsWith("0");
        alarm.setSalivaId(requestSaliva ? Constants.EXTRA_SALIVA_ID_INITIAL : -1);
    }

    private void setAlarmColor(boolean isActive) {
        // Set alarm TextView colors based on alarm's activity state
        int colorId = isActive ? R.color.colorAccent : R.color.colorGrey500;
        timeTextView.setTextColor(getResources().getColor(colorId));
    }

    private void setSuffixIconProps() {
        boolean firstAlarmHasSample = sharedPreferences.getString(Constants.PREF_SALIVA_DISTANCES, "").startsWith("0");
        if (firstAlarmHasSample) {
            checkIcon.setVisibility(alarm.wasSampleTaken() ? View.VISIBLE : View.GONE);
            DateTime lastWakeUpRing = new DateTime(sharedPreferences.getLong(Constants.PREF_LAST_WAKE_UP_ALARM_RING_TIME, Long.MAX_VALUE));
            boolean sampleIsPending = DateTime.now().isAfter(lastWakeUpRing) && !alarm.wasSampleTaken();
            scannerIcon.setVisibility(sampleIsPending ? View.VISIBLE : View.GONE);
            samplePendingIcon.setVisibility(sampleIsPending ? View.VISIBLE : View.GONE);
        } else {
            checkIcon.setVisibility(View.GONE);
            scannerIcon.setVisibility(View.GONE);
            samplePendingIcon.setVisibility(View.GONE);
        }


        if (!sharedPreferences.getString(Constants.PREF_SALIVA_DISTANCES, "").startsWith("0")) {
            checkIcon.setVisibility(View.GONE);
        } else if (alarm != null)
            checkIcon.setVisibility(alarm.wasSampleTaken() ? View.VISIBLE : View.GONE);
    }

    private void initializeScannerIconOnclick() {
        scannerIcon.setOnClickListener(view ->
                AlarmViewFunctionalities.openScanner(requireContext(), alarm)
        );
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

        DateTime time = DateTime.now();
        alarm.setTime(time);
        setInitialSalivaId();
        alarmViewModel.insert(alarm);
        timeTextView.setText(time.toString("HH:mm"));
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

    private String getSampleIdPrefix() {
        String startSample = sharedPreferences.getString(Constants.PREF_START_SAMPLE, Constants.DEFAULT_START_SAMPLE);
        return !startSample.isEmpty() ? startSample.substring(0, 1) : Constants.DEFAULT_START_SAMPLE.substring(0, 1);
    }

    private int getStartSampleId() {
        String startSample = sharedPreferences.getString(Constants.PREF_START_SAMPLE, Constants.DEFAULT_START_SAMPLE);
        try {
            return Integer.parseInt(startSample.substring(1));
        } catch (NumberFormatException e) {
            return Integer.parseInt(Constants.DEFAULT_START_SAMPLE.substring(1));
        }
    }
}