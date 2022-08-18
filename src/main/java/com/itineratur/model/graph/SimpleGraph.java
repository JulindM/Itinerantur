package com.itineratur.model.graph;

import com.itineratur.model.graph.basic.interfaces.ClusterNode;
import com.itineratur.model.graph.basic.interfaces.Edge;
import com.itineratur.model.graph.basic.interfaces.Graph;

import java.util.List;
import java.util.stream.Collectors;

public class SimpleGraph<NodeType extends ClusterNode, EdgeType extends Edge<NodeType>> implements Graph<NodeType, EdgeType> {
    private final List<NodeType> nodes;
    private final List<EdgeType> edges;

    public SimpleGraph(List<NodeType> nodes, List<EdgeType> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    @Override
    public List<NodeType> getNodes() {
        return this.nodes;
    }

    @Override
    public List<EdgeType> getAllEdges() {
        return this.edges;
    }

    @Override
    public List<EdgeType> getEdges(NodeType a, NodeType b) {
        List<EdgeType> found = this.edges.stream().filter(
                edge -> edge.getNodePair().getLeft().equals(a) && edge.getNodePair().getRight().equals(b)
        ).collect(Collectors.toList());
        return found.size() >= 1 ? found : null;
    }
}
