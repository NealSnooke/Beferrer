/**
 * First created Neal Snooke 02/04/2019
 * 
 * represents a group of consecutive movements
 */

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Vector;

public class MovementGroup {

	private Vector<MovementDataItem> movementData;

	// the schedule this group was generated for
	//private SchedulePeriod schedule;
	/**
	 * 
	 * @return
	 */
	public MovementGroup(){
		movementData = new Vector<MovementDataItem>();	
	}

	/**
	 * 
	 * @param item
	 * @return
	 */
	public void addMovement(MovementDataItem item){
		movementData.add(item);
	}

	/**
	 * 
	 * @return
	 */
	public int movementCount(){
		return movementData.size();
	}

	/**
	 * 
	 * @param writer
	 */
	public void GNUplotPositionWithSpeed1(OutputStreamWriter writer) throws IOException {
		//System.out.println(item);
		for (MovementDataItem item : movementData){

			writer.write(item.getStart().lon+", ");
			writer.write(item.getStart().lat+", ");

			writer.write(item.getEnd().lon-item.getStart().lon+", ");
			writer.write(item.getEnd().lat-item.getStart().lat+", ");

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

	/**
	 * 
	 * @param writer
	 */
	public long GNUplotPositionWithSample(OutputStreamWriter writer, long sample) throws IOException {
		//System.out.println(item);
		for (MovementDataItem item : movementData){

			writer.write(item.getStart().lon+", ");
			writer.write(item.getStart().lat+", ");

			writer.write(item.getEnd().lon-item.getStart().lon+", ");
			writer.write(item.getEnd().lat-item.getStart().lat+", ");

			//for 'palette variable '
			writer.write(sample+" ");
			sample++;

			writer.write("\n");
		}

		return sample;
	}

	/**
	 * 
	 * @param writer
	 */
	public long GNUplotPositionGroupsWithSpeed(OutputStreamWriter writer, long sample) throws IOException {
		//System.out.println(item);
		MovementDataItem lastitem = null;

		int elapsedTime = 0;
		for (MovementDataItem item : movementData){
			//x
			if (lastitem != null){
				
				writer.write(elapsedTime+", ");

				//y
				writer.write(lastitem.speed()+", ");

				//delta x
				writer.write(item.duration+", ");

				//delta y
				writer.write(item.speed()-lastitem.speed()+", ");

				//for 'palette variable '
				writer.write(sample+" ");
				writer.write("\n");	
			}

			sample++;
			elapsedTime += item.duration;
			lastitem = item;
		}

		return sample;
	}

	/**
	 * this will remove entries that appear to have gps glitches
	 * so two movements with high speed should be combined into a single entry with 
	 * proper speed between those points.
	 */
	public void cleanMovementData(double maxSpeed){
		
		Vector<MovementDataItem> newMovementData = new Vector<MovementDataItem>();

		MovementDataItem previous = null;

		for (MovementDataItem current : movementData){			
			if (previous != null && 
					previous.speed() > maxSpeed && current.speed() > maxSpeed){

				// combine two movements if speed of two consecutive movements is excessive
				MovementDataItem combined = new MovementDataItem(
						previous.getStart(), current.getEnd(), previous.duration+current.duration,
						previous.getStartDate(), previous.getStartTime(), previous.originalMovementNumber);

				combined.addNote("Combined two movements, both >"+maxSpeed+" m/s");

				if (combined.speed() < maxSpeed) {
					newMovementData.add(combined);
					System.out.println("Combined two movements movements (GPS glitch): "+combined+"\n"+previous+"\n"+current);
				} else {
					newMovementData.add(previous);		//include anyway regardless of speed					
				}

			} else {
				if (previous != null)
					newMovementData.add(previous);		//include anyway regardless of speed	
			}

			previous = current;
		}

		newMovementData.add(movementData.lastElement());

		movementData = newMovementData;
	}

	/**
	 * this will remove entries that appear to have gps glitches
	 * so two movements with high speed should be combined into a single entry with 
	 * proper speed between those points.
	 * 
	 * a single movement with excessive speed (in m/s) will be removed
	 * setting combineStationary will aggregate multiple stationary values
	 */
	public int CombineStationaryValues(){
		
		Vector<MovementDataItem> newMovementData = new Vector<MovementDataItem>();

		int stationary_time = 0; //number of seconds no movement has occurred for
		int stationary_samples = 0; //number of samples no movement has occurred for
		int count = 0; 

		for (MovementDataItem current : movementData){			
			// if previous stationary and current stationary then
			// add the current time duration to the previous and move on without replacing previous
			if (current.isStationary()){
				stationary_time += current.duration;
				stationary_samples++;
			} else {
				// not stationary but previously 'locked up gps' add in previous stationary time to current move
				if (stationary_time > 0){
					current.duration += stationary_time; 
					if (Beferrer.debug)
						System.out.println("Added stationary time of "+stationary_time
							+"s for "+stationary_samples+" samples: "+current);
					count++;

					current.addNote("combined "+(stationary_samples)+" stationary values"); 
				}

				newMovementData.add(current);	
				// if stationary for 1800s and current move speed is > 10m\s
				// assume that previous stationary moves were not real
				//if (current.speed() > 10 && stationary_time > 1800){
				//

				stationary_time = 0;
				stationary_samples = 0;
				//	}

			}
		}

		movementData = newMovementData;
		
		return count;
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

		// index of each movement
		for (int i=0; i<movementData.size(); i++ ){

			GpsPosition pointi = movementData.get(i).getStart();

			int n = 0;
			double distance = 0; //direct line distance to points before and after this point

			// totals for lat and lon for average
			double latTotal = pointi.lat;
			double lonTotal = pointi.lon;	

			while (distance < minDist && (i-n) >= 0 && (i+n-1) < movementData.size()){
				n++;
				
				if ((i-n) >= 0 && (i+n-1) < movementData.size()){ //edges of dataset
					double distprev = pointi.distance(movementData.get(i-n).getStart()); //previous segment
					double distnext = pointi.distance(movementData.get(i+n-1).getEnd()); //next point, or segment 

					distance = distprev+distnext;
					// sum the position coordinates
					latTotal = latTotal+movementData.get(i+n-1).getEnd().lat;
					lonTotal = lonTotal+movementData.get(i+n-1).getEnd().lon;
					latTotal = latTotal+movementData.get(i-n).getStart().lat;
					lonTotal = lonTotal+movementData.get(i-n).getStart().lon;
					//System.out.println(i+":"+n+":"+distance);
				}
				
			}

			if (distance >= minDist) {
				if (Beferrer.debug)
					System.out.println("Point "+i+" Average point positions for "+n+" points. Distance "+ String.format("%.3f", distance));

				// create average
				GpsPosition newpos = new GpsPosition(latTotal/(n*2+1), lonTotal/(n*2+1));

				// create new coordinates
				movementData.get(i).averagedStart = newpos;
				if (i-1 >= 0 )
					movementData.get(i-1).averagedEnd = newpos;

				if (Beferrer.debug)
					System.out.println("Point "+i+" Original "+pointi+" New "+movementData.get(i).averagedStart);
			} else {
				if (Beferrer.debug)
					System.out.println("Point "+i+" not enough distance ("+String.format("%.3f", distance)
					+") between points ("+n+") in group");
			}
		}

		// update positions
		for (MovementDataItem item : movementData){		
			item.moveCoordinate();
		}
	}

	/**
	 * 
	 * @param n
	 */
	public void removeStartnValues(int n){
		for (int i=0; i<n; i++){
			if (!movementData.isEmpty()){
				if (Beferrer.debug)
					System.out.println("Removed start item: "+movementData.firstElement());
				movementData.removeElementAt(0);	
			}
		}
	}

	/**
	 * 
	 * @param n
	 */
	public void removeEndnValues(int n){	
		for (int i=0; i<n; i++){
			if (!movementData.isEmpty()){
				if (Beferrer.debug)
					System.out.println("Removed end item: "+movementData.lastElement());
				movementData.removeElementAt(movementData.size()-1);	
			}
		}
	}

	/**
	 * 
	 */
	public int removeStartEndStationaryValues(){
		//System.out.println("Remove Start End Stationary Values");
		int count = 0;

		boolean found = true;	
		while (found && !movementData.isEmpty()){
			if (movementData.firstElement().isStationary()){
				if (Beferrer.debug) 
					System.out.println("Removed stationary start item: "+movementData.firstElement());
				movementData.removeElementAt(0);	
				count++;
			} else {
				found = false;
			}
		}

		found = true;		
		while (found && !movementData.isEmpty()){
			if (movementData.lastElement().isStationary()){
				if (Beferrer.debug) 
					System.out.println("Removed stationary end item: "+movementData.lastElement());
				movementData.removeElementAt(movementData.size()-1);
				count++;
			} else {
				found = false;
			}
		}

		return count;
	}
	
	/**
	 * 
	 */
	public int selectDates(int dd1, int mm1, int yy1, int dd2, int mm2, int yy2,
			boolean dateRangeStart, boolean dateRangeEnd){
		
		Vector<MovementDataItem> toRemove = new Vector<MovementDataItem>();

		for (MovementDataItem item : movementData){	
			String[] dt = item.getStartDate().split("/");
			
			int y = Integer.parseInt(dt[2]);
			int m = Integer.parseInt(dt[1]);
			int d = Integer.parseInt(dt[0]);
			
			//System.out.println("xxx "+dd1+" "+mm1+" "+yy1+" == "+d+" "+m+" "+y);
			//System.out.println("yyy "+dd2+" "+mm2+" "+yy2+" == "+d+" "+m+" "+y);
			if (dateRangeStart){
				if (y < yy1) { //year tool low, remove
					toRemove.add(item);
					if (Beferrer.debug) System.out.println("remove movement, date too early "+item);
				} else {
					if (y == yy1 && m < mm1) { //year equal, month too low, remove
						toRemove.add(item);
						if (Beferrer.debug) System.out.println("remove movement, date too early "+item);
					} else {
						if (y == yy1 && m == mm1 && d < dd1) { //year equal, month equal, date too low
							toRemove.add(item);
							if (Beferrer.debug) System.out.println("remove movement, date too early "+item);
						}
					}
				}
			}

			if (dateRangeEnd){
				if (y > yy2) { //year tool low, remove
					toRemove.add(item);
					if (Beferrer.debug) System.out.println("remove movement, date too late "+item);
				} else {
					if (y == yy2 && m > mm2) { //year equal, month too low, remove
						toRemove.add(item);
						if (Beferrer.debug) System.out.println("remove movement, date too late "+item);
					} else {
						if (y == yy2 && m == mm2 && d > dd2) { //year equal, month equal, date too low
							toRemove.add(item);
							if (Beferrer.debug) System.out.println("remove movement, date too late "+item);
						}
					}
				}
			}
		}

		movementData.removeAll(toRemove);

		return toRemove.size();
	}
	
	/**
	 * 
	 */
	public int selectTimes(int hh1, int mm1, int ss1, int hh2, int mm2, int ss2,
			boolean timeRangeStart, boolean timeRangeEnd){
		
		Vector<MovementDataItem> toRemove = new Vector<MovementDataItem>();

		for (MovementDataItem item : movementData){	
			String[] dt = item.getStartTime().split(":");
			
			int s = Integer.parseInt(dt[2]);
			int m = Integer.parseInt(dt[1]);
			int h = Integer.parseInt(dt[0]);
			
			int timeInSec = (s)+(m*60)+h*(60*60);
			int startTimeInSec = (ss1)+(mm1*60)+hh1*(60*60);
			int endTimeInSec = (ss2)+(mm2*60)+hh2*(60*60);
					
			//System.out.println("xxx "+hh1+" "+mm1+" "+ss1+" == "+h+" "+m+" "+s);
			//System.out.println("yyy "+hh2+" "+mm2+" "+ss2+" == "+h+" "+m+" "+s);

			if (startTimeInSec < endTimeInSec){
				if (timeInSec <= startTimeInSec || timeInSec >= endTimeInSec){
					toRemove.add(item);
					if (Beferrer.debug) 
						System.out.println("remove movement, outside time range "+item);
				}
			} else {
				if (timeInSec <= startTimeInSec && timeInSec >= endTimeInSec){
					toRemove.add(item);
					if (Beferrer.debug) 
						System.out.println("remove movement, outside (overnight) time range "+item);
				}
			}
		}

		movementData.removeAll(toRemove);

		return toRemove.size();
	}

	/**
	 * Remove points before and after any 'high speed' value in a consecutive group
	 * @param maxSpeed
	 * @return
	 */
	public boolean truncateToGPSErrorInGroup(double maxSpeed){
		

		int item_no = -1;
		int count = 0;
		
		for (MovementDataItem item : movementData){		
			if (item.speed() > maxSpeed){
				item_no = count;
			}

			count++;
		}

		if (item_no > -1) {
				
			if (item_no > movementData.size()/2) {
				//truncate end
				if (Beferrer.debug)
					System.out.println("Remove values after/inc item "+item_no+": "+movementData.elementAt(item_no));
				
				while (movementData.size() > item_no){
					System.out.println("Removed value "+movementData.elementAt(item_no));
					movementData.removeElementAt(item_no);
				}
				
				return true;
				
			} else {
				//truncate start
				if (Beferrer.debug)
					System.out.println("Remove values before/inc item "+item_no+": "+movementData.elementAt(item_no));
				
				for (int i = 0; i <= item_no; i++){
					System.out.println("Removed value "+movementData.elementAt(0));
					movementData.removeElementAt(0);
				}
				return true;
			}
		} 

		return false;
	}
	
	/**
	 * set the  .average_speed  value for each movement item
	 * 
	 * @param time_period
	 */
	public void averageMovementdata(Beferrer tdp){
		double time_period = tdp.movementAverage;
		
		for (int currentitem = 0; currentitem<movementData.size(); currentitem++){
					
			//consider each movement
			MovementDataItem sample = movementData.get(currentitem);
			boolean quit = false;
			
			if (time_period > sample.duration) {

				double tmp_duration_before = ((double)sample.duration)/2;
				double total_speed_before = sample.speed()*tmp_duration_before;
				int index = 1;

				// keep adding time and speed*time values until time period/2 before the point is reached
				while (tmp_duration_before < time_period/2 && !quit){
					if (currentitem-index >= 0){ //beginning of data
						if (movementData.get(currentitem-index).duration+tmp_duration_before <= time_period/2){
							tmp_duration_before += movementData.get(currentitem-index).duration;
							// time*speed
							total_speed_before += movementData.get(currentitem-index).duration*movementData.get(currentitem-index).speed();
							index++;
						} else {
							// add in a fraction of the bigger time period
							total_speed_before += (time_period/2-tmp_duration_before)*movementData.get(currentitem-index).speed();
							tmp_duration_before = time_period/2; // will finish loop
						}
					} else {
						quit=true; // quit
					}
				}

				double tmp_duration_after = ((double)sample.duration)/2;
				double total_speed_after = sample.speed()*tmp_duration_after;
				index = 1;
				// keep adding time and speed*time values until time period/2 after the point is reached
				while (tmp_duration_after < time_period/2 && !quit){
					if (currentitem+index < movementData.size()){ //end of data
						if (movementData.get(currentitem+index).duration+tmp_duration_after <= time_period/2){
							tmp_duration_after += movementData.get(currentitem+index).duration;
							// time*speed
							total_speed_after += movementData.get(currentitem+index).duration*movementData.get(currentitem+index).speed();
							index++;
						} else {
							// add in a fraction of the bigger time period
							total_speed_after += (time_period/2-tmp_duration_after)*movementData.get(currentitem+index).speed();
							tmp_duration_after = time_period/2; // will finish loop
						}
					} else {
						quit=true; // quit
					}
				}

				if (!quit)
					sample.average_speed = (total_speed_before+total_speed_after)/time_period;
			} else {
				//no need to average since movement is longer than time period
				sample.average_speed = sample.speed();
			}

		} //for
	}

	/**
	 * total the time for a category
	 * @param category
	 * @return
	 */
	public long getCategoryTime(String category, boolean averaged, Behaviour beh){
		long time = 0;
		
		for (MovementDataItem item : movementData){	
			if (averaged) {
				if (item.category_averaged(beh).equals(category)){
					time += item.duration;
				}
			} else {
				if (item.category(beh).equals(category)){
					time += item.duration;
				}
			}
		}
		
		return time;
	}
	
	/**
	 * total the time for a category
	 * @param category
	 * @return
	 */
	public double getCategoryDist(String category, boolean averaged, Behaviour beh){
		double dist = 0;
		
		for (MovementDataItem item : movementData){	
			if (averaged) {
				if (item.category_averaged(beh).equals(category)){
					dist += item.distance();
				}
			} else {
				if (item.category(beh).equals(category)){
					dist += item.distance();
				}
			}
		}
		
		return dist;
	}
	
	
	/**
	 * total the time for a category
	 * @param category
	 * @return
	 */
	public long getCategorySamples(String category, boolean averaged, Behaviour beh){
		int samples = 0;
		
		for (MovementDataItem item : movementData){	
			if (averaged) {
				if (item.category_averaged(beh).equals(category)){
					samples++;
				}
			} else {
				if (item.category(beh).equals(category)){
					samples++;
				}
			}
		}
		
		return samples;
	}

	/**
	 * 
	 * @param outputStream
	 * @throws IOException 
	 */
	public long tabularOutput(OutputStreamWriter writer, long sample_no, Beferrer tdp) throws IOException{

		long time = 0;

		for (MovementDataItem item : movementData){	
			//writer.write(item.toString());
			writer.write(item.print());
			writer.write(", "+String.format("%10d", time));
			writer.write(", "+String.format("%9d", item.originalMovementNumber));
			writer.write(", "+String.format("%-12s", item.category(tdp.behaviours))); //- left justified			
			writer.write(", "+String.format("%-12s", item.category_averaged(tdp.behaviours))); //- left justified
			if (item.hasNote()) writer.write(","+item.getNote()+"");
			writer.write("\n");	

			sample_no++;
			time += item.duration;
		}

		return sample_no;	
	}

	/**
	 * 
	 * @param outputStream
	 * @throws IOException 
	 */
	public long tabularOutputCategory(OutputStreamWriter writer, long sample_no, 
			Beferrer tdp, ActivityCategory cat) throws IOException{

		long time = 0;

		for (MovementDataItem item : movementData){	
			//writer.write(item.toString());
			String catname = null;
			if (tdp.movementAverage != 0) {
				catname = item.category_averaged(tdp.behaviours);
			} else {
				catname = item.category(tdp.behaviours);
			}
			
			if (catname.equals(cat.name)){
				writer.write(item.print());
				writer.write(", "+String.format("%10d", time));
				writer.write(", "+String.format("%9d", item.originalMovementNumber));
				writer.write(", "+String.format("%-12s", catname)); //- left justified
				/*if (tdp.movementAverage != 0){
					writer.write(", "+String.format("%-12s", item.category_averaged(tdp.behaviours))); //- left justified
				} else {
					writer.write(", "+String.format("%-12s", item.category(tdp.behaviours))); //- left justified			
				}*/

				//if (item.hasNote()) writer.write(","+item.getNote()+"");
				writer.write("\n");	
			}
			
			sample_no++;
			time += item.duration;
		}

		return sample_no;	
	}

	/**
	 * 
	 */
	public String toString(){

		StringBuffer sb = new StringBuffer();
		
		for (MovementDataItem item : movementData){	
			sb.append(item+"\n");
		}
		
		return sb.toString();
	}
}