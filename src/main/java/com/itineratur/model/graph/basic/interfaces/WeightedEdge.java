package com.itineratur.model.graph.basic.interfaces;

import com.itineratur.model.graph.basic.enums.ConnectionType;

public interface WeightedEdge<NodeType extends ClusterNode> extends Edge<NodeType> {
    Integer getCost();
    ConnectionType getConnectionType();
}
