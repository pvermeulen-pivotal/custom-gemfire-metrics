package com.vmware.data.gemfire.metrics.common;

public class NumberGauge extends Number {
    private Integer value;

    public NumberGauge(int value) {
        this.value = value;
    }

    public void setNewValue(int value) {
        this.value = value;
    }

    @Override
    public int intValue() {
        return this.value;
    }

    @Override
    public long longValue() {
        return Long.parseLong(this.value.toString());
    }

    @Override
    public float floatValue() {
        return Float.parseFloat(this.value.toString());
    }

    @Override
    public double doubleValue() {
        return Double.parseDouble(this.value.toString());
    }
}
