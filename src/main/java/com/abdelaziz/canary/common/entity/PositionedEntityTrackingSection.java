package com.abdelaziz.canary.common.entity;

public interface PositionedEntityTrackingSection {
    void setPos(long chunkSectionPos);

    long getPos();
}