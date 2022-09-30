package com.microsoft.azure.vmagent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.List;
import java.util.HashSet;
import java.util.concurrent.Future;

import org.junit.Test;

import com.microsoft.azure.vmagent.util.AzureUtil;

public class AzureVMCloudTest {

    @Test
    public void setCurrentVirtualMachineCountGivenNullThenSetsNull() {
        // Given
        final AzureVMCloud instance = mkInstance();
        final String templateName = "myTemplate";
        final AzureVMAgentTemplate template = mkTemplate(templateName);
        final Map<String, Integer> vmCount = null;
        final int expectedCloud = 0;
        final int expectedTemplate = 0;

        // When
        instance.setCurrentVirtualMachineCount(vmCount);

        // Then
        final int actualCloud = instance.getApproximateVirtualMachineCount();
        assertThat(actualCloud, equalTo(expectedCloud));
        final int actualTemplate = instance.getApproximateVirtualMachineCountForTemplate(template);
        assertThat(actualTemplate, equalTo(expectedTemplate));
    }

    @Test
    public void setCurrentVirtualMachineCountGivenEmptyThenSetsNull() {
        // Given
        final AzureVMCloud instance = mkInstance();
        final String templateName = "myTemplate";
        final AzureVMAgentTemplate template = mkTemplate(templateName);
        final Map<String, Integer> vmCount = Collections.emptyMap();
        final int expectedCloud = 0;
        final int expectedTemplate = 0;

        // When
        instance.setCurrentVirtualMachineCount(vmCount);

        // Then
        final int actualCloud = instance.getApproximateVirtualMachineCount();
        assertThat(actualCloud, equalTo(expectedCloud));
        final int actualTemplate = instance.getApproximateVirtualMachineCountForTemplate(template);
        assertThat(actualTemplate, equalTo(expectedTemplate));
    }

    @Test
    public void setCurrentVirtualMachineCountGivenOneTemplateThenRecordsCount() {
        // Given
        final AzureVMCloud instance = mkInstance();
        final String templateName = "myTemplate";
        final AzureVMAgentTemplate template = mkTemplate(templateName);
        final int templateCount = 123;
        final Map<String, Integer> vmCount = Collections.singletonMap(templateName, templateCount);
        final int expectedCloud = templateCount;
        final int expectedTemplate = templateCount;

        // When
        instance.setCurrentVirtualMachineCount(vmCount);

        // Then
        final int actualCloud = instance.getApproximateVirtualMachineCount();
        assertThat(actualCloud, equalTo(expectedCloud));
        final int actualTemplate = instance.getApproximateVirtualMachineCountForTemplate(template);
        assertThat(actualTemplate, equalTo(expectedTemplate));
    }

    @Test
    public void setCurrentVirtualMachineCountGivenNewValuesThenReplacesValues() {
        // Given
        final AzureVMCloud instance = mkInstance();
        final String templateName = "myTemplate";
        final AzureVMAgentTemplate template = mkTemplate(templateName);
        instance.setCurrentVirtualMachineCount(Collections.singletonMap("foo", 234));
        final int templateCount = 123;
        final Map<String, Integer> vmCount = Collections.singletonMap(templateName, templateCount);
        final int expectedCloud = templateCount;
        final int expectedTemplate = templateCount;

        // When
        instance.setCurrentVirtualMachineCount(vmCount);

        // Then
        final int actualCloud = instance.getApproximateVirtualMachineCount();
        assertThat(actualCloud, equalTo(expectedCloud));
        final int actualTemplate = instance.getApproximateVirtualMachineCountForTemplate(template);
        assertThat(actualTemplate, equalTo(expectedTemplate));
    }

    @Test
    public void adjustApproximateVirtualMachineCountGivenNegativeAdjustmentThenAdjustsDown() {
        // Given
        final AzureVMCloud instance = mkInstance();
        final String templateName = "myTemplate";
        final AzureVMAgentTemplate template = mkTemplate(templateName);
        final int templateCount = 123;
        final Map<String, Integer> vmCount = Collections.singletonMap(templateName, templateCount);
        final int expectedCloud = 0;
        final int expectedTemplate = 0;
        instance.setCurrentVirtualMachineCount(vmCount);

        // When
        instance.adjustApproximateVirtualMachineCount(-templateCount, template);

        // Then
        final int actualCloud = instance.getApproximateVirtualMachineCount();
        assertThat(actualCloud, equalTo(expectedCloud));
        final int actualTemplate = instance.getApproximateVirtualMachineCountForTemplate(template);
        assertThat(actualTemplate, equalTo(expectedTemplate));
    }

    @Test
    public void adjustApproximateVirtualMachineCountGivenPositiveAdjustmentThenAdjustsUp() {
        // Given
        final AzureVMCloud instance = mkInstance();
        final String templateName = "myTemplate";
        final AzureVMAgentTemplate template = mkTemplate(templateName);
        final int templateCount = 123;
        final Map<String, Integer> vmCount = Collections.singletonMap(templateName, templateCount);
        final int expectedCloud = templateCount + 2;
        final int expectedTemplate = templateCount + 2;
        instance.setCurrentVirtualMachineCount(vmCount);

        // When
        instance.adjustApproximateVirtualMachineCount(2, template);

        // Then
        final int actualCloud = instance.getApproximateVirtualMachineCount();
        assertThat(actualCloud, equalTo(expectedCloud));
        final int actualTemplate = instance.getApproximateVirtualMachineCountForTemplate(template);
        assertThat(actualTemplate, equalTo(expectedTemplate));
    }

    @Test
    public void adjustApproximateVirtualMachineCountGivenComplexScenarioThenKeepsTrack() {
        // Given
        final AzureVMCloud instance = mkInstance();
        final String template1Name = "myTemplate";
        final AzureVMAgentTemplate template1 = mkTemplate(template1Name);
        final String template2Name = "Template2";
        final AzureVMAgentTemplate template2 = mkTemplate(template2Name);
        final String template3Name = "Template3";
        final AzureVMAgentTemplate template3 = mkTemplate(template3Name);
        final int expectedTemplate1 = 1;
        final int expectedTemplate2 = 2;
        final int expectedTemplate3 = 3;
        final int expectedCloud = expectedTemplate1 + expectedTemplate2 + expectedTemplate3;

        // When
        instance.adjustApproximateVirtualMachineCount(0, template1);
        instance.adjustApproximateVirtualMachineCount(1, template1);
        instance.adjustApproximateVirtualMachineCount(10, template2);
        instance.adjustApproximateVirtualMachineCount(100, template3);
        instance.adjustApproximateVirtualMachineCount(1, template1);
        instance.adjustApproximateVirtualMachineCount(-10, template2);
        instance.adjustApproximateVirtualMachineCount(100, template3);
        instance.adjustApproximateVirtualMachineCount(-197, template3);
        instance.adjustApproximateVirtualMachineCount(2, template2);
        instance.adjustApproximateVirtualMachineCount(-1, template1);

        // Then
        final int actualCloud = instance.getApproximateVirtualMachineCount();
        assertThat(actualCloud, equalTo(expectedCloud));
        final int actualTemplate1 = instance.getApproximateVirtualMachineCountForTemplate(template1);
        assertThat(actualTemplate1, equalTo(expectedTemplate1));
        final int actualTemplate2 = instance.getApproximateVirtualMachineCountForTemplate(template2);
        assertThat(actualTemplate2, equalTo(expectedTemplate2));
        final int actualTemplate3 = instance.getApproximateVirtualMachineCountForTemplate(template3);
        assertThat(actualTemplate3, equalTo(expectedTemplate3));
    }

    @Test
    public void calculateNumberOfAgentsToRequestGivenNoLimitsThenReturnsUnchangedCount() {
        // Given
        final String templateName = "templateName";
        final AzureVMAgentTemplate template = mkTemplate(templateName);
        final AzureVMCloud instance = mkInstance();
        final int numberRequested = 99;
        final int expected = numberRequested;

        // When
        int actual = instance.calculateNumberOfAgentsToRequest(template, numberRequested);

        // Then
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void calculateNumberOfAgentsToRequestGivenHighLimitsThenReturnsUnchangedCount() {
        // Given
        final String templateName = "templateName";
        final AzureVMAgentTemplate template = mkTemplate(templateName);
        final AzureVMCloud instance = mkInstance(321);
        template.setMaxVirtualMachinesLimit(123);
        final int numberRequested = 99;
        final int expected = numberRequested;

        // When
        int actual = instance.calculateNumberOfAgentsToRequest(template, numberRequested);

        // Then
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void calculateNumberOfAgentsToRequestGivenTemplateLimitThenReturnsTemplateRemainder() {
        // Given
        final String templateName = "templateName";
        final AzureVMAgentTemplate template = mkTemplate(templateName);
        final AzureVMCloud instance = mkInstance();
        template.setMaxVirtualMachinesLimit(123);
        instance.adjustApproximateVirtualMachineCount(100, template);
        final int numberRequested = 99;
        final int expected = 23;

        // When
        int actual = instance.calculateNumberOfAgentsToRequest(template, numberRequested);

        // Then
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void calculateNumberOfAgentsToRequestGivenCloudLimitThenReturnsCloudRemainder() {
        // Given
        final String templateName = "templateName";
        final AzureVMAgentTemplate template = mkTemplate(templateName);
        final AzureVMCloud instance = mkInstance(321);
        instance.adjustApproximateVirtualMachineCount(300, mkTemplate("otherTemplate"));
        final int numberRequested = 99;
        final int expected = 21;

        // When
        int actual = instance.calculateNumberOfAgentsToRequest(template, numberRequested);

        // Then
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void calculateNumberOfAgentsToRequestGivenBothCloudAndTemplateLimitThenReturnsLowerRemainder() {
        // Given
        final String templateName = "templateName";
        final AzureVMAgentTemplate template = mkTemplate(templateName);
        final AzureVMCloud instance = mkInstance(321);
        instance.adjustApproximateVirtualMachineCount(200, mkTemplate("otherTemplate"));
        template.setMaxVirtualMachinesLimit(123);
        instance.adjustApproximateVirtualMachineCount(100, template);
        final int numberRequested = 99;
        final int expected = 21;

        // When
        int actual = instance.calculateNumberOfAgentsToRequest(template, numberRequested);

        // Then
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void calculateNumberOfAgentsToRequestGivenTooLowLimitThenReturnsZero() {
        // Given
        final String templateName = "templateName";
        final AzureVMAgentTemplate template = mkTemplate(templateName);
        final AzureVMCloud instance = mkInstance(100);
        instance.adjustApproximateVirtualMachineCount(100, template);
        template.setMaxVirtualMachinesLimit(100);
        final int numberRequested = 99;
        final int expected = 0;

        // When
        int actual = instance.calculateNumberOfAgentsToRequest(template, numberRequested);

        // Then
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void uniqueTimestampReturnsUniqueTimestamps() throws Exception {
        long[] data = new long[512];
        for (int i = 0; i < 512; ++i) {
            data[i] = AzureUtil.getUniqueTimestamp();
        }
        java.util.HashSet<Long> uniqueTest = new java.util.HashSet<Long>();
        for (int i = 0; i < 512; ++i) {
            assert(uniqueTest.add(data[i]));
        }
    }

    private static class GetUniqueTimestampsAction implements java.util.concurrent.Callable<long[]> {
        private int iterations;

        public GetUniqueTimestampsAction(int iterations) {
            this.iterations = iterations;
        }

        @Override
        public long[] call() {
            long[] data = new long[iterations];
            for (int i = 0; i < iterations; ++i) {
                try {
                    data[i] = AzureUtil.getUniqueTimestamp();
                }
                catch(Exception e){
                    data[i] = -1;
                }
            }
            return data;
        }
    }

    @Test
    public void uniqueTimestampReturnsUniqueTimestampsThreaded() throws Exception {
        final int cpus = java.lang.Runtime.getRuntime().availableProcessors();
        final int iterationsPerTask = 900 / cpus;

        final ForkJoinPool taskPool = new ForkJoinPool(cpus);
        final ArrayList<GetUniqueTimestampsAction> tasks = new ArrayList<>();

        for(int i = 0; i < cpus; ++i){
            tasks.add(new GetUniqueTimestampsAction(iterationsPerTask));
        }

        final List<Future<long[]>> futures = taskPool.invokeAll(tasks);

        final HashSet<Long> uniqueTest = new HashSet<Long>();
        for (Future<long[]> future : futures) {
            long[] data = future.get();
            for (int i = 0; i < iterationsPerTask; ++i) {
                assertThat(data[i], not(equalTo(-1)));
                assert(uniqueTest.add(data[i]));
            }
        }
    }

    private static AzureVMAgentTemplate mkTemplate(final String templateName) {
        return new AzureVMAgentTemplate(templateName, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false, false);
    }

    private static AzureVMCloud mkInstance(int maxVMsLimitForCloud) {
        return new AzureVMCloud(null, null, Integer.toString(maxVMsLimitForCloud), null, null, null, null, null);
    }

    private static AzureVMCloud mkInstance() {
        return mkInstance(0);
    }
}
