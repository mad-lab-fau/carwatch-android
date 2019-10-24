package de.fau.cs.mad.carwatch.ui.alarm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.subject.Condition;
import de.fau.cs.mad.carwatch.subject.SubjectMap;
import de.fau.cs.mad.carwatch.ui.AddAlarmActivity;

/**
 * ArrayAdapter used to populate MainActivity Alarms ListView
 */
public class AlarmRecyclerAdapter extends RecyclerView.Adapter<AlarmRecyclerAdapter.AlarmViewHolder> {

    private final String TAG = AlarmRecyclerAdapter.class.getSimpleName();

    private Fragment fragment;

    private boolean firstInit = false;

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

    private Context context;

    // Data list (cached copy of alarms)
    private List<Alarm> alarms = null;
    // To handle interactions with database
    private AlarmViewModel alarmViewModel;
    // View to display Snackbar messages
    private View snackBarAnchor;

    public AlarmRecyclerAdapter(Fragment fragment, View snackBarAnchor) {
        this.fragment = fragment;
        this.context = fragment.getContext();
        this.snackBarAnchor = snackBarAnchor;
        alarmViewModel = ViewModelProviders.of(fragment).get(AlarmViewModel.class);

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
        final Resources resources = context.getResources();
        final Alarm alarm = alarms.get(position);

        viewHolder.timeTextView.setText(alarm.getStringTime()); // set alarm time

        // Set repeatTextView text
        if (alarm.isRepeating()) {
            String repetitionText = alarm.getStringOfActiveDays(context);
            viewHolder.repetitionTextView.setText(repetitionText);
        } else {
            viewHolder.repetitionTextView.setText(resources.getString(R.string.no_repeat));
        }

        // check whether hidden alarm should be activated (e.g. subject id was changed in preferences after alarm was created)
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(viewHolder.itemView.getContext());
        String subjectId = sp.getString(Constants.PREF_SUBJECT_ID, null);
        if (subjectId != null) {
            alarm.setHasHiddenTime(SubjectMap.getConditionForSubject(subjectId) == Condition.UNKNOWN_ALARM);
        }

        // Set TextView colors based on alarm's active state
        if (alarm.isActive()) {
            viewHolder.activeSwitch.setChecked(true);
            viewHolder.timeTextView.setTextColor(resources.getColor(R.color.colorAccent));
            viewHolder.repetitionTextView.setTextColor(resources.getColor(R.color.colorDarkText));
            if (!firstInit) {
                updateAlarm(alarm);
            }
        } else {
            viewHolder.activeSwitch.setChecked(false);
            viewHolder.timeTextView.setTextColor(resources.getColor(R.color.colorGrey500));
            viewHolder.repetitionTextView.setTextColor(resources.getColor(R.color.colorGrey500));
        }

        viewHolder.activeSwitch.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) {
                alarm.setActive(true);
                viewHolder.timeTextView.setTextColor(resources.getColor(R.color.colorAccent));
                viewHolder.repetitionTextView.setTextColor(resources.getColor(R.color.colorDarkText));
                // schedule alarm
                Log.d(TAG, "scheduling alarm: " + alarm.getId());
                AlarmHandler.scheduleAlarm(context, alarm, snackBarAnchor);
            } else {
                alarm.setActive(false);
                viewHolder.timeTextView.setTextColor(resources.getColor(R.color.colorGrey500));
                viewHolder.repetitionTextView.setTextColor(resources.getColor(R.color.colorGrey500));
                AlarmHandler.cancelAlarm(context, alarm, snackBarAnchor);
            }

            // Update database and schedule alarm
            Log.d(TAG, "updating database with alarm: " + alarm.getId());
            alarmViewModel.updateActive(alarm);
        });

        viewHolder.itemView.setOnClickListener(view -> {
            Context context = view.getContext();
            Intent intent = new Intent(context, AddAlarmActivity.class);

            Bundle args = new Bundle();
            args.putParcelable(Constants.EXTRA_ALARM, alarm);
            intent.putExtra(Constants.EXTRA_BUNDLE, args);

            fragment.startActivityForResult(intent, Constants.REQUEST_CODE_EDIT_ALARM);
        });
    }

    /**
     * Update current list of alarms and update UI
     *
     * @param alarms List of updated alarms
     */
    public void setAlarms(List<Alarm> alarms) {
        Log.d(TAG, "updating alarms data-set");

        // first time setting the alarms
        firstInit = this.alarms == null;

        this.alarms = alarms;
        notifyDataSetChanged();
    }

    public void updateAlarm(Alarm alarm) {
        if (alarms == null) {
            return;
        }

        for (int i = 0; i < alarms.size(); i++) {
            if (alarm.getId() == alarms.get(i).getId()) {
                alarms.set(i, alarm);
                if (alarm.isActive()) {
                    AlarmHandler.scheduleAlarm(context, alarm, snackBarAnchor);
                } else {
                    AlarmHandler.cancelAlarm(context, alarm, snackBarAnchor);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return alarms == null ? 0 : alarms.size();
    }

    @Override
    public long getItemId(int position) {
        if (alarms == null) {
            return 0;
        }
        return alarms.get(position).getId();
    }

}
