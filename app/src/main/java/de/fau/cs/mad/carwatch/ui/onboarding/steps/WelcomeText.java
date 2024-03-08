package de.fau.cs.mad.carwatch.ui.onboarding.steps;

import de.fau.cs.mad.carwatch.R;

/**
 * create an instance of this fragment.
 */
public class WelcomeText extends BaseWelcomeSlide {

    public WelcomeText () {
        super();
        canShowNextSlide.set(true);
    }

    @Override
    protected int getResourceId() {
        return R.layout.fragment_welcome_text;
    }
}