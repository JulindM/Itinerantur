package com.itineratur.model.graph.basic.interfaces;

import java.time.temporal.TemporalAmount;

public interface Node {
    Integer getId();
    TemporalAmount getServiceTime();
    void setId(Integer id);
}
