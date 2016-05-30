package com.serenity;

import net.serenitybdd.jbehave.SerenityStories;
import net.thucydides.core.steps.StepEventBus;

import org.jbehave.core.annotations.BeforeStory;

public class AcceptanceTestSuite extends SerenityStories {
    @BeforeStory
    public void initStepListener() {
        StepEventBus.getEventBus().registerListener(CustomRetryStepListener.getInstance());
    }
}
