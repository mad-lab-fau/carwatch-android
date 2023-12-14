package de.fau.cs.mad.carwatch.ui.alarm;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.db.Alarm;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.ViewHolder> {
    private final List<Alarm> localAlarms = new ArrayList<>();
    private final Resources resources;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final SwitchMaterial alarmSwitch;
        private final TextView alarmTextView;
        public ViewHolder(View view) {
            super(view);

            alarmSwitch = view.findViewById(R.id.alarm_active_switch);
            alarmTextView = view.findViewById(R.id.alarm_time_text);
        }

        public SwitchMaterial getAlarmSwitch() {
            return alarmSwitch;
        }

        public TextView getAlarmTextView() {
            return alarmTextView;
        }
    }

    public AlarmAdapter(Resources resources) {
        this.resources = resources;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fixed_alarm, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Alarm item = localAlarms.get(position);
        int colorId = item.isActive() ? R.color.colorAccent : R.color.colorGrey500;
        holder.getAlarmTextView().setText(item.getStringTime());
        holder.getAlarmTextView().setTextColor(resources.getColor(colorId));
        holder.getAlarmSwitch().setChecked(item.isActive());
        holder.getAlarmSwitch().setOnClickListener(view -> {
            // TODO trigger warning pop-up
            item.setActive(!item.isActive());
            int textColorId = item.isActive() ? R.color.colorAccent : R.color.colorGrey500;
            holder.getAlarmTextView().setTextColor(resources.getColor(textColorId));
            // TODO schedule alarm
        });
    }

    @Override
    public int getItemCount() {
        return localAlarms.size();
    }

    public void setAlarms(List<Alarm> alarms) {
        localAlarms.clear();
        localAlarms.addAll(alarms);
    }
}
