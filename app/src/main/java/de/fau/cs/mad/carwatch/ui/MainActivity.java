package de.fau.cs.mad.carwatch.ui;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmSoundControl;
import de.fau.cs.mad.carwatch.logger.GenericFileProvider;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.onboarding.SlideShowActivity;
import de.fau.cs.mad.carwatch.util.Utils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int[] NAV_IDS = {R.id.navigation_wakeup, R.id.navigation_alarm, R.id.navigation_bedtime};

    private static DiskLogAdapter sAdapter;

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

        // disable night mode per default
        AppCompatDelegate delegate = getDelegate();
        AppCompatDelegate.setDefaultNightMode(sharedPreferences.getBoolean(Constants.PREF_NIGHT_MODE_ENABLED, false) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        delegate.applyDayNight();

        initializeLoggingUtil(this);

        if (sharedPreferences.getInt(Constants.PREF_CURRENT_SLIDE_SHOW_SLIDE, Constants.INITIAL_SLIDE_SHOW_SLIDE) != Constants.SLIDESHOW_FINISHED_SLIDE_ID) {
            Intent intent = new Intent(this, SlideShowActivity.class);
            startActivity(intent);
            finish();
        }

        clickCounter = 0;

        coordinatorLayout = findViewById(R.id.coordinator);

        BottomNavigationView navView = findViewById(R.id.nav_view);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(NAV_IDS).build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        int currentNavElement = sharedPreferences.getInt(Constants.PREF_CURRENT_NAV_ELEMENT, NAV_IDS[0]);
        navigate(currentNavElement);

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);


        if (getIntent() != null && getIntent().getBooleanExtra(Constants.EXTRA_SHOW_BARCODE_SCANNED_MSG, false)) {
            Snackbar.make(coordinatorLayout, getString(R.string.message_barcode_scanned_successfully), Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (navController != null && navController.getCurrentDestination() != null)
            sharedPreferences.edit().putInt(Constants.PREF_CURRENT_NAV_ELEMENT, navController.getCurrentDestination().getId()).apply();
    }

    @Override
    protected void onStop() {
        super.onStop();
        sharedPreferences.edit().putInt(Constants.PREF_CURRENT_NAV_ELEMENT, NAV_IDS[0]).apply();
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

    public static void initializeLoggingUtil(Context context) {
        if (sAdapter != null)
            return;
        sAdapter = new DiskLogAdapter(LoggerUtil.getFormatStrategy(context)) {
            @Override
            public boolean isLoggable(int priority, @Nullable String tag) {
                return true;
            }
        };
        Logger.addLogAdapter(sAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!Utils.allPermissionsGranted(this)) {
            Utils.requestRuntimePermissions(this);
        }
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
                String participantId = sharedPreferences.getString(Constants.PREF_PARTICIPANT_ID, null);

                try {
                    File zipFile = LoggerUtil.zipDirectory(this, studyName, participantId);
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
            case R.id.menu_reregister:
                sharedPreferences.edit().clear().apply();
                Intent intent = new Intent(this, SlideShowActivity.class);
                intent.putExtra(Constants.EXTRA_SLIDE_SHOW_TYPE, SlideShowActivity.SHOW_APP_INITIALIZATION_SLIDES);
                startActivity(intent);
                finish();
                break;
            case R.id.menu_show_tutorial:
                sharedPreferences.edit().putInt(Constants.PREF_CURRENT_SLIDE_SHOW_SLIDE, Constants.INITIAL_SLIDE_SHOW_SLIDE).apply();
                Intent tutorialIntent = new Intent(this, SlideShowActivity.class);
                tutorialIntent.putExtra(Constants.EXTRA_SLIDE_SHOW_TYPE, SlideShowActivity.SHOW_TUTORIAL_SLIDES);
                startActivity(tutorialIntent);
                break;
            case R.id.menu_app_info:
                showAppInfoDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
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
