package com.itineratur.model.graph.basic;

import com.itineratur.model.graph.basic.interfaces.ClusterNode;

import java.util.List;

public class Cluster <NodeType extends ClusterNode> {
    private Integer id;
    private final List<NodeType> nodes;

    public Cluster(Integer id, List<NodeType> nodes) {
        this.id = id;
        this.nodes = nodes;
    }

    public List<NodeType> getNodes() {
        return this.nodes;
    }

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
}
