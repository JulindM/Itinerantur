package com.itineratur.model.graph.basic;

import com.itineratur.model.graph.basic.enums.ConnectionType;
import com.itineratur.model.graph.basic.interfaces.ClusterNode;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalTime;
import java.time.temporal.TemporalAmount;

public class SpecialTimedNamedDirectedWeightedTemporalEdge<NodeType extends ClusterNode> extends DirectedWeightedTemporalEdge<NodeType>  {
    private final String name;
    private final LocalTime time;

    public SpecialTimedNamedDirectedWeightedTemporalEdge(NodeType a, NodeType b, Integer cost, TemporalAmount travelTime, ConnectionType connectionType, String name, LocalTime time) {
        super(a, b, cost, travelTime, connectionType);
        this.name = name;
        this.time = time;
    }

    public SpecialTimedNamedDirectedWeightedTemporalEdge(Pair<NodeType, NodeType> a, Integer cost, TemporalAmount travelTime, ConnectionType connectionType, String name, LocalTime time) {
        super(a, cost, travelTime, connectionType);
        this.name = name;
        this.time = time;
    }


    public String getName() {
        return name;
    }

    public LocalTime getTime() {
        return time;
    }
}
