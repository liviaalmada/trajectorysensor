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
import java.util.Set;

import org.graphast.model.Edge;
import org.graphast.model.EdgeImpl;
import org.graphast.model.GraphImpl;
import org.graphast.model.Node;
import org.graphast.model.NodeImpl;

import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import trajsensor.em.CandidatesFilter;
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
	private static HashMap<String, ArrayList<String>> trajectoryMapBySensor = new HashMap<String, ArrayList<String>>();
	private static ArrayList<TrajectoryPoint> missingPointsList = new ArrayList<TrajectoryPoint>();
	private static HashSet<String> sensorsCodeList = new HashSet<String>();
	private static HashMap<String, Long> sensorsCodeNodeIdMap = new HashMap<String, Long>();
	private static int[][] adjMatrix;
	private static long[][] maxTimeMatrix;
	
	public static Long getNodeIdOfSensor(String code){
		if(sensorsCodeNodeIdMap!=null){
			return sensorsCodeNodeIdMap.get(code);
		}
		return null;
	}

	public static void read(String pathFile, boolean clearAll) throws IOException, ParseException {
		String strLine = null;
		FileInputStream fstream = new FileInputStream(pathFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		int line = 1, missing = 0;
		if (clearAll)
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

			loadPoints(point);
			loadSensorCode(sensorCodeStr);

			// System.out.println(line + " " + point);
			line++;
			if (line == 15000)
				break;
		}
		// System.out.println(line);
		// System.out.println(missing);
		// System.out.println((double) missing / (double) line);
		// System.out.println(sensorsCodeList);
		br.close();
	}

	private static void clearAll() {
		trajectoryMap.clear();
		missingPointsList.clear();
		sensorsCodeList.clear();
		trajectoryMapBySensor.clear();
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

	private static void loadPoints(TrajectoryPoint point) {
		if (point.getIdVehicle() != null) {
			if (!trajectoryMap.containsKey(point.getIdVehicle())) {
				Trajectory traj = new Trajectory();
				trajectoryMap.put(point.getIdVehicle(), traj);
			}
			trajectoryMap.get(point.getIdVehicle()).addNewPoint(point);
			if (!trajectoryMapBySensor.containsKey(point.getCodeSensor())) {
				ArrayList<String> trajBySensorLst = new ArrayList<String>();
				trajectoryMapBySensor.put(point.getCodeSensor(), trajBySensorLst);
			}
			trajectoryMapBySensor.get(point.getCodeSensor()).add(point.getIdVehicle());

		} else {
			missingPointsList.add(point);
		}
	}

	public static GraphImpl getSensorGraph(String directory) {
		GraphImpl graph = new GraphImpl(directory);
		generateNodes(graph);
		generateMatrices();
		generateEdges(graph);

		return graph;
	}

	private static void generateEdges(GraphImpl graph) {
		for (int i = 0; i < adjMatrix.length; i++) {
			for (int j = 0; j < adjMatrix.length; j++) {

				int[] costs = { (int) maxTimeMatrix[i][j] };
				Edge edge;
				if (adjMatrix[i][j] > 0) {
					System.out.println("Try to add edge: " + "(" + i + " " + j + ")");
					edge = new EdgeImpl(i, j, adjMatrix[i][j], costs);
					graph.addEdge(edge);
				}

			}
		}
	}

	private static void generateMatrices() {
		adjMatrix = new int[sensorsCodeList.size()][sensorsCodeList.size()];
		maxTimeMatrix = new long[sensorsCodeList.size()][sensorsCodeList.size()];

		for (String vehicleId : trajectoryMap.keySet()) {
			System.out.println("Analysis vehicle code " + vehicleId);
			Trajectory trajectory = trajectoryMap.get(vehicleId);
			Iterator<TrajectoryPoint> iterator = trajectory.getIterator();
			TrajectoryPoint ant = null, suc = null;

			while (iterator.hasNext()) {
				if (ant != null) {
					suc = iterator.next();
					// update counting
					Long fromID = sensorsCodeNodeIdMap.get(ant.getCodeSensor());
					Long toID = sensorsCodeNodeIdMap.get(suc.getCodeSensor());
					System.out.print("Analysis edge from " + fromID + " " + toID);
					updateMatricesInfo(ant, suc, fromID, toID);
					ant = suc;
				} else {
					ant = iterator.next();
				}

			}
			// Add info that pass by a node
			Long fromID = sensorsCodeNodeIdMap.get(ant.getCodeSensor());
			adjMatrix[fromID.intValue() - 1][fromID.intValue() - 1]++;
		}

	}

	private static void updateMatricesInfo(TrajectoryPoint ant, TrajectoryPoint suc, Long fromID, Long toID) {
		// update counting
		adjMatrix[fromID.intValue() - 1][toID.intValue() - 1]++;

		// update maximal travel time
		long deltaTime = suc.getTimestamp().getTime() - ant.getTimestamp().getTime();
		if (deltaTime > maxTimeMatrix[fromID.intValue() - 1][toID.intValue() - 1]) {
			maxTimeMatrix[fromID.intValue() - 1][toID.intValue() - 1] = deltaTime;
		}

		// add info that pass by a node
		adjMatrix[fromID.intValue() - 1][fromID.intValue() - 1]++;
		System.out.println(" update to " + adjMatrix[fromID.intValue() - 1][toID.intValue() - 1]);
	}

	private static void generateNodes(GraphImpl graph) {
		int id = 1;
		for (String sensorCode : sensorsCodeList) {
			// Node node = new NodeImpl(id, id);
			Node node = new NodeImpl();
			node.setId((long) id);
			node.setLatitude(id);
			node.setLongitude(id);
			node.setLabel(sensorCode);
			graph.addNode(node);
			sensorsCodeNodeIdMap.put(sensorCode, (long) id);
			id++;
		}
	}

	public static void main(String[] args) {

		try {
			ReadSensorData.read(
					"/media/livia/DATA/Workspace/maven.1470949999564/missingdatatraj/Ofuscado_2014-02-01_A.csv", true);
			// ReadSensorData.reader("C:/Users/Livia/git/trajectorysensor/trajsensor/Ofuscado_2014-12-01_F.csv",false);
			ReadSensorData.read(
					"/media/livia/DATA/Workspace/maven.1470949999564/missingdatatraj/Ofuscado_2014-02-01_T.csv", false);

			GraphImpl sensorGraph = ReadSensorData.getSensorGraph("grafoTeste");
			System.out.println(sensorGraph.getNumberOfNodes());
			System.out.println(sensorGraph.getNumberOfEdges());
			for (int i = 0; i < sensorGraph.getNumberOfNodes(); i++) {
				Node node = sensorGraph.getNode(i);
				System.out.println("Node " + i + " " + node);
				int[] costs = node.getCosts();
				if (costs != null)
					System.out.println(costs[0]);

				try {
					LongList outEdges = sensorGraph.getOutEdges(i);
					LongListIterator iterator = outEdges.iterator();
					while (iterator.hasNext()) {
						Long next = iterator.next();
						Edge edge = sensorGraph.getEdge(next);
						System.out.println(edge.getToNode());
						System.out.println(edge.getCosts()[0]);
						iterator.next();
					}
				} catch (Exception e) {
					System.out.println("No edges.");
				}

			}
			System.out.println("Start save.");
			sensorGraph.save();
			System.out.println("End save.");

			Set<String> keySet = ReadSensorData.trajectoryMapBySensor.keySet();
			for (String string : keySet) {
				System.out.println("Sensor: "+string);
				System.out.println(ReadSensorData.trajectoryMapBySensor.get(string));
			}
			System.out.println("Running filter");
			if(!missingPointsList.isEmpty()){
				TrajectoryPoint trajectoryPoint = missingPointsList.get(0);
				CandidatesFilter.filter(trajectoryPoint, trajectoryMap, trajectoryMapBySensor, sensorGraph);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
