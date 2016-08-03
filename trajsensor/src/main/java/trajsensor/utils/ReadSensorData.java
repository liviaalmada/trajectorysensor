package trajsensor.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.graphast.model.Edge;
import org.graphast.model.EdgeImpl;
import org.graphast.model.GraphImpl;
import org.graphast.model.Node;
import org.graphast.model.NodeImpl;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import trajsensor.model.Trajectory;
import trajsensor.model.TrajectoryPoint;

public class ReadSensorData {

	private static final String REGEX = ";";
	private static final int SENSOR_CODE_INDEX = 0;
	private static final int DATE_INDEX = 1;
	private static final int TIMESTAMP_INDEX = 2;
	private static final int SPEED_INDEX = 5;
	private static final int VEHICLE_ID_INDEX = 8;
	private static HashMap<String, Trajectory> trajectoryMap = new HashMap<String, Trajectory>();
	private static ArrayList<TrajectoryPoint> missingPointsList = new ArrayList<TrajectoryPoint>();
	private static HashSet<String> sensorsCodeList = new HashSet<String>();
	private static HashMap<String, Long> sensorsCodeNodeIdMap = new HashMap<String, Long>();

	public static void reader(String pathFile) throws IOException, ParseException {
		String strLine = null;
		FileInputStream fstream = new FileInputStream(pathFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		int line = 1, missing = 0;
		clearAll();

		while ((strLine = br.readLine()) != null) {
			String[] split = strLine.split(REGEX);
			String sensorCodeStr = split[SENSOR_CODE_INDEX];
			String[] dateStr = split[DATE_INDEX].split("-");
			String[] timestampStr = split[TIMESTAMP_INDEX].split(":");
			String speedStr = split[SPEED_INDEX];
			String vehicleId = null;

			if (split.length == VEHICLE_ID_INDEX + 1) {
				vehicleId = split[VEHICLE_ID_INDEX];
			} else {
				missing++;
			}

			TrajectoryPoint point = new TrajectoryPoint(sensorCodeStr, vehicleId,
					Double.valueOf(speedStr.replace(",", ".")), getCalendarTime(dateStr, timestampStr));

			loadPoints(vehicleId, point);
			loadSensorCode(sensorCodeStr);

			System.out.println(line + " " + point);
			line++;
			if(line == 15000) break;
		}
		System.out.println(line);
		System.out.println(missing);
		System.out.println((double) missing / (double) line);
		System.out.println(sensorsCodeList);
		br.close();
	}

	private static void clearAll() {
		trajectoryMap.clear();
		missingPointsList.clear();
		sensorsCodeList.clear();
	}

	private static Date getCalendarTime(String[] dateStr, String[] timestamp) {
		Calendar cal = GregorianCalendar.getInstance();
		cal.set(Integer.valueOf(dateStr[0]), Integer.valueOf(dateStr[1]) - 1, Integer.valueOf(dateStr[2]),
				Integer.valueOf(timestamp[0]), Integer.valueOf(timestamp[1]), Integer.valueOf(timestamp[2]));
		return cal.getTime();
	}

	private static void loadSensorCode(String sensorCodeStr) {
		if (!sensorsCodeList.contains(sensorCodeStr)) {
			sensorsCodeList.add(sensorCodeStr);
		}
	}

	private static void loadPoints(String vehicleId, TrajectoryPoint point) {
		if (vehicleId != null) {
			if (!trajectoryMap.containsKey(vehicleId)) {
				Trajectory traj = new Trajectory();
				trajectoryMap.put(vehicleId, traj);
			}
			trajectoryMap.get(vehicleId).addNewPoint(point);
		} else {
			missingPointsList.add(point);
		}
	}

	public static GraphImpl getSensorGraph(String directory) {
		GraphImpl graph = new GraphImpl(directory);
		Long id = (long) 1;
		for (String sensorCode : sensorsCodeList) {
			NodeImpl node = new NodeImpl();
			node.setLabel(sensorCode);
			node.setLatitude(id);
			node.setLongitude(id);
			graph.addNode(node);
			sensorsCodeNodeIdMap.put(sensorCode, id);
			id++;
		}

		int[][] adjMatrix = new int[sensorsCodeList.size()][sensorsCodeList.size()];

		for (String vehicleId : trajectoryMap.keySet()) {
			System.out.println("Analysis vehicle code " + vehicleId);

			Trajectory trajectory = trajectoryMap.get(vehicleId);
			Iterator<TrajectoryPoint> iterator = trajectory.getIterator();
			TrajectoryPoint ant = null, suc = null;

			while (iterator.hasNext()) {
				if (ant != null) {
					
					suc = iterator.next();
					// update counting
					Long fromID = sensorsCodeNodeIdMap.get(ant.getIdSensor());
					Long toID = sensorsCodeNodeIdMap.get(suc.getIdSensor());
					System.out.print("Analysis edge from " + fromID + " "+toID);
					adjMatrix[fromID.intValue()-1][toID.intValue()-1]++;
					System.out.println(" update to "+adjMatrix[fromID.intValue()-1][toID.intValue()-1]);
					ant = suc;
				} else {
					ant = iterator.next();
				}
			}
		}

		for (int i = 0; i < adjMatrix.length; i++) {
			for (int j = 0; j < adjMatrix.length; j++) {
				
				int[] costs = {3,2,3,4};
				Edge edge;
				if (adjMatrix[i][j]>0 && i!=j) {
					System.out.println("Try to add edge: "+"("+(i+1)+" "+(j+1)+")");
					edge = new EdgeImpl(i+1, j+1, adjMatrix[i][j], costs);							
					graph.addEdge(edge);
				}

			}
		}

		return graph;
	}

	public static void main(String[] args) {
		try {
			try {
				ReadSensorData.reader("C:/Users/Lívia/git/trajectorysensor/trajsensor/Ofuscado_2014-12-01_A.csv");
				GraphImpl sensorGraph = ReadSensorData.getSensorGraph("grafoTeste");
				System.out.println(sensorGraph.getNumberOfNodes());
				System.out.println(sensorGraph.getNumberOfEdges());
				sensorGraph.save();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
