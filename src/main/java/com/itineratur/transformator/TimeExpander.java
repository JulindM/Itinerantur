package com.itineratur.transformator;

import com.itineratur.model.graph.SimpleClusteredGraph;
import com.itineratur.model.graph.basic.Cluster;
import com.itineratur.model.graph.basic.DirectedWeightedTemporalEdge;
import com.itineratur.model.graph.basic.SimpleTimeWindowClusterNode;
import com.itineratur.model.graph.basic.SimpleTimeExpandedClusterNode;
import com.itineratur.transformator.runner.TimeExpandNodeWaitingRunner;
import com.itineratur.transformator.runner.TimeExpandedNodesConnectionCreatorRunner;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class TimeExpander {
    static final Integer DEFAULT_TIME_GRANULARITY = 5;
    static final Integer DEFAULT_NO_PROCS = Runtime.getRuntime().availableProcessors();

    public static SimpleClusteredGraph<SimpleTimeExpandedClusterNode, DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> timeExpandWaitingClusteredGraph(
            SimpleClusteredGraph<SimpleTimeWindowClusterNode, DirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode>> gatsptwGraph, Integer timeGranularity, Integer procCount
    ) throws InterruptedException, ExecutionException {
        int setGranularity = (timeGranularity == null) ? DEFAULT_TIME_GRANULARITY : timeGranularity;
        int setProcCount = (procCount == null) ? DEFAULT_NO_PROCS : procCount;

        List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> expandedWaitingEdges = new ArrayList<>();
        List<Cluster<SimpleTimeExpandedClusterNode>> expandedClusters = new ArrayList<>();

        List<Cluster<SimpleTimeWindowClusterNode>> clusters = gatsptwGraph.getClusters();

        ExecutorService executorService = Executors.newFixedThreadPool(setProcCount);

        List<TimeExpandNodeWaitingRunner> clusterExpandingNodeWaitingRunners =
                clusters.stream().map(cluster -> new TimeExpandNodeWaitingRunner(cluster, setGranularity)).collect(Collectors.toList());

        List<Future<Pair<Integer, Pair<List<SimpleTimeExpandedClusterNode>, List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>>>>>> futureList = executorService.invokeAll(clusterExpandingNodeWaitingRunners);

        for (Future<Pair<Integer, Pair<List<SimpleTimeExpandedClusterNode>, List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>>>>> future : futureList) {
            Pair<Integer, Pair<List<SimpleTimeExpandedClusterNode>, List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>>>> integerListPair = future.get();
            expandedClusters.add(new Cluster<>(integerListPair.getLeft(), integerListPair.getRight().getLeft()));
            expandedWaitingEdges.addAll(integerListPair.getRight().getRight());
        }

        executorService.shutdown();

        return new SimpleClusteredGraph<>(expandedClusters, expandedWaitingEdges);
    }

    public static void buildWeightedConnectionsInTimeExpandedClusteredGraph (
            SimpleClusteredGraph<SimpleTimeWindowClusterNode, DirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode>> gatsptwGraph,
            SimpleClusteredGraph<SimpleTimeExpandedClusterNode, DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> gatspGraph,
            Integer procCount
    ) throws InterruptedException, ExecutionException {
        int setProcCount = (procCount == null) ? DEFAULT_NO_PROCS : procCount;

        List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> expandedEdges = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(setProcCount);

        List<TimeExpandedNodesConnectionCreatorRunner> timeExpandedNodesConnectionCreatorRunners =
                gatsptwGraph.getNodes().stream().map(node -> new TimeExpandedNodesConnectionCreatorRunner(node, gatsptwGraph, gatspGraph, DEFAULT_TIME_GRANULARITY)).collect(Collectors.toList());

        List<Future<List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>>>> futureList = executorService.invokeAll(timeExpandedNodesConnectionCreatorRunners);

        for (Future<List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>>> future : futureList) {
            List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> edges = future.get();
            expandedEdges.addAll(edges);
        }

        executorService.shutdown();

        gatspGraph.getAllEdges().addAll(expandedEdges);
    }
}
