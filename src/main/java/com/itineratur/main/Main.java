package com.itineratur.main;

import com.itineratur.fileprocessors.GTSPFileManager;
import com.itineratur.fileprocessors.Parser;
import com.itineratur.fileprocessors.TOURFileProcessor;
import com.itineratur.model.exception.FormatException;
import com.itineratur.model.exception.StructureException;
import com.itineratur.model.graph.SimpleClusteredGraph;
import com.itineratur.model.graph.basic.DirectedWeightedTemporalEdge;
import com.itineratur.model.graph.basic.SimpleTimeExpandedClusterNode;
import com.itineratur.model.graph.basic.SimpleTimeWindowClusterNode;
import com.itineratur.transformator.TimeExpander;
import com.itineratur.util.model.GLKHExecutor;
import com.itineratur.util.model.GraphTools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) {

        File csv = new File("routing/nodes.csv");

        final String GLKH_FOLDER_LOCATION = "GLKH";
        final String GLKH_RUN_SCRIPT = "runGLKH";
        final String GTSP_FILENAME = "plan";


        try {
            System.out.println("[Itinerantur] Parsing nodes file: " + csv.toPath() );
            SimpleClusteredGraph<SimpleTimeWindowClusterNode, DirectedWeightedTemporalEdge<SimpleTimeWindowClusterNode>> gatsptwGraph = Parser.generateGATSPWGraphFromCSV(csv);


            System.out.println("[Itinerantur] Shifting internal IDs");
            GraphTools.setNewIds(gatsptwGraph);
            System.out.println("[Itinerantur] Splitting depot cluster in preparation for GLKH");
            GraphTools.splitStartAndEnd(gatsptwGraph);
            System.out.println("[Itinerantur] Running time expansion");
            SimpleClusteredGraph<SimpleTimeExpandedClusterNode, DirectedWeightedTemporalEdge<SimpleTimeExpandedClusterNode>> gatspGraph = TimeExpander.timeExpandWaitingClusteredGraph(gatsptwGraph, null, null);
            System.out.println("[Itinerantur] Building new edges in expanded graph");
            TimeExpander.buildWeightedConnectionsInTimeExpandedClusteredGraph(gatsptwGraph, gatspGraph, null);
            System.out.println("[Itinerantur] Removing unreachable non depot expanded nodes");
            GraphTools.removeUnreachableNodes(gatspGraph);
            System.out.println("[Itinerantur] Removing waiting edges in graph for better GLKH performance");
            GraphTools.removeWaitingEdges(gatspGraph);
            System.out.println("[Itinerantur] Injecting dummy singleton cluster start");
            GraphTools.injectDummySingletonStart(gatspGraph);

            GTSPFileManager gtspFileManager = new GTSPFileManager(gatspGraph);

            System.out.println("[Itinerantur] Generating " + GTSP_FILENAME + ".gtsp in " + GLKH_FOLDER_LOCATION  + " folder");
            gtspFileManager.generateGTSPFile(GLKH_FOLDER_LOCATION + "/" + GTSP_FILENAME + ".gtsp");

            System.out.println("[Itinerantur] Executing GLKH");
            GLKHExecutor.runGLKH(GLKH_FOLDER_LOCATION, GLKH_RUN_SCRIPT, GTSP_FILENAME);

            List<Integer> tourNodesIds = TOURFileProcessor.getTourNodesIds(GLKH_FOLDER_LOCATION + "/" + GTSP_FILENAME + ".tour");

            List<SimpleTimeExpandedClusterNode> tourExpandedNodes = new ArrayList<>();
            tourNodesIds.forEach(id -> tourExpandedNodes.add(gtspFileManager.getNodeFromTourfileNodeId(id)));

            String printableResult = GraphTools.getPrintableResult(tourExpandedNodes, gatspGraph);

            System.out.println("[Itinerantur] Resulting tour");
            System.out.println("----------------------------\n");
            System.out.println(printableResult);
            System.out.println("----------------------------");
        } catch (IOException | InterruptedException | ExecutionException | StructureException | FormatException e) {
            e.printStackTrace();
        }

    }
}
