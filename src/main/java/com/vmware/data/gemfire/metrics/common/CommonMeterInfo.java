package com.vmware.data.gemfire.metrics.common;

import com.vmware.data.gemfire.metrics.exceptions.NoCacheInstanceFound;

import io.micrometer.core.instrument.*;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import io.prometheus.client.CollectorRegistry;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.internal.cache.InternalCache;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Slf4j
public class CommonMeterInfo {
    private io.micrometer.core.instrument.Counter messagesReceivedCounter;
    private io.micrometer.core.instrument.Counter messagesProducedCounter;
    private io.micrometer.core.instrument.Timer messageProcessingTime;
    private PrometheusMeterRegistry prometheusMeterRegistry;
    private Iterable<Tag> applicationTags;
    private NumberGauge messageQueueSizeGauge;
    private boolean client = false;

    @Setter
    private String applicationName;

    public CommonMeterInfo(boolean client) {
        this.client = client;
    }

    public CommonMeterInfo(String applicationName) {
        this.applicationName = applicationName;
    }

    public CommonMeterInfo(String applicationName, boolean client) {
        this.applicationName = applicationName;
        this.client = client;
    }

    public PrometheusMeterRegistry createCountersAndTimers() throws NoCacheInstanceFound {
        CollectorRegistry registry = CollectorRegistry.defaultRegistry;

        prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, registry, Clock.SYSTEM);

        if (client) {
            applicationTags = addCommonTags(false,false);
        } else {
            applicationTags = addCommonTags(true,true);
        }

        messagesReceivedCounter = prometheusMeterRegistry.counter(this.applicationName + "_messages_received", applicationTags);

        messagesProducedCounter = prometheusMeterRegistry.counter(this.applicationName + "_messages_published", applicationTags);

        messageProcessingTime = prometheusMeterRegistry.timer(this.applicationName + "_processing_time", applicationTags);

        messageQueueSizeGauge = prometheusMeterRegistry.gauge(this.applicationName + "_processing_queue_size", applicationTags, new NumberGauge(0));

        return prometheusMeterRegistry;
    }

    public void buildSomeMetrics() {
        Long sleepTime = 500L;
        int counter = 0;
        int sizeOfQueue = 80;

        do {
            messagesReceivedCounter.increment(2d);
            messageQueueSizeGauge.setNewValue(sizeOfQueue);
            Timer.Sample sample = Timer.start();
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ex) {
                // do nothing
            }
            sizeOfQueue = sizeOfQueue - 10;
            counter++;
            long time = sample.stop(messageProcessingTime);
            sleepTime = sleepTime + 100L;
            messagesProducedCounter.increment(4d);
        } while (counter <= 5);
    }

    public Tags addCommonTags(boolean hasLocators, boolean hasCacheServer) throws NoCacheInstanceFound {
        ClientCache clientCache = null;
        InternalCache cache = null;
        int clusterId = 0;
        String memberName = null;
        String hostName = null;

        if (client) {
            try {
                clientCache = ClientCacheFactory.getAnyInstance();
                memberName = clientCache.getDistributedSystem().getDistributedMember().getName();
                hostName = clientCache.getDistributedSystem().getDistributedMember().getHost();
            } catch (Exception ex) {
                log.warn("No client cache was found");
            }
        } else {
            try {
                cache = (InternalCache) CacheFactory.getAnyInstance();
                clusterId = cache.getInternalDistributedSystem().getConfig().getDistributedSystemId();
                memberName = cache.getInternalDistributedSystem().getName();
                hostName = cache.getInternalDistributedSystem().getDistributedMember().getHost();
            } catch (Exception ex) {
                log.warn("No server cache was found");
            }
        }

        if (clientCache == null && cache == null) {
            throw new NoCacheInstanceFound("No CacheFactory or ClientCacheFactory instance found: ");
        }

        Objects.requireNonNull(memberName, "Member Name is null.");
        Objects.requireNonNull(hostName, "Host Name is null.");

        if (hostName.isEmpty()) {
            throw new IllegalArgumentException("Host name must not be empty");
        } else {
            Set<Tag> tags = new HashSet();
            if (!client) {
                tags.add(Tag.of("cluster", String.valueOf(clusterId)));
            }

            if (!memberName.isEmpty()) {
                tags.add(Tag.of("member", memberName));
            }

            tags.add(Tag.of("host", hostName));
            tags.add(Tag.of("type", memberTypeFor(client, hasLocators, hasCacheServer)));

            Tag[] aTags = new Tag[tags.size()];
            tags.toArray(aTags);
            Iterable iterable = () -> Arrays.stream(aTags).iterator();

            return Tags.of(iterable);
        }
    }

    private String memberTypeFor(boolean hasClient, boolean hasLocator, boolean hasServer) {
        if (hasServer && hasLocator) {
            return "server-locator";
        } else if (hasServer) {
            return "server";
        } else if (hasClient) {
            return "client";
        } else {
            return hasLocator ? "locator" : "embedded-cache";
        }
    }
}
