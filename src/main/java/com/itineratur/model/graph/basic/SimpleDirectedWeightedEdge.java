package com.itineratur.model.graph.basic;

import com.itineratur.model.graph.basic.enums.ConnectionType;
import com.itineratur.model.graph.basic.interfaces.ClusterNode;
import com.itineratur.model.graph.basic.interfaces.WeightedEdge;
import org.apache.commons.lang3.tuple.Pair;

public class SimpleDirectedWeightedEdge<NodeType extends ClusterNode> extends DirectedEdge<NodeType> implements WeightedEdge<NodeType> {
    private final Integer cost;
    private final ConnectionType transportationType;

    public SimpleDirectedWeightedEdge(NodeType a, NodeType b, Integer cost, ConnectionType transportationType) {
        super(a, b);
        this.cost = cost;
        this.transportationType = transportationType;
    }

    public SimpleDirectedWeightedEdge(Pair<NodeType, NodeType> a, Integer cost, ConnectionType transportationType) {
        super(a);
        this.cost = cost;
        this.transportationType = transportationType;
    }

    @Override
    public Integer getCost() {
        return this.cost;
    }

    @Override
    public ConnectionType getConnectionType() {
        return transportationType;
    }
}
