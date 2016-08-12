package trajsensor.model;

import java.util.Date;

public class TrajectoryPoint{


	private String idSensor;
	private String idVehicle;
	private double speed;
	private Date timestamp;
	
	public TrajectoryPoint() {
		// TODO Auto-generated constructor stub
	}
	

	public TrajectoryPoint(String idSensor, String idVehicle, double speed, Date timestamp) {
		super();
		this.idSensor = idSensor;
		this.idVehicle = idVehicle;
		this.speed = speed;
		this.timestamp = timestamp;
	}

	public String getCodeSensor() {
		return idSensor;
	}

	public void setIdSensor(String idSensor) {
		this.idSensor = idSensor;
	}

	public String getIdVehicle() {
		return idVehicle;
	}

	public void setIdVehicle(String idVehicle) {
		this.idVehicle = idVehicle;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}
	
	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "SensorData [idSensor=" + idSensor + ", idVehicle=" + idVehicle + ", speed=" + speed + ", timestamp="
				+ timestamp + "]";
	}

	
}
