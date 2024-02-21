package de.fau.cs.mad.carwatch.ui.onboarding;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.ui.MainActivity;

public class SlideShowActivity extends AppCompatActivity {

    public static final String TAG = SlideShowActivity.class.getSimpleName();

    private final List<Fragment> slides = new ArrayList<>();
    private final int firstSkippableSlide = 1;
    private int currentSlide = 0;
    private Button skipButton;
    private Button nextButton;
    private TabLayout tabDots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_show);

        initializeSlides();
        initializeSkipButton();
        initializeNextButton();
        initializeCurrentSlideIndicator();
    }

    private void initializeSkipButton() {
        skipButton = findViewById(R.id.btn_skip_slides);
        skipButton.setOnClickListener(v -> finishSlideShow());
    }

    private void initializeSlides() {
    }

    private void initializeNextButton() {
        nextButton = findViewById(R.id.btn_next_slide);
        nextButton.setOnClickListener(v -> showNextSlide());
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

        selectCurrentTab();
    }

    private void showNextSlide() {
        if (currentSlide < slides.size() - 1) {
            currentSlide++;

            // replace fragment with next slide
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.slide_show_fragment, slides.get(currentSlide));
            transaction.commit();

            selectCurrentTab();

            if (currentSlide >= firstSkippableSlide) {
                skipButton.setVisibility(Button.VISIBLE);
            }
        } else {
            // Last slide reached
            finishSlideShow();
        }
    }

    private void selectCurrentTab() {
        TabLayout.Tab tab = tabDots.getTabAt(currentSlide);
        if (tab != null) {
            tab.select();
        }
    }

    private void finishSlideShow() {
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);
        finish();
    }

    private String currentSlideName() {
        return slides.get(currentSlide).getClass().getSimpleName();
    }
}