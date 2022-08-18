package com.itineratur.model.graph.basic.interfaces;

import org.apache.commons.lang3.tuple.Pair;

public interface Edge <NodeType extends ClusterNode> {
    Pair<NodeType, NodeType> getNodePair();
    void setNodePair(NodeType left, NodeType  right);
}
