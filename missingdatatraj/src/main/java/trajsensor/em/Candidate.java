package trajsensor.em;

import trajsensor.model.TrajectoryPoint;

public class Candidate {
	private TrajectoryPoint point;
	private double probability;

	public TrajectoryPoint getPoint() {
		return point;
	}

	public void setPoint(TrajectoryPoint point) {
		this.point = point;
	}

	public double getProbability() {
		return probability;
	}

	public void setProbability(double probability) {
		this.probability = probability;
	}

}
