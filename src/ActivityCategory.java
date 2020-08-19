/**
 * First created Neal Snooke 26/07/2018
 * nns@aber.ac.uk
 * 
 * Major enhancement and graphing 18/03/2019
 */
public class ActivityCategory {

	public String name;
	public double maxSpeed;
	
	ActivityCategory(String name, double maxSpeed){
		this.name = name;
		this.maxSpeed = maxSpeed;
	}
	
	/**
	 * 
	 */
	public String toString(){
		return name+" <= "+maxSpeed+"m/s";
	}
}
