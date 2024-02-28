package de.fau.cs.mad.carwatch.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setColorScheme();
        initializeLoggingUtil();
        initializeSlides();
        initializeSkipButton();
        initializeNextButton();
        initializeCurrentSlideIndicator();
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
        slides.add(new WelcomeText());
        slides.add(new PermissionRequest());
        slides.add(new QrFragment());
    }


    private void initializeNextButton() {
        nextButton = findViewById(R.id.btn_next_slide);
        nextButton.setOnClickListener(v -> nextSlide());
    }

    private void initializeCurrentSlideIndicator() {
        tabDots = findViewById(R.id.tab_dots);
        for (int i = 0; i < slides.size(); i++) {
            tabDots.addTab(tabDots.newTab());
        }

        // disable onclick of tab dots
        LinearLayout tabStrip = ((LinearLayout) tabDots.getChildAt(0));
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            tabStrip.getChildAt(i).setOnTouchListener((v, event) -> true);
        }

        highlightDot(currentSlidePosition);
    }

    private void nextSlide() {
        WelcomeSlide currentSlide = slides.get(currentSlidePosition);
        currentSlide.onSlideFinished();

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