package com.abdelaziz.canary.common.util.deduplication;

public interface CanaryInternerWrapper<T> {

    T getCanonical(T value);

    void deleteCanonical(T value);
}