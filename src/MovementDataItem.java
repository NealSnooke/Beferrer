/**
 * First created Neal Snooke 01/03/2019
 * 
 * simple public data container
 */

public class MovementDataItem {

	public long originalMovementNumber = 0;
	
	private GpsPosition start;
	private GpsPosition end;
	
	private String startdate;
	private String starttime;
	
	public GpsPosition averagedStart;
	public GpsPosition averagedEnd;
	
	//any additional info regarding this movement...
	private String note = null;
	
	public int duration;
	//public double distance;
	
	//public ArrayList<Double> average_speed;
	public double average_speed = -1; //result for whatever average time has been chosen
	
	/**
	 * 
	 * @param filename
	 */
	public MovementDataItem (GpsPosition start, GpsPosition end, int duration,
			String date, String time, long originalMovementNumber){
		this.start = start;
		this.end = end;
		
		averagedStart = null;
		averagedEnd = null;
		
		this.startdate = date;
		this.starttime = time;
		
		this.duration = duration;
		//this.distance = distance();
		this.originalMovementNumber = originalMovementNumber;
		//average_speed = new ArrayList<Double>();
	}
	
	//public setDuration(double dist){	
	//}
	
	public GpsPosition getStart(){
		return start;
	}
	
	public GpsPosition getEnd(){
		return end;
	}
	
	public String getStartDate(){
		return startdate;
	}
	public String getStartTime(){
		return starttime;
	}
	
	/**
	 * used for averaging positions to reduce gps error
	 */
	public void moveCoordinate(){
		if (averagedStart != null)
			start = averagedStart;

		if (averagedEnd != null)
			end = averagedEnd;
	}

	/**
	 * The great circle distance or the orthodromic distance is the shortest distance 
	 * between two points on a sphere 
	 * @param other
	 * @return distance in m
	 */
	public double distance()
	{ 
		//This code is contributed by Prasad Kshirsagar
		// The math module contains a function 
		// named toRadians which converts from 
		// degrees to radians. 
		double lon1 = Math.toRadians(start.lon); 
		double lon2 = Math.toRadians(end.lon); 
		double lat1 = Math.toRadians(start.lat); 
		double lat2 = Math.toRadians(end.lat); 

		// Haversine formula  
		double dlon = lon2 - lon1;  
		double dlat = lat2 - lat1; 
		double a = Math.pow(Math.sin(dlat / 2), 2) 
				+ Math.cos(lat1) * Math.cos(lat2) 
				* Math.pow(Math.sin(dlon / 2),2); 

		double c = 2 * Math.asin(Math.sqrt(a)); 

		// Radius of earth in kilometers. Use 3956  
		// for miles 
		double r = 6371; 

		// calculate the result (in m)
		return((c * r)*1000); 
	}  
	
	/**
	 * 
	 * @param notetext
	 */
	public void addNote (String notetext){
		if (note == null) {
			note = notetext;
		} else {
			note = note+"; "+notetext;
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean hasNote(){
		return note != null;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getNote(){
		return note;
	}

	/**
	 * 
	 * @param other
	 * @return
	 */
	public double speed(){
		return distance()/duration;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isStationary(){
		return start.samePos(end);
	}
	
	/**
	 * 
	 * @param describe_stationary
	 * @return
	 */
	public String category(Behaviour beh){
		for (ActivityCategory c : beh.items){
			if (speed() < 0)
				return "";
			if (speed() <= c.maxSpeed) {
				return c.name;
			}
		}
		/*
		if (speed()<0.02) {
			if (describe_stationary)
				return("Stationary");
			else 
				return "";
		} else if (speed()<0.33){
			return("Foraging");
		} else if (speed()<10) {
			return("Moving");
		} else {
			return("GPS ERROR");
		}*/
		return("GPS ERROR");
	}
	
	/**
	 * 
	 * @param describe_stationary
	 * @return
	 */
	public String category_averaged(Behaviour beh){
		for (ActivityCategory c : beh.items){
			if (average_speed < 0)
				return "";
			if (average_speed <= c.maxSpeed) {
				return c.name;
			}
		}
		
		return("GPS ERROR");
	}
	
	/**
	 * 
	 * @param speed
	 * @return
	 */
	public static String category(double speed, Behaviour beh){
		for (ActivityCategory c : beh.items){
			if (speed < 0)
				return "";
			if (speed <= c.maxSpeed) {
				return c.name;
			}
		}
		return("GPS ERROR");
	}

	/**
	 * 
	 */
	public String toString(){
		StringBuffer sb = new StringBuffer();

		sb.append(
				String.format("%9s", startdate)+" "+
				String.format("%8s",starttime) +
				" ["+start.toString()+"], ["+
						end.toString()+"], "+
						
						"duration (s):" + String.format("%5d",duration)+", "+
						"dist (m):"+ String.format("%8.3f", distance())+", "+
						"vel (m/s):"+String.format("%8.3f", speed())
						//+String.format("%8s",timeSecs)
				);

		return sb.toString();
	}
	
	/**
	 * 
	 */
	public String toStringShort(){
		StringBuffer sb = new StringBuffer();

		sb.append(
				String.format("%9s", startdate)+" "+
				String.format("%8s",starttime) +
				" ["+start.toString()+"], ["+
						end.toString()+"], "+
						
						"duration (s):" + String.format("%5d",duration)
						//+String.format("%8s",timeSecs)
				);

		return sb.toString();
	}
	
	/**
	 * terse csv output of data
	 */
	public String print(){
		StringBuffer sb = new StringBuffer();
		
		String avs = "        ";
		if (average_speed >= 0){
			avs = String.format("%8.3f", average_speed);
		}
	
		sb.append(
				String.format("%9s", startdate)+", "+
						String.format("%8s",starttime)
						+", "+start.toString()
						+", "+end.toString()
						+", "+String.format("%8d",duration)
						+", "+String.format("%8.3f", distance())
						+", "+String.format("%8.3f", speed())
						+", "+avs
						//+String.format("%8s",timeSecs)
				);

		return sb.toString();
	}

}
