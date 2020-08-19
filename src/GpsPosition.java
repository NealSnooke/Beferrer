/**
 * First created Neal Snooke 01/03/2019
 * 
 * simple public data container
 */

public class GpsPosition {
	public double lat;
	public double lon;
	
	GpsPosition (double lat, double lon){
		this.lat = lat;
		this.lon = lon;
	}
	
	GpsPosition (GpsPosition other){
		this.lat = other.lat;
		this.lon = other.lon;
	}

	public boolean samePos(GpsPosition other){
		return other.lat == lat &&
				other.lon == lon;
	}
	
	/**
	 * The great circle distance or the orthodromic distance is the shortest distance 
	 * between two points on a sphere 
	 * @param other
	 * @return distance in m
	 */
	public double distance(GpsPosition other)
	{ 
		//TODO change to vincenties formua
		
		//This code is contributed by Prasad Kshirsagar
		// The math module contains a function 
		// named toRadians which converts from 
		// degrees to radians. 
		double lon1 = Math.toRadians(lon); 
		double lon2 = Math.toRadians(other.lon); 
		double lat1 = Math.toRadians(lat); 
		double lat2 = Math.toRadians(other.lat); 

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
				String.format("%9.6f", lat)+", "+
						String.format("%9.6f", lon)
						);

		return sb.toString();
	}
}
