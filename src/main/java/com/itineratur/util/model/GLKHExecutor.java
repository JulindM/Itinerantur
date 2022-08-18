package com.itineratur.util.model;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

import java.io.IOException;

public class GLKHExecutor {
    public static void runGLKH(String glkhFolderLocation, String glkhRunScript, String gtspFilename) throws IOException {
        CommandLine runGLKHCommand = new CommandLine("sh").addArgument("-c");
        runGLKHCommand.addArgument("cd " + glkhFolderLocation + " && ./" + glkhRunScript + " " + gtspFilename, false);

        DefaultExecutor oDefaultExecutor = new DefaultExecutor();
        oDefaultExecutor.execute(runGLKHCommand);
    }
}