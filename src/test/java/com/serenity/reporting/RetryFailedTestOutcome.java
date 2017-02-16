package com.serenity.reporting;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.thucydides.core.guice.Injectors;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestResult;
import net.thucydides.core.model.TestStep;
import net.thucydides.core.reports.ExecutorServiceProvider;
import net.thucydides.core.reports.ReportService;
import net.thucydides.core.reports.TestOutcomeLoader;
import net.thucydides.core.reports.TestOutcomes;
import net.thucydides.core.reports.html.HtmlAggregateStoryReporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Maria_Akulova on 5/26/2016.
 */
public class RetryFailedTestOutcome {
    private static final Logger LOG = LoggerFactory.getLogger(RetryFailedTestOutcome.class.getSimpleName());
    private File reportDir;
    private RetryFailedTestStep retryFailedTestStep;

    public RetryFailedTestOutcome(final String reportDir) {
        this.reportDir = new File(reportDir);
        retryFailedTestStep = new RetryFailedTestStep();
    }

    public static void main(final String[] args) throws IOException {
        LOG.info("RUN RETRY REPORTER");

        RetryFailedTestOutcome retryFailedTestOutcome = new RetryFailedTestOutcome("target/site/serenity");
        retryFailedTestOutcome.updateResults();
        Injectors.getInjector().getInstance(ExecutorServiceProvider.class).getExecutorService().shutdown();
    }

    private List<TestOutcome> getFailedTestOutcomes() {
        return getTestOutcomes().getOutcomes().stream().filter(outcome -> outcome.isError() || outcome.isFailure())
                .map(outcome -> TestOutcome.class.cast(outcome)).collect(Collectors.toList());
    }

    private TestOutcomes getTestOutcomes() {
        TestOutcomes testOutcomes = null;
        try {
            HtmlAggregateStoryReporter reporter = new HtmlAggregateStoryReporter("default");
            testOutcomes = TestOutcomeLoader.loadTestOutcomes().inFormat(reporter.getFormat()).from(reportDir)
                    .withHistory().withRequirementsTags();
        } catch (IOException e) {
            LOG.info(String.valueOf(e));
        }
        return testOutcomes;
    }

    public void updateResults() {
        List<TestOutcome> updatedTestOutcome = new ArrayList<>();
        ReportService reportService = new ReportService(reportDir, ReportService.getDefaultReporters());

        for (TestOutcome outcome : getFailedTestOutcomes()) {
            if (null == outcome.getDataTable()) {
                ingoreFailedTestWithExample(outcome);
            } else {
                ingoreFailedTest(outcome);
            }
            removeGlobalError(outcome);
            updatedTestOutcome.add(outcome);
        }
        reportService.generateReportsFor(updatedTestOutcome);
    }

    private void ingoreFailedTestWithExample(final TestOutcome outcome) {
        List<TestStep> failedSteps = retryFailedTestStep.getFailedSteps(outcome);
        if (!retryFailedTestStep.isNeedUpdate(outcome)) {
            failedSteps.remove(failedSteps.size() - 1);
        }
        retryFailedTestStep.setFailedToIgnore(failedSteps);
        logScenarioUpdate(outcome);
    }

    private void ingoreFailedTest(final TestOutcome outcome) {
        for (TestStep testStep : outcome.getTestSteps()) {
            if (retryFailedTestStep.isNeedUpdate(testStep)) { // NOSONAR
                retryFailedTestStep.setFailedToIgnore(retryFailedTestStep.getFailedSteps(testStep));
                setSuccessfulDataTableRow(outcome, testStep);
                LOG.info("Example updated: " + testStep.getDescription());
            }
        }
        logScenarioUpdate(outcome);
    }

    private void logScenarioUpdate(final TestOutcome outcome) {
        LOG.info("Scenario updated: " + outcome.getName());
    }

    private void setSuccessfulDataTableRow(final TestOutcome outcome, final TestStep testStep) {
        outcome.getDataTable().getRows()
                .get(outcome.getTestSteps().stream()
                        .filter(step -> step.getDescription().split(" ")[0].matches("\\[[0-9]{1,3}\\]"))
                        .collect(Collectors.toList()).indexOf(testStep))
                .setResult(TestResult.SUCCESS);
    }

    /**
     * remove global error to prevent incorrect outcome result
     * @param outcome
     */
    private void removeGlobalError(final TestOutcome outcome) {
        outcome.setTestFailureCause(null);
        outcome.setTestFailureClassname(null);
        outcome.setTestFailureMessage(null);
    }
}
