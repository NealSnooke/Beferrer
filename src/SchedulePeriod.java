import java.util.Vector;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * First created Neal Snooke 01/03/2019
 * 
 * simple public data container
 */

public class SchedulePeriod {
	public long min;
	public long max;

	// each of the sets of data related to this schedule
	public Vector<MovementGroup> movementData;

	SchedulePeriod (long min, long max, Vector<MovementDataItem>allMovementData ){
		this.min = min;
		this.max = max;
		movementData = new Vector<MovementGroup>();

		buildGroups(allMovementData);
	}

	/**
	 * Split the movement data into groups based on the timeslots for this schedule
	 * 
	 * @param allMovementData
	 */
	public void buildGroups(Vector<MovementDataItem>allMovementData){
		System.out.println("----------------------------------------------");
		System.out.println("Build sample groups");
			
		int groupn = 0;
		
		//MovementGroup newGroup = new MovementGroup();
		MovementGroup newGroup = null;
		
		for (MovementDataItem item : allMovementData){	
			//System.out.println(item);
			if (in(item.duration)) {

				//TODO for longer time periods then create a movement to a suitable point in the finer grained data

				if (newGroup == null) {
					newGroup = new MovementGroup();
					groupn++;
					
					movementData.add(newGroup);
					System.out.println("New group ("+groupn
							+ ") starts at "+item.toStringShort());
				}

				newGroup.addMovement(item);
				
				if (Beferrer.debug)
					System.out.println("Added: "+item);
				
				//System.out.println("Add item: "+item);
			} else {
				// indicate new group
				if (newGroup != null){
					if (Beferrer.debug)
						System.out.println("End group at:"+item.toStringShort());
				}
				newGroup = null;
			}
		}
		
		System.out.println("Total of "+movementData.size()+" movement groups found");
	}

	/**
	 * 
	 * @param outputStream
	 * @throws IOException 
	 */
	public void tabularOutput(OutputStreamWriter writer, Beferrer tdp) throws IOException{
		writer.write("date, time, start lat, start lon, end lat, end lon"
				+ ", duration(s), distance(m), speed(m/s), average speed over "+tdp.movementAverage+"s (m/s)"
				+ ", movement group elapsed time(s), sample_no, speed_category, average_speed_category"+"\n");

		long sample_no = 0;
		for (MovementGroup group : movementData){
			writer.write("\n");
			sample_no = group.tabularOutput(writer, sample_no, tdp);
		}

	}
	
	/**
	 * 
	 * @param outputStream
	 * @throws IOException 
	 */
	public long tabularOutputCategory(OutputStreamWriter writer, long sample_no,
			Beferrer tdp, ActivityCategory cat) throws IOException{
		writer.write("date, time, start lat, start lon, end lat, end lon"
				+ ", duration(s), distance(m), speed(m/s), average speed over "+tdp.movementAverage+"s (m/s)"
				+ ", movement group elapsed time(s), sample_no, behaviour_category"+"\n");

		for (MovementGroup group : movementData){
			//writer.write("\n");
			sample_no = group.tabularOutputCategory(writer, sample_no, tdp, cat);
		}
		
		return sample_no;
	}

	/**
	 * 
	 * @param duration
	 * @return
	 */
	public boolean in(long duration){
		return (duration >= min) && (duration < max);
	}

	/**
	 * output gnuplot file with points or lines
	 * @param suffix
	 * @param schedule - samples with this elapsed time, null uses all samples
	 * @throws IOException  
	 */
	public void GNUplotPositionWithSpeed1(OutputStreamWriter writer, 
			GpsPosition northWestBoundary, GpsPosition southEastBoundary) throws IOException {

		if (northWestBoundary != null && southEastBoundary != null){
			writer.write("set xrange ["+northWestBoundary.lon+":"+southEastBoundary.lon+"]\n");
			writer.write("set yrange ["+northWestBoundary.lat+":"+southEastBoundary.lat+"]\n");
		} else {
			writer.write("set autoscale\n");
		}

		writer.write("set xlabel \"longitude\"\n");
		writer.write("show xlabel\n");
		writer.write("set ylabel \"latitide\"\n");
		writer.write("show ylabel\n");
		writer.write("set cblabel \"speed m/s\"\n");

		

		// set up palette colours focussed on the speed categories 
		writer.write("set palette defined (0 \"black\", 0.02 \"green\", 0.33 \"blue\", 2 \"red\", 30 \"yellow\")\n");
		writer.write("plot '-' with vectors nohead linecolor palette z title \"movements at sample rate "+this.toStringRange()+"\"\n"); //palette colours

		// output each group
		for (MovementGroup item : movementData){	
			item.GNUplotPositionWithSpeed1(writer);		
		}
	}

	/**
	 * output gnuplot file with points or lines
	 * @param suffix
	 * @param schedule - samples with this elapsed time, null uses all samples
	 * @throws IOException  
	 */
	public void GNUplotPositionWithSample(OutputStreamWriter writer, 
			GpsPosition northWestBoundary, GpsPosition southEastBoundary) throws IOException {

		if (northWestBoundary != null && southEastBoundary != null){
			writer.write("set xrange ["+northWestBoundary.lon+":"+southEastBoundary.lon+"]\n");
			writer.write("set yrange ["+northWestBoundary.lat+":"+southEastBoundary.lat+"]\n");
		} else {
			writer.write("set autoscale\n");
		}
		
		writer.write("set xlabel \"longitude\"\n");
		writer.write("show xlabel\n");
		writer.write("set ylabel \"latitide\"\n");
		writer.write("show ylabel\n");
		writer.write("set cblabel \"sample (time sequence) \"\n");

		//main group
		//FIXME
		//writer.write("set xrange [52.25:52.26]\n");

		long samples = 0;
		for (MovementGroup item : movementData){	
			samples += item.movementCount();	
		}

		// set up palette colours focussed on the speed categories 
		//writer.write("set palette defined (0 \"black\", 1 \"green\", 2 \"blue\", 3 \"red\", 4 \"yellow\")\n");
		writer.write("set palette defined (0 \"black\", "+
				(int)(samples*.33)+" \"blue\", "+
				(int)(samples*.66)+" \"red\", "+
				samples+" \"yellow\")"+
				"\n");
		writer.write("plot '-' with vectors nohead linecolor palette z title \"movements at sample rate "+this.toStringRange()+"\"\n"); //palette colours

		samples = 0;
		// output each group
		for (MovementGroup item : movementData){	
			samples = item.GNUplotPositionWithSample(writer, samples);		
		}
	}

	/**
	 * output gnuplot file with points or lines
	 * @param suffix
	 * @param schedule - samples with this elapsed time, null uses all samples
	 * @throws IOException  
	 */
	public void GNUplotPositionGroupsWithSpeed(OutputStreamWriter writer, int maxspeedrange) throws IOException {

		writer.write("set xlabel \"time in group(s)\"\n");
		writer.write("show xlabel\n");
		writer.write("set ylabel \"speed\"\n");
		writer.write("show ylabel\n");
		writer.write("set cblabel \"dataset sample (temporal order) \"\n");

		//main group
		//FIXME
		writer.write("set yrange [0:"+maxspeedrange+"]\n");

		long samples = 0;
		for (MovementGroup item : movementData){	
			samples += item.movementCount();	
		}

		// set up palette colours focussed on the speed categories 
		//writer.write("set palette defined (0 \"black\", 1 \"green\", 2 \"blue\", 3 \"red\", 4 \"yellow\")\n");
		writer.write("set palette defined (0 \"black\", "+
				(int)(samples*.33)+" \"blue\", "+
				(int)(samples*.66)+" \"red\", "+
				samples+" \"yellow\")"+
				"\n");
		writer.write("plot '-' with vectors nohead linecolor palette z title \"groups: movements at sample rate "+this.toStringRange()+"\"\n"); //palette colours

		samples = 0;
		// output each group
		for (MovementGroup item : movementData){	
			
			samples = item.GNUplotPositionGroupsWithSpeed(writer, samples);		
		}
	}

	/**
	 * 
	 * @param suffix
	 * @param schedules
	 * @param averaged - use raw or averaged values
	 * @throws IOException 
	 */
	public void saveMovementStats(OutputStreamWriter writer, boolean averaged, Behaviour beh) throws IOException{

		long stationary_time = 0;
		long foraging_time = 0;
		long moving_time = 0;
		//long gps_error_time = 0;

		double stationary_dist = 0;
		double foraging_dist = 0;
		double moving_dist = 0;
		//double gps_error_dist = 0;

		int stationary_n = 0;
		int foraging_n = 0;
		int moving_n = 0;
		int gps_error_n = 0;
		int n_samples = 0;

		for (MovementGroup item : movementData){	
			stationary_time += item.getCategoryTime("Stationary", averaged, beh);
			stationary_dist += item.getCategoryDist("Stationary", averaged, beh);
			stationary_n += item.getCategorySamples("Stationary", averaged, beh);

			foraging_time += item.getCategoryTime("Foraging", averaged, beh);
			foraging_dist += item.getCategoryDist("Foraging", averaged, beh);
			foraging_n += item.getCategorySamples("Foraging", averaged, beh);

			moving_time += item.getCategoryTime("Moving", averaged, beh);
			moving_dist += item.getCategoryDist("Moving", averaged, beh);
			moving_n += item.getCategorySamples("Moving", averaged, beh);

			//gps_error_time += item.getCategoryTime("GPS ERROR", averaged, beh);
			//gps_error_dist += item.getCategoryDist("GPS ERROR", averaged, beh);
			//gps_error_n += item.getCategorySamples("GPS ERROR", averaged, beh);

			//n_samples += item.getCategorySamples("GPS ERROR", averaged);
			n_samples += stationary_n+foraging_n+moving_n+gps_error_n;
		}

		writer.write("Schedule period: "+toStringRange()+" seconds ("+n_samples+" movements)\n");
		writer.write("Stationary: "+Beferrer.formatSeconds(stationary_time)+" "+
				String.format("%9.2f", stationary_dist)+"m ("+stationary_n+")\n");
		writer.write("Foraging:   "+Beferrer.formatSeconds(foraging_time)+" "+
				String.format("%9.2f", foraging_dist)+"m ("+foraging_n+")\n");
		writer.write("Moving:     "+Beferrer.formatSeconds(moving_time)+" "+
				String.format("%9.2f", moving_dist)+"m ("+moving_n+")\n");
		//writer.write("GPS ERROR:  "+formatSeconds(gps_error_time)+"\n\n");
		writer.write("\n");


		//writer.write("Total Stationary: "+formatSeconds(total_stationary_time)+"\n");
		//writer.write("Total Foraging:   "+formatSeconds(total_foraging_time)+"\n");
		//writer.write("Total Moving:     "+formatSeconds(total_moving_time)+"\n");
		//writer.write("Total GPS ERROR:  "+formatSeconds(total_gps_error_time)+"\n");
	}

	/**
	 * 
	 * @param maxSpeed
	 * @param combineStationary
	 */
	public void cleanMovementData(double maxSpeed){
		if (maxSpeed > 0){
			System.out.println("----------------------------------------------");
			System.out.println("Clean movement data - remove remove p2 where p1->p2>"
					+ maxSpeed+"m/s and p2->p2>"
					+ maxSpeed+"m/s but p1->p3<"
					+ maxSpeed+"m/s");
			

			for (MovementGroup item : movementData){	
				item.cleanMovementData(maxSpeed);
			}
		}
	}

	/**
	 * 
	 */
	public void removeStartEndStationaryValues(){
		System.out.println("----------------------------------------------");
		System.out.println("Remove stationary start/end values from groups");
		
		int groupn = 0;
		for (MovementGroup item : movementData){	
			int count = item.removeStartEndStationaryValues();
			
			if (count != 0)
				System.out.println("Removed "+count+" items from group "+groupn);
			groupn++;
		}
	}
	
	/**
	 * 
	 */
	public void selectDates(int dd1, int mm1, int yy1, int dd2, int mm2, int yy2,
			boolean dateRangeStart, boolean dateRangeEnd){
		System.out.println("----------------------------------------------");
		System.out.println("Select required dates");
		
		int groupn = 0;
		for (MovementGroup item : movementData){	
			int count = item.selectDates(dd1, mm1, yy1, dd2, mm2, yy2, dateRangeStart, dateRangeEnd);
			
			if (count != 0)
				System.out.println("Removed "+count+" items from group "+groupn);
			groupn++;
		}
		
		//remove any empty groups
		removeEmptyGroups();
	}
	
	/**
	 * 
	 */
	public void selectTimeOfDay(int hh1, int mm1, int ss1, int hh2, int mm2, int ss2,
			boolean timeRangeStart, boolean timeRangeEnd){
		System.out.println("----------------------------------------------");
		System.out.println("Select required time of day");
		
		int groupn = 0;
		for (MovementGroup item : movementData){	
			int count = item.selectTimes(hh1, mm1, ss1, hh2, mm2, ss2, timeRangeStart, timeRangeEnd);
			
			if (count != 0)
				System.out.println("Removed "+count+" items from group "+groupn);
			groupn++;
		}
		
		//remove any empty groups
		removeEmptyGroups();
	}

	/**
	 * 
	 */
	public void removeStartnValues(int n){
		if (n != 0){
			System.out.println("----------------------------------------------");
			System.out.println("Remove first "+n+" values from each movement group sequence");

			int groupn = 0;
			for (MovementGroup item : movementData){
				if (Beferrer.debug) 
					System.out.println("Group: "+groupn);

				item.removeStartnValues(n);

				groupn++;
			}

			//remove any empty groups
			removeEmptyGroups();
		}
	}
	
	/**
	 * 
	 */
	public void ListMovementGroups(){
		
		System.out.println("----------------------------------------------");
		System.out.println("List movement groups");
		
		int counter = 0;
		for (MovementGroup item : movementData){
			System.out.println("Group "+ counter);
			System.out.println(item);	
			
			counter++;
		}
	}

	/**
	 * 
	 */
	public void removeEndnValues(int n){
		if (n != 0){
			System.out.println("----------------------------------------------");
			System.out.println("Remove last "+n+" values from each movement group sequence");

			int groupn = 0;
			for (MovementGroup item : movementData){
				if (Beferrer.debug) 
					System.out.println("Group: "+groupn);

				item.removeEndnValues(n);

				groupn++;
			}

			//remove any empty groups
			removeEmptyGroups();
		}
	}

	/**
	 * 
	 * @param maxSpeed
	 */
	public void truncateToGPSErrorInGroup(double maxSpeed){
		if (maxSpeed >0 ){
			System.out.println("----------------------------------------------");
			System.out.println("Truncate start and end of group with move above "+maxSpeed+" m/s");
			//System.out.println("Truncate to "+maxSpeed+"m/s at extremeties of group");


			for (MovementGroup item : movementData){
				int count = 0;
				while (item.truncateToGPSErrorInGroup(maxSpeed)){count++;};
				
				if (count>0)
					System.out.println("Removed "+count+" sets of items from group");
			}

			//remove any empty groups
			removeEmptyGroups();
		}
	}

	/**
	 */
	private void removeEmptyGroups(){
		Vector<MovementGroup> toRemove = new Vector<MovementGroup>();

		for (MovementGroup item : movementData){
			if (item.movementCount() == 0){
				toRemove.add(item);
				if (Beferrer.debug) System.out.println("Removing Empty group");
			}
		}
		movementData.removeAll(toRemove);
	}

	/**
	 * 
	 */
	public void CombineStationaryValues(){
		System.out.println("----------------------------------------------");
		System.out.println("Movement combine stationary values");
		
		int c = 0;
		for (MovementGroup item : movementData){
			
			int count = item.CombineStationaryValues();
			if (count > 0)
				System.out.println("Group "+c+" combined "+count+" items");
			c++;
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
	public void AverageGPSResolution(double minDist){
		if (minDist != 0){
			System.out.println("----------------------------------------------");
			System.out.println("Average GPS positions for movement less than "+ minDist+"m");
			
			int groupn = 0;
			for (MovementGroup item : movementData){	
				if (Beferrer.debug)
					System.out.println("Group "+groupn);
				groupn++;
				
				item.AverageGPSResolution(minDist);
			}
		}
	}

	/**
	 * 
	 * @param time_period
	 */
	public void averageMovementdata(Beferrer tdp){
		if (tdp.movementAverage != 0){
			System.out.println("----------------------------------------------");
			System.out.println("Create rolling average movement data over "+tdp.movementAverage+"s");
			
			for (MovementGroup item : movementData){
				item.averageMovementdata(tdp);
			}
		}
	}


	/**
	 * 
	 */
	public String toString(){

		return "Sample group range "+min+"s -> "+max+"s";
	}

	/**
	 * 
	 */
	public String toStringRange(){
		if (max == Integer.MAX_VALUE)
			return "";
		else
			return min+"-"+max+"s";
	}
}
