package com.itineratur.model.graph.basic.interfaces;

import java.time.temporal.TemporalAmount;

public interface WeightedTemporalEdge<NodeType extends ClusterNode> extends WeightedEdge<NodeType> {
    TemporalAmount getTravelTime();
}
