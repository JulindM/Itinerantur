package com.itineratur.model.graph;

import com.itineratur.model.graph.basic.Cluster;
import com.itineratur.model.graph.basic.interfaces.ClusterNode;
import com.itineratur.model.graph.basic.interfaces.ClusteredGraph;
import com.itineratur.model.graph.basic.interfaces.Edge;
import com.itineratur.util.model.GraphTools;

import java.util.List;

public class SimpleClusteredGraph<NodeType extends ClusterNode, EdgeType extends Edge<NodeType>> extends SimpleGraph<NodeType, EdgeType> implements ClusteredGraph<NodeType, EdgeType> {
    private final List<Cluster<NodeType>> clusterList;

    public SimpleClusteredGraph(List<Cluster<NodeType>> clusterList, List<EdgeType> edges) {
        super(null, edges);
        this.clusterList = clusterList;
    }

    @Override
    public List<Cluster<NodeType>> getClusters() {
        return this.clusterList;
    }

    @Override
    public List<NodeType> getNodes() {
        return GraphTools.getNodesFromClusters(this.clusterList);
    }
}
