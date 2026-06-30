package de.fau.cs.mad.carwatch.ui.onboarding;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.databinding.Observable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.ui.AppInfoBottomSheet;
import de.fau.cs.mad.carwatch.ui.HeaderUiHelper;
import de.fau.cs.mad.carwatch.ui.MainActivity;
import de.fau.cs.mad.carwatch.ui.barcode.QrFragment;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.EndTutorialSlide;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.ParticipantIdQuery;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.PermissionRequest;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.StudyParticipationNotice;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.StudyDetailsSlide;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.TutorialSlide;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.WelcomeSlide;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.WelcomeText;
import de.fau.cs.mad.carwatch.util.OnSwipeTouchListener;
import de.fau.cs.mad.carwatch.util.Utils;

public class SlideShowActivity extends AppCompatActivity implements QrFragment.ScanSuccessListener {

    public static final String TAG = SlideShowActivity.class.getSimpleName();
    public static final int SHOW_ALL_SLIDES = 0;
    public static final int SHOW_APP_INITIALIZATION_SLIDES = 1;
    public static final int SHOW_TUTORIAL_SLIDES = 2;
    private static final int TRANSITION_DIRECTION_NONE = 0;
    private static final int TRANSITION_DIRECTION_FORWARD = 1;
    private static final int TRANSITION_DIRECTION_BACKWARD = -1;
    private static final long SLIDE_TRANSITION_DURATION_MS = 220L;

    private final List<WelcomeSlide> slides = new ArrayList<>();
    private int slideShowType;
    private int currentSlidePosition = 0;
    private int qrScannerSlidePosition = -1;
    private int tutorialStartPosition = -1;
    private int tutorialEndPosition = -1;
    private int numberOfTutorialSlides = 0;
    private boolean canShowNextSlide = false;
    private boolean canShowPreviousSlide = false;
    private SharedPreferences sharedPreferences;
    private Button skipButton;
    private Button nextButton;
    private View slideNavigation;
    private View header;
    private View appInfoButton;
    private View slideShowFragment;
    private LinearLayout tabDots;
    private TextView headerTitle;
    private boolean waitingForPermissionResult = false;
    private boolean slideTransitionRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_show);
        slideShowFragment = findViewById(R.id.slide_show_fragment);
        header = findViewById(R.id.header);
        appInfoButton = findViewById(R.id.btn_header_app_info);
        slideNavigation = findViewById(R.id.slide_navigation);
        tabDots = findViewById(R.id.tab_dots);
        headerTitle = findViewById(R.id.tv_header_title);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        HeaderUiHelper.configureTransparentStatusBar(this, sharedPreferences);
        currentSlidePosition = sharedPreferences.getInt(Constants.PREF_CURRENT_SLIDE_SHOW_SLIDE, Constants.INITIAL_SLIDE_SHOW_SLIDE);

        slideShowType = getIntent().getIntExtra(Constants.EXTRA_SLIDE_SHOW_TYPE, SHOW_ALL_SLIDES);

        initializeSlides();
        addSwipeListener();
        initializeAppInfoButton();
        initializeSkipButton();
        initializeNextButton();
        showSlide(currentSlidePosition);
    }

    private void initializeAppInfoButton() {
        appInfoButton.setOnClickListener(view -> AppInfoBottomSheet.show(this));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addSwipeListener() {
        slideShowFragment.setOnTouchListener(createSwipeTouchListener(true));
    }

    private OnSwipeTouchListener createSwipeTouchListener(boolean consumeTouchEvents) {
        return new OnSwipeTouchListener(this, consumeTouchEvents) {
            @Override
            public void onSwipeLeft() {
                if (!slideTransitionRunning && canShowNextSlide && currentSlidePosition < slides.size() - 1) {
                    nextSlide();
                }
            }

            @Override
            public void onSwipeRight() {
                if (!slideTransitionRunning && canShowPreviousSlide) {
                    previousSlide();
                }
            }
        };
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
        skipButton.setOnClickListener(v -> {
            if (slideTransitionRunning) {
                return;
            }
            if (slides.get(currentSlidePosition) instanceof StudyDetailsSlide) {
                performReregistrationFromStudyDetails();
                return;
            }
            if (isBackButtonSlide(slides.get(currentSlidePosition))) {
                previousSlide();
                return;
            }
            finishSlideShow();
        });
    }

    private void initializeSlides() {
        numberOfTutorialSlides = 0;
        qrScannerSlidePosition = -1;
        tutorialStartPosition = -1;
        tutorialEndPosition = -1;

        switch (slideShowType) {
            case SHOW_APP_INITIALIZATION_SLIDES:
                qrScannerSlidePosition = addSlide(new QrFragment());
                break;
            case SHOW_TUTORIAL_SLIDES:
                tutorialStartPosition = slides.size();
                for (TutorialSlide slide : createTutorialSlides()) {
                    addSlide(slide);
                    numberOfTutorialSlides++;
                }
                addSlide(new EndTutorialSlide());
                tutorialEndPosition = slides.size() - 1;
                break;
            default:
                addSlide(new WelcomeText());
                addSlide(new StudyParticipationNotice());
                addSlide(new PermissionRequest());
                qrScannerSlidePosition = addSlide(new QrFragment());
                tutorialStartPosition = slides.size();
                for (TutorialSlide slide : createTutorialSlides()) {
                    addSlide(slide);
                    numberOfTutorialSlides++;
                }
                addSlide(new EndTutorialSlide());
                tutorialEndPosition = slides.size() - 1;
                break;
        }
    }

    private List<TutorialSlide> createTutorialSlides() {
        List<TutorialSlide> tutorialSlides = new ArrayList<>();

        TutorialSlide wakeupScreenTutorial = TutorialSlide.newInstance(
                getString(R.string.headline_wakeup_screen_tutorial),
                getString(R.string.description_wakeup_screen),
                R.drawable.img_screenshot_wakeup_screen,
                slideShowType == SHOW_ALL_SLIDES
        );
        TutorialSlide wakeUpAlarmTutorial = TutorialSlide.newInstance(
                getString(R.string.headline_alarm_tutorial),
                getString(R.string.description_wakeup_alarm),
                R.drawable.img_screenshot_alarm_screen_no_saliva_alarms,
                true
        );
        TutorialSlide salivaAlarmsTutorial = TutorialSlide.newInstance(
                getString(R.string.headline_alarm_tutorial),
                getString(R.string.description_saliva_alarm_overview),
                R.drawable.img_screenshot_alarm_screen_saliva_alarms_highlighted,
                true
        );
        TutorialSlide alarmSymbolsTutorial = TutorialSlide.newInstance(
                getString(R.string.headline_alarm_tutorial),
                getString(R.string.description_alarm_symbols_tutorial),
                R.drawable.img_screenshot_alarm_screen_symbols_highlighted,
                true
        );
        TutorialSlide bedtimeScreenTutorial = TutorialSlide.newInstance(
                getString(R.string.headline_bedtime_screen_tutorial),
                getString(R.string.description_bedtime_screen),
                R.drawable.img_screenshot_bedtime_screen,
                true
        );
        TutorialSlide scanScreenTutorial = TutorialSlide.newInstance(
                getString(R.string.headline_scan_screen_tutorial),
                getString(R.string.description_scan_screen),
                R.drawable.img_screenshot_barcode_cam,
                true
        );

        tutorialSlides.add(wakeupScreenTutorial);
        tutorialSlides.add(wakeUpAlarmTutorial);
        tutorialSlides.add(salivaAlarmsTutorial);
        tutorialSlides.add(alarmSymbolsTutorial);
        tutorialSlides.add(bedtimeScreenTutorial);
        tutorialSlides.add(scanScreenTutorial);

        return tutorialSlides;
    }


    private void initializeNextButton() {
        nextButton = findViewById(R.id.btn_next_slide);
        nextButton.setOnClickListener(v -> nextSlide());
    }

    private void showSlide(int position) {
        showSlide(position, TRANSITION_DIRECTION_NONE);
    }

    private void showSlide(int position, int transitionDirection) {
        slideTransitionRunning = transitionDirection != TRANSITION_DIRECTION_NONE;
        applySlide(position, transitionDirection, () -> {
            if (transitionDirection == TRANSITION_DIRECTION_NONE) {
                slideTransitionRunning = false;
                return;
            }
            slideShowFragment.postDelayed(
                    () -> slideTransitionRunning = false,
                    SLIDE_TRANSITION_DURATION_MS
            );
        });
    }

    private void applySlide(int position, int transitionDirection, Runnable onFragmentCommitted) {
        WelcomeSlide slide = slides.get(position);
        boolean isStudyDetailsSlide = slide instanceof StudyDetailsSlide;
        boolean isWelcomeSlide = slide instanceof WelcomeText;
        boolean isStudyParticipationNotice = slide instanceof StudyParticipationNotice;
        boolean hideHeader = isStudyDetailsSlide
                || isStudyParticipationNotice;
        header.setVisibility(hideHeader ? View.GONE : View.VISIBLE);
        int topMargin = isStudyDetailsSlide ? 0 : dpToPx((isWelcomeSlide || isStudyParticipationNotice) ? 72 : 112);
        setSlideContentTopMargin(topMargin);
        headerTitle.setVisibility(isWelcomeSlide ? View.INVISIBLE : View.VISIBLE);
        if (isStudyDetailsSlide) {
            headerTitle.setText(R.string.title_study_configuration);
        } else if (slide instanceof PermissionRequest) {
            headerTitle.setText(R.string.title_setup);
        } else if (slide instanceof TutorialSlide || slide instanceof EndTutorialSlide) {
            headerTitle.setText(R.string.title_tutorial);
        } else {
            headerTitle.setText(R.string.app_name);
        }
        appInfoButton.setVisibility(slide instanceof WelcomeText ? View.VISIBLE : View.GONE);
        initButtonsForSlide(slide);
        replaceFragment(slide.getFragment(), transitionDirection, onFragmentCommitted);
        updateVisibleDots(position);
        setDotsVisibleForSlide(slide);
        highlightDot(position);
    }

    private void setSlideContentTopMargin(int marginTop) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) slideShowFragment.getLayoutParams();
        if (layoutParams.topMargin == marginTop) {
            return;
        }
        layoutParams.topMargin = marginTop;
        slideShowFragment.setLayoutParams(layoutParams);
    }

    private void previousSlide() {
        if (slideTransitionRunning) {
            return;
        }
        if (currentSlidePosition <= 0) {
            return;
        }
        currentSlidePosition--;
        sharedPreferences.edit().putInt(Constants.PREF_CURRENT_SLIDE_SHOW_SLIDE, currentSlidePosition).apply();
        showSlide(currentSlidePosition, TRANSITION_DIRECTION_BACKWARD);
    }

    private void nextSlide() {
        if (slideTransitionRunning) {
            return;
        }
        WelcomeSlide currentSlide = slides.get(currentSlidePosition);
        if (currentSlide instanceof PermissionRequest) {
            boolean permissionDialogShown = Utils.requestRuntimePermissions(this);
            if (permissionDialogShown) {
                waitingForPermissionResult = true;
                nextButton.setEnabled(false);
                return;
            }
        }

        advanceFromCurrentSlide();
    }

    private void advanceFromCurrentSlide() {
        WelcomeSlide currentSlide = slides.get(currentSlidePosition);
        currentSlide.onSlideFinished();

        if (currentSlidePosition == qrScannerSlidePosition) {
            int tutorialSlidePos = currentSlidePosition + 1;

            if (!sharedPreferences.getBoolean(Constants.PREF_PARTICIPANT_ID_WAS_SET, false)) {
                addSlide(currentSlidePosition + 1, new ParticipantIdQuery());
                tutorialSlidePos++;
            }

            int studyDetailsSlidePos = tutorialSlidePos;
            addSlide(studyDetailsSlidePos, new StudyDetailsSlide());
            tutorialSlidePos++;

            if (slideShowType == SHOW_ALL_SLIDES) {
                // recreate tutorial slides after study configuration was loaded
                recreateTutorialSlides(tutorialSlidePos);
                tutorialStartPosition = studyDetailsSlidePos;
                tutorialEndPosition = slides.size() - 1;
            }
        }

        if (currentSlidePosition >= slides.size() - 1) {
            finishSlideShow();
            return;
        }

        currentSlidePosition++;
        sharedPreferences.edit().putInt(Constants.PREF_CURRENT_SLIDE_SHOW_SLIDE, currentSlidePosition).apply();
        showSlide(currentSlidePosition, TRANSITION_DIRECTION_FORWARD);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != Utils.REQUEST_CODE_RUNTIME_PERMISSIONS || !waitingForPermissionResult) {
            return;
        }

        waitingForPermissionResult = false;
        if (Utils.allPermissionsGranted(this)) {
            advanceFromCurrentSlide();
        } else {
            nextButton.setEnabled(canShowNextSlide);
        }
    }

    @Override
    public void onQrCodeScanSuccessful() {
        if (!slideTransitionRunning && currentSlidePosition == qrScannerSlidePosition && canShowNextSlide) {
            nextSlide();
        }
    }

    private void performReregistrationFromStudyDetails() {
        AlarmHandler.resetStudyConfiguration(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        int targetSlidePosition = qrScannerSlidePosition >= 0
                ? qrScannerSlidePosition
                : Constants.INITIAL_SLIDE_SHOW_SLIDE;
        sharedPreferences.edit()
                .putInt(Constants.PREF_CURRENT_SLIDE_SHOW_SLIDE, targetSlidePosition)
                .putBoolean(Constants.PREF_FIRST_RUN_QR, true)
                .putBoolean(Constants.PREF_PARTICIPANT_ID_WAS_SET, false)
                .apply();

        Intent restartIntent = new Intent(this, SlideShowActivity.class);
        restartIntent.putExtra(Constants.EXTRA_SLIDE_SHOW_TYPE, SHOW_ALL_SLIDES);
        startActivity(restartIntent);
        finish();
    }

    private void initButtonsForSlide(WelcomeSlide slide) {
        slideNavigation.setVisibility(slide instanceof QrFragment ? View.GONE : View.VISIBLE);
        boolean isStudyDetailsSlide = slide instanceof StudyDetailsSlide;
        boolean showBackButton = isBackButtonSlide(slide);
        skipButton.setText(isStudyDetailsSlide
                ? R.string.btn_reregister
                : showBackButton ? R.string.btn_back : R.string.btn_skip_all);
        setNavigationButtonWidths();
        setSkipButtonVisibility(isStudyDetailsSlide || showBackButton || slide.getSkipButtonIsVisible().get());
        slide.getSkipButtonIsVisible().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                setSkipButtonVisibility(slide instanceof StudyDetailsSlide
                        || isBackButtonSlide(slide)
                        || slide.getSkipButtonIsVisible().get());
            }
        });
        canShowNextSlide = slide.getCanShowNextSlide().get();
        boolean isLastSlide = slides.indexOf(slide) == slides.size() - 1;
        nextButton.setEnabled(canShowNextSlide);
        nextButton.setText(isStudyDetailsSlide
                ? R.string.btn_confirm_continue_tutorial
                : isLastSlide ? R.string.btn_to_app : R.string.btn_next);
        slide.getCanShowNextSlide().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                canShowNextSlide = slide.getCanShowNextSlide().get();
                nextButton.setEnabled(canShowNextSlide);
            }
        });
        canShowPreviousSlide = slide.getCanShowPreviousSlide().get();
        slide.getCanShowPreviousSlide().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                canShowPreviousSlide = slide.getCanShowPreviousSlide().get();
            }
        });
    }

    private boolean isBackButtonSlide(WelcomeSlide slide) {
        return !(slide instanceof StudyDetailsSlide)
                && !slide.getSkipButtonIsVisible().get()
                && slide.getCanShowPreviousSlide().get();
    }

    private void setNavigationButtonWidths() {
        setButtonWidth(skipButton, dpToPx(130));
        setButtonWidth(nextButton, dpToPx(130));
        skipButton.setMinWidth(dpToPx(130));
        nextButton.setMinWidth(dpToPx(130));
    }

    private void setButtonWidth(Button button, int width) {
        ViewGroup.LayoutParams params = button.getLayoutParams();
        params.width = width;
        button.setLayoutParams(params);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    /**
     * Replaces the tutorial slides with respect to the study configuration
     * @param firstSlidePos the position of the first tutorial slide
     */
    private void recreateTutorialSlides(int firstSlidePos) {
        List<TutorialSlide> tutorialSlides = createTutorialSlides();
        for (int i = 0; i < tutorialSlides.size(); i++) {
            int pos = firstSlidePos + i;
            if (i < numberOfTutorialSlides) {
                slides.set(pos, tutorialSlides.get(i));
            } else {
                addSlide(pos, tutorialSlides.get(i));
            }
        }

        numberOfTutorialSlides = tutorialSlides.size();
    }

    private int addSlide(WelcomeSlide slide) {
        int pos = slides.size();
        addSlide(pos, slide);
        return pos;
    }

    private void addSlide(int position, WelcomeSlide slide) {
        slides.add(position, slide);
        addDot(position);
    }

    private void replaceFragment(Fragment fragment, int transitionDirection, Runnable onCommit) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        fragment.setEnterTransition(null);
        fragment.setExitTransition(null);
        if (transitionDirection == TRANSITION_DIRECTION_FORWARD) {
            transaction.setCustomAnimations(
                    R.anim.slide_fade_enter_from_right,
                    R.anim.slide_fade_exit_to_left
            );
        } else if (transitionDirection == TRANSITION_DIRECTION_BACKWARD) {
            transaction.setCustomAnimations(
                    R.anim.slide_fade_enter_from_left,
                    R.anim.slide_fade_exit_to_right
            );
        }
        transaction.replace(R.id.slide_show_fragment, fragment);
        transaction.runOnCommit(() -> {
            View root = fragment.getView();
            if (root != null) {
                root.setOnTouchListener(createSwipeTouchListener(false));
            }
            if (onCommit != null) {
                onCommit.run();
            }
        });
        transaction.commit();
    }

    private void highlightDot(int position) {
        for (int i = 0; i < tabDots.getChildCount(); i++) {
            tabDots.getChildAt(i).setSelected(i == position);
        }
    }

    private void updateVisibleDots(int position) {
        if (isInTutorialRange(position)) {
            for (int i = 0; i < tabDots.getChildCount(); i++) {
                setDotVisibility(i, tutorialStartPosition <= i && i <= tutorialEndPosition);
            }
            return;
        }

        int firstAccessiblePosition = getFirstAccessibleSlidePosition(position);
        for (int i = 0; i < tabDots.getChildCount(); i++) {
            setDotVisibility(i, i >= firstAccessiblePosition);
        }
    }

    private boolean isInTutorialRange(int position) {
        return tutorialStartPosition >= 0
                && tutorialEndPosition >= tutorialStartPosition
                && tutorialStartPosition <= position
                && position <= tutorialEndPosition;
    }

    private int getFirstAccessibleSlidePosition(int position) {
        int firstAccessiblePosition = position;
        while (firstAccessiblePosition > 0
                && slides.get(firstAccessiblePosition).getCanShowPreviousSlide().get()) {
            firstAccessiblePosition--;
        }

        return firstAccessiblePosition;
    }

    private void setDotVisibility(int position, boolean isVisible) {
        if (position >= tabDots.getChildCount()) {
            return;
        }
        View tab = tabDots.getChildAt(position);
        tab.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    private void setDotsVisibleForSlide(WelcomeSlide slide) {
        boolean hideDots = slide instanceof WelcomeText
                || slide instanceof StudyParticipationNotice
                || slide instanceof PermissionRequest;
        tabDots.setVisibility(hideDots ? View.GONE : View.VISIBLE);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addDot(int position) {
        FrameLayout dot = new FrameLayout(this);
        dot.setBackgroundResource(R.drawable.slide_tab_selector);
        dot.setOnTouchListener((v, event) -> true);

        int dotCellSize = getResources().getDimensionPixelSize(R.dimen.slide_dot_cell_size);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotCellSize, dotCellSize);
        int marginHorizontal = getResources().getDimensionPixelSize(R.dimen.slide_dot_margin_horizontal);
        params.setMarginStart(marginHorizontal);
        params.setMarginEnd(marginHorizontal);
        tabDots.addView(dot, position, params);
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
}
