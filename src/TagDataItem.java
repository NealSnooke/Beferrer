/**
 * First created Neal Snooke 26/07/2018
 * 
 * simple public data container
 */

import java.util.*;

public class TagDataItem {

	//public double lat;
	//public double lon;
	public GpsPosition pos;
	public String date;
	public String time;
	public long timeSecs;
	
	// the original position, when pos has become the average of surrounding points
	// to remove GPS resolution noise
	public GpsPosition originalPosition;

	/**
	 * 
	 * @param filename
	 */
	public TagDataItem (String lat, String lon, String date, String time){
		pos = new GpsPosition(Double.parseDouble(lat), Double.parseDouble(lon));
		this.date = date;
		this.time = time;
		
		Calendar c = Calendar.getInstance();
		
		String[] dt = date.split("/");
		String[] tp = time.split(":");
		
		/*
		for (String s : dt){	
			System.out.println(s+" ");
		}
		for (String s : tp){	
			System.out.println(s+" ");
		}*/

		//yy mm date hh, mm, ss
		c.set(	Integer.parseInt(dt[2]),
				Integer.parseInt(dt[1])-1,
				Integer.parseInt(dt[0]),
				Integer.parseInt(tp[0]),
				Integer.parseInt(tp[1]),
				Integer.parseInt(tp[2])); //note month is 0-11 0 = jan

		//System.out.println(c.getTime());

		timeSecs = c.getTimeInMillis()/1000;
		//System.out.println(timeSecs);
				/*
				Integer.parseInt(tp[0])*3600
				+ Integer.parseInt(tp[1])*60 
				+Integer.parseInt(tp[2]);
				*/
	}

	/**
	 * 
	 * @param last
	 * @return
	 */
	public int elapsedTime(TagDataItem last){
		long time = (timeSecs-last.timeSecs);
		//if (time < 0) time = (3600*24)+time;
		return (int)time;
	}

	/**
	 * 
	 * @param other
	 * @return
	 */
	public double speed(TagDataItem other){
		return distance(other)/elapsedTime(other);
	}

	/**
	 * The great circle distance or the orthodromic distance is the shortest distance 
	 * between two points on a sphere 
	 * @param other
	 * @return distance in m
	 */
	public double distance(TagDataItem other)
	{ 
		//This code is contributed by Prasad Kshirsagar
		// The math module contains a function 
		// named toRadians which converts from 
		// degrees to radians. 
		double lon1 = Math.toRadians(pos.lon); 
		double lon2 = Math.toRadians(other.pos.lon); 
		double lat1 = Math.toRadians(pos.lat); 
		double lat2 = Math.toRadians(other.pos.lat); 

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
	 */
	public String toString(){
		StringBuffer sb = new StringBuffer();

		sb.append(
				String.format("%9.6f", pos.lat)+" "+
						String.format("%9.6f", pos.lon)+" "+
						String.format("%9s", date)+" "+
						String.format("%8s",time) 
						//+String.format("%8s",timeSecs)
				);

		return sb.toString();
	}
	
	/**
	 * 
	 */
	public String toStringDateTime(){
		StringBuffer sb = new StringBuffer();

		sb.append(
						String.format("%9s", date)+" "+
						String.format("%8s",time) 
						//+String.format("%8s",timeSecs)
				);

		return sb.toString();
	}


}
