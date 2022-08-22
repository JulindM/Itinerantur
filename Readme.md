# Itinerantur

Itineratntur is a tool that given a generalized asymmetric graph with time windows, 
tries to find an optimal route that solves the GATSPTW Problem (Generalized Asymmetric Traveling Salesman Problem with Time Windows).

_The main solver for the internal reduced GATSP Problem is [GLKH](http://webhotel4.ruc.dk/~keld/research/GLKH/)._

Supports multi modal travel input with edges that are cost weighted and travel time annotated. 

Supported modals:
 + Train
 + Foot
 + Bike

Written in Java 8 with the Gradle as the build chain.

_This project was written as part of my bachelors thesis. For the documentation on how to use the tool and its architecture please refer to the Thesis PDF File (written in german) in the documentation folder._
