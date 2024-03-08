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
import androidx.databinding.Observable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.transition.TransitionInflater;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.ui.MainActivity;
import de.fau.cs.mad.carwatch.ui.barcode.QrFragment;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.ParticipantIdQuery;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.PermissionRequest;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.TutorialSlide;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.WelcomeSlide;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.WelcomeText;
import de.fau.cs.mad.carwatch.util.OnSwipeTouchListener;

public class SlideShowActivity extends AppCompatActivity {

    public static final String TAG = SlideShowActivity.class.getSimpleName();
    public static final int SHOW_ALL_SLIDES = 0;
    public static final int SHOW_APP_INITIALIZATION_SLIDES = 1;
    public static final int SHOW_TUTORIAL_SLIDES = 2;

    private final List<WelcomeSlide> slides = new ArrayList<>();
    private int slideShowType;
    private int currentSlidePosition = 0;
    private int qrScannerSlidePosition = -1;
    private boolean canShowNextSlide = false;
    private boolean canShowPreviousSlide = false;
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
        currentSlidePosition = sharedPreferences.getInt(Constants.PREF_CURRENT_SLIDE_SHOW_SLIDE, Constants.INITIAL_SLIDE_SHOW_SLIDE);

        slideShowType = getIntent().getIntExtra(Constants.EXTRA_SLIDE_SHOW_TYPE, SHOW_ALL_SLIDES);

        initializeSlides();
        addSwipeListener();
        initializeSkipButton();
        initializeNextButton();
        showSlide(currentSlidePosition);
    }

    private void addSwipeListener() {
        FragmentContainerView slideShowFragment = findViewById(R.id.slide_show_fragment);
        slideShowFragment.setOnTouchListener(new OnSwipeTouchListener(this) {
             @Override
             public void onSwipeLeft() {
                 if (canShowNextSlide) {
                     nextSlide();
                 }
             }

            @Override
            public void onSwipeRight() {
                if (canShowPreviousSlide) {
                    previousSlide();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // if the user leaves the app during the tutorial, we assume that the tutorial is finished
        if (slideShowType == SHOW_TUTORIAL_SLIDES) {
            sharedPreferences.edit().putInt(Constants.PREF_CURRENT_SLIDE_SHOW_SLIDE, Constants.SLIDESHOW_FINISHED_SLIDE_ID).apply();
        }
    }

    private void initializeSkipButton() {
        skipButton = findViewById(R.id.btn_skip_slides);
        skipButton.setOnClickListener(v -> finishSlideShow());
    }

    private void initializeSlides() {
        switch (slideShowType) {
            case SHOW_APP_INITIALIZATION_SLIDES:
                addSlide(new QrFragment());
                qrScannerSlidePosition = 0;
                break;
            case SHOW_TUTORIAL_SLIDES:
                for (TutorialSlide slide : createTutorialSlides()) {
                    addSlide(slide);
                }
                break;
            default:
                addSlide(new WelcomeText());
                addSlide(new PermissionRequest());
                addSlide(new QrFragment());
                qrScannerSlidePosition = 2;
                for (TutorialSlide slide : createTutorialSlides()) {
                    addSlide(slide);
                }
                break;
        }
    }

    private List<TutorialSlide> createTutorialSlides() {
        List<TutorialSlide> tutorialSlides = new ArrayList<>();
        boolean manualScanEnabled = sharedPreferences.getBoolean(Constants.PREF_MANUAL_SCAN, false);
        boolean eveningSampleRequired = sharedPreferences.getBoolean(Constants.PREF_HAS_EVENING, false);

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
        String bedtimeScreenDescription = eveningSampleRequired ? getString(R.string.description_bedtime_screen_tutorial) : getString(R.string.description_bedtime_screen_tutorial_short);
        String scanScreenDescription = getString(R.string.description_scan_screen_tutorial);

        if (manualScanEnabled)
            scanScreenDescription += " " + getString(R.string.scan_screen_navigation_hint);

        tutorialSlides.add(TutorialSlide.newInstance(wakeupScreenHeadline, wakeupScreenDescription, wakeupScreenImageId, false));
        tutorialSlides.add(TutorialSlide.newInstance(alarmScreenHeadline, alarmScreenDescription, alarmScreenImageId, true));
        tutorialSlides.add(TutorialSlide.newInstance(bedtimeScreenHeadline, bedtimeScreenDescription, bedtimeScreenImageId, true));
        tutorialSlides.add(TutorialSlide.newInstance(scanScreenHeadline, scanScreenDescription, scanScreenImageId, true));

        return tutorialSlides;
    }


    private void initializeNextButton() {
        nextButton = findViewById(R.id.btn_next_slide);
        nextButton.setOnClickListener(v -> nextSlide());
    }

    private void showSlide(int position) {
        WelcomeSlide slide = slides.get(position);
        initButtonsForSlide(slide);
        replaceFragment(slide.getFragment());
        highlightDot(position);
    }

    private void previousSlide() {
        if (currentSlidePosition <= 0) {
            return;
        }
        currentSlidePosition--;
        sharedPreferences.edit().putInt(Constants.PREF_CURRENT_SLIDE_SHOW_SLIDE, currentSlidePosition).apply();
        setSlideTransition(currentSlidePosition, currentSlidePosition + 1);
        showSlide(currentSlidePosition);
    }

    private void nextSlide() {
        WelcomeSlide currentSlide = slides.get(currentSlidePosition);
        currentSlide.onSlideFinished();

        if (currentSlidePosition == qrScannerSlidePosition) {
            int tutorialSlidePos = currentSlidePosition + 1;

            if (!sharedPreferences.getBoolean(Constants.PREF_SUBJECT_ID_IS_SET, false)) {
                addSlide(currentSlidePosition + 1, new ParticipantIdQuery());
                tutorialSlidePos++;
            }

            if (slideShowType == SHOW_ALL_SLIDES) {
                // recreate tutorial slides after study configuration was loaded
                recreateTutorialSlides(tutorialSlidePos);
            }
        }

        if (currentSlidePosition >= slides.size() - 1) {
            finishSlideShow();
            return;
        }

        currentSlidePosition++;
        sharedPreferences.edit().putInt(Constants.PREF_CURRENT_SLIDE_SHOW_SLIDE, currentSlidePosition).apply();
        setSlideTransition(currentSlidePosition, currentSlidePosition - 1);
        showSlide(currentSlidePosition);
    }

    private void setSlideTransition(int positionNextSlide, int positionPrevSlide) {
        boolean enterRight = positionNextSlide > positionPrevSlide;
        TransitionInflater inflater = TransitionInflater.from(this);

        if (0 <= positionPrevSlide && positionPrevSlide < slides.size()) {
            Fragment prevSlide = slides.get(positionPrevSlide).getFragment();
            prevSlide.setExitTransition(inflater.inflateTransition(enterRight ? R.transition.slide_left : R.transition.slide_right));
        }
        if (0 <= positionNextSlide && positionNextSlide < slides.size()) {
            Fragment nextSlide = slides.get(positionNextSlide).getFragment();
            nextSlide.setEnterTransition(inflater.inflateTransition(enterRight ? R.transition.slide_right : R.transition.slide_left));
        }
    }

    private void initButtonsForSlide(WelcomeSlide slide) {
        canShowNextSlide = slide.getCanShowNextSlide().get();
        canShowPreviousSlide = slide.getCanShowPreviousSlide().get();
        setSkipButtonVisibility(slide.getSkipButtonIsVisible().get());
        nextButton.setEnabled(canShowNextSlide);
        slide.getSkipButtonIsVisible().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                setSkipButtonVisibility(slide.getSkipButtonIsVisible().get());
            }
        });
        slide.getCanShowNextSlide().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                canShowNextSlide = slide.getCanShowNextSlide().get();
                nextButton.setEnabled(canShowNextSlide);
            }
        });
        slide.getCanShowPreviousSlide().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                canShowPreviousSlide = slide.getCanShowPreviousSlide().get();
            }
        });
    }

    /**
     * Replaces the tutorial slides with respect to the study configuration
     * @param firstSlidePos the position of the first tutorial slide
     */
    private void recreateTutorialSlides(int firstSlidePos) {
        List<TutorialSlide> tutorialSlides = createTutorialSlides();
        for (int i = 0; i < tutorialSlides.size(); i++) {
            slides.set(firstSlidePos + i, tutorialSlides.get(i));
        }
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
        sharedPreferences.edit().putInt(Constants.PREF_CURRENT_SLIDE_SHOW_SLIDE, Constants.SLIDESHOW_FINISHED_SLIDE_ID).apply();
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