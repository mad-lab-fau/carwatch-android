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
        tabDots.addTab(tabDots.newTab());
        disableTabDotOnClick(position);
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

    private void disableTabDotOnClick(int position) {
        LinearLayout tabStrip = ((LinearLayout) tabDots.getChildAt(0));
        tabStrip.getChildAt(position).setOnTouchListener((v, event) -> true);
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