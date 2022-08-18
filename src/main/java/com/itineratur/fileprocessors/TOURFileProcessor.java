package com.itineratur.fileprocessors;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TOURFileProcessor {
    public static List<Integer> getTourNodesIds(String location) throws IOException {
        File outFile = new File(location);
        List<String> lines = FileUtils.readLines(outFile);
        List<Integer> ids = new ArrayList<>();

        boolean walking = false;
        for (String line : lines) {
            if (line.equals("TOUR_SECTION")) {
                walking = true;
                continue;
            }
            if (walking) {
                if (line.equals("-1")) {
                    break;
                }
                ids.add(Integer.parseInt(line));
            }
        }

        return ids;
    }
}
