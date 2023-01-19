package com.vmware.data.gemfire.metrics.exceptions;

public class RegistryDoesNotExistException extends Exception {

    public RegistryDoesNotExistException(String message) {
        super("Registry does not exist " + message);
    }
}
