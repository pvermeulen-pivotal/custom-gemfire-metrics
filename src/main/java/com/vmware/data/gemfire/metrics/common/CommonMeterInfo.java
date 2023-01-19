package com.vmware.data.gemfire.metrics.common;

import com.vmware.data.gemfire.metrics.exceptions.NoCacheInstanceFound;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Tag;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

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
    private Counter messagesReceivedCounter;
    private Counter messagesProducedCounter;
    private Histogram messageProcessingTime;
    private Gauge messageQueueSizeGauge;
    private String applicationName;

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public CommonMeterInfo() {
    }

    public CommonMeterInfo(String applicatioName) {
        this.applicationName = applicatioName;
    }

    public PrometheusMeterRegistry createCountersAndTimers() throws NoCacheInstanceFound {
        CollectorRegistry registry = CollectorRegistry.defaultRegistry;

        messagesReceivedCounter = Counter.build(this.applicationName + "_messages_received", "Number of messages received by " + this.applicationName)
                .register(registry);

        messagesProducedCounter = Counter.build(this.applicationName + "_messages_published", "Number of messages produced by " + this.applicationName)
                .register(registry);

        messageProcessingTime = Histogram.build(this.applicationName + "_processing_time", "Time to process message for application " + this.applicationName)
                .register(registry);

        messageQueueSizeGauge = Gauge.build(this.applicationName + "_processing_queue_size", "Message queue depth")
                .register(registry);

        PrometheusMeterRegistry prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, registry, Clock.SYSTEM);
        prometheusMeterRegistry.config().commonTags(addCommonTags(false,false));

        return prometheusMeterRegistry;
    }

    public void buildSomeMetrics() {
        Long sleepTime = 500L;
        int counter = 0;
        messageQueueSizeGauge.set(0d);

        do {
            messagesReceivedCounter.inc(2d);
            messageQueueSizeGauge.set(100d);
            Histogram.Timer timer = messageProcessingTime.startTimer();
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ex) {
                // do nothing
            }
            messageQueueSizeGauge.dec(10d);
            counter++;
            timer.observeDuration();
            timer.close();
            sleepTime = sleepTime + 100L;
            messagesProducedCounter.inc(4d);
        } while (counter <= 5);
    }


    public Iterable<Tag> addCommonTags(boolean hasLocators, boolean hasCacheServer) throws NoCacheInstanceFound {
        ClientCache clientCache = null;
        InternalCache cache = null;
        int clusterId = 0;
        String memberName = null;
        String hostName = null;
        boolean isClient = false;

        try {
            clientCache = ClientCacheFactory.getAnyInstance();
            memberName = clientCache.getDistributedSystem().getDistributedMember().getName();
            hostName = clientCache.getDistributedSystem().getDistributedMember().getHost();
            isClient = true;
        } catch (Exception ex) {
            log.warn("Not using client cache");
        }

        try {
            cache = (InternalCache) CacheFactory.getAnyInstance();
            clusterId = cache.getInternalDistributedSystem().getConfig().getDistributedSystemId();
            memberName = cache.getInternalDistributedSystem().getName();
            hostName = cache.getInternalDistributedSystem().getDistributedMember().getHost();
        } catch (Exception ex) {
            log.warn("Not using server cache");
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
            if (!isClient) {
                tags.add(Tag.of("cluster", String.valueOf(clusterId)));
            }

            if (!memberName.isEmpty()) {
                tags.add(Tag.of("member", memberName));
            }

            tags.add(Tag.of("host", hostName));
            tags.add(Tag.of("member.type", memberTypeFor(isClient, hasLocators, hasCacheServer)));

            Tag[] aTags = new Tag[tags.size()];
            tags.toArray(aTags);
            Iterable iterable = () -> Arrays.stream(aTags).iterator();
            return iterable;
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
