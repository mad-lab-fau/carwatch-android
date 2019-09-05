package de.fau.cs.mad.carwatch.ui.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.snackbar.Snackbar;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.MainActivity;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.databinding.FragmentAlarmBinding;
import de.fau.cs.mad.carwatch.service.AlarmReceiver;

public class AlarmFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = AlarmFragment.class.getSimpleName();

    private boolean isInitialized = false;

    private AlarmManager alarmManager;

    private AlarmViewModel alarmViewModel;
    private CoordinatorLayout coordinatorLayout;

    private PeriodFormatter formatter = new PeriodFormatterBuilder()
            .appendHours()
            .appendSuffix(" hour", " hours")
            .appendSeparatorIfFieldsBefore(" from ")
            .appendMinutes()
            .appendSuffix(" minute", " minutes")
            .appendSeparatorIfFieldsBefore(" from ")
            .toFormatter();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        alarmViewModel = ViewModelProviders.of(this).get(AlarmViewModel.class);

        FragmentAlarmBinding dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_alarm, container, false);
        View root = dataBinding.getRoot();
        dataBinding.setViewmodel(alarmViewModel);

        if (getContext() != null) {
            alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        }

        if (getActivity() != null) {
            coordinatorLayout = getActivity().findViewById(R.id.coordinator);
        }

        final TextView textView = root.findViewById(R.id.tv_alarm);
        textView.setOnClickListener(this);

        final Switch enableSwitch = root.findViewById(R.id.switch_enable);
        enableSwitch.setOnCheckedChangeListener(this);

        alarmViewModel.getAlarmString().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String alarm) {
                textView.setText(alarm);
            }
        });

        alarmViewModel.getAlarmEnabled().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                setAlarm(alarmViewModel.getAlarm().getValue(), aBoolean);
            }
        });

        alarmViewModel.getAlarm().observe(this, new Observer<DateTime>() {
            @Override
            public void onChanged(DateTime dateTime) {
                setAlarm(dateTime, alarmViewModel.getAlarmEnabled().getValue());
                isInitialized = true;
            }
        });

        return root;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_alarm:
                DateTime time = alarmViewModel.getAlarm().getValue();
                if (time == null) {
                    return;
                }

                TimePickerDialog dialog = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        DateTime time = new LocalTime(hourOfDay, minute).toDateTimeToday();

                        alarmViewModel.setAlarm(time);
                    }
                }, time.getHourOfDay(), time.getMinuteOfHour(), true);

                dialog.show();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.switch_enable:
                alarmViewModel.setAlarmEnabled(isChecked);
                break;
        }
    }


    public void setAlarm(DateTime time, boolean enableAlarm) {
        if (enableAlarm) {
            if (time != null) {
                Intent intent = new Intent(getActivity(), AlarmReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), Constants.REQUEST_CODE_ALARM, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                Intent intentShow = new Intent(getActivity(), MainActivity.class);
                intentShow.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pendingIntentShow = PendingIntent.getActivity(getContext(), Constants.REQUEST_CODE_ALARM_ACTIVITY, intentShow, PendingIntent.FLAG_UPDATE_CURRENT);

                AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(time.getMillis(), pendingIntentShow);
                alarmManager.setAlarmClock(info, pendingIntent);

                Period timeDiff = new Period(DateTime.now(), time);

                if (coordinatorLayout != null && isInitialized) {
                    Snackbar.make(coordinatorLayout, "Alarm set for " + formatter.print(timeDiff) + "now.", Snackbar.LENGTH_SHORT).show();
                }
            }
        } else {
            Intent intent = new Intent(getActivity(), AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), Constants.REQUEST_CODE_ALARM, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            alarmManager.cancel(pendingIntent);
        }
    }
}