package de.fau.cs.mad.carwatch.ui.alarm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.databinding.FragmentAlarmBinding;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.ui.AddAlarmActivity;

public class AlarmFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = AlarmFragment.class.getSimpleName();

    private CoordinatorLayout coordinatorLayout;

    private LinearLayout alarmLayout;
    private TextView timeTextView;
    private SwitchMaterial activeSwitch;

    // View to display Snackbar messages
    private View snackBarAnchor;

    private boolean firstInit = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        AlarmViewModel alarmViewModel = ViewModelProviders.of(this).get(AlarmViewModel.class);

        FragmentAlarmBinding dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_alarm, container, false);
        View root = dataBinding.getRoot();
        dataBinding.setViewmodel(alarmViewModel);

        if (getActivity() != null) {
            coordinatorLayout = getActivity().findViewById(R.id.coordinator);
        }

        // TODO final TextView noAlarmsTextView = root.findViewById(R.id.tv_no_alarm);

        // Add an observer on the LiveData returned by getAllAlarms.
        alarmViewModel.getAllAlarms().observe(getViewLifecycleOwner(), alarms -> {
            // Update the cached copy of the words in the adapter.
            firstInit = alarms == null; // TODO this doesn't make sense
            //noAlarmsTextView.setVisibility(alarms == null || alarms.size() == 0 ? View.VISIBLE : View.GONE);
            Log.e(TAG, "onCreateView: "+ alarms);
            Log.e(TAG, "onCreateView: "+ alarms.get(0));

            if (!firstInit) {
                setAlarmView(alarms.get(0)); //
            }else{
                getActivity().findViewById(R.id.alarm).setVisibility(View.GONE);
            }
        });

        //fab.setOnClickListener(this);

        alarmLayout = root.findViewById(R.id.alarm);
        timeTextView = root.findViewById(R.id.alarm_time_text);
        activeSwitch = root.findViewById(R.id.alarm_active_switch);

        return root;
    }

    @Override
    public void onClick(View v) {
       /* if (v.getId() == R.id.fab) {
            Intent intent = new Intent(getContext(), AddAlarmActivity.class);
            startActivityForResult(intent, Constants.REQUEST_CODE_NEW_ALARM);
        }*/
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (data.hasExtra(Constants.EXTRA_ALARM)) {
                Alarm alarm = data.getParcelableExtra(Constants.EXTRA_ALARM);
                updateAlarm(alarm);

                if (data.hasExtra(Constants.EXTRA_EDIT)) {
                    showAlarmReminderDialog();
                }
            }
        } else {
            Snackbar.make(coordinatorLayout, R.string.alarm_not_saved, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void setAlarmView(Alarm alarm) {
        Log.e(TAG, "setAlarmView: "+alarm);
        AlarmViewModel alarmViewModel = ViewModelProviders.of(this).get(AlarmViewModel.class);

        getActivity().findViewById(R.id.alarm).setVisibility(View.VISIBLE);

        final Context context = getContext();
        final Resources resources = getResources();
        Log.e(TAG, "setAlarmView: "+alarm);
        timeTextView.setText(alarm.getStringTime()); // set alarm time

        // Set TextView colors based on alarm's active state
        if (alarm.isActive()) {
            activeSwitch.setChecked(true);
            timeTextView.setTextColor(resources.getColor(R.color.colorAccent));
            if (!firstInit) {
                // only if this is not the first time the alarm is called (when launching the fragment)
                // => otherwise, the Snackbar message would always be shown when the user switches to this fragment...
                updateAlarm(alarm);
            }
        } else {
            activeSwitch.setChecked(false);
            timeTextView.setTextColor(resources.getColor(R.color.colorGrey500));
        }

        activeSwitch.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) {
                alarm.setActive(true);
                timeTextView.setTextColor(resources.getColor(R.color.colorAccent));
                // schedule alarm
                Log.d(TAG, "scheduling alarm: " + alarm.getId());
                AlarmHandler.scheduleAlarm(context, alarm, coordinatorLayout);
            } else {
                alarm.setActive(false);
                timeTextView.setTextColor(resources.getColor(R.color.colorGrey500));
                AlarmHandler.cancelAlarm(context, alarm, coordinatorLayout);
            }

            // Update database and schedule alarm
            Log.d(TAG, "updating database with alarm: " + alarm.getId());
            alarmViewModel.updateActive(alarm);
        });

        alarmLayout.setOnClickListener(view -> {
            Intent intent = new Intent(context, AddAlarmActivity.class);

            Bundle args = new Bundle();
            args.putParcelable(Constants.EXTRA_ALARM, alarm);
            intent.putExtra(Constants.EXTRA_BUNDLE, args);

            startActivityForResult(intent, Constants.REQUEST_CODE_EDIT_ALARM);
        });
    }

    private void updateAlarm(Alarm alarm) {

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