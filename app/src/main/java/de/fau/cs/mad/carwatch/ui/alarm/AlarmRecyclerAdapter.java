package de.fau.cs.mad.carwatch.ui.alarm;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.ui.AddAlarmActivity;

/**
 * ArrayAdapter used to populate MainActivity Alarms ListView
 */
public class AlarmRecyclerAdapter extends RecyclerView.Adapter<AlarmRecyclerAdapter.AlarmViewHolder> {

    private final String TAG = AlarmRecyclerAdapter.class.getSimpleName();

    private Fragment fragment;

    /**
     * View for each alarm
     */
    public class AlarmViewHolder extends RecyclerView.ViewHolder {

        private final TextView timeTextView;
        private final TextView repetitionTextView;
        private final Switch activeSwitch;

        private AlarmViewHolder(View itemView) {
            super(itemView);
            timeTextView = itemView.findViewById(R.id.alarm_time_text);
            repetitionTextView = itemView.findViewById(R.id.alarm_repetition_text);
            activeSwitch = itemView.findViewById(R.id.alarm_active_switch);
        }
    }

    // Data list (cached copy of alarms)
    private List<Alarm> alarms = Collections.emptyList();
    // To handle interactions with database
    private AlarmViewModel alarmViewModel;
    // To schedule alarms
    private AlarmHandler alarmHandler;

    public AlarmRecyclerAdapter(Fragment fragment, View snackBarAnchor) {
        this.fragment = fragment;
        alarmViewModel = ViewModelProviders.of(fragment).get(AlarmViewModel.class);
        alarmHandler = new AlarmHandler(fragment.getContext(), snackBarAnchor);

        setHasStableIds(true); // so Switch interaction has smooth animations
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final @NonNull AlarmViewHolder viewHolder, int position) {
        final Resources resources = viewHolder.itemView.getContext().getResources();
        final Alarm alarm = alarms.get(position);

        viewHolder.timeTextView.setText(alarm.getStringTime()); // set alarm time

        // Set repeatTextView text
        if (alarm.isRepeating()) {
            String repetitionText = alarm.getStringOfActiveDays();
            viewHolder.repetitionTextView.setText(repetitionText);
        } else {
            viewHolder.repetitionTextView.setText(resources.getString(R.string.no_repeat));
        }

        // Set TextView colors based on alarm's active state
        if (alarm.isActive()) {
            viewHolder.activeSwitch.setChecked(true);
            viewHolder.timeTextView.setTextColor(resources.getColor(R.color.colorAccent));
            viewHolder.repetitionTextView.setTextColor(resources.getColor(R.color.colorDarkText));
        } else {
            viewHolder.activeSwitch.setChecked(false);
            viewHolder.timeTextView.setTextColor(resources.getColor(R.color.colorGrey500));
            viewHolder.repetitionTextView.setTextColor(resources.getColor(R.color.colorGrey500));
        }

        viewHolder.activeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    alarm.setActive(true);
                    viewHolder.timeTextView.setTextColor(resources.getColor(R.color.colorAccent));
                    viewHolder.repetitionTextView.setTextColor(resources.getColor(R.color.colorDarkText));
                    // schedule alarm
                    Log.d(TAG, "scheduling alarm: " + alarm.getId());
                    alarmHandler.scheduleAlarm(alarm);
                } else {
                    alarm.setActive(false);
                    viewHolder.timeTextView.setTextColor(resources.getColor(R.color.colorGrey500));
                    viewHolder.repetitionTextView.setTextColor(resources.getColor(R.color.colorGrey500));
                    alarmHandler.cancelAlarm(alarm);
                }

                // Update database and schedule alarm
                Log.d(TAG, "updating database with alarm: " + alarm.getId());
                alarmViewModel.updateActive(alarm);
            }
        });

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = view.getContext();
                Intent intent = new Intent(context, AddAlarmActivity.class);

                Bundle args = new Bundle();
                args.putParcelable(Constants.EXTRA_ALARM, alarm);
                intent.putExtra(Constants.EXTRA_BUNDLE, args);

                fragment.startActivityForResult(intent, Constants.REQUEST_CODE_EDIT_ALARM);
            }
        });
    }

    /**
     * Update current list of alarms and update UI
     *
     * @param alarms List of updated alarms
     */
    public void setAlarms(List<Alarm> alarms) {
        Log.d(TAG, "updating alarms data-set");
        this.alarms = alarms;
        notifyDataSetChanged();
    }

    public void updateAlarm(Alarm alarm) {
        for (int i = 0; i < alarms.size(); i++) {
            if (alarm.getId() == alarms.get(i).getId()) {
                alarms.set(i, alarm);
                if (alarm.isActive()) {
                    alarmHandler.scheduleAlarm(alarm);
                } else {
                    alarmHandler.cancelAlarm(alarm);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return alarms.size();
    }

    @Override
    public long getItemId(int position) {
        return alarms.get(position).getId();
    }

}
