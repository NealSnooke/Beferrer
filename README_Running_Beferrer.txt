Some examples that I have used that should work OK. All assume the data files Collar_13.TXT and Collar_12.TXT are in a folder called testData that is in the subfolder from the working folder where the program is being run from.

The Beferrer.jar file is in the working folder or accessible on the system path, as is Java 1.8. 

Modify parth as required - there should be error messages that will say where the program is looking if it can't find files. Read the help page for info on generating graphs and what all the options do.

java -jar Beferrer.jar -help

if the jar file is somewhere else than provide the path (e.g. in the folder above the data)
java -jar ../Beferrer.jar -help

//combine non-moving sequences
java -jar Beferrer.jar -f "testData/Collar_13.TXT" -p 0

//position average to 2 (default)
java -jar Beferrer.jar -f "testData/Collar_13.TXT"

java -jar Beferrer.jar -fd "testData/" -fs ".TXT" -c 52.26,-3.62,52.25,-3.602 -cg 20 -g 250 400 -gg 15 -p 0 -cts 08:30:00 -cte 18:00:00 -cds 29/05/2018 -cde 1/08/2018

java -jar Beferrer.jar -fd "testData/" -fs ".TXT" -c 52.26,-3.62,52.25,-3.602 -cg 20 -g 0 4 -gs 5  -ge 5 -gg 15 -p 2 -cts 08:30:00 -cte 18:00:00 -StartDate 29/05/2018 -EndDate 1/08/2018

java -jar Beferrer.jar -fd "testData/" -fs ".TXT" -c 52.26,-3.62,52.25,-3.602 -cg 20 -g 250 400 -gg 15 -p 8 -cts 08:30:00 -cte 18:00:00 -cds 29/05/2018 -cde 1/08/2018




