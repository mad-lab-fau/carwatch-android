package de.fau.cs.mad.carwatch.ui.alarm;

import android.os.Handler;
import android.os.Looper;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import de.fau.cs.mad.carwatch.ui.CarwatchDialogBuilder;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.db.Alarm;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.ViewHolder> {
    private final List<Alarm> localAlarms = new ArrayList<>();
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

    public AlarmAdapter(AlarmViewModel alarmViewModel, String sampleIdPrefix, int startSampleid) {
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
        boolean sampleTaken = isSampleTaken(holder.itemView, item);
        int adjustedSampleId = item.getSalivaId() + startSampleId;
        String sampleName = sampleIdPrefix + adjustedSampleId + ":";
        holder.getSampleNameTextView().setText(sampleName);
        setSwitchProperties(holder, item, sampleTaken);
        holder.getAlarmTextView().setText(item.getStringTime());
        holder.getAlarmTextView().setTextColor(ContextCompat.getColor(holder.itemView.getContext(), colorId));
        setScanClickProperties(holder, item, sampleTaken);
        setIconProperties(holder, item, sampleTaken);
        setIconAlignment(holder, item.getStringTime());
    }

    @Override
    public int getItemCount() {
        return localAlarms.size();
    }

    public void setAlarms(List<Alarm> alarms) {
        localAlarms.clear();
        localAlarms.addAll(alarms);
        Collections.sort(localAlarms, (left, right) -> {
            int leftMinuteOfDay = getMinuteOfDay(left);
            int rightMinuteOfDay = getMinuteOfDay(right);
            if (leftMinuteOfDay != rightMinuteOfDay) {
                return Integer.compare(leftMinuteOfDay, rightMinuteOfDay);
            }

            return Integer.compare(left.getId(), right.getId());
        });
    }

    private static int getMinuteOfDay(Alarm alarm) {
        DateTime time = alarm.getTime();
        return time.getHourOfDay() * 60 + time.getMinuteOfHour();
    }

    private void setIconAlignment(@NonNull ViewHolder holder, @NonNull String alarmTime) {
        boolean isSingleDigitTime = alarmTime.length() > 1
                && Character.isDigit(alarmTime.charAt(0))
                && alarmTime.charAt(1) == ':';
        int margin = holder.itemView.getResources().getDimensionPixelSize(
                isSingleDigitTime
                        ? R.dimen.sample_suffix_icon_margin_start_single_digit_time
                        : R.dimen.sample_suffix_icon_margin_start
        );
        setStartMargin(holder.getScannerIcon(), margin);
        setStartMargin(holder.getCheckIcon(), margin);
    }

    private void setStartMargin(@NonNull View view, int margin) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        layoutParams.setMarginStart(margin);
        view.setLayoutParams(layoutParams);
    }

    private void setSwitchProperties(ViewHolder holder, Alarm item, boolean sampleTaken) {
        SwitchMaterial alarmSwitch = holder.getAlarmSwitch();

        alarmSwitch.setChecked(item.isActive());
        boolean isLater = DateTime.now().isBefore(item.getTime());
        boolean isEveningReminder = item.getId() == Constants.EXTRA_ALARM_ID_EVENING;
        alarmSwitch.setEnabled((isEveningReminder || isLater) && !sampleTaken);
        if (isLater && !isEveningReminder) {
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> alarmSwitch.setEnabled(false),
                    item.getTime().getMillis() - DateTime.now().getMillis()
            );
        }
        alarmSwitch.setOnClickListener(view -> {
            if (item.isActive()) {
                deactivateAlarm(view, holder, item);
            } else {
                activateAlarm(view, holder, item);
            }
        });
    }

    private void setScanClickProperties(ViewHolder holder, Alarm item, boolean sampleTaken) {
        holder.itemView.setClickable(!sampleTaken);
        holder.itemView.setOnClickListener(sampleTaken
                ? null
                : view -> AlarmViewFunctionalities.requestOpenBarcodeScanner(view.getContext(), item));
    }

    private void setIconProperties(@NonNull ViewHolder holder, @NonNull Alarm alarm, boolean sampleTaken) {
        int checkVisibility = sampleTaken ? View.VISIBLE : View.GONE;
        int scannerVisibility = sampleTaken ? View.GONE : View.VISIBLE;
        int statusIconVisibility = sampleTaken ? View.GONE : View.VISIBLE;
        holder.getCheckIcon().setVisibility(checkVisibility);
        holder.getScannerIcon().setVisibility(scannerVisibility);
        holder.getScannerIcon().setOnClickListener(null);
        holder.getScannerIcon().setClickable(false);
        holder.getSampleStatusIcon().setVisibility(statusIconVisibility);
        boolean isBeforeSampleTime = DateTime.now().isBefore(alarm.getTime());
        int src = isBeforeSampleTime ? R.drawable.ic_hourglass : R.drawable.ic_pending;
        holder.getSampleStatusIcon().setImageResource(src);

        if (isBeforeSampleTime) {
            switchIconAfterAlarm(holder.getSampleStatusIcon(), alarm.getTimeToNextRing());
        }
    }

    private boolean isSampleTaken(@NonNull View view, @NonNull Alarm alarm) {
        if (alarm.wasSampleTaken()) {
            return true;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(view.getContext());
        int dayId = sharedPreferences.getInt(Constants.PREF_DAY_COUNTER, 1);
        int startIndex = getStartSampleIndex(sharedPreferences);
        Set<String> scannedBarcodes = sharedPreferences.getStringSet(Constants.PREF_SCANNED_BARCODES, Collections.emptySet());

        for (String barcode : scannedBarcodes) {
            if (barcode == null || barcode.length() < 7) {
                continue;
            }

            try {
                int scannedDay = Integer.parseInt(barcode.substring(3, 5));
                int scannedSampleId = Integer.parseInt(barcode.substring(5, 7)) - startIndex;
                if (scannedDay == dayId && scannedSampleId == alarm.getSalivaId()) {
                    return true;
                }
            } catch (NumberFormatException e) {
                // Ignore malformed legacy barcode entries.
            }
        }

        return false;
    }

    private int getStartSampleIndex(SharedPreferences sharedPreferences) {
        String startSample = sharedPreferences.getString(Constants.PREF_START_SAMPLE, Constants.DEFAULT_START_SAMPLE);
        try {
            return Integer.parseInt(startSample.substring(1));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return Integer.parseInt(Constants.DEFAULT_START_SAMPLE.substring(1));
        }
    }

    private void switchIconAfterAlarm(ImageView sampleStatusIcon, DateTime timeToNextRing) {
        long delay = timeToNextRing.getMillis() - DateTime.now().getMillis();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (sampleStatusIcon != null)
                sampleStatusIcon.setImageResource(R.drawable.ic_pending);
        }, delay);
    }

    private void deactivateAlarm(View view, ViewHolder holder, Alarm alarm) {
        AlertDialog.Builder dialogBuilder = new CarwatchDialogBuilder(view.getContext());
        AlertDialog dialog = dialogBuilder
                .setIcon(R.drawable.ic_warning_24dp)
                .setTitle(R.string.warning_title)
                .setMessage(R.string.cancel_saliva_alarm_message)
                .setNegativeButton(R.string.no, (dialogInterface, i) -> holder.getAlarmSwitch().setChecked(true))
                .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                    alarm.setActive(false);
                    AlarmHandler.cancelAlarm(view.getContext(), alarm, view);
                    holder.getAlarmTextView().setTextColor(ContextCompat.getColor(view.getContext(), R.color.colorGrey500));
                    alarmViewModel.update(alarm);
                })
                .create();
        dialog.show();
    }

    private void activateAlarm(View view, ViewHolder holder, Alarm alarm) {
        alarm.setActive(true);
        AlarmHandler.scheduleSalivaAlarm(view.getContext(), alarm, view);
        holder.getAlarmTextView().setTextColor(ContextCompat.getColor(view.getContext(), R.color.colorAccent));
        alarmViewModel.update(alarm);
    }
}
