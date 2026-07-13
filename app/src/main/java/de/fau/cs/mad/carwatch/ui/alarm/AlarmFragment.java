package de.fau.cs.mad.carwatch.ui.alarm;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import de.fau.cs.mad.carwatch.ui.CarwatchDialogBuilder;

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

    private SharedPreferences sharedPreferences;
    private AlarmViewModel alarmViewModel;
    private AlarmAdapter adapter;
    private AlarmAdapter eveningAdapter;
    private CoordinatorLayout coordinatorLayout;
    private View rootView;
    private LinearLayout alarmContent;
    private View alarmPrimaryCard;
    private LinearLayout alarmPrimaryCardContent;
    private View alarmPrimaryIconBadge;
    private View alarmPrimaryTimeCard;
    private View alarmPrimaryTimeRow;
    private boolean contentRevealed;
    private Alarm alarm;
    private TextView timeTextView;
    private LinearLayout alarmSeparator;
    private LinearLayout salivaAlarmsContainer;
    private LinearLayout eveningSampleAlarmContainer;
    private TextView salivaAlarmsHeader;
    private TextView eveningSampleAlarmHeader;
    private SwitchMaterial activeSwitch;
    private Alarm eveningAlarm;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        FragmentAlarmBinding dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_alarm, container, false);
        View root = dataBinding.getRoot();
        rootView = root;
        rootView.setAlpha(1f);
        dataBinding.setViewmodel(alarmViewModel);

        if (getActivity() != null) {
            coordinatorLayout = getActivity().findViewById(R.id.coordinator);
        }

        alarmContent = root.findViewById(R.id.alarm_content);
        alarmPrimaryCard = root.findViewById(R.id.alarm_primary_card);
        alarmPrimaryCardContent = root.findViewById(R.id.alarm_primary_card_content);
        alarmPrimaryIconBadge = root.findViewById(R.id.alarm_primary_icon_badge);
        alarmPrimaryTimeCard = root.findViewById(R.id.alarm_primary_time_card);
        alarmPrimaryTimeRow = root.findViewById(R.id.alarm_primary_time_row);
        timeTextView = root.findViewById(R.id.alarm_time_text);
        activeSwitch = root.findViewById(R.id.alarm_active_switch);
        salivaAlarmsContainer = root.findViewById(R.id.saliva_alarms);
        eveningSampleAlarmContainer = root.findViewById(R.id.evening_sample_alarm);
        salivaAlarmsHeader = root.findViewById(R.id.tv_saliva_alarms);
        eveningSampleAlarmHeader = root.findViewById(R.id.tv_evening_sample_alarm);
        alarmSeparator = root.findViewById(R.id.alarm_separator);
        setAlarmContentOffset(false);

        // Add an observer on the LiveData returned by getAlarm
        alarmViewModel.getAlarmLiveData(Constants.EXTRA_ALARM_ID_INITIAL).observe(getViewLifecycleOwner(), alarm -> {
            if (alarm == null) {
                initializeAlarm();
            } else {
                this.alarm = alarm;
            }
            setAlarmView();
            initializeSalivaAlarmsAdapter(root);
        });

        alarmViewModel.getAlarmLiveData(Constants.EXTRA_ALARM_ID_EVENING).observe(getViewLifecycleOwner(), alarm -> {
            this.eveningAlarm = alarm;
            ensureEveningReminderAlarm();
            if (adapter != null && eveningAdapter != null) {
                setSalivaAlarmAdapterItems(alarmViewModel.getAlarms().getValue());
            }
        });

        return root;
    }

    private void initializeSalivaAlarmsAdapter(View root) {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
        GridLayoutManager eveningLayoutManager = new GridLayoutManager(getContext(), 1);
        adapter = new AlarmAdapter(alarmViewModel, getSampleIdPrefix(), getStartSampleId());
        eveningAdapter = new AlarmAdapter(alarmViewModel, getSampleIdPrefix(), getStartSampleId());
        RecyclerView recyclerView = root.findViewById(R.id.saliva_alarms_list);
        RecyclerView eveningRecyclerView = root.findViewById(R.id.evening_sample_alarm_list);
        recyclerView.setLayoutManager(layoutManager);
        eveningRecyclerView.setLayoutManager(eveningLayoutManager);
        recyclerView.setAdapter(adapter);
        eveningRecyclerView.setAdapter(eveningAdapter);
        setSalivaAlarmAdapterItems(alarmViewModel.getAlarms().getValue());

        // update adapter if alarms change
        alarmViewModel.getAlarms().observe(getViewLifecycleOwner(), this::setSalivaAlarmAdapterItems);
    }

    private void setSalivaAlarmAdapterItems(List<Alarm> alarms) {
        if (alarms == null)
            return;

        ensureEveningReminderAlarm();
        List<Alarm> sampleAlarms = new ArrayList<>();
        List<Alarm> eveningAlarms = new ArrayList<>();
        boolean wakeupTriggered = sharedPreferences.contains(Constants.PREF_LAST_WAKE_UP_ALARM_RING_TIME);
        boolean wakeupScanPending = sharedPreferences.getBoolean(Constants.PREF_WAKEUP_SCAN_PENDING, false);
        boolean wakeupStarted = wakeupTriggered || wakeupScanPending;

        String salivaDistances = sharedPreferences.getString(Constants.PREF_SALIVA_DISTANCES, "");
        if (AlarmHandler.requiresImmediateWakeupSample(salivaDistances) && wakeupStarted) {
            long wakeUpTimeMillis = wakeupTriggered
                    ? sharedPreferences.getLong(Constants.PREF_LAST_WAKE_UP_ALARM_RING_TIME, Long.MAX_VALUE)
                    : sharedPreferences.getLong(Constants.PREF_WAKEUP_SCAN_PENDING_TIME, DateTime.now().getMillis());
            DateTime wakeUpTime = new DateTime(wakeUpTimeMillis);
            Alarm initialAlarm = getInitialAlarm(alarms);
            Alarm initialSampleAlarm = new Alarm(
                    wakeUpTime,
                    false,
                    false,
                    Constants.FIRST_SAMPLE_ALARM_ID,
                    Constants.EXTRA_SALIVA_ID_INITIAL,
                    isWakeupSampleTaken(initialAlarm)
            );
            sampleAlarms.add(initialSampleAlarm);
        }

        // initial alarm is not shown in list
        for (Alarm alarm : alarms) {
            if (alarm.getId() == Constants.EXTRA_ALARM_ID_EVENING) {
                if (wakeupTriggered && sharedPreferences.getBoolean(Constants.PREF_HAS_EVENING, false)) {
                    eveningAlarms.add(alarm);
                }
                continue;
            }
            if (alarm.getId() != Constants.EXTRA_ALARM_ID_INITIAL) {
                sampleAlarms.add(alarm);
            }
        }
        adapter.setAlarms(sampleAlarms);
        eveningAdapter.setAlarms(eveningAlarms);
        salivaAlarmsContainer.setVisibility(sampleAlarms.isEmpty() ? View.GONE : View.VISIBLE);
        eveningSampleAlarmContainer.setVisibility(eveningAlarms.isEmpty() ? View.GONE : View.VISIBLE);
        salivaAlarmsHeader.setVisibility(sampleAlarms.isEmpty() ? View.GONE : View.VISIBLE);
        eveningSampleAlarmHeader.setVisibility(eveningAlarms.isEmpty() ? View.GONE : View.VISIBLE);
        alarmSeparator.setVisibility(sampleAlarms.isEmpty() && eveningAlarms.isEmpty() ? View.GONE : View.VISIBLE);
        setAlarmContentOffset(!sampleAlarms.isEmpty() || !eveningAlarms.isEmpty());
        revealContent();
    }

    private void setAlarmContentOffset(boolean hasDisplayedAlarms) {
        if (alarmContent == null) {
            return;
        }

        alarmContent.setTranslationY(0f);
        alarmContent.setGravity(hasDisplayedAlarms
                ? Gravity.TOP | Gravity.CENTER_HORIZONTAL
                : Gravity.CENTER);
        alarmContent.setPadding(
                alarmContent.getPaddingLeft(),
                alarmContent.getPaddingTop(),
                alarmContent.getPaddingRight(),
                getResources().getDimensionPixelSize(hasDisplayedAlarms
                        ? R.dimen.alarm_content_list_bottom_padding
                        : R.dimen.primary_screen_bottom_padding));

        if (alarmPrimaryCard != null && alarmPrimaryCard.getLayoutParams() instanceof LinearLayout.LayoutParams params) {
            // The prompt can wrap to an additional line on narrow screens or with a larger
            // font. A fixed compact height clips the time row in that case, leaving no visible
            // way to set the next wake-up alarm while sample alarms are displayed.
            params.height = hasDisplayedAlarms
                    ? ViewGroup.LayoutParams.WRAP_CONTENT
                    : getResources().getDimensionPixelSize(R.dimen.primary_card_height);
            params.bottomMargin = getResources().getDimensionPixelSize(hasDisplayedAlarms
                    ? R.dimen.alarm_primary_card_list_margin_bottom
                    : R.dimen.alarm_primary_card_margin_bottom);
            alarmPrimaryCard.setLayoutParams(params);
        }

        setWakeupCardSpacing(hasDisplayedAlarms);
    }

    private void setWakeupCardSpacing(boolean compact) {
        if (alarmPrimaryCardContent != null) {
            int verticalPadding = getResources().getDimensionPixelSize(R.dimen.alarm_primary_card_list_padding_vertical);
            alarmPrimaryCardContent.setPadding(
                    alarmPrimaryCardContent.getPaddingLeft(),
                    compact ? verticalPadding : getResources().getDimensionPixelSize(R.dimen.alarm_primary_card_padding_top),
                    alarmPrimaryCardContent.getPaddingRight(),
                    compact ? verticalPadding : getResources().getDimensionPixelSize(R.dimen.alarm_primary_card_padding_bottom));
        }

        setBottomMargin(alarmPrimaryIconBadge, compact
                ? R.dimen.alarm_primary_icon_list_margin_bottom
                : R.dimen.alarm_primary_icon_margin_bottom);
        setTopMargin(alarmPrimaryTimeCard, compact
                ? R.dimen.alarm_primary_time_card_list_margin_top
                : R.dimen.alarm_primary_time_card_margin_top);

        if (alarmPrimaryTimeRow != null) {
            alarmPrimaryTimeRow.setMinimumHeight(getResources().getDimensionPixelSize(compact
                    ? R.dimen.alarm_primary_time_row_list_min_height
                    : R.dimen.alarm_primary_time_row_min_height));
        }
    }

    private void setBottomMargin(View view, int dimenRes) {
        if (view == null || !(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams params)) {
            return;
        }

        params.bottomMargin = getResources().getDimensionPixelSize(dimenRes);
        view.setLayoutParams(params);
    }

    private void setTopMargin(View view, int dimenRes) {
        if (view == null || !(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams params)) {
            return;
        }

        params.topMargin = getResources().getDimensionPixelSize(dimenRes);
        view.setLayoutParams(params);
    }

    private void revealContent() {
        if (contentRevealed || rootView == null) {
            return;
        }

        contentRevealed = true;
        rootView.setAlpha(1f);
    }

    private Alarm getInitialAlarm(List<Alarm> alarms) {
        for (Alarm alarm : alarms) {
            if (alarm.getId() == Constants.EXTRA_ALARM_ID_INITIAL) {
                return alarm;
            }
        }
        return alarm;
    }

    private boolean isWakeupSampleTaken(Alarm initialAlarm) {
        if (initialAlarm.wasSampleTaken()) {
            return true;
        }

        long wakeupSampleTakenTime = sharedPreferences.getLong(Constants.PREF_WAKEUP_SAMPLE_TAKEN_TIME, 0);
        if (wakeupSampleTakenTime == 0) {
            return false;
        }

        DateTime takenDate = new DateTime(wakeupSampleTakenTime);
        return takenDate.withTime(LocalTime.MIDNIGHT).equals(LocalTime.MIDNIGHT.toDateTimeToday());
    }

    private void ensureEveningReminderAlarm() {
        boolean hasEveningSample = sharedPreferences.getBoolean(Constants.PREF_HAS_EVENING, false);
        if (!hasEveningSample) {
            if (eveningAlarm != null) {
                AlarmHandler.cancelAlarm(requireContext(), eveningAlarm, null);
                alarmViewModel.delete(eveningAlarm);
                eveningAlarm = null;
            }
            return;
        }

        int eveningSalivaId = sharedPreferences.getInt(Constants.PREF_EVENING_SALIVA_ID, -1);
        boolean eveningSampleTaken = isEveningSampleTaken();
        if (eveningAlarm == null) {
            eveningAlarm = new Alarm(
                    getEveningReminderTime(),
                    false,
                    true,
                    Constants.EXTRA_ALARM_ID_EVENING,
                    eveningSalivaId,
                    eveningSampleTaken
            );
            alarmViewModel.insert(eveningAlarm);
            return;
        }

        boolean changed = false;
        if (eveningAlarm.getSalivaId() != eveningSalivaId) {
            eveningAlarm.setSalivaId(eveningSalivaId);
            changed = true;
        }
        if (eveningAlarm.wasSampleTaken() != eveningSampleTaken) {
            eveningAlarm.setWasSampleTaken(eveningSampleTaken);
            if (eveningSampleTaken && eveningAlarm.isActive()) {
                eveningAlarm.setActive(false);
                AlarmHandler.cancelAlarm(requireContext(), eveningAlarm, null);
            }
            changed = true;
        }
        if (changed) {
            alarmViewModel.update(eveningAlarm);
        }
    }

    private boolean isEveningSampleTaken() {
        DateTime date = new DateTime(sharedPreferences.getLong(Constants.PREF_EVENING_TAKEN, 0));
        return date.equals(LocalTime.MIDNIGHT.toDateTimeToday());
    }

    private DateTime getEveningReminderTime() {
        int defaultMinutes = Constants.DEFAULT_EVENING_REMINDER_TIME.getHourOfDay() * 60
                + Constants.DEFAULT_EVENING_REMINDER_TIME.getMinuteOfHour();
        int minutes = sharedPreferences.getInt(Constants.PREF_EVENING_REMINDER_TIME_MINUTES, defaultMinutes);
        return new LocalTime(minutes / 60, minutes % 60).toDateTimeToday();
    }

    private void setAlarmView() {
        final Context context = getContext();

        setWakeupAlarmTimeText();
        activeSwitch.setChecked(alarm.isActive());
        setAlarmColor(alarm.isActive());

        // define behavior on activity switch change
        activeSwitch.setOnClickListener(view -> {
            alarm.setActive(activeSwitch.isChecked());
            setInitialSalivaId();
            setAlarmColor(activeSwitch.isChecked());
            scheduleAlarm(context);
            updateAlarm();
        });

        // Define behavior on time update. Keep both the time and its containing row actionable so
        // the alarm can always be changed, even when the compact layout is used.
        View.OnClickListener showTimePicker = view -> {
            DateTime time;
            if (alarm.getTime() == null) {
                time = DateTime.now();
            } else {
                time = alarm.getTime();
            }
            TimePickerDialog timePicker = new TimePickerDialog(context, (timePicker1, selectedHour, selectedMinute) -> {
                LocalTime selectedTime = new LocalTime(selectedHour, selectedMinute);
                DateTime selectedDateTime = selectedTime.toDateTimeToday();
                if (isWakeupInitializedToday()) {
                    selectedDateTime = selectedDateTime.plusDays(1);
                }
                alarm.setTime(selectedDateTime);
                setWakeupAlarmTimeText();
                alarm.setActive(true);
                setInitialSalivaId();
                scheduleAlarm(context);
                updateAlarm();
            }, time.getHourOfDay(), time.getMinuteOfHour(), true);
            timePicker.show();
        };
        timeTextView.setOnClickListener(showTimePicker);
        alarmPrimaryTimeRow.setOnClickListener(showTimePicker);
        alarmPrimaryCard.setOnClickListener(showTimePicker);
    }

    private boolean isWakeupInitializedToday() {
        if (!sharedPreferences.contains(Constants.PREF_LAST_WAKE_UP_ALARM_RING_TIME)) {
            return false;
        }
        DateTime wakeup = new DateTime(sharedPreferences.getLong(Constants.PREF_LAST_WAKE_UP_ALARM_RING_TIME, 0));
        return wakeup.toLocalDate().equals(DateTime.now().toLocalDate());
    }

    private void setInitialSalivaId() {
        String salivaDistances = sharedPreferences.getString(Constants.PREF_SALIVA_DISTANCES, "");
        boolean requestSaliva = AlarmHandler.requiresImmediateWakeupSample(salivaDistances);
        alarm.setSalivaId(requestSaliva ? Constants.EXTRA_SALIVA_ID_INITIAL : -1);
    }

    private void setWakeupAlarmTimeText() {
        timeTextView.setText(alarm.getStringTime());
        timeTextView.post(() -> {
            int availableWidth = timeTextView.getWidth()
                    - timeTextView.getCompoundPaddingLeft()
                    - timeTextView.getCompoundPaddingRight();
            if (availableWidth <= 0 || timeTextView.length() == 0) {
                return;
            }

            float maxTextSizePx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 45, getResources().getDisplayMetrics());
            float minTextSizePx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 20, getResources().getDisplayMetrics());
            timeTextView.getPaint().setTextSize(maxTextSizePx);
            float measuredWidth = timeTextView.getPaint().measureText(timeTextView.getText().toString());
            float fittedSizePx = measuredWidth > availableWidth
                    ? maxTextSizePx * availableWidth / measuredWidth
                    : maxTextSizePx;
            timeTextView.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    Math.max(fittedSizePx - 1f, minTextSizePx));
        });
    }

    private void setAlarmColor(boolean isActive) {
        // Set alarm TextView colors based on alarm's activity state
        int colorId = isActive ? R.color.colorAccent : R.color.colorGrey500;
        timeTextView.setTextColor(ContextCompat.getColor(requireContext(), colorId));
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
        setWakeupAlarmTimeText();
        sharedPreferences.edit().putInt(Constants.PREF_CURRENT_ALARM_ID, alarm.getId() + 1).apply();
    }

    private void showAlarmReminderDialog() {
        if (getContext() == null) {
            return;
        }
        new CarwatchDialogBuilder(getContext())
                .setIcon(R.drawable.ic_info_24dp)
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
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return 0;
        }
    }
}
