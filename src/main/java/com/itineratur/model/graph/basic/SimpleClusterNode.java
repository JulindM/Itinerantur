package com.itineratur.model.graph.basic;

import com.itineratur.model.graph.basic.interfaces.ClusterNode;

import java.time.temporal.TemporalAmount;

public class SimpleClusterNode implements ClusterNode {
    private Integer id;
    private Integer clusterId;
    private final TemporalAmount serviceTime;

    public SimpleClusterNode(Integer id, Integer clusterId, TemporalAmount serviceTime) {
        this.id = id;
        this.clusterId = clusterId;
        this.serviceTime = serviceTime;
    }

    @Override
    public Integer getId() {
        return this.id;
    }

    @Override
    public TemporalAmount getServiceTime() {
        return this.serviceTime;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getClusterId() {
        return clusterId;
    }

    @Override
    public void setClusterId(Integer id) {
        this.clusterId = id;
    }
}
