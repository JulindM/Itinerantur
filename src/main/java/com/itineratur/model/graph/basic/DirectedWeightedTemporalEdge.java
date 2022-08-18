package com.itineratur.model.graph.basic;

import com.itineratur.model.graph.basic.enums.ConnectionType;
import com.itineratur.model.graph.basic.interfaces.ClusterNode;
import com.itineratur.model.graph.basic.interfaces.WeightedTemporalEdge;
import org.apache.commons.lang3.tuple.Pair;

import java.time.temporal.TemporalAmount;

public class DirectedWeightedTemporalEdge<NodeType extends ClusterNode> extends SimpleDirectedWeightedEdge<NodeType> implements WeightedTemporalEdge<NodeType> {
    private final TemporalAmount travelTime;

    public DirectedWeightedTemporalEdge(NodeType a, NodeType b, Integer cost, TemporalAmount travelTime, ConnectionType connectionType) {
        super(a, b, cost, connectionType);
        this.travelTime = travelTime;
    }

    public DirectedWeightedTemporalEdge(Pair<NodeType, NodeType> a, Integer cost, TemporalAmount travelTime, ConnectionType connectionType) {
        super(a, cost, connectionType);
        this.travelTime = travelTime;
    }

    public TemporalAmount getTravelTime() {
        return travelTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != DirectedWeightedTemporalEdge.class) {return false;}
        return this.getNodePair().getLeft().equals(((DirectedWeightedTemporalEdge) obj).getNodePair().getLeft()) &&
                this.getNodePair().getRight().equals(((DirectedWeightedTemporalEdge) obj).getNodePair().getRight());
    }
}
