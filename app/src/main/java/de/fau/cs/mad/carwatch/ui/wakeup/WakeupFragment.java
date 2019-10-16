package de.fau.cs.mad.carwatch.ui.wakeup;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.json.JSONException;
import org.json.JSONObject;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.alarmmanager.TimerHandler;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.MainActivity;
import de.fau.cs.mad.carwatch.ui.ScannerActivity;
import de.fau.cs.mad.carwatch.userpresent.UserPresentService;

public class WakeupFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = WakeupFragment.class.getSimpleName();

    private Button yesButton;
    private Button noButton;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_wakeup, container, false);

        yesButton = root.findViewById(R.id.button_yes);
        noButton = root.findViewById(R.id.button_no);

        yesButton.setOnClickListener(this);
        noButton.setOnClickListener(this);

        return root;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_no:
                if (getActivity() != null) {
                    Snackbar.make(getActivity().findViewById(R.id.coordinator), getString(R.string.feedback_thanks), Snackbar.LENGTH_SHORT).show();
                    ((MainActivity) getActivity()).navigate(R.id.navigation_alarm);
                }
                break;
            case R.id.button_yes:
                // create Json object and log information
                try {
                    JSONObject json = new JSONObject();
                    json.put(Constants.LOGGER_EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_SPONTANEOUS);
                    LoggerUtil.log(Constants.LOGGER_ACTION_SPONTANEOUS_AWAKENING, json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
                DateTime date = new DateTime(sp.getLong(Constants.PREF_MORNING_TAKEN, 0));
                if (date.equals(LocalTime.MIDNIGHT.toDateTimeToday())) {
                    showWakeupWarningDialog();
                } else {
                    showWakeupDialog();
                }
                break;
        }
    }


    private void showWakeupDialog() {
        if (getContext() == null) {
            return;
        }
        Drawable icon = getResources().getDrawable(R.drawable.ic_wakeup_24dp);
        icon.setTint(getResources().getColor(R.color.colorPrimary));

        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.wakeup_title))
                .setCancelable(false)
                .setIcon(icon)
                .setMessage(getString(R.string.wakeup_text))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    if (UserPresentService.serviceRunning) {
                        UserPresentService.stopService(getContext());
                    }

                    TimerHandler.scheduleSalivaTimer(getContext(), Constants.EXTRA_ALARM_ID_SPONTANEOUS, Constants.EXTRA_SALIVA_ID_DEFAULT);

                    Intent intent = new Intent(getContext(), ScannerActivity.class);
                    intent.putExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_SPONTANEOUS);
                    intent.putExtra(Constants.EXTRA_SALIVA_ID, Constants.EXTRA_SALIVA_ID_DEFAULT);
                    startActivityForResult(intent, Constants.REQUEST_CODE_SCAN);
                })
                .show();
    }

    private void showWakeupWarningDialog() {
        if (getContext() == null) {
            return;
        }
        Drawable icon = getResources().getDrawable(R.drawable.ic_warning_24dp);
        icon.setTint(getResources().getColor(R.color.colorPrimary));

        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.warning_title))
                .setCancelable(false)
                .setIcon(icon)
                .setMessage(getString(R.string.warning_already_taken_wakeup))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                })
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (getActivity() == null || data == null) {
            return;
        }

        if (requestCode == Constants.REQUEST_CODE_SCAN) {
            if (resultCode == Activity.RESULT_OK) {
                long alarmTime = data.getLongExtra(Constants.EXTRA_ALARM_TIME, 0);
                DateTime time = new DateTime(alarmTime);
                AlarmHandler.showAlarmSetMessage(getContext(), getActivity().findViewById(R.id.coordinator), time);
            }
        }
    }
}