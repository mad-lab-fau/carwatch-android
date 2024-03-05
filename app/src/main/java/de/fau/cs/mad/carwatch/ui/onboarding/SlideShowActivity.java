package de.fau.cs.mad.carwatch.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.databinding.Observable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.ui.MainActivity;
import de.fau.cs.mad.carwatch.ui.barcode.QrFragment;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.ParticipantIdQuery;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.PermissionRequest;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.TutorialSlide;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.WelcomeSlide;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.WelcomeText;

public class SlideShowActivity extends AppCompatActivity {

    public static final String TAG = SlideShowActivity.class.getSimpleName();

    private final List<WelcomeSlide> slides = new ArrayList<>();
    private int currentSlidePosition = 0;
    private SharedPreferences sharedPreferences;
    private Button skipButton;
    private Button nextButton;
    private TabLayout tabDots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_show);
        tabDots = findViewById(R.id.tab_dots);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setColorScheme();
        initializeLoggingUtil();
        initializeSlides();
        initializeSkipButton();
        initializeNextButton();
    }

    private void initializeLoggingUtil() {
        MainActivity.initializeLoggingUtil(this);
    }

    private void setColorScheme() {
        AppCompatDelegate delegate = getDelegate();
        boolean enableNightMode = sharedPreferences.getBoolean(Constants.PREF_NIGHT_MODE_ENABLED, false);
        int colorScheme = enableNightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(colorScheme);
        delegate.applyDayNight();
    }

    private void initializeSkipButton() {
        skipButton = findViewById(R.id.btn_skip_slides);
        skipButton.setOnClickListener(v -> finishSlideShow());
    }

    private void initializeSlides() {
        addSlide(new WelcomeText());
        addSlide(new PermissionRequest());
        addSlide(new QrFragment());

        boolean manualScanEnabled = sharedPreferences.getBoolean(Constants.PREF_MANUAL_SCAN, true);
        int wakeupScreenImageId = manualScanEnabled ? R.drawable.img_screenshot_wakeup_screen_extended_menu : R.drawable.img_screenshot_wakeup_screen_small_menu;
        int alarmScreenImageId = manualScanEnabled ? R.drawable.img_screenshot_alarm_screen_extended_menu : R.drawable.img_screenshot_alarm_screen_small_menu;
        int bedtimeScreenImageId = manualScanEnabled ? R.drawable.img_screenshot_bedtime_screen_extended_menu : R.drawable.img_screenshot_bedtime_screen_small_menu;
        int scanScreenImageId = manualScanEnabled ? R.drawable.img_screenshot_barcode_cam_extended_menu : R.drawable.img_screenshot_barcode_cam_no_nav_bar;

        String wakeupScreenHeadline = getString(R.string.headline_wakeup_screen_tutorial);
        String alarmScreenHeadline = getString(R.string.headline_alarm_tutorial);
        String bedtimeScreenHeadline = getString(R.string.headline_bedtime_screen_tutorial);
        String scanScreenHeadline = getString(R.string.headline_scan_screen_tutorial);
        String wakeupScreenDescription = getString(R.string.description_wakeup_screen_tutorial);
        String alarmScreenDescription = getString(R.string.description_alarm_tutorial);
        String bedtimeScreenDescription = getString(R.string.description_bedtime_screen_tutorial);
        String scanScreenDescription = getString(R.string.description_scan_screen_tutorial);
        if (manualScanEnabled)
            scanScreenDescription += " " + getString(R.string.scan_screen_navigation_hint);

        addSlide(TutorialSlide.newInstance(wakeupScreenHeadline, wakeupScreenDescription, wakeupScreenImageId));
        addSlide(TutorialSlide.newInstance(alarmScreenHeadline, alarmScreenDescription, alarmScreenImageId));
        addSlide(TutorialSlide.newInstance(bedtimeScreenHeadline, bedtimeScreenDescription, bedtimeScreenImageId));
        addSlide(TutorialSlide.newInstance(scanScreenHeadline, scanScreenDescription, scanScreenImageId));

        highlightDot(currentSlidePosition);
    }


    private void initializeNextButton() {
        nextButton = findViewById(R.id.btn_next_slide);
        nextButton.setOnClickListener(v -> nextSlide());
    }

    private void nextSlide() {
        WelcomeSlide currentSlide = slides.get(currentSlidePosition);
        currentSlide.onSlideFinished();

        int qrCodeSlidePos = 2;
        if (currentSlidePosition == qrCodeSlidePos && !sharedPreferences.contains(Constants.PREF_SUBJECT_ID)) {
            addSlide(currentSlidePosition + 1, new ParticipantIdQuery());
        }

        if (currentSlidePosition >= slides.size() - 1) {
            finishSlideShow();
            return;
        }

        currentSlidePosition++;
        WelcomeSlide nextSlide = slides.get(currentSlidePosition);
        initButtonsForSlide(nextSlide);
        replaceFragment(nextSlide.getFragment());
        highlightDot(currentSlidePosition);
    }

    private void initButtonsForSlide(WelcomeSlide slide) {
        setSkipButtonVisibility(slide.getSkipButtonIsVisible().get());
        nextButton.setEnabled(slide.getNextButtonIsEnabled().get());
        slide.getSkipButtonIsVisible().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                setSkipButtonVisibility(slide.getSkipButtonIsVisible().get());
            }
        });
        slide.getNextButtonIsEnabled().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                nextButton.setEnabled(slide.getNextButtonIsEnabled().get());
            }
        });
    }

    private void addSlide(WelcomeSlide slide) {
        addSlide(slides.size(), slide);
    }

    private void addSlide(int position, WelcomeSlide slide) {
        slides.add(position, slide);
        tabDots.addTab(tabDots.newTab(), position);
        prepareTabDot(position);
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.slide_show_fragment, fragment);
        transaction.commit();
    }

    private void highlightDot(int position) {
        TabLayout.Tab tab = tabDots.getTabAt(position);
        if (tab != null) {
            tab.select();
        }
    }

    private void prepareTabDot(int position) {
        LinearLayout tabStrip = ((LinearLayout) tabDots.getChildAt(0));
        View tab = tabStrip.getChildAt(position);

        // disable onclick
        tab.setOnTouchListener((v, event) -> true);

        // decrease space between dots
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tab.getLayoutParams();
        params.weight = 0;
        params.width = 15;
        params.setMarginStart(10);
        params.setMarginEnd(10);
        tab.setLayoutParams(params);
        tabDots.requestLayout();
    }

    private void finishSlideShow() {
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);
        finish();
    }

    private void setSkipButtonVisibility(boolean isVisible) {
        skipButton.setVisibility(isVisible ? Button.VISIBLE : Button.INVISIBLE);
    }

    private String currentSlideName() {
        return slides.get(currentSlidePosition).getClass().getSimpleName();
    }
}