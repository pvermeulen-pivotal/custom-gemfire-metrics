package com.vmware.data.gemfire.metrics.exceptions;

public class RegistryExistsException extends Exception {

    public RegistryExistsException(String message) {
        super("Registry already exists " + message);
    }
}
