package com.itineratur.model.graph.basic;

import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalTime;
import java.time.temporal.TemporalAmount;

public class SimpleTimeWindowClusterNode extends SimpleClusterNode {
    private final Pair<LocalTime, LocalTime> timeWindow;

    public SimpleTimeWindowClusterNode(Integer id, Integer clusterId, Pair<LocalTime, LocalTime> timeWindow, TemporalAmount serviceTime) {
        super(id, clusterId, serviceTime);
        this.timeWindow = timeWindow;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != SimpleTimeWindowClusterNode.class) {return false;}
        return this.getId().equals(((SimpleTimeWindowClusterNode) obj).getId())
                && this.getTimeWindow().equals(((SimpleTimeWindowClusterNode) obj).getTimeWindow());
    }

    public Pair<LocalTime, LocalTime> getTimeWindow() {
        return this.timeWindow;
    }
}
