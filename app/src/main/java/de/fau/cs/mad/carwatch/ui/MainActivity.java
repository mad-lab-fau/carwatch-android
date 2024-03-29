package de.fau.cs.mad.carwatch.ui;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashSet;
import java.util.Objects;

import de.fau.cs.mad.carwatch.BuildConfig;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmSoundControl;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeResultFragment;
import de.fau.cs.mad.carwatch.logger.GenericFileProvider;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.util.Utils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int[] NAV_IDS = {R.id.navigation_wakeup, R.id.navigation_alarm, R.id.navigation_bedtime, R.id.navigation_scanner};

    public static DiskLogAdapter sAdapter;

    private SharedPreferences sharedPreferences;

    private CoordinatorLayout coordinatorLayout;

    private NavController navController;

    private int clickCounter = 0;
    private static final int CLICK_THRESHOLD_TOAST = 2;
    private static final int CLICK_THRESHOLD_KILL = 5;

    private AlertDialog subjectIdDialog;
    private AlertDialog scanQrDialog;
    private AlertDialog openBatteryOptimizationDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        clickCounter = 0;

        // disable night mode per default
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

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

    @Override
    protected void onStart() {
        super.onStart();
        updateBottomNavigationBar();
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
        navController.navigate(R.id.navigation_wakeup);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!Utils.allPermissionsGranted(this)) {
            Utils.requestRuntimePermissions(this);
        }
        if (!sharedPreferences.getBoolean(Constants.PREF_REQUESTED_IGNORE_BATTERY_OPTIMIZATIONS, false) && !Utils.batteryOptimizationIgnored(this)) {
            openBatteryOptimizationDialog();
        } else if (sharedPreferences.getBoolean(Constants.PREF_FIRST_RUN_QR, true)) {
            // if user launched app for the first time (PREF_FIRST_RUN_QR) => display Dialog to scan study QR code
            showScanQrDialog();
        } else if (sharedPreferences.getBoolean(Constants.PREF_FIRST_RUN_SUBJECT_ID, true)) {
            // if user launched app for the first time (PREF_FIRST_RUN_SUBJECT_ID) => display Dialog to enter Subject ID
            showSubjectIdDialog();
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
                String studyName = sharedPreferences.getString(Constants.PREF_STUDY_NAME, null);
                String subjectId = sharedPreferences.getString(Constants.PREF_SUBJECT_ID, null);

                try {
                    File zipFile = LoggerUtil.zipDirectory(this, studyName, subjectId);
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
            case R.id.menu_scan:
                navigate(R.id.navigation_scanner);
                break;
            case R.id.menu_reregister:
                sharedPreferences.edit().clear().apply();
                Intent currIntent = getIntent();
                finish();
                startActivity(currIntent);
                break;
            case R.id.menu_app_info:
                showAppInfoDialog();
                break;


        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // dismiss optional dialogs to prevent leaking windows
        if (subjectIdDialog != null) {
            subjectIdDialog.dismiss();
        }
        if (scanQrDialog != null) {
            scanQrDialog.dismiss();
        }
        if (openBatteryOptimizationDialog != null) {
            openBatteryOptimizationDialog.dismiss();
        }
    }

    private void showSubjectIdDialog() {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final View dialogView = getLayoutInflater().inflate(R.layout.widget_subject_id_dialog, null);
        final EditText subjectIdEditText = dialogView.findViewById(R.id.edit_text_subject_id);


        subjectIdDialog = dialogBuilder
                .setCancelable(false)
                .setTitle(getString(R.string.title_subject_id))
                .setMessage(getString(R.string.message_subject_id))
                .setView(dialogView)
                .setPositiveButton(R.string.ok, null)
                .create();

        subjectIdDialog.setOnShowListener(dialog -> ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String subjectId = subjectIdEditText.getText().toString();

            // store subject id
            sharedPreferences.edit()
                    .putBoolean(Constants.PREF_FIRST_RUN_SUBJECT_ID, false)
                    .putString(Constants.PREF_SUBJECT_ID, subjectId)
                    .apply();
            try {
                JSONObject json = new JSONObject();
                json.put(Constants.LOGGER_EXTRA_SUBJECT_ID, subjectId);
                LoggerUtil.log(Constants.LOGGER_ACTION_SUBJECT_ID_SET, json);

                logAppPhoneMetadata();
                logStudyData();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (!sharedPreferences.getBoolean(Constants.PREF_FIRST_RUN_SUBJECT_ID, true)) {
                // if default settings were changed successfully => dismiss Dialog
                dialog.dismiss();
            }
        }));
        subjectIdDialog.show();
    }

    private void showScanQrDialog() {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        scanQrDialog =
                dialogBuilder
                        .setCancelable(false)
                        .setTitle(getString(R.string.title_scan_qr))
                        .setMessage(getString(R.string.message_scan_qr))
                        .setPositiveButton(R.string.ok, null)
                        .create();

        scanQrDialog.setOnShowListener(dialog -> ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            Intent intent = new Intent(this, QrActivity.class);
            startActivity(intent);

            if (!sharedPreferences.getBoolean(Constants.PREF_FIRST_RUN_QR, true)) {
                // if default settings were changed successfully => dismiss Dialog
                dialog.dismiss();
            }
        }));
        scanQrDialog.show();
    }

    private void openBatteryOptimizationDialog() {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        openBatteryOptimizationDialog = dialogBuilder
                .setCancelable(false)
                .setTitle(getString(R.string.title_battery_optimization))
                .setMessage(getString(R.string.message_battery_optimization))
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    Utils.openBatteryOptimizationSettings(this);
                    sharedPreferences.edit().putBoolean(Constants.PREF_REQUESTED_IGNORE_BATTERY_OPTIMIZATIONS, true).apply();
                    openBatteryOptimizationDialog.dismiss();
                })
                .create();
        openBatteryOptimizationDialog.show();
    }

    private void updateBottomNavigationBar() {
        // control bottom navigation view options
        boolean showScanItem = sharedPreferences.getBoolean(Constants.PREF_MANUAL_SCAN, false);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        Menu navMenu = navView.getMenu();
        navMenu.findItem(R.id.navigation_scanner).setVisible(showScanItem);
    }

    private void logAppPhoneMetadata() {
        try {
            // App Metadata – Version Code and Version Name
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_APP_VERSION_CODE, BuildConfig.VERSION_CODE);
            json.put(Constants.LOGGER_EXTRA_APP_VERSION_NAME, BuildConfig.VERSION_NAME);
            LoggerUtil.log(Constants.LOGGER_ACTION_APP_METADATA, json);

            // Phone Metadata – Brand, Manufacturer, Model, Android SDK level, Security Patch (if applicable), Build Release
            json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_PHONE_BRAND, Build.BRAND);
            json.put(Constants.LOGGER_EXTRA_PHONE_MANUFACTURER, Build.MANUFACTURER);
            json.put(Constants.LOGGER_EXTRA_PHONE_MODEL, Build.MODEL);
            json.put(Constants.LOGGER_EXTRA_PHONE_VERSION_SDK_LEVEL, Build.VERSION.SDK_INT);
            json.put(Constants.LOGGER_EXTRA_PHONE_VERSION_SECURITY_PATCH, Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Build.VERSION.SECURITY_PATCH : ""); // this
            json.put(Constants.LOGGER_EXTRA_PHONE_VERSION_RELEASE, Build.VERSION.RELEASE); // this

            LoggerUtil.log(Constants.LOGGER_ACTION_PHONE_METADATA, json);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void logStudyData() {
        try {
            // construct human-readable sample ids
            boolean hasEveningSalivette = sharedPreferences.getBoolean(Constants.PREF_HAS_EVENING, false);
            String salivaDistancesString = sharedPreferences.getString(Constants.PREF_SALIVA_DISTANCES, "");
            String salivaTimesString = sharedPreferences.getString(Constants.PREF_SALIVA_TIMES, "");
            int[] salivaDistances = Utils.decodeArrayFromString(salivaDistancesString);
            String[] salivaTimes = salivaTimesString.split(Constants.QR_PARSER_LIST_SEPARATOR);
            String startSample = sharedPreferences.getString(Constants.PREF_START_SAMPLE, "");
            String samplePrefix = startSample.substring(0, 1);
            int startSampleIdx = Integer.parseInt(startSample.substring(1));
            LinkedHashSet<String> salivaIds = new LinkedHashSet<>();
            for (int i = startSampleIdx; i < salivaDistances.length + startSampleIdx + salivaTimes.length; i++) {
                String sampleId = samplePrefix + i;
                salivaIds.add(sampleId);
            }
            if (hasEveningSalivette) {
                salivaIds.add(samplePrefix + Constants.EXTRA_SALIVA_ID_EVENING);
            }
            // log all relevant study data
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_STUDY_NAME, sharedPreferences.getString(Constants.PREF_STUDY_NAME, ""));
            json.put(Constants.LOGGER_EXTRA_NUM_SUBJECTS, sharedPreferences.getInt(Constants.PREF_NUM_SUBJECTS, 0));
            json.put(Constants.LOGGER_EXTRA_SALIVA_DISTANCES, salivaDistancesString);
            json.put(Constants.LOGGER_EXTRA_SALIVA_TIMES, salivaTimesString);
            json.put(Constants.LOGGER_EXTRA_STUDY_DAYS, sharedPreferences.getInt(Constants.PREF_NUM_DAYS, 0));
            json.put(Constants.LOGGER_EXTRA_SALIVA_IDS, salivaIds);
            json.put(Constants.LOGGER_EXTRA_HAS_EVENING_SALIVETTE, hasEveningSalivette);
            json.put(Constants.LOGGER_EXTRA_SHARE_EMAIL_ADDRESS, sharedPreferences.getString(Constants.PREF_SHARE_EMAIL_ADDRESS, ""));
            json.put(Constants.LOGGER_EXTRA_CHECK_DUPLICATES, sharedPreferences.getBoolean(Constants.PREF_CHECK_DUPLICATES, false));
            json.put(Constants.LOGGER_EXTRA_MANUAL_SCAN, sharedPreferences.getBoolean(Constants.PREF_MANUAL_SCAN, false));
            LoggerUtil.log(Constants.LOGGER_ACTION_STUDY_DATA, json);
        } catch (
                JSONException e) {
            e.printStackTrace();
        }
    }

    private void showAppInfoDialog() {
        AppInfoDialog dialog = new AppInfoDialog();
        dialog.show(getSupportFragmentManager(), "app_info");
    }

    public void showKillWarningDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(getString(R.string.title_kill_alarms))
                .setMessage(getString(R.string.message_kill_alarms))
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    AlarmHandler.killAll(getApplication());
                    AlarmSoundControl.getInstance().stopAlarmSound();
                    NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager != null)
                        notificationManager.cancelAll();
                })
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
        String extra_email = sharedPreferences.getString(Constants.PREF_SHARE_EMAIL_ADDRESS, "");
        sharingIntent.setType("application/octet-stream");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sharingIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{extra_email});
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, zipFile.getName());
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.title_share_dialog)));
    }
}
