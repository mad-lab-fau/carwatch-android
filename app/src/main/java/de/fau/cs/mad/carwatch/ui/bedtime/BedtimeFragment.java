package de.fau.cs.mad.carwatch.ui.bedtime;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.TimerHandler;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.MainActivity;
import de.fau.cs.mad.carwatch.ui.ScannerActivity;

public class BedtimeFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = BedtimeFragment.class.getSimpleName();

    private Button yesButton;
    private Button noButton;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_bedtime, container, false);

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
                    json.put(Constants.LOGGER_EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_EVENING);
                    LoggerUtil.log(Constants.LOGGER_ACTION_EVENING_SALIVETTE, json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                showBedtimeDialog();
                break;
        }
    }


    private void showBedtimeDialog() {
        Drawable icon = getResources().getDrawable(R.drawable.ic_bedtime_24dp);
        icon.setTint(getResources().getColor(R.color.colorPrimary));

        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.evening_title))
                .setCancelable(false)
                .setIcon(icon)
                .setMessage(getString(R.string.evening_text))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    TimerHandler.scheduleSalivaTimer(getContext(), Constants.EXTRA_ALARM_ID_EVENING, Constants.EXTRA_SALIVA_ID_EVENING);

                    Intent intent = new Intent(getContext(), ScannerActivity.class);
                    intent.putExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_EVENING);
                    intent.putExtra(Constants.EXTRA_SALIVA_ID, Constants.EXTRA_SALIVA_ID_EVENING);
                    startActivityForResult(intent, Constants.REQUEST_CODE_SCAN);
                })
                .show();
    }
}