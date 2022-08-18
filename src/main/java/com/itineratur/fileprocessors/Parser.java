package com.itineratur.fileprocessors;

import com.itineratur.model.exception.FormatException;
import com.itineratur.model.exception.StructureException;
import com.itineratur.model.graph.SimpleClusteredGraph;
import com.itineratur.model.graph.basic.Cluster;
import com.itineratur.model.graph.basic.DirectedWeightedTemporalEdge;
import com.itineratur.model.graph.basic.SimpleTimeWindowClusterNode;
import com.itineratur.model.graph.basic.SpecialTimedNamedDirectedWeightedTemporalEdge;
import com.itineratur.model.graph.basic.enums.ConnectionType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.stream.Collectors;

public class Parser {
    private static final int NODESFILE_COL_LENGTH = 4;
    private static final int ROUTING_MIN_SPLIT_LENGTH = 2;
    private static final int COSTS_MIN_COMBO_LENGTH = 3;

    /**
     * Parser that generates a GATPSTW Graph
     * @param csvFile CSV File with all the Clustered Nodes
     * @return A Clustered GATPSTW Graph
     */
    public static SimpleClusteredGraph<SimpleTimeWindowClusterNode, DirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode>> generateGATSPWGraphFromCSV(File csvFile) throws IOException, FormatException, StructureException {
        CSVParser csvParser = CSVParser.parse(csvFile, StandardCharsets.UTF_8, CSVFormat.RFC4180);
        List<CSVRecord> records = csvParser.getRecords();

        Map<Integer, SimpleTimeWindowClusterNode> nodesHashMap = new HashMap<>();
        List<Cluster<SimpleTimeWindowClusterNode>> clusters = new ArrayList<>();

        for (CSVRecord csvRecord : records) {
            if (csvRecord.size() != NODESFILE_COL_LENGTH) {
                throw new FormatException("A CSV Record should have " + NODESFILE_COL_LENGTH + " columns. I read " + csvRecord.size());
            }

            int id;
            try {
                id = Integer.parseInt(csvRecord.get(0).trim());
            } catch (NumberFormatException n) {
                throw new FormatException("Node ID contained non numerical characters " + csvRecord.get(0));
            }

            String[] windowString = csvRecord.get(2).trim().split("#");
            if (windowString.length != 2) {
                throw new FormatException("TimeWindow definition for ID: " + id + " did not contain 2 time windows but " + csvRecord.get(2));
            }

            List<LocalTime> timeWindow;
            try {
                timeWindow = Arrays.stream(windowString).map(time -> LocalTime.parse(time.trim(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)).collect(Collectors.toList());
            } catch (DateTimeParseException d) {
                throw new FormatException("TimeWindow definition for ID: " + id + " is not ISO-8601 compliant. " + d.getMessage());
            }

            String[] clusterIdServiceTimePair = csvRecord.get(1).split("@");

            int clusterId;
            try {
                clusterId = Integer.parseInt(clusterIdServiceTimePair[0].trim());
            } catch (NumberFormatException n) {
                throw new FormatException("Cluster ID contained non numerical characters " + csvRecord.get(0));
            }

            TemporalAmount serviceTime;
            try {
                int serviceTimeTemp = Integer.parseInt(clusterIdServiceTimePair[1].trim());
                serviceTime = Duration.ofMinutes(serviceTimeTemp);
            } catch (NumberFormatException n) {
                throw new FormatException("Cluster ID contained non numerical characters " + csvRecord.get(0));
            }

            nodesHashMap.put(id, new SimpleTimeWindowClusterNode(id, clusterId, new ImmutablePair<>(timeWindow.get(0), timeWindow.get(1)), serviceTime));
        }

        List<DirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode>> edges = new ArrayList<>();
        for (CSVRecord csvRecord : records) {
            int id = Integer.parseInt(csvRecord.get(0).trim());
            SimpleTimeWindowClusterNode node = nodesHashMap.get(id);

            edges.addAll(getNodeEdges(csvRecord, node, nodesHashMap));
        }

        for (CSVRecord csvRecord : records) {
            String[] clusterIdServiceTimePair = csvRecord.get(1).split("@");
            int id = Integer.parseInt(clusterIdServiceTimePair[0].trim());

            List<Cluster<SimpleTimeWindowClusterNode>> foundNodes = clusters.stream().filter(cluster -> cluster.getId() == id).collect(Collectors.toList());
            Cluster<SimpleTimeWindowClusterNode> found;
            if (foundNodes.size() == 0) {
                found = new Cluster<>(id, new ArrayList<>());
                clusters.add(found);
            } else {
                found = foundNodes.get(0);
            }

            found.getNodes().add(nodesHashMap.get(Integer.parseInt(csvRecord.get(0).trim())));
        }

        for (Cluster<SimpleTimeWindowClusterNode> cluster : clusters) {
            if (cluster.getNodes().stream().distinct().count() != cluster.getNodes().size()) {
                throw new StructureException("There is more than one node with the same ID in the cluster " + cluster.getId());
            }
        }

        return new SimpleClusteredGraph<>(clusters, edges);
    }

    public static List<DirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode>> getNodeEdges(CSVRecord csvRecord, SimpleTimeWindowClusterNode originNode, Map<Integer, SimpleTimeWindowClusterNode> nodes) throws StructureException {
        List<DirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode>> edges = new ArrayList<>();
        String destinations = csvRecord.get(3);

        if (destinations.length() == 0) return new ArrayList<>();

        String[] destinationsList = destinations.split("-");

        for (String destination : destinationsList) {
            String[] routeBlock = destination.split("~");

            if (routeBlock.length < ROUTING_MIN_SPLIT_LENGTH) {
                throw new StructureException("Bad destination information for ID: " + originNode.getId());
            }

            int id;
            try {
                id = Integer.parseInt(routeBlock[0]);
            } catch (NumberFormatException e) {
                throw new StructureException("Bad format destination for ID: " + originNode.getId());
            }

            SimpleTimeWindowClusterNode toNode = nodes.get(id);
            if (toNode == null) throw new StructureException("Nonexistent destination ID " + id + " for " + originNode.getId());

            String[] connections = routeBlock[1].split("/");
            if (connections.length > COSTS_MIN_COMBO_LENGTH || connections.length == 0) throw new StructureException("Bad connection combinations for ID: " + originNode.getId());

            for (int i = 0; i < 2; i++) {
                if (connections[i].length() == 0) continue;

                String[] costTimePart = connections[i].split(":");
                if (costTimePart.length != 2) throw new StructureException("Bad cost:time combination for ID: " + originNode.getId());

                int cost, duration;
                try {
                    cost = Integer.parseInt(costTimePart[0]);
                    duration = Integer.parseInt(costTimePart[1]);
                } catch (NumberFormatException e) {
                    throw new StructureException("Bad format for cost:time combination for ID: " + originNode.getId());
                }

                switch (i) {
                    case 0:
                        edges.add(new DirectedWeightedTemporalEdge<>(originNode, toNode, cost, Duration.ofMinutes(duration), ConnectionType.FOOT));
                        break;
                    case 1:
                        edges.add(new DirectedWeightedTemporalEdge<>(originNode, toNode, cost, Duration.ofMinutes(duration), ConnectionType.BIKE));
                        break;
                }
            }

            if (connections.length == 3 &&connections[2].length() != 0) {
                String[] trainConnections = connections[2].split("&");
                for (String trainConnection : trainConnections) {
                    String name = null;
                    LocalTime time = null;
                    int cost, duration;

                    String[] connectionInformation = trainConnection.split("%");
                    if (connectionInformation.length != 3) throw new StructureException("Bad train connection information for ID: " + originNode.getId());

                    for (int i = 0; i < connectionInformation.length; i++) {
                        String part = connectionInformation[i];
                        switch (i) {
                            case 0:
                                if (part.length() == 0) throw new StructureException("Unnamed train connection for ID: " + originNode.getId());
                                name = part;
                                break;
                            case 1:
                                if (part.length() != 4) throw new StructureException("No or bad departure time given for" +
                                        " train line " + name + " time for ID: " + originNode.getId());

                                int hour = 0, minutes = 0;
                                String[] timeSplit = part.split("");

                                try {
                                    for (int j = 0; j < timeSplit.length; j += 2) {
                                        switch (j) {
                                            case 0:
                                                hour = Integer.parseInt(timeSplit[j] + timeSplit[j + 1]);
                                                break;
                                            case 2:
                                                minutes = Integer.parseInt(timeSplit[j] + timeSplit[j + 1]);
                                                break;
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    throw new StructureException("Bad number format for train connection information for ID: " + originNode.getId());
                                }
                                time = LocalTime.of(hour, minutes);
                                break;
                        }
                    }

                    String[] costTimePart = connectionInformation[2].split(":");
                    if (costTimePart.length != 2) throw new StructureException("Bad train line cost:time combination for ID: " + originNode.getId());

                    try {
                        cost = Integer.parseInt(costTimePart[0]);
                        duration = Integer.parseInt(costTimePart[1]);
                    } catch (NumberFormatException e) {
                        throw new StructureException("Bad format for train line cost:time combination for ID: " + originNode.getId());
                    }

                    edges.add(new SpecialTimedNamedDirectedWeightedTemporalEdge<>(originNode, toNode, cost, Duration.ofMinutes(duration), ConnectionType.TRAIN, name, time));
                }
            }
        }

        return edges;
    }
}
