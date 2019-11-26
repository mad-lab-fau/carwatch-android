package de.fau.cs.mad.carwatch.ui;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.orhanobut.logger.DiskLogAdapter;
import com.orhanobut.logger.Logger;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;

import de.fau.cs.mad.carwatch.BuildConfig;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeResultFragment;
import de.fau.cs.mad.carwatch.logger.GenericFileProvider;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.subject.SubjectIdCheck;
import de.fau.cs.mad.carwatch.subject.SubjectMap;
import de.fau.cs.mad.carwatch.util.Utils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int[] NAV_IDS = {R.id.navigation_wakeup, R.id.navigation_alarm, R.id.navigation_bedtime};

    public static DiskLogAdapter sAdapter;

    private SharedPreferences sharedPreferences;

    private CoordinatorLayout coordinatorLayout;

    private NavController navController;

    private int clickCounter = 0;
    private static final int CLICK_THRESHOLD_TOAST = 2;
    private static final int CLICK_THRESHOLD_KILL = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences.getBoolean(Constants.PREF_FIRST_RUN, true)) {
            try {
                JSONObject json = new JSONObject();
                json.put(Constants.LOGGER_EXTRA_VERSION_CODE, BuildConfig.VERSION_CODE);
                LoggerUtil.log(Constants.LOGGER_ACTION_APP_VERSION, json);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // if user launched app for the first time (PREF_FIRST_RUN) => display Dialog to enter Subject ID
            showSubjectIdDialog();
        }

        clickCounter = 0;

        // disable night mode per default
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        AppCompatDelegate delegate = getDelegate();
        AppCompatDelegate.setDefaultNightMode(sharedPreferences.getBoolean(Constants.PREF_NIGHT_MODE_ENABLED, false) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        delegate.applyDayNight();

        coordinatorLayout = findViewById(R.id.coordinator);

        BottomNavigationView navView = findViewById(R.id.nav_view);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(NAV_IDS).build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        navigate();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        if (sAdapter == null) {
            sAdapter = new DiskLogAdapter(LoggerUtil.getFormatStrategy(this)) {
                @Override
                public boolean isLoggable(int priority, @Nullable String tag) {
                    return true;
                }
            };
            Logger.addLogAdapter(sAdapter);
        }
    }

    public void navigate(int navId) {
        for (int id : NAV_IDS) {
            if (id == navId) {
                navController.navigate(navId);
                return;
            }
        }
    }

    public void navigate() {
        if (checkInterval(DateTime.now(), Constants.MORNING_TIMES)) {
            navController.navigate(R.id.navigation_wakeup);
        } else if (checkInterval(DateTime.now(), Constants.EVENING_TIMES)) {
            navController.navigate(R.id.navigation_bedtime);
        } else {
            navController.navigate(R.id.navigation_alarm);
        }
    }


    private boolean checkInterval(DateTime time, DateTime[] interval) {
        return new Interval(interval[0], interval[1]).contains(time);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Utils.allPermissionsGranted(this)) {
            Utils.requestRuntimePermissions(this);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        BarcodeResultFragment.dismiss(getSupportFragmentManager());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_share:
                String subjectId = sharedPreferences.getString(Constants.PREF_SUBJECT_ID, null);

                try {
                    File zipFile = LoggerUtil.zipDirectory(this, subjectId);
                    createFileShareDialog(zipFile);
                } catch (FileNotFoundException e) {
                    Snackbar.make(coordinatorLayout, Objects.requireNonNull(e.getMessage()), Snackbar.LENGTH_SHORT).show();
                }
                break;
            case R.id.menu_kill:
                clickCounter++;
                if (clickCounter >= CLICK_THRESHOLD_KILL) {
                    showKillWarningDialog();
                    clickCounter = 0;
                } else if (clickCounter >= CLICK_THRESHOLD_TOAST) {
                    Snackbar.make(coordinatorLayout, getString(R.string.hint_clicks_kill_alarms, (CLICK_THRESHOLD_KILL - clickCounter)), Snackbar.LENGTH_SHORT).show();
                }
                break;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            /*case R.id.menu_scan:
                startActivity(new Intent(this, BarcodeActivity.class));
                break;
            */

        }
        return super.onOptionsItemSelected(item);
    }


    @SuppressLint("InflateParams")
    private void showSubjectIdDialog() {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final View dialogView = getLayoutInflater().inflate(R.layout.widget_subject_id_dialog, null);
        final EditText editText = dialogView.findViewById(R.id.edit_text_subject_id);
        editText.setText(sharedPreferences.getString(Constants.PREF_SUBJECT_ID, ""));

        AlertDialog warningDialog =
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(getString(R.string.title_invalid_subject_id))
                        .setMessage(getString(R.string.message_invalid_subject_id))
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                        })
                        .create();

        AlertDialog subjectIdDialog =
                dialogBuilder
                        .setCancelable(false)
                        .setTitle(getString(R.string.title_subject_id))
                        .setMessage(getString(R.string.message_subject_id))
                        .setView(dialogView)
                        .setPositiveButton(R.string.ok, null)
                        .create();

        subjectIdDialog.setOnShowListener(dialog -> ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String subjectId = editText.getText().toString().toUpperCase();

            if (SubjectIdCheck.isValidSubjectId(subjectId)) {
                sharedPreferences.edit()
                        .putBoolean(Constants.PREF_FIRST_RUN, false)
                        .putString(Constants.PREF_SUBJECT_ID, subjectId)
                        .putInt(Constants.PREF_DAY_COUNTER, 0)
                        .apply();

                try {
                    JSONObject json = new JSONObject();
                    json.put(Constants.LOGGER_EXTRA_SUBJECT_ID, subjectId);
                    json.put(Constants.LOGGER_EXTRA_SUBJECT_CONDITION, SubjectMap.getConditionForSubject(subjectId));
                    LoggerUtil.log(Constants.LOGGER_ACTION_SUBJECT_ID_SET, json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                dialog.dismiss();
            } else {
                warningDialog.show();
            }
        }));

        subjectIdDialog.show();
    }

    public void showKillWarningDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(getString(R.string.title_kill_alarms))
                .setMessage(getString(R.string.message_kill_alarms))
                .setPositiveButton(R.string.yes, (dialog, which) -> AlarmHandler.killAll(getApplication()))
                .setNegativeButton(R.string.cancel, ((dialog, which) -> {
                }))
                .show();
    }

    private void createFileShareDialog(File zipFile) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        Uri uri = GenericFileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() +
                        ".logger.fileprovider",
                zipFile);
        sharingIntent.setType("application/octet-stream");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sharingIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{Constants.SHARE_EMAIL_ADDRESS});
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, zipFile.getName());
        startActivity(Intent.createChooser(sharingIntent, "Share Logs via..."));
    }

}
