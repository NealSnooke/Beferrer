/**
 * First created Neal Snooke 26/07/2018
 * nns@aber.ac.uk
 * 
 * Major enhancement and graphing 18/03/2019
 */
import java.util.Vector;

public class Behaviour {
	public Vector<ActivityCategory> items = null;
	
	/**
	 * 
	 * @param b
	 */
	public void addItem(ActivityCategory b) {
		if (items == null){
			items = new Vector<ActivityCategory>();
			items.add(b);
		} else {
			if (items.get(items.size()-1).maxSpeed < b.maxSpeed){
				items.add(b);
			} else {
				System.out.println("ERROR: can't add behaviour category with speed: "+b.maxSpeed
				+" after previous behaviour with higher speed ("+items.get(items.size()-1)+")");
				System.exit(1);
			}
		}
	}
	
	/**
	 * 
	 */
	public String toString(){
		StringBuffer sb = new StringBuffer();

		for (ActivityCategory i : items)
			sb.append(i.toString()+"\n");

		return sb.toString();
	}
}
