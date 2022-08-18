package com.itineratur.model.graph.basic;

import java.time.LocalTime;
import java.time.temporal.TemporalAmount;

public class SimpleTimeExpandedClusterNode extends SimpleClusterNode {
    private final LocalTime time;
    private final String uniqueExpandedId;

    public SimpleTimeExpandedClusterNode(Integer id, Integer clusterId, LocalTime time, TemporalAmount serviceTime) {
        super(id, clusterId, serviceTime);
        this.time = time;
        this.uniqueExpandedId = this.getId() + this.time.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != SimpleTimeExpandedClusterNode.class) {return false;}
        return this.getId().equals(((SimpleTimeExpandedClusterNode) obj).getId())
                && this.getClusterId().equals(((SimpleTimeExpandedClusterNode) obj).getClusterId())
                && this.time.equals(((SimpleTimeExpandedClusterNode) obj).getTime());
    }

    public LocalTime getTime() {
        return this.time;
    }

    public String getUniqueTimeExpandedId() {
        return this.uniqueExpandedId;
    }
}
