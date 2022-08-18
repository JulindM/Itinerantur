package com.itineratur.util.model;

import com.itineratur.model.graph.SimpleClusteredGraph;
import com.itineratur.model.graph.SimpleGraph;
import com.itineratur.model.graph.basic.*;
import com.itineratur.model.graph.basic.enums.ConnectionType;
import com.itineratur.model.graph.basic.interfaces.ClusterNode;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GraphTools {
    private static final int INTERNAL_DEPOTCLUSTER_ID = 2;
    private static final int SPLIT_DEPOT_ID_SHIFT = 1000;
    private static final int DUMMY_START_CLUSTER_ID = 1;
    private static final int CLUSTER_ID_SHIFT = 2;
    private static final int NODE_ID_SHIFT = 1;

    public static <NodeType extends ClusterNode> List<NodeType> getNodesFromClusters(List<Cluster<NodeType>> clusterList) {
        List<NodeType> nodeList = new ArrayList<>();
        clusterList.forEach(cluster -> nodeList.addAll(cluster.getNodes()));
        return nodeList;
    }

    public static void injectDummySingletonStart(SimpleClusteredGraph<SimpleTimeExpandedClusterNode, DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> graph) {
        SimpleTimeExpandedClusterNode dummyNode = new SimpleTimeExpandedClusterNode(0, DUMMY_START_CLUSTER_ID, LocalTime.MIDNIGHT, Duration.ofMinutes(0));
        ArrayList<SimpleTimeExpandedClusterNode> dummyList = new ArrayList<>();
        dummyList.add(dummyNode);
        graph.getClusters().add(new Cluster<>(DUMMY_START_CLUSTER_ID, dummyList));

        List<SimpleTimeExpandedClusterNode> startCollection = new ArrayList<>(graph.getClusters()
                .stream()
                .filter(c -> c.getId().equals(INTERNAL_DEPOTCLUSTER_ID))
                .map(Cluster::getNodes)
                .collect(Collectors.toList())
                .get(0));

        startCollection.forEach(n -> graph.getAllEdges().add(new DirectedWeightedTemporalEdge<>(dummyNode, n, 0, Duration.ZERO, ConnectionType.FILLER)));

        graph.getClusters().sort(Comparator.comparing(Cluster::getId));
        Integer lastId = graph.getClusters().get(graph.getClusters().size() - 1).getId();

        List<SimpleTimeExpandedClusterNode> endCollection = new ArrayList<>(graph.getClusters()
                .stream()
                .filter(c -> c.getId().equals(lastId))
                .map(Cluster::getNodes)
                .collect(Collectors.toList())
                .get(0));

        endCollection.forEach(n -> graph.getAllEdges().add(new DirectedWeightedTemporalEdge<>(n, dummyNode, 0, Duration.ZERO, ConnectionType.FILLER)));
    }

    public static void setNewIds(SimpleClusteredGraph<SimpleTimeWindowClusterNode, DirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode>> graph) {
        graph.getClusters().forEach(c -> {
            int cId = c.getId() + CLUSTER_ID_SHIFT;
            c.setId(cId);
            c.getNodes().forEach(n -> {
                n.setId(n.getId() + NODE_ID_SHIFT);
                n.setClusterId(cId);
            });
        });
    }

    public static void splitStartAndEnd(SimpleClusteredGraph<SimpleTimeWindowClusterNode, DirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode>> graph) {
        List<SimpleTimeWindowClusterNode> startNodes = graph.getClusters().stream().filter(c -> c.getId() == INTERNAL_DEPOTCLUSTER_ID).collect(Collectors.toList()).get(0).getNodes();
        graph.getClusters().sort(Comparator.comparing(Cluster::getId));
        Integer lastId = graph.getClusters().get(graph.getClusters().size() - 1).getId() + 1;

        ArrayList<SimpleTimeWindowClusterNode> cloned = new ArrayList<>();

        startNodes.forEach(n -> cloned.add(new SimpleTimeWindowClusterNode(n.getId(), lastId, n.getTimeWindow(), n.getServiceTime())));
        graph.getClusters().add(new Cluster<>(lastId, cloned));

        graph.getAllEdges().forEach(e -> {
            SimpleTimeWindowClusterNode endNode = e.getNodePair().getRight();

            if (startNodes.contains(endNode)) {
                List<SimpleTimeWindowClusterNode> matching = cloned.stream().filter(n -> n.getId().equals(endNode.getId())).collect(Collectors.toList());

                if (matching.size() == 1) {
                    SimpleTimeWindowClusterNode match = matching.get(0);
                    match.setId(match.getId() + SPLIT_DEPOT_ID_SHIFT);
                    e.setNodePair(e.getNodePair().getLeft(), match);
                } else if (matching.size() == 0) {
                    List<SimpleTimeWindowClusterNode> existing = cloned.stream().filter(n -> n.getId().equals(endNode.getId() + SPLIT_DEPOT_ID_SHIFT)).collect(Collectors.toList());
                    if (existing.size() == 1) {
                        SimpleTimeWindowClusterNode match = existing.get(0);
                        e.setNodePair(e.getNodePair().getLeft(), match);
                    }
                }
            }
        });
    }

    public static String getPrintableResult(List<SimpleTimeExpandedClusterNode> tourNodes, SimpleGraph<SimpleTimeExpandedClusterNode, DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> gatspGraph) {
        String template = "%s: %s -> %s %s%s (%s) (Service for %s)\n";
        StringBuilder res = new StringBuilder();

        for (int i = 0; i < tourNodes.size() - 1; i++) {
            if (tourNodes.get(i).getId().equals(0)) {
                continue;
            }

            DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode> movement = gatspGraph.getEdges(tourNodes.get(i), tourNodes.get(i + 1)).get(0);

            if (movement.getClass() == SpecialCombinedDirectedWeightedTemporalEdge.class) {
                res.append("------ WAIT ")
                        .append(((SpecialCombinedDirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>) movement)
                                .getWaitingEdges()
                                .stream()
                                .map(DirectedWeightedTemporalEdge::getTravelTime)
                                .reduce(Duration.ZERO, (t1, t2) -> ((Duration) t1).plus((Duration) t2)).toString())
                        .append("\n");

                movement = ((SpecialCombinedDirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>) movement).getMainEdge();
            }

            String special = " ";
            if (movement.getClass() == SpecialTimedNamedDirectedWeightedTemporalEdge.class) {
                special = " " + ((SpecialTimedNamedDirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>) movement).getName();
            }

            LocalTime startTime = movement.getNodePair().getLeft().getTime();
            Integer originId = tourNodes.get(i).getId() - NODE_ID_SHIFT;

            int destinationId = tourNodes.get(i + 1).getId() - NODE_ID_SHIFT;
            if (destinationId - SPLIT_DEPOT_ID_SHIFT > -1) destinationId = destinationId - SPLIT_DEPOT_ID_SHIFT;

            res.append(String.format(template, startTime.toString(), originId, destinationId, movement.getConnectionType(), special, movement.getTravelTime(), movement.getNodePair().getRight().getServiceTime()));
        }

        return res.toString();
    }

    public static void removeWaitingEdges(SimpleClusteredGraph<SimpleTimeExpandedClusterNode, DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> gatspGraph) {
        gatspGraph.getAllEdges().removeIf(e -> e.getConnectionType() == ConnectionType.WAITING);
    }

    public static void removeUnreachableNodes(SimpleClusteredGraph<SimpleTimeExpandedClusterNode, DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> gatspGraph) {
        System.out.print("------------- " + gatspGraph.getNodes().size()  + " -> ");
        gatspGraph.getClusters().forEach(c -> {
                    if (c.getId() - CLUSTER_ID_SHIFT != 0) {
                        c.getNodes().removeIf(n ->
                                gatspGraph.getAllEdges().stream().filter(e -> e.getNodePair().getRight().equals(n)).count() == 1 &&
                                        gatspGraph.getAllEdges().stream().filter(e -> e.getNodePair().getLeft().equals(n)).count() == 1
                        );
                    }
                }
        );
        System.out.println(gatspGraph.getNodes().size());
    }
}
