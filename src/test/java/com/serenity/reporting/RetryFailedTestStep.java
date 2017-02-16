package com.serenity.reporting;

import java.util.List;
import java.util.stream.Collectors;

import net.thucydides.core.guice.Injectors;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestResult;
import net.thucydides.core.model.TestStep;
import net.thucydides.core.util.EnvironmentVariables;

/**
 * Created by Maria_Akulova on 5/30/2016.
 */
public class RetryFailedTestStep {

    private static EnvironmentVariables environmentVariables = Injectors.getInjector()
            .getProvider(EnvironmentVariables.class).get();
    private final String TAKE_SCREENSHOT_OPTION = environmentVariables.getProperties().getProperty("serenity.take.screenshots",
            "AFTER_EACH_STEP");

    /**
     * recursively failed steps set to ignore
     * @param testSteps
     */
    public void setFailedToIgnore(final List<TestStep> testSteps) {
        testSteps.stream().forEach(step -> {
            step.setResult(TestResult.IGNORED);
            if ("FOR_FAILURES".equals(TAKE_SCREENSHOT_OPTION)) {
                while (step.getScreenshots().size() > 0) {
                    step.removeScreenshot(0);
                }
            }
            setFailedToIgnore(getFailedSteps(step));
        });
    }

    public <T> List<TestStep> getFailedSteps(final T parent) {
        return (TestStep.class.isInstance(parent) ? ((TestStep) parent).getChildren()
                : ((TestOutcome) parent).getTestSteps()).stream().filter(step -> step.isError() || step.isFailure())
                        .collect(Collectors.toList());
    }

    public <T> boolean isNeedUpdate(final T test) {
        boolean isError = TestStep.class.isInstance(test) ? ((TestStep) test).isError()
                : ((TestOutcome) test).isError();
        boolean isFailure = TestStep.class.isInstance(test) ? ((TestStep) test).isFailure()
                : ((TestOutcome) test).isFailure();
        return (isError || isFailure) && isSuccessLastResult(test);
    }

    /**
     * check if successful retry was
     * @param test should be TestOutcome or TestStep
     * @return
     */
    private <T> boolean isSuccessLastResult(final T test) {
        List<TestStep> step = TestStep.class.isInstance(test) ? ((TestStep) test).getChildren()
                : ((TestOutcome) test).getTestSteps();
        return step.isEmpty()
                ? TestStep.class.isInstance(test) ? ((TestStep) test).isSuccessful() : ((TestOutcome) test).isSuccess()
                : step.get(step.size() - 1).isSuccessful();
    }
}
