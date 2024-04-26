package de.fau.cs.mad.carwatch.ui.onboarding.steps;

import de.fau.cs.mad.carwatch.R;

public class EndTutorialSlide extends BaseWelcomeSlide {

    public EndTutorialSlide() {
        isSkipButtonVisible.set(true);
        canShowNextSlide.set(true);
        canShowPreviousSlide.set(true);
    }

    @Override
    protected int getResourceId() {
        return R.layout.fragment_end_tutorial_slide;
    }
}