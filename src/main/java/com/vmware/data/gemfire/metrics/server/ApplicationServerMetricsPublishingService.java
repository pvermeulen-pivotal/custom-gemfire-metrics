package com.vmware.data.gemfire.metrics.server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import com.vmware.data.gemfire.metrics.exceptions.RegistryDoesNotExistException;
import com.vmware.data.gemfire.metrics.exceptions.RegistryExistsException;

import io.micrometer.core.instrument.MeterRegistry;

import io.micrometer.prometheus.PrometheusMeterRegistry;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.geode.metrics.MetricsPublishingService;
import org.apache.geode.metrics.MetricsSession;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;

@Slf4j
public class ApplicationServerMetricsPublishingService implements MetricsPublishingService, MetricsSession {
    private static ApplicationServerMetricsPublishingService instance;
    private static final String METRICS_PORT_PROPERTY = "metrics-port";
    private static final int DEFAULT_METRICS_PORT = 9115;
    private static final String CAPTURE_JVM_GEMFIRE_STATS_PROPERTY = "capture-jvm-gemfire-stats";
    private static final boolean DEFAULT_CAPTURE_JVM_GEMFIRE_STATS = false;
    private static final String HOST_NAME = "localhost";
    private Collection<PrometheusMeterRegistry> persistentMeterRegistries;
    private int port;
    private boolean statsJvmGemfire;
    private HttpServer server;
    private HttpContext context;
    private MetricsSession metricsSession;

    public static ApplicationServerMetricsPublishingService getInstance() {
        return instance;
    }

    private void clearAndCloseMeterRegistry() {
        persistentMeterRegistries.forEach(mr -> {
            mr.clear();
            mr.close();
        });
    }

    @Override
    public void start(MetricsSession metricsSession) {
        instance = this;

        this.metricsSession = metricsSession;

        persistentMeterRegistries = new ArrayList<>();

        String str = System.getProperty(METRICS_PORT_PROPERTY);
        if (str != null && str.length() > 0) {
            port = Integer.parseInt(str);
        } else {
            port = DEFAULT_METRICS_PORT;
        }

        str = System.getProperty(CAPTURE_JVM_GEMFIRE_STATS_PROPERTY);
        if (str != null && str.length() > 0) {
            statsJvmGemfire = Boolean.parseBoolean(str);
        } else {
            statsJvmGemfire = DEFAULT_CAPTURE_JVM_GEMFIRE_STATS;
        }

        InetSocketAddress address = new InetSocketAddress(HOST_NAME, port);

        server = null;
        try {
            server = HttpServer.create(address, 0);
            context = server.createContext("/application-metrics");
            context.setHandler(this::requestHandler);
            server.start();
            int boundPort = server.getAddress().getPort();
            log.info("Started {} http://{}:{}/", getClass().getSimpleName(), HOST_NAME, boundPort);
        } catch (Exception ex) {
            log.error("Error while starting HTTPServer {}", getClass().getSimpleName(), ex);
        }
    }

    private void requestHandler(HttpExchange httpExchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        persistentMeterRegistries.forEach(r -> {
            sb.append(r.scrape());
        });
        final byte[] scrapeBytes = sb.toString().getBytes();
        httpExchange.sendResponseHeaders(200, scrapeBytes.length);
        final OutputStream responseBody = httpExchange.getResponseBody();
        responseBody.write(scrapeBytes);
        responseBody.close();
    }

    @Override
    public void stop(MetricsSession metricsSession) {
        clearAndCloseMeterRegistry();
        server.removeContext(context);
        server.stop(0);
    }

    public Boolean getRegistry(MeterRegistry meterRegistry) {
        return persistentMeterRegistries.contains((PrometheusMeterRegistry) meterRegistry);
    }

    @SneakyThrows
    @Override
    public void addSubregistry(MeterRegistry meterRegistry) {
        if (!persistentMeterRegistries.contains(meterRegistry)) {
            persistentMeterRegistries.add((PrometheusMeterRegistry) meterRegistry);
            if (statsJvmGemfire) {
                metricsSession.addSubregistry(meterRegistry);
            }
        } else {
            throw new RegistryExistsException("Registry exists for " + meterRegistry.toString());
        }
    }

    @SneakyThrows
    @Override
    public void removeSubregistry(MeterRegistry meterRegistry) {
        if (persistentMeterRegistries.contains(meterRegistry)) {
            persistentMeterRegistries.remove(meterRegistry);
            if (statsJvmGemfire) {
                metricsSession.removeSubregistry(meterRegistry);
            }
        } else {
            throw new RegistryDoesNotExistException("Registry does not exist for " + meterRegistry.toString());
        }
    }
}
