package de.fau.cs.mad.carwatch.ui.alarm;

import android.content.res.Resources;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.db.Alarm;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.ViewHolder> {
    private final List<Alarm> localAlarms = new ArrayList<>();
    private final Resources resources;
    private final AlarmViewModel alarmViewModel;
    private final String sampleIdPrefix;
    private final int startSampleId;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final SwitchMaterial alarmSwitch;
        private final TextView sampleNameTextView;
        private final TextView alarmTextView;
        private final ImageView scannerIcon;
        private final ImageView checkIcon;
        private final ImageView sampleStatusIcon;

        public ViewHolder(View view) {
            super(view);

            alarmSwitch = view.findViewById(R.id.alarm_active_switch);
            sampleNameTextView = view.findViewById(R.id.tv_sample_name);
            alarmTextView = view.findViewById(R.id.alarm_time_text);
            scannerIcon = view.findViewById(R.id.iv_scanner_icon);
            checkIcon = view.findViewById(R.id.iv_check_icon);
            sampleStatusIcon = view.findViewById(R.id.iv_sample_status_icon);
        }

        public SwitchMaterial getAlarmSwitch() {
            return alarmSwitch;
        }

        public TextView getAlarmTextView() {
            return alarmTextView;
        }

        public ImageView getScannerIcon() {
            return scannerIcon;
        }

        public ImageView getCheckIcon() {
            return checkIcon;
        }

        public TextView getSampleNameTextView() {
            return sampleNameTextView;
        }

        public ImageView getSampleStatusIcon() {
            return sampleStatusIcon;
        }
    }

    public AlarmAdapter(Resources resources, AlarmViewModel alarmViewModel, String sampleIdPrefix, int startSampleid) {
        this.resources = resources;
        this.alarmViewModel = alarmViewModel;
        this.sampleIdPrefix = sampleIdPrefix;
        this.startSampleId = startSampleid;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Alarm item = localAlarms.get(position);
        int colorId = item.isActive() ? R.color.colorAccent : R.color.colorGrey500;
        int adjustedSampleId = item.getSalivaId() + startSampleId;
        String sampleName = sampleIdPrefix + adjustedSampleId + ":";
        setIconProperties(holder, item);
        holder.getSampleNameTextView().setText(sampleName);
        holder.getAlarmTextView().setText(item.getStringTime());
        holder.getAlarmTextView().setTextColor(resources.getColor(colorId));
        holder.getAlarmSwitch().setChecked(item.isActive());
        holder.getAlarmSwitch().setEnabled(!item.wasSampleTaken());
        holder.getAlarmSwitch().setOnClickListener(view -> {
            if (item.isActive()) {
                deactivateAlarm(view, holder, item);
            } else {
                activateAlarm(view, holder, item);
            }
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

    private void setIconProperties(@NonNull ViewHolder holder, @NonNull Alarm alarm) {
        int checkVisibility = alarm.wasSampleTaken() ? View.VISIBLE : View.GONE;
        int scannerVisibility = alarm.wasSampleTaken() ? View.GONE : View.VISIBLE;
        int statusIconVisibility = alarm.wasSampleTaken() ? View.GONE : View.VISIBLE;
        holder.getCheckIcon().setVisibility(checkVisibility);
        holder.getScannerIcon().setVisibility(scannerVisibility);
        holder.getScannerIcon().setOnClickListener(view -> AlarmViewFunctionalities.requestOpenBarcodeScanner(view.getContext(), alarm));
        holder.getSampleStatusIcon().setVisibility(statusIconVisibility);
        boolean isBeforeSampleTime = DateTime.now().isBefore(alarm.getTime());
        int src = isBeforeSampleTime ? R.drawable.ic_hourglass : R.drawable.ic_pending;
        holder.getSampleStatusIcon().setImageResource(src);

        if (isBeforeSampleTime) {
            switchIconAfterAlarm(holder.getSampleStatusIcon(), alarm.getTimeToNextRing());
        }
    }

    private void switchIconAfterAlarm(ImageView sampleStatusIcon, DateTime timeToNextRing) {
        long delay = timeToNextRing.getMillis() - DateTime.now().getMillis();
        new Handler().postDelayed(() -> {
            if (sampleStatusIcon != null)
                sampleStatusIcon.setImageResource(R.drawable.ic_pending);
        }, delay);
    }

    private void deactivateAlarm(View view, ViewHolder holder, Alarm alarm) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(view.getContext());
        AlertDialog dialog = dialogBuilder
                .setTitle(R.string.warning_title)
                .setMessage(R.string.cancel_fixed_alarm_message)
                .setNegativeButton(R.string.no, (dialogInterface, i) -> {
                    holder.getAlarmSwitch().setChecked(true);
                })
                .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                    alarm.setActive(false);
                    AlarmHandler.cancelAlarm(view.getContext(), alarm, view);
                    holder.getAlarmTextView().setTextColor(resources.getColor(R.color.colorGrey500));
                    alarmViewModel.update(alarm);
                })
                .create();
        dialog.show();
    }

    private void activateAlarm(View view, ViewHolder holder, Alarm alarm) {
        alarm.setActive(true);
        AlarmHandler.scheduleSalivaAlarm(view.getContext(), alarm, view);
        holder.getAlarmTextView().setTextColor(resources.getColor(R.color.colorAccent));
        alarmViewModel.update(alarm);
    }
}
