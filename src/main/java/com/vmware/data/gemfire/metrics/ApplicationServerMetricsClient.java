package com.vmware.data.gemfire.metrics;

import com.vmware.data.gemfire.metrics.common.CommonMeterInfo;
import com.vmware.data.gemfire.metrics.exceptions.NoCacheInstanceFound;
import com.vmware.data.gemfire.metrics.exceptions.ServiceNotAvailableException;
import com.vmware.data.gemfire.metrics.server.ApplicationServerMetricsPublishingService;

import io.micrometer.prometheus.PrometheusMeterRegistry;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;

import java.util.Random;
import java.util.Scanner;

@Slf4j
@Getter
public class ApplicationServerMetricsClient {
    private PrometheusMeterRegistry prometheusMeterRegistry;
    private String applicationName;
    private ClientCache cache;
    private CommonMeterInfo commonMeterInfo;
    private Random random;

    public ApplicationServerMetricsClient(String applicationName) {
        this.applicationName = applicationName;
        commonMeterInfo = new CommonMeterInfo(applicationName);
        random = new Random();
    }

    private void run() throws ServiceNotAvailableException, NoCacheInstanceFound, InterruptedException {
        ClientCacheFactory ccf = new ClientCacheFactory();
        cache = ccf.addPoolLocator("localhost", 10334)
                .set("log-file", "client.log")
                .create();

        ApplicationServerMetricsPublishingService publishingService =
                ApplicationServerMetricsPublishingService.getInstance();

        if (publishingService == null) {
            throw new ServiceNotAvailableException("Unable to get publishing service instance");
        }

        System.out.println("\nBuilding Metrics\n");

        prometheusMeterRegistry = commonMeterInfo.createCountersAndTimers();

        publishingService.addSubregistry(prometheusMeterRegistry);

        commonMeterInfo.buildSomeMetrics();

        waitForUIScrape();

        publishingService.removeSubregistry(prometheusMeterRegistry);

        cache.close();
    }

    private void waitForUIScrape() {
        System.out.println("\nMetrics built\n\nUse URL http://localhost:9114/application-metrics to view metrics\n");
        System.out.println("\nPress \"c\" and enter to continue after viewing metrics\n");
        Scanner sc = new Scanner(System.in);
        sc.hasNext();
        System.out.println("\nStopping metrics service\n");
    }

    public static void main(String[] args) {
        ApplicationServerMetricsClient client = new ApplicationServerMetricsClient("test_client");
        try {
            client.run();
        } catch (ServiceNotAvailableException | NoCacheInstanceFound | InterruptedException ex) {
            log.error("client exception: ", ex);
        }
    }
}
