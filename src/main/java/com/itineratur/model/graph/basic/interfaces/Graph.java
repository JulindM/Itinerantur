package com.itineratur.model.graph.basic.interfaces;

import java.util.List;

public interface Graph <NodeType extends ClusterNode, EdgeType extends Edge<NodeType>> {
    List<NodeType> getNodes();
    List<EdgeType> getAllEdges();

    List<EdgeType> getEdges(NodeType a, NodeType b);
}
