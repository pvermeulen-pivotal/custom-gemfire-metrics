package com.vmware.data.gemfire.metrics.exceptions;

public class ServiceNotAvailableException extends Exception {
    public ServiceNotAvailableException(String message) {
        super("Application Server Metrics Publishing Service not Available for " + message);
    }
}
