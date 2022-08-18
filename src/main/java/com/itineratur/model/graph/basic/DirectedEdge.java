package com.itineratur.model.graph.basic;

import com.itineratur.model.graph.basic.interfaces.ClusterNode;
import com.itineratur.model.graph.basic.interfaces.Edge;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class DirectedEdge<NodeType extends ClusterNode> implements Edge<NodeType> {
    Pair<NodeType, NodeType> nodeTypePair;

    public DirectedEdge(NodeType a, NodeType b) {
        this.nodeTypePair = new ImmutablePair<>(a, b);
    }

    public DirectedEdge(Pair<NodeType, NodeType> a) {
        this.nodeTypePair = a;
    }

    @Override
    public Pair<NodeType, NodeType> getNodePair() {
        return this.nodeTypePair;
    }

    @Override
    public void setNodePair(NodeType left, NodeType right) {
        this.nodeTypePair = new ImmutablePair<>(left, right);
    }
}
