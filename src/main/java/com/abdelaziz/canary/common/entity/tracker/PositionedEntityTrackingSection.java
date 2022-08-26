package com.abdelaziz.canary.common.entity.tracker;

public interface PositionedEntityTrackingSection {
    void setPos(long chunkSectionPos);

    long getPos();
}
