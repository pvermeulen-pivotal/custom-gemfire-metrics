package com.vmware.data.gemfire.metrics.server;

import com.vmware.data.gemfire.metrics.common.CommonMeterInfo;
import com.vmware.data.gemfire.metrics.exceptions.RegistryDoesNotExistException;
import com.vmware.data.gemfire.metrics.exceptions.RegistryExistsException;
import com.vmware.data.gemfire.metrics.exceptions.ServiceNotAvailableException;

import io.micrometer.core.instrument.MeterRegistry;

import io.micrometer.prometheus.PrometheusMeterRegistry;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.internal.cache.InternalCache;

@Slf4j
public class ApplicationServerMetricsJMXFunction implements Function {
    private PrometheusMeterRegistry prometheusMeterRegistry;
    private String applicationName;
    private InternalCache cache;
    private CommonMeterInfo commonMeterInfo;
    private ApplicationServerMetricsPublishingService publishingService;

    public ApplicationServerMetricsJMXFunction() throws ServiceNotAvailableException {
        cache = (InternalCache) CacheFactory.getAnyInstance();
        publishingService = ApplicationServerMetricsPublishingService.getInstance();
        if (publishingService == null) {
            throw new ServiceNotAvailableException("Publishing service has not been provisioned");
        }
        commonMeterInfo = new CommonMeterInfo(false);
    }

    private void addSubRegistry(MeterRegistry meterRegistry) throws RegistryExistsException {
        if (Boolean.TRUE.equals(publishingService.getRegistry(meterRegistry))) {
            throw new RegistryExistsException(meterRegistry.toString());
        } else {
            publishingService.addSubregistry(meterRegistry);
        }
    }

    private void removeSubRegistry(MeterRegistry meterRegistry) throws RegistryDoesNotExistException {
        if (Boolean.FALSE.equals(publishingService.getRegistry(meterRegistry))) {
            throw new RegistryDoesNotExistException(meterRegistry.toString());
        } else {
            publishingService.removeSubregistry(meterRegistry);
        }
    }

    @Override
    public boolean hasResult() {
        return true;
    }

    @SneakyThrows
    @Override
    public void execute(FunctionContext functionContext) {
        String[] params = (String[]) functionContext.getArguments();
        applicationName = params[0];
        if ("start".equals(params[1].toLowerCase())) {
            commonMeterInfo.setApplicationName(applicationName);
            prometheusMeterRegistry = commonMeterInfo.createCountersAndTimers();
            try {
                addSubRegistry(prometheusMeterRegistry);
                commonMeterInfo.buildSomeMetrics();
                functionContext.getResultSender().lastResult(true);
            } catch (RegistryExistsException ex) {
                log.warn("Registry exists {}", ex.getMessage());
                functionContext.getResultSender().lastResult(false);
            }
        } else {
            if (prometheusMeterRegistry != null) {
                try {
                    removeSubRegistry(prometheusMeterRegistry);
                    functionContext.getResultSender().lastResult(true);
                } catch (RegistryDoesNotExistException ex) {
                    log.warn("Registry does not exist {}", ex.getMessage());
                    functionContext.getResultSender().lastResult(false);
                }
            } else {
                throw new RegistryDoesNotExistException("The registry was never created");
            }
        }
    }

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    @Override
    public boolean optimizeForWrite() {
        return false;
    }

    @Override
    public boolean isHA() {
        return false;
    }
}
