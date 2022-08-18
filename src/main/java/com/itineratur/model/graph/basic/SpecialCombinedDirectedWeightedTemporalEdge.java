package com.itineratur.model.graph.basic;

import com.itineratur.model.graph.basic.enums.ConnectionType;
import com.itineratur.model.graph.basic.interfaces.ClusterNode;
import org.apache.commons.lang3.tuple.Pair;

import java.time.temporal.TemporalAmount;
import java.util.List;

public class SpecialCombinedDirectedWeightedTemporalEdge<NodeType extends ClusterNode> extends DirectedWeightedTemporalEdge<NodeType> {
    private final List<DirectedWeightedTemporalEdge<NodeType>> waitingEdges;
    private final DirectedWeightedTemporalEdge<NodeType> mainEdge;

    public SpecialCombinedDirectedWeightedTemporalEdge(NodeType a,
                                                       NodeType b,
                                                       Integer cost,
                                                       TemporalAmount travelTime,
                                                       List<DirectedWeightedTemporalEdge<NodeType>> waitingEdges,
                                                       DirectedWeightedTemporalEdge<NodeType> mainEdge) {
        super(a, b, cost, travelTime, ConnectionType.COMBINED);
        this.waitingEdges = waitingEdges;
        this.mainEdge = mainEdge;
    }

    public SpecialCombinedDirectedWeightedTemporalEdge(Pair<NodeType, NodeType> a,
                                                       Integer cost,
                                                       TemporalAmount travelTime,
                                                       List<DirectedWeightedTemporalEdge<NodeType>> waitingNodes,
                                                       DirectedWeightedTemporalEdge<NodeType> mainEdge) {
        super(a, cost, travelTime, ConnectionType.COMBINED);
        this.waitingEdges = waitingNodes;
        this.mainEdge = mainEdge;
    }

    public List<DirectedWeightedTemporalEdge<NodeType>> getWaitingEdges() {
        return waitingEdges;
    }

    public DirectedWeightedTemporalEdge<NodeType> getMainEdge() {
        return mainEdge;
    }
}
