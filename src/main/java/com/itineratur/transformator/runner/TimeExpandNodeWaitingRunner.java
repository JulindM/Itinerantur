package com.itineratur.transformator.runner;

import com.itineratur.model.graph.basic.Cluster;
import com.itineratur.model.graph.basic.DirectedWeightedTemporalEdge;
import com.itineratur.model.graph.basic.SimpleTimeWindowClusterNode;
import com.itineratur.model.graph.basic.SimpleTimeExpandedClusterNode;
import com.itineratur.model.graph.basic.enums.ConnectionType;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class TimeExpandNodeWaitingRunner implements Callable<Pair<Integer, Pair<List<SimpleTimeExpandedClusterNode>, List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>>>>> {
    private final Cluster<SimpleTimeWindowClusterNode> cluster;
    private final Integer granularity;

    public TimeExpandNodeWaitingRunner(Cluster<SimpleTimeWindowClusterNode> cluster, Integer granularity) {
        this.cluster = cluster;
        this.granularity = granularity;
    }

    @Override
    public Pair<Integer, Pair<List<SimpleTimeExpandedClusterNode>, List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>>>> call() {
        List<SimpleTimeExpandedClusterNode> expandedNodes = new ArrayList<>();
        List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> expandedIntraClusterEdges = new ArrayList<>();

        for (SimpleTimeWindowClusterNode node : this.cluster.getNodes()) {
            Range<LocalTime> timeRange = Range.between(node.getTimeWindow().getLeft(), node.getTimeWindow().getRight());

            LocalTime tempTime = node.getTimeWindow().getLeft();

            SimpleTimeExpandedClusterNode prevNode = new SimpleTimeExpandedClusterNode(node.getId(), node.getClusterId(), tempTime, node.getServiceTime());
            expandedNodes.add(prevNode);

            tempTime = tempTime.plus(Duration.ofMinutes(granularity.longValue()));
            int added = 1;
            while (timeRange.contains(tempTime) && added <= 1440) {
                SimpleTimeExpandedClusterNode expandedNode = new SimpleTimeExpandedClusterNode(node.getId(), node.getClusterId(), tempTime, node.getServiceTime());

                expandedNodes.add(expandedNode);
                expandedIntraClusterEdges.add(new DirectedWeightedTemporalEdge<>(prevNode, expandedNode, 1, Duration.ofMinutes(this.granularity), ConnectionType.WAITING));

                prevNode = expandedNode;
                tempTime = tempTime.plus(Duration.ofMinutes(granularity.longValue()));
                added++;
            }

            if (tempTime.isAfter(node.getTimeWindow().getRight()) && !node.getTimeWindow().getRight().equals(expandedNodes.get(expandedNodes.size() - 1).getTime())) {
                SimpleTimeExpandedClusterNode lastNode = new SimpleTimeExpandedClusterNode(node.getId(), node.getClusterId(), node.getTimeWindow().getRight(), node.getServiceTime());
                expandedIntraClusterEdges.add(new DirectedWeightedTemporalEdge<>(expandedNodes.get(expandedNodes.size() - 1), lastNode, 1, Duration.ofMinutes(0), ConnectionType.WAITING));
                expandedNodes.add(lastNode);
            }
        }

        return new ImmutablePair<>(this.cluster.getId(), new ImmutablePair<>(expandedNodes, expandedIntraClusterEdges));
    }
}


