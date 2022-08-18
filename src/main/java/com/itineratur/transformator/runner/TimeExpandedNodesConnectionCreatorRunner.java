package com.itineratur.transformator.runner;

import com.itineratur.model.graph.basic.*;
import com.itineratur.model.graph.basic.enums.ConnectionType;
import com.itineratur.model.graph.basic.interfaces.ClusteredGraph;
import com.itineratur.model.graph.basic.interfaces.Edge;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.MINUTES;

public class TimeExpandedNodesConnectionCreatorRunner implements Callable<List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>>> {
    private final SimpleTimeWindowClusterNode node;
    private final ClusteredGraph<SimpleTimeWindowClusterNode, DirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode>> timeWindowClusteredGraph;
    private final ClusteredGraph<SimpleTimeExpandedClusterNode, DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> timedClusteredGraph;
    private final Integer granularity;

    public TimeExpandedNodesConnectionCreatorRunner(SimpleTimeWindowClusterNode node,
                                                    ClusteredGraph<SimpleTimeWindowClusterNode, DirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode>> timeWindowClusteredGraph,
                                                    ClusteredGraph<SimpleTimeExpandedClusterNode, DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> timedClusteredGraph,
                                                    Integer granularity) {
        this.node = node;
        this.timeWindowClusteredGraph = timeWindowClusteredGraph;
        this.timedClusteredGraph = timedClusteredGraph;
        this.granularity = granularity;
    }

    @Override
    public List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> call() {
        List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> connectingEdges = new ArrayList<>();

        List<Cluster<SimpleTimeWindowClusterNode>> clustersToLook =
                this.timeWindowClusteredGraph.getClusters().stream().filter(cluster -> !cluster.getId().equals(node.getClusterId())).collect(Collectors.toList());

        final List<SimpleTimeWindowClusterNode> destinationTimeWindowNodes = new ArrayList<>();
        clustersToLook.forEach(simpleTimeWindowNodeCluster ->
                simpleTimeWindowNodeCluster.getNodes()
                        .stream()
                        .map(toCheck -> this.timeWindowClusteredGraph.getEdges(this.node, toCheck))
                        .filter(Objects::nonNull)
                        .map(list -> list.get(0))
                        .map(Edge::getNodePair)
                        .forEach(toAdd -> destinationTimeWindowNodes.add(toAdd.getRight())));

        List<SimpleTimeExpandedClusterNode> originTimeExpandedNodes = this.timedClusteredGraph.getNodes().stream().filter(toCheck -> toCheck.getId().equals(this.node.getId())).collect(Collectors.toList());
        for (SimpleTimeWindowClusterNode outboundNode : destinationTimeWindowNodes) {
            List<DirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode>> edges = this.timeWindowClusteredGraph.getEdges(this.node, outboundNode);

            for (SimpleTimeExpandedClusterNode fromNode : originTimeExpandedNodes) {
                List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> hits = this.timedClusteredGraph.getNodes()
                        .stream()
                        .filter(toNode -> toNode.getId().equals(outboundNode.getId()))
                        .map(toNode -> getCheapestConnectionIfPossible(fromNode, toNode, edges))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                connectingEdges.addAll(hits);
            }
        }

        return connectingEdges;
    }

    private DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode> getCheapestConnectionIfPossible(SimpleTimeExpandedClusterNode fromNode, SimpleTimeExpandedClusterNode toNode, List<DirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode>> edges) {
        List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> waitingNodesInclusive =
                timedClusteredGraph.getAllEdges()
                        .stream()
                        .filter(edge -> edge.getNodePair().getLeft().getId().equals(fromNode.getId()) && edge.getConnectionType() == ConnectionType.WAITING)
                        .filter(edge -> MINUTES.between(fromNode.getTime(), edge.getNodePair().getLeft().getTime()) >= 0)
                        .sorted(Comparator.comparing(e -> e.getNodePair().getLeft().getTime()))
                        .collect(Collectors.toList());

        List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> actualWaitedEdges = new ArrayList<>();
        DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode> cheapestBetweenTwoNodes = getCheapestBetweenTwoNodesWithFilter(fromNode, toNode, edges, null);

        List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> counterWaitedEdges = new ArrayList<>();
        for (DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode> waitingEdge : waitingNodesInclusive) {
            counterWaitedEdges.add(waitingEdge);
            DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode> cheapestBetweenTwoNodesAfterWaiting = getCheapestBetweenTwoNodesWithFilter(waitingEdge.getNodePair().getRight(), toNode, edges, ConnectionType.TRAIN);
            if (cheapestBetweenTwoNodes == null && cheapestBetweenTwoNodesAfterWaiting != null
                    || cheapestBetweenTwoNodes != null && cheapestBetweenTwoNodesAfterWaiting != null && cheapestBetweenTwoNodesAfterWaiting.getCost() < cheapestBetweenTwoNodes.getCost()) {
                actualWaitedEdges.addAll(counterWaitedEdges);
                cheapestBetweenTwoNodes = cheapestBetweenTwoNodesAfterWaiting;
            }
        }

        if (actualWaitedEdges.size() > 0) {
            return new SpecialCombinedDirectedWeightedTemporalEdge<>(
                    fromNode,
                    toNode,
                    actualWaitedEdges.stream().map(SimpleDirectedWeightedEdge::getCost).reduce(0, Integer::sum) + cheapestBetweenTwoNodes.getCost(),
                    Duration.ofMinutes((long) actualWaitedEdges.size() * this.granularity).plus((Duration) cheapestBetweenTwoNodes.getTravelTime()),
                    actualWaitedEdges,
                    cheapestBetweenTwoNodes
            );
        }

        return cheapestBetweenTwoNodes;
    }

    private DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode> getCheapestBetweenTwoNodesWithFilter(SimpleTimeExpandedClusterNode fromNode,
                                                                                                             SimpleTimeExpandedClusterNode toNode,
                                                                                                             List<DirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode>> edges,
                                                                                                             ConnectionType connectionType) {
        DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode> cheapestNormalConnection = null;

        for (DirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode> edge : edges) {
            if (connectionType != null & edge.getConnectionType() != connectionType) {
                continue;
            }

            long arrivaldiff = MINUTES.between(fromNode.getTime().plus(edge.getTravelTime()).plus(toNode.getServiceTime()), toNode.getTime());

            if (edge.getClass() == SpecialTimedNamedDirectedWeightedTemporalEdge.class) {
                long departurediff = MINUTES.between(fromNode.getTime(), ((SpecialTimedNamedDirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode>) edge).getTime());

                if (departurediff >= 0 & arrivaldiff >= 0 & departurediff < this.granularity & arrivaldiff < this.granularity)
                    if (cheapestNormalConnection == null || edge.getCost() < cheapestNormalConnection.getCost()) {
                        cheapestNormalConnection = new SpecialTimedNamedDirectedWeightedTemporalEdge<>(
                                fromNode,
                                toNode,
                                edge.getCost(),
                                edge.getTravelTime(),
                                edge.getConnectionType(),
                                ((SpecialTimedNamedDirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode>) edge).getName(),
                                null
                        );
                    }
            } else {
                if (arrivaldiff >= 0 & arrivaldiff < this.granularity)
                    if (cheapestNormalConnection == null || edge.getCost() < cheapestNormalConnection.getCost()) {
                        cheapestNormalConnection = new DirectedWeightedTemporalEdge<>(
                                fromNode,
                                toNode,
                                edge.getCost(),
                                edge.getTravelTime(),
                                edge.getConnectionType()
                        );
                    }
            }
        }

        return cheapestNormalConnection;
    }
}
