package trajsensor.model;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

public class Trajectory {

	private PriorityQueue<TrajectoryPoint> rawDataQueue;
	
	public Trajectory() {
		rawDataQueue = new PriorityQueue<TrajectoryPoint>(new SensorDataComparator());
	}
	
	public void addNewPoint(TrajectoryPoint d){
		rawDataQueue.add(d);
	}
	
	public Iterator<TrajectoryPoint> getIterator(){
		return rawDataQueue.iterator();
	}
	
	@SuppressWarnings("unused")
	private class SensorDataComparator implements Comparator<TrajectoryPoint> {

		public int compare(TrajectoryPoint o1, TrajectoryPoint o2) {
			// TODO Auto-generated method stub
			if (o1.getTimestamp().before(o2.getTimestamp()))
				return -1;
			else if (o1.getTimestamp().before(o2.getTimestamp()))
				return 1;
			else
				return 0;
		}

	}
	

}
