package com.itineratur.fileprocessors;

import com.itineratur.model.exception.StructureException;
import com.itineratur.model.graph.basic.Cluster;
import com.itineratur.model.graph.basic.DirectedWeightedTemporalEdge;
import com.itineratur.model.graph.basic.SimpleTimeExpandedClusterNode;
import com.itineratur.model.graph.basic.interfaces.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GTSPFileManager {
    private static final Integer MAX_COSTS = 999;
    private final Map<Integer, SimpleTimeExpandedClusterNode> nodeMap;
    private final ClusteredGraph<SimpleTimeExpandedClusterNode, DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> graph;

    private String header =
            "NAME: Test\n" +
                    "TYPE: AGTSP\n" +
                    "COMMENT: TEST\n" +
                    "DIMENSION: %d\n" +
                    "GTSP_SETS: %s\n" +
                    "EDGE_WEIGHT_TYPE: EXPLICIT\n" +
                    "EDGE_WEIGHT_FORMAT: FULL_MATRIX\n";

    public GTSPFileManager(ClusteredGraph<SimpleTimeExpandedClusterNode, DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> graph) {
        this.graph = graph;

        this.graph.getClusters().sort(Comparator.comparing(Cluster::getId));

        for (Cluster<SimpleTimeExpandedClusterNode> cluster : this.graph.getClusters()) {
            cluster.getNodes().sort(Comparator.comparing(SimpleTimeExpandedClusterNode::getId));
        }

        this.header = String.format(header, this.graph.getNodes().size(), this.graph.getClusters().size());

        this.nodeMap = new HashMap<>();
    }


    public void generateGTSPFile(String location) throws IOException, StructureException {
        String edgeCosts = this.generateEdgeWeightSection();
        String clustersList = this.generateGTSPSetSection();

        File fileHandler = new File(location);

        String total =
                this.header +
                        edgeCosts +
                        clustersList;

        FileWriter writer = new FileWriter(fileHandler);
        writer.write(total);
        writer.close();
    }

    private String generateEdgeWeightSection() throws StructureException {
        StringBuilder weights = new StringBuilder();

        weights.append("EDGE_WEIGHT_SECTION\n");

        for (Cluster<SimpleTimeExpandedClusterNode> cluster1: this.graph.getClusters()) {
            for (SimpleTimeExpandedClusterNode node1: cluster1.getNodes()) {
                for (Cluster<SimpleTimeExpandedClusterNode> cluster2: this.graph.getClusters()) {
                    for (SimpleTimeExpandedClusterNode node2: cluster2.getNodes()) {
                        if (node1.equals(node2)) {
                            weights.append(MAX_COSTS);
                            weights.append(" ");
                            continue;
                        }
                        weights.append(this.getCorrespondingEdgeCosts(node1, node2));
                        weights.append(" ");
                    }
                }
                weights.append("\n");
            }
        }

        return weights.toString();
    }

    private String generateGTSPSetSection() {
        StringBuilder clustersSection = new StringBuilder();

        clustersSection.append("GTSP_SET_SECTION:\n");

        int counter = 1;
        for (Cluster<SimpleTimeExpandedClusterNode> cluster: this.graph.getClusters()) {
            clustersSection.append(cluster.getId());
            clustersSection.append(" ");

            for (SimpleTimeExpandedClusterNode timeExpandedClusterNode : cluster.getNodes()) {
                this.nodeMap.put(counter, timeExpandedClusterNode);
                clustersSection.append(counter);

                counter++;
                clustersSection.append(" ");
            }

            clustersSection.append("-1\n");
        }

        return clustersSection.toString();
    }

    private Integer getCorrespondingEdgeCosts(SimpleTimeExpandedClusterNode one, SimpleTimeExpandedClusterNode two) throws StructureException {
        List<DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> eList = this.graph.getAllEdges()
                .parallelStream()
                .filter(n1 ->
                        n1.getNodePair().getLeft().getUniqueTimeExpandedId().equals(one.getUniqueTimeExpandedId())
                                &&
                                n1.getNodePair().getRight().getUniqueTimeExpandedId().equals(two.getUniqueTimeExpandedId())
                )
                .collect(Collectors.toList());
        if (eList.size() > 1) {
            throw new StructureException("There are 2 edges pointing at the same pair of time expanded nodes");
        }
        if (eList.size() < 1) {
            return MAX_COSTS;
        }
        else {
            return eList.get(0).getCost();
        }
    }

    public SimpleTimeExpandedClusterNode getNodeFromTourfileNodeId(Integer id) {
        return this.nodeMap.get(id);
    }
}
