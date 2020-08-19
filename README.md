# Beferrer
A small java project to process data created by some bespoke animal tags.

This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.

# Operation Overview
The program accepts as input a text file full of raw data uploaded from the tags. 

It processes that data in a number of steps controlled by the command line parameters (described in detail by running the tool with the --help option) to produce .csv files that contain the cleaned and processed data and behaviour categorisation. To run the tool use the command *java -jar Beferrer.jar <options>* where options are specified as required. Running *java -jar Beferrer --help* will provide a list of all optional parameters.

In addition .gnuplot text files are produced that contain a script and data that can be used by the gnuplot tool (http://www.gnuplot.info) to directly produce graphs. The .gnuplot file can be simply provided to the gnuplot command as a parameter and it will produce .eps output that can be viewed in most .pdf viwers.

#Folder structure

The /src folder contains the java source files

The /testData folder contains several files containing raw tag data that is used as input to the tool.

The /Example_output folder contains a few example graphs and sets of output data produced by the tool.




