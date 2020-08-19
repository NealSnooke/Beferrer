/**
 * First created Neal Snooke 26/07/2018
 * nns@aber.ac.uk
 * 
 * Major enhancement and graphing 18/03/2019
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Vector;

// uses commons-cli-1.3.1.jar
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/*
 * Written by Neal Snooke v1.1 completed 10 May 2019
 * 
 * use examples:

-fd "../version2data/" -fs ".TXT" -c 52.26,-3.62,52.25,-3.602 -cg 20 -g 0 4 -gs 5 -ge 5 -gg 15 -p 2 -cts 08:30:00 -cte 18:00:00 -StartDate 29/05/2018 -EndDate 1/08/2018

-fd "../version2data/" -fs ".TXT" -c 52.26,-3.62,52.25,-3.602 -cg 20 -g 250 400 -gg 15 -p 0 -cts 08:30:00 -cte 18:00:00 -StartDate 29/05/2018 -EndDate 1/08/2018
-fd "../version2data/" -fs ".TXT" -c 52.26,-3.62,52.25,-3.602 -cg 20 -g 250 400 -gg 15 -p 0 -cts 08:30:00 -cte 18:00:00 -cds 29/05/2018 -cde 1/08/2018

//combine non-moving sequences
-f "../version2data/Collar_13.TXT" -p 0

//position average to 2 (default)
-f "../version2data/Collar_13.TXT"
*/

public class Beferrer {

	private Vector<TagDataItem> rowData;

	//cleaned and processed data for each animal movement
	private Vector<MovementDataItem> movementData;

	private Vector<SchedulePeriod> scheduleGroupings;

	private String fileName = null;
	private String inputFileExtension = null;
	private String folder = null;
	public String compressedOptions = null; //for use as filename...
	
	private String gnuplotFileName;
	private double LINEWIDTH = 0.25; //on gnuplot graphs

	GpsPosition northWestBoundary = null;
	GpsPosition southEastBoundary = null;
	double glitchSpeed = 30;
	
	int startPointsRemoved = 0;
	int endPointsRemoved = 0;
	double trimGroupBelowSpeed = 0;
	
	public double movementAverage = 0; // number of seconds to average movement over
	private double positionAverage = 2; //meters to average over
	
	private int timePeriodMin = 0; // the frequency required to split the data
	private int timePeriodMax = Integer.MAX_VALUE; //around 68 years...
	
	// date and times to select output data from
	private boolean dateRangeStart = false;
	private boolean timeOfDayRangeStart = false;
	private boolean dateRangeEnd = false;
	private boolean timeOfDayRangeEnd = false;
	private int start_d = -1;
	private int start_m = -1;
	private int start_y = -1;
	private int start_hh = -1;
	private int start_mm = -1;
	private int start_ss = -1;
	// date and times to select output data from
	private int end_d = -1;
	private int end_m = -1;
	private int end_y = -1;
	private int end_hh = -1;
	private int end_mm = -1;
	private int end_ss = -1;
	
	public static boolean debug = false; // output data after main processing steps
	
	
	public Behaviour behaviours = new Behaviour();

	OutputStreamWriter gnuplot_cl_filewriter = null;

	/**
	 * 
	 * @param args
	 */
	public static void main(String args[]){
		System.out.println("Welcome to Beferrer v1.3 - animal behaviour inference processor (--help for options)");
		/**
		for (String s: args) {
            System.out.println(s);
        }
        /**/

		Beferrer tdp = new Beferrer();

		//parse command line options
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		
		//Option help = new Option( "help", "print this message" );
		//options.addOption(help);
		options.addOption( "h", "help", false, "This page. In case of problems or queries contact nns@aber.ac.uk .");

		options.addOption(Option.builder("a")
				.longOpt("Activity")
				.desc( "Activity category specification based on speed. "
						+ "Provide behaviour name (bn) and maximum speed (sn) for each behaviour required "
						+ "as a comma separated list. No spaces in parameter list. "
						+ "Default is <Stationary,0.02,Foraging,0.33,Moving,10.0>" )
				.hasArgs() // an argument to the option is expected
				.argName("b1,s1,b2,s2...")
				.valueSeparator(',')
				.build() );
		
		options.addOption(Option.builder("c")
				.longOpt("CoordinatesFence")
				.desc( "Ignore coordinates outside the region <N,W,S,E>. "
						+ "Comma separated decimal lat,log,lat,lon values. "
						+ "No spaces in parameter list." )
				.hasArgs() // an argument to the option is expected
				.argName("N,W,S,E")
				.valueSeparator(',')
				.build() );
		
		options.addOption(Option.builder("cg")
				.longOpt("GlitchSpeed")
				.desc( "Minimum speed considered as a single point gps glitch. "
						+ "Data point will be removed if adjacent points provide movement with speed below the specified value." )
				.hasArg() // an argument to the option is expected
				.argName("speed")
				.build() );	
		
		options.addOption(Option.builder("cts")
				.longOpt("StartTime")
				.desc( "Ignore daily data before the specified time. "
						+ "If StartTime greater than EndTime then overnight period. ")
				.hasArgs() // an argument to the option is expected
				.argName("hh:mm:ss")
				.valueSeparator(':')
				.build() );	
		
		options.addOption(Option.builder("cte")
				.longOpt("EndTime")
				.desc( "Ignore daily data after the specified time." )
				.hasArgs() // an argument to the option is expected
				.argName("hh:mm:ss")
				.valueSeparator(':')
				.build() );	
		
		options.addOption(Option.builder("cds")
				.longOpt("StartDate")
				.desc( "Ignore data before the specified date. Must use 4 digit year." )
				.hasArgs() // an argument to the option is expected
				.argName("dd/mm/yyyy")
				.valueSeparator('/')
				.build() );	
		
		options.addOption(Option.builder("cde")
				.longOpt("EndDate")
				.desc( "Ignore data after the specified date. Must use 4 digit year." )
				.hasArgs() // an argument to the option is expected
				.argName("dd/mm/yyyy")
				.valueSeparator('/')
				.build() );	
	
		//TODO
		options.addOption(Option.builder("fd")
				.longOpt("Folder")
				.desc( "Input folder/directory containing data files. "
						+ "If neither file (-f) or folder (-fd) is specified all files in the current working folder will be used "
						+ "subject to any suffix specification set with -fs" )
				.hasArg() // an argument to the option is expected
				.argName("folder")
				.build() );	
		
		options.addOption(Option.builder("f") //the identification string of the Option
				.longOpt("File") //an alias and more descriptive identification string
				.desc( "Input file name (including path if required). Ignored if -fd used." )
				.hasArg() // an argument to the option is expected
				.argName("name") // for usage statement
				.build() );
		
		options.addOption(Option.builder("fs") //the identification string of the Option
				.longOpt("Suffix") //an alias and more descriptive identification string
				.desc( "Suffix to be used to select input file names. Include the '.' when the suffix is a file type extension. e.g. .TXT "
						+ "Do not use for -f option where complete filename is used. Omission in conjunction with -fd will process every file in the folder." )
				.hasArg() // an argument to the option is expected
				.argName("suffix") // for usage statement
				.build() );
		
		options.addOption(Option.builder("m")
				.longOpt("MovementAverage")
				.desc( "Average the movement speed data over the specified time period (seconds). "
						+ "Omission or 0 value will result in no averaging of movements." )
				.hasArg() // an argument to the option is expected
				.argName("time")
				.build() );
		
		options.addOption(Option.builder("p")
				.longOpt("PositionAverage")
				.desc( "Average the position coordinates over the specified distance (meters) prior to generating movements. "
						+ "Smooths gps track in the presence of gps lockup or gps noise. "
						+ "Alternatively set to 0 to combine non-moving sequences (gps lockup) into a single longer duration move. "
						+ "Default if not set "+tdp.positionAverage+"m" )
				.hasArg() // an argument to the option is expected
				.argName("dist")
				.build() );

		options.addOption(Option.builder("g")
				//.required(true)
				.longOpt("GroupTime")
				.desc( "Time resolution of data to consider. "
						+ "Each sequential set of coordinates that satisfy this range is considered a sample group "
						+ "ie. a track segment that includes movements between min and max seconds duration. "
						+ "Extracts sample group sequences from a mixed resulution data set. Defult is all data." )
				.hasArgs() // an argument to the option is expected
				.argName("min:max")
				.valueSeparator(':')
				.build() );
	
		options.addOption(Option.builder("gs")
				.longOpt("TrimGroupStart")
				.desc( "Remove n of points from the start of each sample group." )
				.hasArg() // an argument to the option is expected
				.argName("n")
				.build() );	
		
		options.addOption(Option.builder("ge")
				.longOpt("TrimGroupEnd")
				.desc( "Remove n points from the end of each sample group." )
				.hasArg() // an argument to the option is expected
				.argName("n")
				.build() );
		
		options.addOption(Option.builder("gg")
				.longOpt("TrimGroupBelowSpeed")
				.desc( "All movements before or after a movement greater than the specified speed are removed from each group.")
				.hasArg() // an argument to the option is expected
				.argName("speed")
				.build() );	
		
		options.addOption(Option.builder("v")
				.longOpt("Verbose")
				.desc( "Detailed output of the processing steps. Use for explanation or debugging (can be long so redirect to a file)." )
				//.hasArg() // an argument to the option is expected
				//.argName("n")
				.build() );	

		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);
			
			System.out.println("Options:");

			// validate that scale has been set
			if( line.hasOption( "File" ) ) {
				tdp.fileName = line.getOptionValue("File");
				//System.out.println( line.getOptionValue( "scale" ) );
				//GUISCALE = (float)(Integer.parseInt(line.getOptionValue("scale")))/100;
				System.out.println( "Filename: "+tdp.fileName);
			}

			if( line.hasOption( "Suffix" ) ) {
				tdp.inputFileExtension = line.getOptionValue("Suffix");
				//System.out.println( line.getOptionValue( "scale" ) );
				//GUISCALE = (float)(Integer.parseInt(line.getOptionValue("scale")))/100;
				System.out.println( "filename suffix: "+tdp.inputFileExtension);
			}
			
			if( line.hasOption( "Folder" ) ) {
				tdp.folder = line.getOptionValue("Folder");
				//ensure path ends with "/" etc
				if (!tdp.folder.endsWith(File.separator))
					tdp.folder = tdp.folder+File.separator;
				
				//System.out.println( line.getOptionValue( "scale" ) );
				//GUISCALE = (float)(Integer.parseInt(line.getOptionValue("scale")))/100;
				System.out.println( "Folder: "+tdp.folder);
			}
			
			if( line.hasOption( "TrimGroupBelowSpeed" ) ) {
				tdp.glitchSpeed = Double.parseDouble(line.getOptionValue("TrimGroupBelowSpeed"));
				System.out.println( "GPS glitch removal speed : "+tdp.glitchSpeed+" m/s");
				tdp.compressedOptions = tdp.compressedOptions+"gg"+line.getOptionValue("GlitchSpeed");
			}
			
			if( line.hasOption( "StartTime" ) ) {
				String[] t = line.getOptionValues("StartTime");
				System.out.println( "Start Time: "+t[0]+"hh "+t[1]+"mm "+t[2]+"ss");
				
				tdp.start_hh = Integer.parseInt(t[0]);
				tdp.start_mm = Integer.parseInt(t[1]);
				tdp.start_ss =Integer.parseInt(t[2]);
				tdp.compressedOptions = tdp.compressedOptions+"ct"+t[0]+t[1]+t[2]+"-";
				tdp.timeOfDayRangeStart = true;
			}
			
			if( line.hasOption( "EndTime" ) ) {
				String[] t = line.getOptionValues("EndTime");
				System.out.println( "End Time: "+t[0]+"hh "+t[1]+"mm "+t[2]+"ss");
				//tdp.compressedOptions = tdp.compressedOptions+"t"+tdp.glitchSpeed+t[0]+t[1];
				tdp.end_hh = Integer.parseInt(t[0]);
				tdp.end_mm = Integer.parseInt(t[1]);
				tdp.end_ss =Integer.parseInt(t[2]);
				tdp.compressedOptions = tdp.compressedOptions+t[0]+t[1]+t[2];
				tdp.timeOfDayRangeEnd = true;
			}
			
			if( line.hasOption( "StartDate" ) ) {
				String[] t = line.getOptionValues("StartDate");
				System.out.println( "Start Date: "+t[0]+"dd "+t[1]+"mm "+t[2]+"yy");	
				tdp.start_d = Integer.parseInt(t[0]);
				tdp.start_m = Integer.parseInt(t[1]);
				tdp.start_y =Integer.parseInt(t[2]);
				tdp.compressedOptions = tdp.compressedOptions+"cd"+t[0]+t[1]+t[2]+"-";
				tdp.dateRangeStart = true;
			}
			
			if( line.hasOption( "EndDate" ) ) {
				String[] t = line.getOptionValues("EndDate");
				System.out.println( "End Date: "+t[0]+"dd "+t[1]+"mm "+t[2]+"yy");
				tdp.end_d = Integer.parseInt(t[0]);
				tdp.end_m = Integer.parseInt(t[1]);
				tdp.end_y =Integer.parseInt(t[2]);
				tdp.compressedOptions = tdp.compressedOptions+t[0]+t[1]+t[2]; //added to previous
				tdp.dateRangeEnd = true;
			}
			
			if (line.hasOption( "TrimGroupStart" ) ) {
				tdp.startPointsRemoved = Integer.parseInt(line.getOptionValue("TrimGroupStart"));
				System.out.println( "Remove "+tdp.startPointsRemoved+" samples at the start of each group");
				tdp.compressedOptions = tdp.compressedOptions+"gs"+tdp.startPointsRemoved;
			}
			
			if (line.hasOption( "TrimGroupEnd" ) ) {	
				tdp.endPointsRemoved = Integer.parseInt(line.getOptionValue("TrimGroupEnd"));
				System.out.println( "Remove "+tdp.endPointsRemoved+" samples at the end of each group");
				tdp.compressedOptions = tdp.compressedOptions+"ge"+tdp.endPointsRemoved;
			}
			
			if (line.hasOption( "TrimGroupBelowSpeed" ) ) {	
				tdp.trimGroupBelowSpeed = Double.parseDouble(line.getOptionValue("TrimGroupBelowSpeed"));
				System.out.println( "Trim groups (tracks) to movemens below "+tdp.trimGroupBelowSpeed+"m/s");
				tdp.compressedOptions = tdp.compressedOptions+"gg"+tdp.endPointsRemoved;
			}
			
			if (line.hasOption( "MovementAverage" ) ) {	
				tdp.movementAverage = Double.parseDouble(line.getOptionValue("MovementAverage"));
				System.out.println( "Movement average over "+tdp.movementAverage+" seconds");
				tdp.compressedOptions = tdp.compressedOptions+"m"+line.getOptionValue("MovementAverage");
			}
			
			if (line.hasOption( "PositionAverage" ) ) {	
				tdp.positionAverage = Double.parseDouble(line.getOptionValue("PositionAverage"));
				System.out.println( "Position average over "+tdp.positionAverage+" meters");
				tdp.compressedOptions = tdp.compressedOptions+"p"+line.getOptionValue("PositionAverage");
			}
			
			if (line.hasOption( "GroupTime" ) ) {
				String[] a = line.getOptionValues("GroupTime");
				if (a.length != 2){
					System.out.println("-g --GroupTime  Requires exactly 2 values - "+a.length + "values provided");
				} else {
					
					tdp.timePeriodMin = Integer.parseInt(a[0]);
					tdp.timePeriodMax = Integer.parseInt(a[1]);
					System.out.println( "GroupTime selects movements between "+tdp.timePeriodMin+" and "+tdp.timePeriodMax+" seconds");
				}
				tdp.compressedOptions = tdp.compressedOptions+"g"+a[0]+"-"+a[1];
			}

			if (line.hasOption( "CoordinatesFence" ) ) {
				String[] a = line.getOptionValues("CoordinatesFence");
				if (a.length != 4){
					System.out.println("Coordinate fence ignored - requires exactly 4 values - "
							+ "NW lat, NW long, SE lat, SE long (found "+a.length+").  ");
				} else {

					tdp.northWestBoundary = new GpsPosition(Double.parseDouble(a[0]), Double.parseDouble(a[1]));
					tdp.southEastBoundary = new GpsPosition(Double.parseDouble(a[2]), Double.parseDouble(a[3]));
					System.out.println("North West boundary: "+tdp.northWestBoundary+" (lat, lon)");
					System.out.println("South East boundary: "+tdp.southEastBoundary+" (lat, lon)");
					
					tdp.compressedOptions = tdp.compressedOptions+"c"
							+"N"+a[0]
							+"W"+a[1]
							+"S"+a[2]
							+"E"+a[3];
					//+"N"+a[0].replaceAll("-", "_")
					//+"W"+a[1].replaceAll("-", "_")
					//+"S"+a[2].replaceAll("-", "_")
					//+"E"+a[3].replaceAll("-", "_");
				}
			}

			if( line.hasOption( "Activity" ) ) {
				String[] a = line.getOptionValues("Activity");
				//System.out.println("activity items "+a.length);
				System.out.println("Activities:");
				
				for (int i=0; i<a.length; i++){
					i++;
					
					if (i<a.length){
						//System.out.println(a[i-1]);	
						tdp.behaviours.addItem(new ActivityCategory(a[i-1], Double.parseDouble(a[i])));
					} else {
						System.out.println("Activity category speed missing: "+ a[i-1]);
						System.exit(1);
					}
				}
				
				System.out.println(tdp.behaviours);
			} else { //set up default behaviours
				tdp.behaviours.addItem(new ActivityCategory("Stationary",0.02));
				tdp.behaviours.addItem(new ActivityCategory("Foraging",0.33));
				tdp.behaviours.addItem(new ActivityCategory("Moving",10.0));
			}

			if( line.hasOption( "help" ) ) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "java -jar Beferrer", options );
				
				System.out.println("\nBeferrer accepts comma delimeted text files as input. Each valid line should contain\n"
						+ "four items: lat, long, date, time. Lat and long are decimals. Dates are in dd/mm/yyyy \n"
						+ "format and times in hh:mm:ss format. An example of a valid line of data is:\n"
						+ "52.439037, -3.993740, 27/5/2018, 10:5:54\n"
						+ "Any lines that do not contain four items in this format are ignored.\n"
						+ "\nOutput files are created in the same folder as the input data.\n"
						+ "These file names include a compact version of the (non default) command line options\n"
						+ "to allow output to be easily preserved if the tool is run with different options.\n"
						+ "\nA summary of the processing steps is output to the terminal. More detailed \n"
						+ "information can be produced using the -v (or --Verbose) option.\n"
						+ "\nThe tool also produces several data files for the widely used (free) gnuplot tool to \n"
						+ "allow quick visual sanity checking of the results. Obtain gnuplot from:\n"
						+ "http://www.gnuplot.info\n"
						+ "Assuming that gnuplot is properly installed, the contents of the file prefixed\n"
						+ "'gnuplot_commands' can be pasted onto the command line (after setting the working folder \n"
						+ "to the location of the output) to produce .eps graphs.\n"
						+ "These files contain gnuplot commands and the relevant data, in plain text format and can \n"
						+ "be edited as desired, e.g. to change line widths, axis ranges, titles etc.");
				
				System.exit(1);
			}
			
			if( line.hasOption( "Verbose" ) ) {
				debug = true;
				System.out.println( "Verbose output on");
			}
		}
		catch( NumberFormatException e ) {
			System.out.println("Number format problem with command line parameter: "+e.getMessage());
		    System.out.println("Check all command line parameter values are correctly specified and not missing");
		    System.exit(1);
		}
		catch( ParseException e ) {
			System.out.println(e.getMessage());
		    System.out.println("Try \"--help\" option for more details.");
		    System.exit(1);
		}

		// find the list of files to process
		// result is a list of the full canonical path names for each file
		Vector<String> fileNamesToProcess = tdp.findFilenames();
		
		System.out.println("Files to process:");
		for (String n : fileNamesToProcess){
			System.out.println("File: "+n);
		}
		
		// fix filename for windows
		tdp.compressedOptions = tdp.fixFilename(tdp.compressedOptions);
		
		//System.out.println("options: "+tdp.compressedOptions);
		
		tdp.setupGnuplotClFile();
		
		for (String n : fileNamesToProcess){
			tdp.processFile(n, tdp);
		}
			
		tdp.closeGnuplotClFile();
		System.out.println("FINISHED - SUCCESSFUL");
	}
	
	/**
	 * 
	 * @param suffix
	 * @return
	 */
	public Vector<String> findFilenames(){
		
		Vector<String> result = new Vector<String>();
		
		// single file requested
		if (fileName != null){
			// expand the file name
			File myFile;
			if (inputFileExtension != null)
				myFile = new File(fileName+inputFileExtension);
			else
				myFile = new File(fileName);
			
			try {
				result.add(myFile.getCanonicalPath()); // the unique full path
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}
			
			return result;
		}

		// multiple files
		// working directory
		if (folder == null){
			System.out.println("No file or folder specified, using working folder");
			folder = "";
		} 
		
		//find items
		File folderitem = new File(folder);
		String[] files = folderitem.list();
		
		if (files == null){
			System.out.println("Invalid folder: "+folder);
		}

		for (String file : files)
		{
			if (inputFileExtension != null){
				if (file.endsWith(inputFileExtension)) {
					//File myFile = new File(file);
					try {
						result.add(folderitem.getCanonicalPath()+File.separator+file); // the unique full path
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(0);
					}
				}
				//System.out.println(file);
			} else {
				try {
					result.add(folderitem.getCanonicalPath()+File.separator+file); // the unique full path
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(0);
				}
			}
		}

		return result;
	}

	/**
	 * process one data file
	 * 
	 * @param filename
	 * @param tdp
	 */
	public void processFile(String filename, Beferrer tdp){

		// load file, skippping any gps items that are out of given region or chronological order
		tdp.fileName = filename;
		tdp.loadFile(); // only consider positions within this range
		// output points data to screen
		if (debug) tdp.listPointsData();
		
		//remove gps error points, based on implausible movements
		//tdp.cleanRawDataRecursive(100.0);
		//max 10m/s		
		// average positions where distance is less than specified. does not change the number of points
		//tdp.AverageGPSResolution(2.0, 300); // maximum *distance* to average over(m), maximum number of samples to average

		// convert series of points into movements
		tdp.createMovementData();
		// output movement data to screen
		if (debug) tdp.listMovementData();

		// select contiguous sequences of data movements of a specific duration. 
		//   this allows the data taken at a specific sample period rate to be selected (e.g. 0-3 seconds)
		//   creates a Vector of MovementGroup (contiguous movement set) of data within SchedulePeriod at the specific sample period rate,
		//   a group does not contain movements either faster or slower than specified (any such movements terminates a group).
		// TODO for longer time periods we need to include 'relevant' samples from denser sequences...
		SchedulePeriod sp = new SchedulePeriod(tdp.timePeriodMin, tdp.timePeriodMax, tdp.movementData);
		if (debug) sp.ListMovementGroups();

		// remove obvious single point GPS glitches.
		//   two movements with high speed should be combined into a single entry provided 
		//   the combined movement results in a proper speed between those points. 
		sp.cleanMovementData(tdp.glitchSpeed); // 30 m/s max speed used to remove gps glitches

		// locked gps values (low movement optimisation by hardware) at start and end of a sampling group are 
		//   unreliable since we don't know how long the condition might have existed (or will persist) for, 
		//   before sampling started (after ended) - hence duration/speed or averaging for that period may be very wrong.
		sp.removeStartEndStationaryValues();
		
		// remove the first n movements from each group
		sp.removeStartnValues(tdp.startPointsRemoved);
		// remove the first n movements from each group
		sp.removeEndnValues(tdp.endPointsRemoved);
		
		sp.selectDates(tdp.start_d, tdp.start_m, tdp.start_y, tdp.end_d, tdp.end_m, tdp.end_y,
				tdp.dateRangeStart, tdp.dateRangeEnd);

		sp.selectTimeOfDay(tdp.start_hh, tdp.start_mm, tdp.start_ss, tdp.end_hh, tdp.end_mm, tdp.end_ss, 
				tdp.timeOfDayRangeStart, tdp.timeOfDayRangeEnd);
		
		// remove all points before or after a specified high Speed value in each group
		// this is because removing a point that causes a high speed movement just propagates the movement 
		// note - cleanMovementData will already have removed single point 'gps' glitches, so these 
		//   are more likely to be a 'settling in' period when the GPS unit turns on after a while that leads to 
		//   a set of inaccurate positions before accuracy is regained.
		sp.truncateToGPSErrorInGroup(tdp.trimGroupBelowSpeed); // remove any starts or ends of groups after a high speed value (in m/s) 15m/s

		// Only do ONE of CombineStationaryValues OR AverageGPSResolution.
		//
		// combine stationary values into a single (longer duration) movement incorporating the first non zero value (fix gps locking)
		if (tdp.positionAverage == 0)
			sp.CombineStationaryValues();
		else
			// alternatively could average gps positions. An average position is created using all points up to the 
			//   specified distance from the point being adjusted 'averaged'
			sp.AverageGPSResolution(tdp.positionAverage); // maximum *distance* to average over(m)

		// average resulting movements
		sp.averageMovementdata(tdp); // create averages (sets MovementData.average_speed value)
		
		System.out.println("----------------------------------------------");
		
		tdp.saveMovementData(tdp.compressedOptions, sp, tdp);
	
		// save the statistics for each behaviour category
		tdp.saveMovementStats(tdp.compressedOptions+"_stats_raw", sp, false, tdp); // not averaged over averaging_period
		tdp.saveMovementStats(tdp.compressedOptions+"_stats_averaged", sp, true, tdp); // averaged over averaging_period
		
		makePlots(tdp, sp);
		
		return;
	}

	/**
	 * 
	 * @param filename
	 * @param tdp
	 * @param sp
	 */
	public void makePlots(Beferrer tdp, SchedulePeriod sp){
		// create plots
		//tdp.LINEWIDTH = 0.75;
		tdp.GNUplotPositionWithSpeed1(tdp.compressedOptions+"_LatLonSpeed_"+sp.toStringRange()+".gnuplot", sp);
		
		tdp.LINEWIDTH = 0.85;
		tdp.GNUplotPositionWithSample(tdp.compressedOptions+"_LatLonSample_"+sp.toStringRange()+".gnuplot", sp);

		tdp.LINEWIDTH = 0.2;
		int maxspeedrange = 15;
		tdp.GNUplotPositionGroupsWithSpeed(tdp.compressedOptions+"_GroupsSpeed_"+sp.toStringRange()+".gnuplot", sp, maxspeedrange);
		//tdp.addGnuplotClCommand("gnuplot "+tdp.gnuplotFileName+"GroupsSpeed_"+sp.toStringRange()+".gnuplot");

	}
	
	public void dummy(String filename, Beferrer tdp, SchedulePeriod sp){
		System.exit(-1);

		// Below this line, the code works with the grouped data tracks yet

		//tdp.GNUplotPositions("gps.gnuplot");
		//tdp.GNUplotTime("time.gnuplot");

		// this will remove entries that appear to have gps glitches 
		// and combine non moving periods into a bigger movement over a longer time period
		// mutually exclusive with tdp.AverageGPSResolution() since that will have averaged out 'locked' positions
		//tdp.cleanMovementData(1000, true); //10 m/s max speed, combineStationary values

		//tdp.averaging_period = 20; //seconds //FIXME
		//tdp.averageMovementdata(tdp.averaging_period); // set  .average_speed  value for each data item

		//tdp.saveMovementData("movement_data.txt");

		tdp.scheduleGroupings = new Vector<SchedulePeriod>(); //FIXME
		//tdp.scheduleGroupings.add(new SchedulePeriod(0, 3)); // 0 to <3 seconds
		//tdp.scheduleGroupings.add(new SchedulePeriod(3, 240)); //3 to <240 seconds
		//tdp.scheduleGroupings.add(new SchedulePeriod(240, 900)); 
		//tdp.scheduleGroupings.add(new SchedulePeriod(900, 3000)); 
		//tdp.scheduleGroupings.add(new SchedulePeriod(3000, 100000));
		//tdp.scheduleGroupings.add(new SchedulePeriod(0, 240)); //this is possible aas well


		// plot lat/lon by time between samples
		for (SchedulePeriod s : tdp.scheduleGroupings){
			// plot animal tracks .lat, .lon, coloured by .speed (raw speed value)
			tdp.GNUplotPositionWithSpeed("LatLonSpeed_"+s.toStringRange()+".gnuplot", s); 
			// add the command to generate gnuplot to the CL file
			tdp.addGnuplotClCommand("gnuplot "+tdp.gnuplotFileName+"LatLonSpeed_"+s.toStringRange()+".gnuplot");
		}
		// all data (all sampling times)
		tdp.GNUplotPositionWithSpeed("LatLonSpeed.gnuplot", null);
		tdp.addGnuplotClCommand("gnuplot "+tdp.gnuplotFileName+"LatLonSpeed.gnuplot");

		tdp.GNUplotPositionWithSpeedSVG("LatLonSpeed.gnuplot", null);

		// plot lat/lon by time between samples not very useful really
		/**
		for (SchedulePeriod s : tdp.scheduleGroupings){
			tdp.GNUplotPositionWithSampleperiod("LatLonSampleperiod_"+s.toStringRange()+".gnuplot", s);
			tdp.addGnuplotClCommand("gnuplot "+tdp.gnuplotFileName+"LatLonSampleperiod_"+s.toStringRange()+".gnuplot");
		}
		/**/
		// all data
		tdp.GNUplotPositionWithSampleperiod("LatLonSampleperiod.gnuplot", null); //filename, range, linewidth
		tdp.addGnuplotClCommand("gnuplot "+tdp.gnuplotFileName+"LatLonSampleperiod.gnuplot");

		/*
		// create several bar chart speed plots by sample number, with the .average speed as a line 
		int samples_per_graph = 500; //FIXME
		for (int i=0; i < tdp.movementData.size()-samples_per_graph; i+=samples_per_graph){
			// uses tdp.averaging_period
			tdp.addGnuplotClCommand("gnuplot "+tdp.gnuplotFileName+"speed_"+samples_per_graph+"samples_"+i+".gnuplot");

			tdp.GNUplotSpeedBySamples(
					"speed_"+samples_per_graph+"samples_"+i+".gnuplot", // gnuplot filename
					i, i+samples_per_graph, // range of samples
					true //histogram style or lines
					);
		}
		 */

		// plot average speed bar chart, coloured by time of day
		tdp.GNUplotAveSpeedBySamplesGroup("ave_speed_group.gnuplot", 
				-1, 5, //time 0-5 seconds
				5, //min 5 samples in group
				50, // max_speed discount any averages higher than this (GPS error)
				350.0, // max_group_duration - start a new average group after this time
				5, // skip_first_n skip first n samples in each group of samples
				true, tdp.behaviours); //histogram style plot
		tdp.addGnuplotClCommand("gnuplot "+tdp.gnuplotFileName+"ave_speed_group.gnuplot");

		//tdp.saveFile("suffix");
	}

	/**
	 * 
	 */
	public Beferrer(){
		rowData = new  Vector<TagDataItem>();
		compressedOptions = "";
	}

	/**
	 * 
	 */
	private void setupGnuplotClFile(){

		String gpFileName;
				
		if (folder != null){
			gpFileName = folder+"gnuplot_commands_"+compressedOptions;
		} else {
			if (fileName != null)
				gpFileName = fileName+"_"+compressedOptions+"_gnuplot_commands";
			else 
				gpFileName = ".."+File.separator+compressedOptions+"gnuplot_commands";
		}
		//
		
		File myFile = new File(gpFileName);
		try {
			gnuplotFileName = myFile.getCanonicalPath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//set up gnuplot instuction file
		FileOutputStream filewriter= null;
		try {
			filewriter = new FileOutputStream(gnuplotFileName);
		} catch (FileNotFoundException e) {
			System.out.println("File write error! - "+ gnuplotFileName);
			return;
		}

		try {
			gnuplot_cl_filewriter = new OutputStreamWriter(filewriter,"UTF8");
		} catch (IOException e) {
			System.out.println("File open error! - "+ gnuplotFileName);
		}
		
		System.out.println("GNU Plot command file: "+gnuplotFileName);
	}

	/**
	 * 
	 */
	private void closeGnuplotClFile(){
		try {
			gnuplot_cl_filewriter.close();
		} catch (IOException e) {
			System.out.println("Cant close file error! - "+ fileName+"_gnuplotCL");
		}	

	}

	private void addGnuplotClCommand(String command){
		try {
			gnuplot_cl_filewriter.write(command+"\n");
		} catch (IOException e) {
			System.out.println("Cant close file error! - "+ fileName+"_gnuplotCL");
		}	
	}


	/**
	 * to prevent short fast movements caused by GPS resolution limitations
	 * this function will convert any point that has less than the minimum distance specified
	 * into the position that is the average of n surrounding points.
	 * n is determined as the number of points needed such that the combined distance from the current point
	 * to point current+n and current-n > minDist specified.
	 * 
	 * the original data is preserved in 
	 */
	public void AverageGPSResolution(double minDist, int maxPointsToAverage){

		for (int i=0; i<rowData.size(); i++ ){

			TagDataItem pointi = rowData.get(i);

			int n = 0;
			double distance = 0;

			double latTotal = rowData.get(i).pos.lat;
			double lonTotal = rowData.get(i).pos.lon;	

			while (distance < minDist && n < maxPointsToAverage){
				n++;

				if ((i-n) > 0 && (i+n) < rowData.size()){ //edges of dataset
					double distprev = pointi.distance(rowData.get(i-n));
					double distnext = pointi.distance(rowData.get(i+n));

					distance = distprev+distnext;
					latTotal = latTotal+rowData.get(i+n).pos.lat;
					lonTotal = lonTotal+rowData.get(i+n).pos.lon;
					latTotal = latTotal+rowData.get(i-n).pos.lat;
					lonTotal = lonTotal+rowData.get(i-n).pos.lon;
					//System.out.println(i+":"+n+":"+distance);
				}
			}

			System.out.println("Point "+i+" Average point positions for "+n+" points "+ distance+" distance");
			// copy original coordinate
			pointi.originalPosition = new GpsPosition(pointi.pos);
			// create average
			pointi.pos.lat = latTotal/(n*2+1);
			pointi.pos.lon = lonTotal/(n*2+1);

			System.out.println("Point "+i+"Original "+pointi.originalPosition+" New "+pointi.pos);

			// if the time has not changed then cant calculate speed 
			// usually this is a duplicate entry in the data

			//if (last != null ) {

			// skip duplicate entries (same time...)
			//if (i.timeSecs != last.timeSecs) {

			//}
			//}
		}
	}

	/**
	 * 
	 * @return
	 */
	public void cleanRawDataRecursive(double maxSpeed){
		while (!cleanRawData(maxSpeed)) {
			System.out.println("Another go."+rowData.size()+" items remaining");
		}
	}
	/**
	 * 
	 * @param maxSpeed
	 */
	private boolean cleanRawData(double maxSpeed){
		Vector<TagDataItem> toRemove = new Vector<TagDataItem>();

		TagDataItem lastItem = null;

		for (TagDataItem i : rowData){
			if (lastItem != null) {
				double speed = lastItem.distance(i)/i.elapsedTime(lastItem);
				if (speed >= maxSpeed){
					toRemove.add(i);
					System.out.println("Clean raw data removed item due to high speed: "+String.format("%7.2f",speed)
					+" last:"+lastItem.toStringDateTime()+" this:"+i.toStringDateTime()
					+" dist:"+String.format("%8.2f",lastItem.distance(i))
					+" time:"+(i.timeSecs-lastItem.timeSecs)

							);
				}
			}

			lastItem = i;
		}

		rowData.removeAll(toRemove);

		return toRemove.isEmpty();
	}

	/**
	 * 
	 */
	public void createMovementData(){
		System.out.println("----------------------------------------------");
		System.out.println("Create movement data");	
		
		TagDataItem last = null;
		long originalMovementNumber = 0;

		movementData = new Vector<MovementDataItem>();

		for (TagDataItem i : rowData){


			// if the time has not changed then cant calculate speed 
			// usually this is a duplicate entry in the data

			if (last != null ) {

				// skip duplicate entries (same time...)
				if (i.timeSecs != last.timeSecs) {

					//System.out.print(i);
					//double dist = i.distance(last);
					//double speed = i.speed(last);

					MovementDataItem movementItem = new MovementDataItem(
							last.pos, i.pos, i.elapsedTime(last),
							i.date, i.time, originalMovementNumber); //date at start of movement

					movementData.add(movementItem);
					originalMovementNumber++;

					//System.out.print(" time:" + String.format("%5d",i.elapsedTime(last))+
					//		" dist:"+ String.format("%8.3f", dist)+
					//		" speed:"+String.format("%8.3f", speed));		

					//System.out.println(movementItem);
				}
			}


			last = i;
		}
		
		System.out.println("Created " + movementData.size() + " valid movements");
	}
	
	/**
	 * 
	 */
	public void listMovementData(){
		System.out.println("----------------------------------------------");
		System.out.println("List movement data");

		for (MovementDataItem i : movementData){

			System.out.println(i);
		}
	}

	/**
	 * this will remove entries that appear to have gps glitches
	 * so two movements with high speed should be combined into a single entry with 
	 * proper speed.
	 * 
	 * a single movement with excessive speed (in m/s) will be removed
	 * setting combineStationary will aggregate multiple stationary values
	 */
	public void cleanMovementData(double maxSpeed, boolean combineStationary){
		System.out.println("Clean movement data");
		Vector<MovementDataItem> newMovementData = new Vector<MovementDataItem>();

		MovementDataItem previous = null;
		int stationary_time = 0; //number of seconds no movement has occurred for
		int stationary_samples = 0; //number of samples no movement has occurred for

		for (MovementDataItem current : movementData){			
			if (previous != null){
				// detect gps glitch
				if (previous.speed() > maxSpeed && current.speed() > maxSpeed){
					// combine two movements if speed of two consecutive movements is excessive
					MovementDataItem combined = new MovementDataItem(
							previous.getStart(), current.getEnd(), previous.duration+current.duration,
							previous.getStartDate(), previous.getStartTime(), previous.originalMovementNumber);

					combined.addNote("Combined two movements, both >"+maxSpeed+" m/s");

					if (combined.speed() < maxSpeed) {
						newMovementData.add(combined);
						System.out.println("Combined movement: "+previous+"\n"+current+"\n"+combined);
					} else {
						System.out.println("Removed point due to excessive speed: "+previous+"\n"+current+"\n"+combined);
					}
					stationary_time = 0;
					stationary_samples = 0;

				} else {
					//look for excessive speed
					if (previous.speed() > maxSpeed){
						System.out.println("Single movement excessive speed "+previous);
					} else {

						// if previous stationary and current stationary then
						// add the current time duration to the previous and move on without replacing previous
						if (combineStationary && previous.isStationary() && current.isStationary()){
							stationary_time += current.duration;
							stationary_samples++;

							previous.duration += current.duration; 
							current = previous; // this will become previous at end of loop	
						} else {

							if (stationary_time > 0){
								System.out.println("Added stationary time of "+stationary_time
										+"s for "+stationary_samples+" samples: "+previous);
								previous.addNote("combined "+(stationary_samples+1)+" stationary values "); 
							}

							// 'locked up gps' add in previous stationary time to current move
							//
							// if stationary for 1800s and current move speed is > 10m\s
							// assume that previous stationary moves were not real
							//if (current.speed() > 10 && stationary_time > 1800){
							//
							if (current.speed() > 0 && stationary_time > 0){
								current.duration += previous.duration; 
								current.addNote("Lockup - combined previous stationary "+ stationary_samples+" values");
							} else {
								newMovementData.add(previous);		
							}

							stationary_time = 0;
							stationary_samples = 0;
						}
					}
				}
			}

			previous = current;
		}

		if (movementData.lastElement().speed() <= maxSpeed) {
			newMovementData.add(movementData.lastElement());
		}

		movementData = newMovementData;
	}



	/**
	 * 
	
	public void listData1(){
		System.out.println("----------------------------------------------");
		System.out.println("List raw data");
		
		TagDataItem last = null;

		for (TagDataItem i : rowData){


			// if the time has not changed then cant calculate speed 
			// usually this is a duplicate entry in the data

			if (last != null ) {

				if (i.timeSecs != last.timeSecs) {
					System.out.print(i);

					double dist = i.distance(last);
					double speed = i.speed(last);
					System.out.print(" time:" + String.format("%5d",i.elapsedTime(last))+
							" dist:"+ String.format("%8.3f", dist)+
							" speed:"+String.format("%8.3f", speed));	

					if (speed<0.02) {
						System.out.print("");
					} else if (speed<0.33){
						System.out.print(" Grazing");
					} else if (speed<10) {
						System.out.print(" Moving");
					} else {
						System.out.print(" GPS ERROR");
					}

					System.out.println();
				}
			}


			last = i;
		}
	} */
	
	/**
	 * 
	 */
	public void listPointsData(){
		System.out.println("----------------------------------------------");
		System.out.println("List raw points data");

		//TagDataItem last = null;

		for (TagDataItem i : rowData){

			System.out.println(i);
		}

	}

	/**
	 * 
	 */
	public void loadFile(){
		System.out.println("----------------------------------------------");
		System.out.println("Load file data "+fileName);
		
		rowData.clear();
		
		int lineCount = 0;
		try {

			FileInputStream tagFileStream = new FileInputStream(fileName);	
			BufferedReader br = new BufferedReader(new InputStreamReader(tagFileStream));

			TagDataItem item = null;

			String line = br.readLine();
			while (line != null) {			

				//sb.append(System.lineSeparator());
				item = processDataline(line, item, northWestBoundary, southEastBoundary);

				line = br.readLine();
				lineCount++;
			}

			tagFileStream.close();
		}

		catch (FileNotFoundException e1) {
			System.out.println("Can't open file for reading: "+fileName);
			System.exit(1);
			return;

		} catch (IOException e) {} // can't close file!					}

		System.out.println("Loaded "+this.rowData.size()+" valid data points (read "+lineCount+ " lines)");
	}

	/**
	 * add a line of data
	 * returns null is failed
	 */
	private TagDataItem processDataline(String line, TagDataItem lastItem, 
			GpsPosition northWestBoundary, GpsPosition southEastBoundary){
		//split by comma or comma space
		String[] result = line.split("( |, )");

		long lastTimeSecs = 0;
		if (lastItem != null) {
			lastTimeSecs = lastItem.timeSecs;
		}

		if (result.length == 4){
			TagDataItem item = new TagDataItem(result[0], result[1], result[2], result[3]);

			//remove zero coordinates
			boolean validCoordinate = true;
			
			if (item.pos.lat == 0 && item.pos.lon == 0) {
				System.out.println("Removing zero coordinate: "+item);
				return lastItem;
			}
			
			if (northWestBoundary != null && southEastBoundary != null){ //user specified boundary
				if ((	   item.pos.lat >= northWestBoundary.lat // inside boundary
						&& item.pos.lat <= southEastBoundary.lat 
						&& item.pos.lon >= northWestBoundary.lon 
						&& item.pos.lon <= southEastBoundary.lon)) {
					validCoordinate = false;
				}
			}
			
			if (validCoordinate){
				//remove any items that are out of chronological order
				if (item.timeSecs >= lastTimeSecs){
					rowData.add(item);
					lastTimeSecs = item.timeSecs;
					lastItem = item;
				} else {
					System.out.println("Skipping out of chronological order item: "+item);
				}
			} else {
				System.out.println("Skipping coordinate outside Coordinate Fence: "+
						item+" (boundary: "+northWestBoundary+" "+southEastBoundary+")");
			}
		}

		return lastItem;
	}

	/**
	 * 
	 * @param lastItem
	 * @throws IOException 
	 
	private void writeLine(OutputStreamWriter writer, TagDataItem lastItem) throws IOException{
		writer.write(lastItem.pos.lat+", ");
		writer.write(lastItem.pos.lon+", ");
		writer.write(lastItem.date+", ");
		writer.write(lastItem.time);
		writer.write("\n");
	}
*/
	
	/**
	 * 

	public void saveFile(String suffix){
		FileOutputStream filewriter = null;
		try {
			filewriter = new FileOutputStream(fileName+"_"+suffix);
		} catch (FileNotFoundException e) {
			System.out.println("File write error! - "+ fileName);
			return;
		}

		try {
			OutputStreamWriter writer = new OutputStreamWriter(filewriter,"UTF8");
			// the writer doesn't seem to add the UTF8 magic numbers...
			// so add them manually
			//byte [] magicUTF8 = {(byte)0x0ef, (byte)0x0bb, (byte)0x0bf};
			//filewriter.write(magicUTF8);

			// consider each line of data
			// lastItem - always the previous data item
			// timediff - time between two adjacent data items
			// lastLineIncluded - flag to indicate if the previous data item was included in the output
			// lastTimeIncluded - last time output


			TagDataItem lastItem = null;
			long timediff = 0;

			boolean lastLineIncluded = false;
			long lastTimeIncluded = 0; // to store the time of the last line output

			for (TagDataItem item : rowData){
				if (lastItem != null){	//can't do any calculation with first item		

					timediff = Math.abs(item.timeSecs - lastItem.timeSecs);

					//System.out.println("Time Diff "+timediff);
					if (item.timeSecs < lastItem.timeSecs){ // new day causes time to start at 0
						lastTimeIncluded = item.timeSecs; // negative
					}

					// if the required amount of time has passed between consecutive data items
					// (ie skip 'busy' periods if required)
					if (timediff >= busyPeriodMinTimeDiff && timediff <= busyPeriodMaxTimeDiff)	{

						// 
						if (!lastLineIncluded){
							writeLine(writer, lastItem);
						}

						writeLine(writer, item);

						lastLineIncluded = true;
						lastTimeIncluded = item.timeSecs;

					} else { // time between consecutive lines is not within the range required
						lastLineIncluded = false;

						// pick occasionally from 'busy' periods, as required period elapses
						if (
								Math.abs(item.timeSecs - lastTimeIncluded) > busyPeriodMinTimeDiff
								&& Math.abs(item.timeSecs - lastTimeIncluded) <= busyPeriodMaxTimeDiff) 
						{
							writeLine(writer, item);
							lastTimeIncluded = item.timeSecs;
							lastLineIncluded = true;
						} 
					}

				} 

				lastItem = item; 
			}		

			writer.close();
		} catch (IOException e) {
			System.out.println("File write error! - "+ fileName);
		}	

	}
	 */

	/**
	 * 
	 * @param suffix
	 */
	public void GNUplotGpsPositions(String suffix){
		System.out.println("Creating GNUplot file"+fileName+"_"+suffix);

		FileOutputStream filewriter = null;
		try {
			filewriter = new FileOutputStream(fileName+"_"+suffix);
		} catch (FileNotFoundException e) {
			System.out.println("File write error! - "+ fileName);
			return;
		}

		try {
			OutputStreamWriter writer = new OutputStreamWriter(filewriter,"UTF8");
			writer.write("set terminal postscript noenhanced colour linewidth 1.0 \"helvetica\" 10\n");
			writer.write("set output \""+fileName+"_"+suffix+".eps\"\n");

			writer.write("set autoscale\n");

			writer.write("set title \"Tag data (behaviour identification experiment nns)"+fileName+"_"+suffix+"\"\n");
			writer.write("set xlabel \"longitude\"\n");
			writer.write("show xlabel\n");
			writer.write("set ylabel \"latitide\"\n");
			writer.write("show ylabel\n");
			writer.write("set cblabel \"speed m/s\"\n");

			//main group
			//writer.write("set xrange [52.25:52.26]\n");

			writer.write("set palette defined (0 \"black\", 0.02 \"green\", 0.33 \"blue\", 4 \"red\")\n");
			//startup group
			writer.write("set xrange [52.395:52.4]\n");

			//points
			//writer.write("plot '-' w p ls 1\n");

			//writer.write("plot '-' with points linecolor palette \n");
			writer.write("plot '-' with linespoints linecolor palette \n");

			for (MovementDataItem item : movementData){						
				writer.write(item.getStart().lat+", ");
				writer.write(item.getStart().lon+", ");

				//for 'palette variable '
				if (item.speed() < 4) {
					//if (item.speed() < 0.5)
					writer.write((item.speed())+"\n");
				} else {
					writer.write(4+"\n");
				}
			}
			writer.write("e\n");

			writer.close();

		} catch (IOException e) {
			System.out.println("File write error! - "+ fileName);
		}	
	}

	/**
	 * to plot the gps positions coloured according to time period
	 * @param suffix
	 */
	public void GNUplotPositionWithSampleperiod(String suffix, SchedulePeriod schedule){
		System.out.println("Creating GNUplot file"+fileName+"_"+suffix);

		FileOutputStream filewriter = null;
		try {
			filewriter = new FileOutputStream(fileName+"_"+suffix);
		} catch (FileNotFoundException e) {
			System.out.println("File write error! - "+ fileName);
			return;
		}

		try {
			OutputStreamWriter writer = new OutputStreamWriter(filewriter,"UTF8");
			writer.write("set terminal postscript noenhanced colour linewidth "+ LINEWIDTH +" \"helvetica\" 10\n");
			writer.write("set output \""+fileName+"_"+suffix+".eps\"\n");

			writer.write("set autoscale\n");
			//writer.write("set xrange [52.24:52.28]\n");

			writer.write("set title \"Tag data (behaviour identification experiment nns)"+fileName+"_"+suffix+"\"\n");
			writer.write("set xlabel \"longitude\"\n");
			writer.write("show xlabel\n");
			writer.write("set ylabel \"latitide\"\n");
			writer.write("show ylabel\n");
			writer.write("set cblabel \"time between samples (s)\"\n");

			//main group
			writer.write("set xrange [52.25:52.26]\n");

			writer.write("set palette defined (0 \"blue\", 60 \"purple\", 300 \"orange\", 3600 \"gold\", 25000 \"red\")\n");
			//writer.write("set palette defined (0 \"blue\", 60 \"magenta\", 300 \"orange\", 3600 \"gold\")\n");

			//startup group
			//writer.write("set xrange [52.395:52.4]\n");

			//points
			//writer.write("plot '-' w p ls 1\n");

			//writer.write("plot '-' with linespoints linecolor palette \n");
			//writer.write("plot '-' with points linecolor palette \n");
			// vectors
			writer.write("plot '-' with vectors nohead linecolor palette z\n"); //palette colours

			for (MovementDataItem item : movementData){						
				if (schedule == null || schedule.in(item.duration)) {
					writer.write(item.getStart().lat+", ");
					writer.write(item.getStart().lon+", ");

					// for vectors
					writer.write(item.getEnd().lat-item.getStart().lat+", ");
					writer.write(item.getEnd().lon-item.getStart().lon+", ");


					//for 'palette variable '
					//if (item.duration < 3600) {
					if (item.duration < 25000) {
						//if (item.speed() < 0.5)
						writer.write((item.duration)+"\n");
					} else {
						writer.write(25000+"\n");
						//writer.write(3600+"\n");
					}
				}
			}
			writer.write("e\n");

			writer.close();

		} catch (IOException e) {
			System.out.println("File write error! - "+ fileName);
		}	
	}

	/**
	 * 
	 * @param suffix
	 * @param schedule
	 */
	public void GNUplotPositionWithSpeed1(String suffix, SchedulePeriod schedule){
		String nameComposite = fixFilename(fileName)+"_"+suffix;
		String nameOnly = fixFilename(fileName).substring(fileName.lastIndexOf(File.separator)+1);
		addGnuplotClCommand("gnuplot "+nameComposite);
		System.out.println("Creating GNUplot file"+nameComposite);

		FileOutputStream filewriter = null;
		try {
			filewriter = new FileOutputStream(nameComposite);
		} catch (FileNotFoundException e) {
			System.out.println("File write error! - "+ nameComposite);
			return;
		}

		try {
			OutputStreamWriter writer = new OutputStreamWriter(filewriter,"UTF8");

			writer.write("set terminal postscript noenhanced colour linewidth "+ LINEWIDTH +" \"helvetica\" 10\n");
			writer.write("set output \""+fileName+"_"+suffix+".eps\"\n");		
			
			writer.write("set title \""+nameOnly+"_"+suffix+"\"\n");

			schedule.GNUplotPositionWithSpeed1(writer, northWestBoundary, southEastBoundary);

			writer.close();

		} catch (IOException e) {
			System.out.println("File write error! - "+ nameComposite);
		}	
	}

	/**
	 * 
	 * @param suffix
	 * @param schedule
	 */
	public void GNUplotPositionWithSample(String suffix, SchedulePeriod schedule){
		String nameComposite = fixFilename(fileName)+"_"+suffix;
		String nameOnly = fixFilename(fileName).substring(fileName.lastIndexOf(File.separator)+1);
		addGnuplotClCommand("gnuplot "+nameComposite);
		System.out.println("Creating GNUplot file"+nameComposite);

		FileOutputStream filewriter = null;
		try {
			filewriter = new FileOutputStream(nameComposite);
		} catch (FileNotFoundException e) {
			System.out.println("File write error! - "+ nameComposite);
			return;
		}

		try {
			OutputStreamWriter writer = new OutputStreamWriter(filewriter,"UTF8");

			writer.write("set terminal postscript noenhanced colour linewidth "+ LINEWIDTH +" \"helvetica\" 10\n");
			writer.write("set output \""+fileName+"_"+suffix+".eps\"\n");		
			writer.write("set autoscale\n");
			writer.write("set title \""+nameOnly+"_"+suffix+"\"\n");

			schedule.GNUplotPositionWithSample(writer, northWestBoundary, southEastBoundary);

			writer.close();

		} catch (IOException e) {
			System.out.println("File write error! - "+ nameComposite);
		}	
	}

	/**
	 * 
	 * @param suffix
	 * @param schedule
	 * 
	 * plot each group (x) against speed (y)
	 */
	public void GNUplotPositionGroupsWithSpeed(String suffix, SchedulePeriod schedule, int maxspeedrange){
		String nameComposite = fixFilename(fileName)+"_"+suffix;
		String nameOnly = fixFilename(fileName).substring(fileName.lastIndexOf(File.separator)+1);
		addGnuplotClCommand("gnuplot "+nameComposite);
		System.out.println("Creating GNUplot file"+nameComposite);

		FileOutputStream filewriter = null;
		try {
			filewriter = new FileOutputStream(nameComposite);
		} catch (FileNotFoundException e) {
			System.out.println("File write error! - "+ nameComposite);
			return;
		}

		try {
			OutputStreamWriter writer = new OutputStreamWriter(filewriter,"UTF8");

			writer.write("set terminal postscript noenhanced colour linewidth "+ LINEWIDTH +" \"helvetica\" 10\n");
			writer.write("set output \""+fileName+"_"+suffix+".eps\"\n");		
			writer.write("set autoscale\n");
			writer.write("set title \""+nameOnly+"_"+suffix+"\"\n");

			schedule.GNUplotPositionGroupsWithSpeed(writer, maxspeedrange); //max speed value

			writer.close();

		} catch (IOException e) {
			System.out.println("File write error! - "+ nameComposite);
		}	
	}

	/**
	 * output gnuplot file with points or lines
	 * @param suffix
	 * @param schedule - samples with this elapsed time, null uses all samples
	 */
	public void GNUplotPositionWithSpeed(String suffix, SchedulePeriod schedule){
		System.out.println("Creating GNUplot file"+fileName+"_"+suffix);

		FileOutputStream filewriter = null;
		try {
			filewriter = new FileOutputStream(fileName+"_"+suffix);
		} catch (FileNotFoundException e) {
			System.out.println("File write error! - "+ fileName);
			return;
		}

		try {
			OutputStreamWriter writer = new OutputStreamWriter(filewriter,"UTF8");
			writer.write("set terminal postscript noenhanced colour linewidth "+ LINEWIDTH +" \"helvetica\" 10\n");
			writer.write("set output \""+fileName+"_"+suffix+".eps\"\n");		

			writer.write("set autoscale\n");
			//writer.write("set xrange [52.24:52.28]\n");

			writer.write("set title \"Tag data (behaviour identification experiment nns)"+fileName+"_"+suffix+"\"\n");

			writer.write("set xlabel \"longitude\"\n");
			writer.write("show xlabel\n");
			writer.write("set ylabel \"latitide\"\n");
			writer.write("show ylabel\n");
			writer.write("set cblabel \"speed m/s\"\n");


			//main group
			writer.write("set xrange [52.25:52.26]\n");

			//startup group
			//writer.write("set xrange [52.395:52.4]\n");

			//points
			//writer.write("plot '-' w p ls 1\n");

			//writer.write("plot '-' with vectors ls 1\n");

			// colours according to last column
			//writer.write("plot '-' with vectors filled linecolor palette \n");
			//writer.write("plot '-' with vectors nohead linecolor palette \n");

			//writer.write("plot '-' with vectors nohead linecolor variable \n"); //line colours

			// set up palette colours focussed on the speed categories 
			writer.write("set palette defined (0 \"black\", 0.02 \"green\", 0.33 \"blue\", 2 \"red\", 30 \"yellow\")\n");
			if (schedule != null)
				writer.write("plot '-' with vectors nohead linecolor palette z title \"movements at sample rate "+schedule.toStringRange()+"\"\n"); //palette colours
			else
				writer.write("plot '-' with vectors nohead linecolor palette z title \"all movements\"\n"); //palette colours

			MovementDataItem lastValidMovement = null;
			int itemInGroup = 0;

			for (MovementDataItem item : movementData){	
				//System.out.println(item);
				if (schedule == null || schedule.in(item.duration)) {
					System.out.println("XXX");
					//if (lastValidMovement!=null) System.out.println(item.start+" "+lastValidMovement.end);
					if (lastValidMovement==null || item.getStart().samePos(lastValidMovement.getEnd())) { //still part of a sequence of movements
						System.out.println("YYY");
						lastValidMovement = item;
						itemInGroup++;

						if (itemInGroup >=0) {
							//if (item.speed() < 30) {
							writer.write(item.getStart().lat+", ");
							writer.write(item.getStart().lon+", ");

							writer.write(item.getEnd().lat-item.getStart().lat+", ");
							writer.write(item.getEnd().lon-item.getStart().lon+", ");

							//for 'palette variable' limiting
							if (item.speed() < 30) {
								//if (item.speed() < 0.5)
								writer.write((item.speed())+" ");
							} else {
								writer.write(30+" ");
							}

							writer.write("\n");
						} else {System.out.println("SKIP");}
					} else {
						itemInGroup = 0;
						lastValidMovement = null;
						System.out.println("ZZZ");
					}
				} else {
					itemInGroup = 0;
					lastValidMovement = null;
					System.out.println("QQQ");
				}


			}

			writer.close();

		} catch (IOException e) {
			System.out.println("File write error! - "+ fileName);
		}	
	}


	/**
	 * output gnuplot graph of time vs speed
	 * 
	 * plots bar chart of .item
	 * with line for .average_speed
	 * 
	 * @param suffix

	public void GNUplotSpeedBySamples(String suffix, int firstSample, int lastSample, boolean histogram_style){
		//System.out.println("Creating GNUplot file"+fileName+"_"+suffix);

		FileOutputStream filewriter = null;
		try {
			filewriter = new FileOutputStream(fileName+"_"+suffix);
		} catch (FileNotFoundException e) {
			System.out.println("File write error! - "+ fileName);
			return;
		}

		try {
			OutputStreamWriter writer = new OutputStreamWriter(filewriter,"UTF8");
			writer.write("set terminal postscript noenhanced colour linewidth 1.0 \"helvetica\" 10\n");
			writer.write("set output \""+fileName+"_"+suffix+".eps\"\n");

			writer.write("set autoscale\n");
			//writer.write("set xrange [52.24:52.28]\n");

			writer.write("set title \"Tag data (behaviour identification experiment nns)"+fileName+"_"+suffix+"\"\n");

			//writer.write("set xlabel \"time\"\n");
			writer.write("set xlabel \"sample number\"\n");

			writer.write("show xlabel\n");
			writer.write("set ylabel \"speed\"\n");
			writer.write("show ylabel\n");
			writer.write("set cblabel \"time between samples (s)\"\n");

			writer.write("set datafile separator \",\"\n");
			//speed axis
			writer.write("set yrange [-0.5:3]\n");	//FIXME

			//writer.write("set datafile missing '0'\n");

			long sample_n = 0;
			writer.write("$data << EOD\n");
			for (MovementDataItem item : movementData){	

				if (sample_n >firstSample && sample_n <lastSample){

					// histogram style needs vectors
					writer.write(sample_n+", "); // x
					writer.write("0, "); // y
					writer.write("0, "); // delta x
					writer.write(item.speed()+", "); //delta y

					//for 'palette variable ' truncate at one hour max
					if (item.duration < 3600) {					
						writer.write((item.duration)+"\n");
					} else {
						writer.write(3600+"\n");
					}
				}

				// next sample
				sample_n++;				
			}

			writer.write("\n\n"); // new dataset

			sample_n = 0;
			for (MovementDataItem item : movementData){	

				if (sample_n >firstSample && sample_n <lastSample){

					//x value
					writer.write(sample_n+", ");	

					// y value, prevent 0's from being plotted with NaN
					if (item.average_speed > 0)
						writer.write(item.average_speed+", ");
					else 
						writer.write("NaN\n");

					//for 'palette variable ' truncate at one hour max
					if (item.duration < 3600) {
						writer.write((item.duration)+"\n");
					} else {
						writer.write(3600+"\n");
					}
				}

				// next sample
				sample_n++;				
			}

			writer.write("EOD\n");

			// set up palette colours focussed on the speed categories 
			writer.write("set palette defined (0 \"dark-green\", 60 \"green\", 300 \"blue\", 3600 \"red\")\n");
			if (histogram_style){
				//writer.write("plot '$data' using 1:2:3:4:5 index 0 with vectors nohead linecolor palette z \n"); //palette colours		
				//writer.write("plot '-' using 1:2:3 index 1 with lines linecolor palette z\n"); //palette colours
				writer.write("plot '$data' using 1:2:3:4:5 index 0 with vectors nohead linecolor palette z title \"speed\" ,"+
						//"'$data' using 1:2:3 index 1 with lines linewidth 2 linecolor palette z\n"); //palette colours
						"'$data' using 1:2 index 1 with lines linewidth 2 linecolor \"orange\" title \"average speed ("+averaging_period+"sec)\"\n"); 
			} else {
				writer.write("plot '$data' using 1:4:5 index 0 with lines linecolor palette z,"+ //palette colours
						"'$data' using 1:2 index 1 with lines linewidth 2 linecolor \"orange\" \n"); 
			}

			writer.close();

		} catch (IOException e) {
			System.out.println("File write error! - "+ fileName);
		}	
	}
	 */

	/**
	 * output gnuplot graph of samples vs speed - for each group of values at a specific time period
	 * Only samples within the time range specified are included
	 * @param suffix
	 */
	public void GNUplotAveSpeedBySamplesGroup(String suffix, 
			int minTime, int maxTime, // sample period in raw data
			int min_samples_for_average, // discount sample periods with less than this number of samples
			int max_speed, // discount any results with > this speed.
			double max_group_duration, // max time for a sample group
			int skip_first_n, // skip first n samples in each group (GPS noise...)
			boolean histogram_style, Behaviour beh){
		//System.out.println("Creating GNUplot file"+fileName+"_"+suffix);

		FileOutputStream filewriter = null;
		FileOutputStream filewriterdata = null;
		try {
			filewriter = new FileOutputStream(fileName+"_"+suffix);
			filewriterdata = new FileOutputStream(fileName+"_data_"+suffix);
		} catch (FileNotFoundException e) {
			System.out.println("File write error! - "+ fileName);
			return;
		}

		try {
			OutputStreamWriter writerdata = new OutputStreamWriter(filewriterdata,"UTF8");
			// dont delete
			//writerdata.write("sample group, "+ 
			//		" n_samples, "+
			//		" distance (m), "+
			//		" duration (s), "+ 
			//		" ave speed (m/s): "+
			//		" start pos, "+
			//		" ave pos, "+
			//		" end_pos, "+
			//		" date, "+
			//		" hour of day, "+
			//		" behaviour category"+
			//		"\n"
			//		);	
			//
			OutputStreamWriter writer = new OutputStreamWriter(filewriter,"UTF8");
			writer.write("set terminal postscript noenhanced colour linewidth 1.0 \"helvetica\" 10\n");
			writer.write("set output \""+fileName+"_"+suffix+".eps\"\n");

			writer.write("set autoscale\n");
			//writer.write("set xrange [52.24:52.28]\n");

			writer.write("set title \"Tag data (behaviour identification experiment nns)"+fileName+"_"+suffix+"\"\n");

			//writer.write("set xlabel \"time\"\n");
			writer.write("set xlabel \"sample group\"\n");

			writer.write("show xlabel\n");
			writer.write("set ylabel \"ave speed\"\n");
			writer.write("show ylabel\n");
			writer.write("set cblabel \"hour of day (0/24 = midnight)\"\n");

			writer.write("set datafile separator \",\"\n");
			//speed axis
			writer.write("set yrange [0:4]\n");	//FIXME

			//writer.write("set datafile missing '0'\n");

			int sample_group = 0;
			//double group_total_speed = 0;
			double group_total_duration = 0;
			double group_total_distance = 0;
			int group_sample_count = 0;
			long sample_n = 0;
			int group_start_time_secs = 0;
			long sample_start_group = 0;
			Vector<MovementDataItem> groupItems= new Vector<MovementDataItem>();

			//GpsPosition startPos = null;
			//String startDate = null;
			//GpsPosition endPos = null;

			//double TotalLat = 0;
			//double TotalLon = 0;

			writer.write("$data << EOD\n");

			for (MovementDataItem item : movementData){	
				//System.out.println("duration "+item.duration);
				if (item.duration >= minTime && item.duration <= maxTime &&
						group_total_duration <= max_group_duration){

					group_sample_count++;
					//System.out.println("QQ "+item);

					if (group_sample_count > skip_first_n) {
						group_total_duration += item.duration;
						group_total_distance += item.distance();
						//group_total_speed += item.speed();
						//TotalLat += (item.getStart().lat+item.getEnd().lat)/2;
						//TotalLon += (item.getStart().lon+item.getEnd().lon)/2;
						groupItems.add(item);

						if (group_start_time_secs == 0){
							String[] tp = item.getStartTime().split(":");
							group_start_time_secs = Integer.parseInt(tp[0])*3600
									+ Integer.parseInt(tp[1])*60 
									+Integer.parseInt(tp[2]);
							sample_start_group = sample_n;
							//startPos = item.getStart();
							//startDate = item.getStartDate();
							//TotalLat = (item.getStart().lat+item.getEnd().lat)/2;
							//TotalLon = (item.getStart().lon+item.getEnd().lon)/2;
							groupItems.clear();
						}
					}
				} else { //group finished
					//System.out.println("group: "+sample_group+ " n: "+group_sample_count+" dist: "+group_total_distance+ " duration: "+group_total_duration);

					if (group_total_distance/group_total_duration < max_speed && //limit max speed on graph
							(group_sample_count >= min_samples_for_average)){ //enough samples in average
						// write ave data
						// histogram style needs vectors
						System.out.println("group: "+sample_group+ " n: "+group_sample_count+" dist: "+group_total_distance+ " duration: "+group_total_duration+ " ave speed: "+group_total_distance/group_total_duration);

						writer.write(sample_group+", "); // x
						writer.write("0, "); // y
						writer.write("0, "); // delta x
						writer.write(group_total_distance/group_total_duration+", "); //delta y

						writer.write(group_sample_count+", ");
						writer.write((int)(group_start_time_secs/3600)+", ");		
						writer.write(sample_start_group+", ");

						writer.write( MovementDataItem.category(group_total_distance/group_total_duration, beh)+"\n");
						//for 'palette variable ' truncate at one hour max
						/*if (item.duration < 3600) {					
							writer.write((item.duration)+"\n");
						} else {
							writer.write(3600+"\n");
						}*/
						//System.out.println("zzz "+(group_sample_count-skip_first_n)+"  "+TotalSamples);
						//GpsPosition avepos = new GpsPosition(TotalLat/(group_sample_count-skip_first_n), 
						//		TotalLon/(group_sample_count-skip_first_n));

						/* dont delete
						writerdata.write(String.format("%4d",sample_group)+ 
								", "+String.format("%4d",group_sample_count)+
								", "+String.format("%9.2f", group_total_distance)+ 
								", "+String.format("%4d", (int)group_total_duration)+ 
								", "+String.format("%9.3f",group_total_distance/group_total_duration)+
								", "+startPos+
								", "+avepos+
								", "+item.end+
								", "+String.format("%9s", startDate)+
								" , "+ String.format("%3d", (int)(group_start_time_secs/3600))+
								" , "+ MovementDataItem.category(group_total_distance/group_total_duration)+
								"\n"
								);	
						 */
						for (MovementDataItem itemg : groupItems){
							writerdata.write(
									itemg+
									" , "+ MovementDataItem.category(group_total_distance/group_total_duration, beh)+
									"\n"
									);	
						}

						sample_group++;
					}

					group_start_time_secs = 0;

					// start a new group with the current item
					if ((item.duration >= minTime) && (item.duration <= maxTime) && (group_total_duration > max_group_duration)){
						//System.out.println("new group");
						group_sample_count = skip_first_n; //don't skip
						group_total_duration = item.duration;
						group_total_distance = item.distance();
						//group_total_speed = item.speed();
						//TotalLat = (item.getStart().lat+item.getEnd().lat)/2;
						//TotalLon = (item.getStart().lon+item.getEnd().lon)/2;					

						String[] tp = item.getStartTime().split(":");
						group_start_time_secs = Integer.parseInt(tp[0])*3600
								+ Integer.parseInt(tp[1])*60 
								+Integer.parseInt(tp[2]);
						sample_start_group = sample_n;
						groupItems.clear();
						groupItems.add(item);

					} else {
						//System.out.println("clear group");
						group_sample_count = 0;
						group_total_duration = 0;

						group_total_distance = 0;
						//group_total_speed = 0;
					}

				}

				// next sample
				sample_n++;				
			}

			/*
			writer.write("\n\n"); // new dataset

			sample_n = 0;
			for (MovementDataItem item : movementData){	

				if (sample_n >firstSample && sample_n <lastSample){

					//x value
					writer.write(sample_n+", ");	

					// y value, prevent 0's from being plotted with NaN
					if (item.average_speed != 0)
						writer.write(item.average_speed+", ");
					else 
						writer.write("NaN\n");

					//for 'palette variable ' truncate at one hour max
					if (item.duration < 3600) {
						writer.write((item.duration)+"\n");
					} else {
						writer.write(3600+"\n");
					}
				}

				// next sample
				sample_n++;				
			}
			 */
			writer.write("EOD\n");

			// set up palette colours focussed on the speed categories 
			writer.write("set palette defined (0 \"black\", 6 \"blue\", 12 \"yellow\", 18 \"red\", 23 \"black\")\n");
			if (histogram_style){
				//writer.write("plot '$data' using 1:2:3:4:5 index 0 with vectors nohead linecolor palette z \n"); //palette colours		
				//writer.write("plot '-' using 1:2:3 index 1 with lines linecolor palette z\n"); //palette colours
				writer.write("plot '$data' using 1:2:3:4:6 index 0 with vectors nohead linewidth 2.5 linecolor palette z "
						+ "title \"sample period "+minTime+"-"+maxTime+"s\" ,"+
						//"'$data' using 1:2:3 index 1 with lines linewidth 2 linecolor palette z\n"); //palette colours
						//"'$data' using 1:2 index 1 with lines linewidth 2 linecolor \"orange\" title \"average speed ("+averaging_period+"sec)\""+
						"\n"); 
			} else {
				writer.write("plot '$data' using 1:4:6 index 0 with lines linecolor palette z,"+ //palette colours
						"'$data' using 1:2 index 1 with lines linewidth 2 linecolor \"orange\" \n"); 
			}

			writerdata.close();
			writer.close();

		} catch (IOException e) {
			System.out.println("File write error! - "+ fileName);
		}	
	}


	/**
	 * 
	 * @param suffix
	 * @param schedules
	 * @param averaged - use raw or averaged values
	 */
	public void saveMovementStats(String suffix, SchedulePeriod sp, boolean averaged, Beferrer tdp){
		String nameComposite = fileName+"_"+suffix+".dat";
		System.out.println("Creating movement stats data file"+nameComposite);

		FileOutputStream filewriter = null;
		try {
			filewriter = new FileOutputStream(nameComposite);
		} catch (FileNotFoundException e) {
			System.out.println("File write error! - "+ nameComposite);
			return;
		}

		try {
			OutputStreamWriter writer = new OutputStreamWriter(filewriter,"UTF8");
			writer.write("Tag data file: "+nameComposite+"\n\n");

			sp.saveMovementStats(writer, averaged, tdp.behaviours);

			writer.write("\n");			

			writer.close();

		} catch (IOException e) {
			System.out.println("File write error! - "+ nameComposite);
		}	
	}

	/**
	 * 
	 * @param result
	 * @return
	 */
	public static String formatSeconds(Long result){

		return 
				String.format("%4s",result/(3600*24))+" days "+
				String.format("%02d",(result % (3600*24)) / 3600)+":"+
				String.format("%02d",result/60 % 60)+":"+
				String.format("%02d",result % 60);
	}

	/**
	 * 
	 * @param suffix

	public void saveMovementDataOLD(String suffix){
		System.out.println("Creating movement data file"+fileName+"_"+suffix);

		FileOutputStream filewriter = null;
		try {
			filewriter = new FileOutputStream(fileName+"_"+suffix);
		} catch (FileNotFoundException e) {
			System.out.println("File write error! - "+ fileName);
			return;
		}

		try {
			OutputStreamWriter writer = new OutputStreamWriter(filewriter,"UTF8");
			writer.write("Tag data file: "+fileName+"_"+suffix+"\n");
			writer.write("date, time, start coordinate (lat, lon), end coordinate (lat, lon)"
					+ ", duration (s), distance (m), velocity (m/s), average vel.,"
					+ ", elapsed time, sample_no, raw_speed_category, average_speed_category"+"\n\n");

			long time = 0;
			long sample_no = 0;
			for (MovementDataItem item : movementData){	
				//writer.write(item.toString());
				writer.write(item.print());
				writer.write(", "+String.format("%10d", time));
				writer.write(", "+String.format("%9d", sample_no));
				writer.write(", "+String.format("%-12s", item.category(false))); //- left justified			
				//writer.write(", "+String.format("%-12s", item.averaged_category(false))); //- left justified
				if (item.hasNote()) writer.write(item.getNote()+"");
				writer.write("\n");	

				sample_no++;
				time += item.duration;
			}

			writer.close();

		} catch (IOException e) {
			System.out.println("File write error! - "+ fileName);
		}	
	}	 */
	
	String fixFilename(String filename){
		//System.out.println("XXXX"+filename);
		String r1  = filename.replaceAll(" ", "_"); // spaces to underscore
		//System.out.println("YYYYY"+r1);
		String r2 = r1.replaceAll("\\.", "_"); // dots to underscore
		//System.out.println("ZZZZZ"+r2);
		return r2;
	}

	/**
	 * 
	 * @param suffix
	 */
	public void saveMovementData(String suffix, SchedulePeriod sp, Beferrer tdp){
		String nameComposite = fixFilename(fileName)+"_"+suffix+".csv";
		System.out.println("Creating movement data file"+nameComposite);

		FileOutputStream filewriter = null;
		
		try {
			filewriter = new FileOutputStream(nameComposite);
		} catch (FileNotFoundException e) {
			System.out.println("File write error! - "+ nameComposite);
			return;
		}

		try {
			OutputStreamWriter writer = new OutputStreamWriter(filewriter,"UTF8");
			writer.write("Tag data file: "+nameComposite+"\n");

			sp.tabularOutput(writer, tdp);

			writer.close();

		} catch (IOException e) {
			System.out.println("File write error! - "+ nameComposite);
		}	

		//save each individual behaviour category
		long sample_no =0;
		
		for (ActivityCategory cat : tdp.behaviours.items){
			nameComposite = fixFilename(fileName)+"_"+suffix+"_Bhr_"+cat.name+".csv";
			
			FileOutputStream filewritercat = null;
			try {
				filewritercat = new FileOutputStream(nameComposite);
			} catch (FileNotFoundException e) {
				System.out.println("File write error! - "+ nameComposite);
				return;
			}

			try {
				OutputStreamWriter writer = new OutputStreamWriter(filewritercat,"UTF8");
				writer.write("Tag data file: "+nameComposite+"\n");

				sample_no = sp.tabularOutputCategory(writer, sample_no, tdp, cat);

				writer.close();

			} catch (IOException e) {
				System.out.println("File write error! - "+ nameComposite);
			}	
		}
	}

	/**
	 * output gnuplot file with points or lines
	 * @param suffix
	 * @param schedule - samples with this elapsed time, null uses all samples
	 */
	public void GNUplotPositionWithSpeedSVG(String suffix, SchedulePeriod schedule){
		System.out.println("Creating GNUplot file"+fileName+"_"+suffix);

		FileOutputStream filewriter = null;
		try {
			filewriter = new FileOutputStream(fileName+"_"+suffix);
		} catch (FileNotFoundException e) {
			System.out.println("File write error! - "+ fileName);
			return;
		}

		try {
			OutputStreamWriter writer = new OutputStreamWriter(filewriter,"UTF8");

			writer.write("set terminal svg noenhanced linewidth 1.0 font \"helvetica, 10\"\n");			
			writer.write("set output \""+fileName+"_"+suffix+".svg\"\n");

			writer.write("set autoscale\n");
			//writer.write("set xrange [52.24:52.28]\n");

			writer.write("set title \"Tag data (behaviour identification experiment nns)"+fileName+"_"+suffix+"\"\n");

			writer.write("set xlabel \"longitude\"\n");
			writer.write("show xlabel\n");
			writer.write("set ylabel \"latitide\"\n");
			writer.write("show ylabel\n");
			writer.write("set cblabel \"speed m/s\"\n");


			//main group
			writer.write("set xrange [52.25:52.26]\n");

			//startup group
			//writer.write("set xrange [52.395:52.4]\n");

			//points
			//writer.write("plot '-' w p ls 1\n");

			//writer.write("plot '-' with vectors ls 1\n");

			// colours according to last column
			//writer.write("plot '-' with vectors filled linecolor palette \n");
			//writer.write("plot '-' with vectors nohead linecolor palette \n");

			//writer.write("plot '-' with vectors nohead linecolor variable \n"); //line colours

			// set up palette colours focussed on the speed categories 
			writer.write("set palette defined (0 \"black\", 0.02 \"green\", 0.33 \"blue\", 2 \"red\")\n");
			if (schedule != null)
				writer.write("plot '-' with vectors nohead linecolor palette z title \"movements at sample rate "+schedule.toStringRange()+"\"\n"); //palette colours
			else
				writer.write("plot '-' with vectors nohead linecolor palette z title \"all movements\"\n"); //palette colours

			for (MovementDataItem item : movementData){	
				//System.out.println(item);
				if (schedule == null || schedule.in(item.duration)) {
					writer.write(item.getStart().lat+", ");
					writer.write(item.getStart().lon+", ");

					writer.write(item.getEnd().lat-item.getStart().lat+", ");
					writer.write(item.getEnd().lon-item.getStart().lon+", ");

					//for 'palette variable '
					if (item.speed() < 2) {
						//if (item.speed() < 0.5)
						writer.write((item.speed())+" ");
					} else {
						writer.write(2+" ");
					}

					writer.write("\n");
				} 

			}

			writer.close();

		} catch (IOException e) {
			System.out.println("File write error! - "+ fileName);
		}	
	}

	/**
	 * output gnuplot graph of time vs speed
	 * @param suffix

	public void GNUplotSpeedByTime(String suffix){
		System.out.println("Creating GNUplot file"+fileName+"_"+suffix);
		FileOutputStream filewriter = null;
		try {
			filewriter = new FileOutputStream(fileName+"_"+suffix);
		} catch (FileNotFoundException e) {
			System.out.println("File write error! - "+ fileName);
			return;
		}

		try {
			OutputStreamWriter writer = new OutputStreamWriter(filewriter,"UTF8");
			writer.write("set terminal postscript noenhanced colour linewidth 1.0 \"helvetica\" 10\n");
			writer.write("set output \""+fileName+"_"+suffix+".eps\"\n");

			writer.write("set autoscale\n");
			//writer.write("set xrange [52.24:52.28]\n");

			writer.write("set title \"Tag data (behaviour identification experiment nns)"+fileName+"_"+suffix+"\"\n");

			//writer.write("set xlabel \"time\"\n");
			writer.write("set xlabel \"sample number\"\n");

			writer.write("show xlabel\n");
			writer.write("set ylabel \"speed\"\n");
			writer.write("show ylabel\n");
			writer.write("set cblabel \"time between samples (s)\"\n");


			//speed axis
			writer.write("set yrange [-0.5:3]\n");

			//startup group
			//writer.write("set xrange [52.395:52.4]\n");

			//points
			//writer.write("plot '-' w p ls 1\n");

			//writer.write("plot '-' with vectors ls 1\n");

			// colours according to last column
			//writer.write("plot '-' with vectors filled linecolor palette \n");
			//writer.write("plot '-' with vectors nohead linecolor palette \n");

			//writer.write("plot '-' with vectors nohead linecolor variable \n"); //line colours

			// set up palette colours focussed on the speed categories 
			writer.write("set palette defined (0 \"dark-green\", 60 \"green\", 300 \"blue\", 3600 \"red\")\n");
			//writer.write("plot '-' with vectors nohead linecolor palette z\n"); //palette colours
			writer.write("plot '-' with lines linecolor palette z\n"); //palette colours


			long time = 0;
			for (MovementDataItem item : movementData){	
				//if (time > 300000 && time < 2300000){
				//if (time > 470000 && time < 580000){
				//if (time > 490000 && time < 500000){
				//if (time > 498200 && time < 498900){

				//if (time > 491000 && time < 491000+350){
				//if (time > 498900 && time < 520000){
				//if (time > 580000 && time < 900000){
				//if (time >4000 && time <6000){
				//if (time >6000 && time <8000){
				//if (time >8000 && time <10000){
				//if (time >10000 && time <12000){
				//if (time >12000 && time <14000){
				//if (time >50000 && time <52000){
				if (time >52000 && time <54000){
					//System.out.println(item);


					writer.write(time+", ");
					//if (item.speed()!=0) 
					writer.write(item.speed()+", ");
					//else 
					//writer.write("-1, ");
					//writer.write(item.duration+", ");
					//writer.write("0, ");

					//writer.write(time+", ");
					//writer.write("0, ");
					//writer.write("0, ");
					//writer.write(item.speed()+", ");			 

					//for 'palette variable '
					if (item.duration < 3600) {
						//if (item.speed() < 0.5)
						writer.write((item.duration)+"\n");
					} else {
						writer.write(3600+"\n");
					}

					//writer.write("\n");
				}

				// next point start
				//time += item.duration;
				time++;
				System.out.println(time);
			}

			writer.close();

		} catch (IOException e) {
			System.out.println("File write error! - "+ fileName);
		}	
	}	 */
}