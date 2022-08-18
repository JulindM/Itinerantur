package com.itineratur.model.graph.basic.interfaces;

import com.itineratur.model.graph.basic.Cluster;

import java.util.List;

public interface ClusteredGraph <NodeType extends ClusterNode, EdgeType extends Edge<NodeType>> extends Graph<NodeType, EdgeType> {
    List<Cluster<NodeType>> getClusters();
}
