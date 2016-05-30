package com.serenity;

import java.util.Optional;
import java.util.stream.Stream;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestStep;
import net.thucydides.core.steps.*;
import net.thucydides.core.util.EnvironmentVariables;

import org.jbehave.core.failures.RestartingScenarioFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Maria_Akulova on 5/26/2016.
 */
public class CustomRetryStepListener extends ConsoleLoggingListener {

    public static final String SERENITY_JBEHAVE_RETRY_MAX_COUNT = "serenity.jbehave.retry.max.count";
    private static final Logger LOG = LoggerFactory.getLogger(CustomRetryStepListener.class.getSimpleName());
    private byte counter;
    private String testName;

    private CustomRetryStepListener(EnvironmentVariables environmentVariables) {
        super(environmentVariables);
    }

    public static CustomRetryStepListener getInstance() {
        return CustomRetryStepListenerHolder.instance;
    }

    @Override
    public void testStarted(final String description) {
        testName = description;
    }

    @Override
    public void testFinished(final TestOutcome result) {
        counter = 0;
    }

    @Override
    public void exampleFinished() {
        counter = 0;
    }

    private byte getRetryCount() {
        return counter;
    }

    private void incrementRetryCount() {
        counter++;
    }

    private byte getMaxRetryCount() {
        return Optional.ofNullable(System.getProperty(SERENITY_JBEHAVE_RETRY_MAX_COUNT)).map(Byte::parseByte)
                .orElse((byte) 0);
    }

    /**
     *
     * @param testStep
     * @return true if test or example, false if step
     */
    private boolean isTest(final TestStep testStep) {
        return null == testStep || isSplittedAsStep(testStep);
    }

    private boolean isSplittedAsStep(TestStep testStep) {
        return testStep.getDescription().split(" ")[0].matches("\\[[0-9]{1,3}\\]");
    }

    private boolean isStep(TestStep testStep) {
        return Stream.of("Given", "When", "Then", "And")
                .anyMatch(w -> testStep.getDescription().replace("［］", "").startsWith(w));
    }

    /**
     * Recursively search failed step in failed scenario
     *
     * @param testStep
     * @return
     */
    private boolean isFailedStep(final TestStep testStep) {
        return null == testStep.getException()
                ? isFailedStep(testStep.getChildren().get(testStep.getChildren().size() - 1))
                : isSplittedAsStep(testStep) || isStep(testStep);
    }

    @Override
    public void stepFailed(final StepFailure failure) {
        StepEventBus stepEventBus = StepEventBus.getEventBus();
        BaseStepListener baseStepListener = stepEventBus.getBaseStepListener();
        com.google.common.base.Optional<TestStep> currentStep = stepEventBus.getCurrentStep();

        LOG.info("STEP FAILED: " + transformStepDescription());

        if (0 != getMaxRetryCount() && getRetryCount() != getMaxRetryCount() && isTest(currentStep.orNull())) {
            incrementRetryCount();
            baseStepListener.clearForcedResult();
            stepEventBus.clearStepFailures();
            if (!(currentStep.isPresent() && isFailedStep(currentStep.get()))) {
                baseStepListener.stepFinished();
            }
            Serenity.getCurrentSession().keySet().forEach(key -> Serenity.getCurrentSession().remove(key));
            // Uncomment if your application should be maximized during running test
            // ThucydidesWebDriverSupport.getDriver().manage().window().maximize();
            throw new RestartingScenarioFailure(
                    "\nRestarting scenario: " + testName + " Step Name: " + transformStepDescription());
        }
    }

    private String transformStepDescription() {
        return StepEventBus.getEventBus().getCurrentStep().transform(step -> step.getDescription()).orNull();
    }

    @Override
    public void stepStarted(final ExecutedStepDescription description) {
        LOG.info("STEP STARTED: " + transformStepDescription());
    }

    @Override
    public void stepIgnored() {
        LOG.info("STEP IGNORED: " + transformStepDescription());
    }

    @Override
    public void stepPending() {
        LOG.info("STEP PENDING: " + transformStepDescription());
    }

    @Override
    public void stepPending(final String message) {
        LOG.info("STEP PENDING: " + transformStepDescription() + message);
    }

    @Override
    public void stepFinished() {
        LOG.info("STEP FINISHED: " + transformStepDescription());
    }

    private static class CustomRetryStepListenerHolder {
        private final static CustomRetryStepListener instance = new CustomRetryStepListener(
                (Injectors.getInjector().getProvider(EnvironmentVariables.class).get()).copy());
    }

}
