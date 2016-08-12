package trajsensor.em;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.graphast.model.Edge;
import org.graphast.model.GraphImpl;

import it.unimi.dsi.fastutil.longs.LongList;
import trajsensor.model.Trajectory;
import trajsensor.model.TrajectoryPoint;
import trajsensor.utils.ReadSensorData;

public class CandidatesFilter {

	public static void filter(TrajectoryPoint missingDataPoint, HashMap<String, Trajectory> trajectoryMap,
			HashMap<String, ArrayList<String>> trajectoryMapBySensor, GraphImpl graph) {

		Long nodeIdOfSensor = ReadSensorData.getNodeIdOfSensor(missingDataPoint.getCodeSensor());
		LongList outEdges = graph.getOutEdges(nodeIdOfSensor);
		LongList inEdges = graph.getInEdges(nodeIdOfSensor);

		ArrayList<TrajectoryPoint> candidates = new ArrayList<TrajectoryPoint>();
		ArrayList<Trajectory> allTrajectoriesLst = new ArrayList<Trajectory>(trajectoryMap.values());

		for (Trajectory trajectory : allTrajectoriesLst) {
			Iterator<TrajectoryPoint> iterator = trajectory.getIterator();
			TrajectoryPoint ant = null, suc;
			while (iterator.hasNext()) {
				if (ant != null) {
					suc = iterator.next();
					System.out.println("Test: "+ant+" "+missingDataPoint+" "+suc);
					testCandidate(ant, missingDataPoint, suc, graph);
				} else {
					ant = iterator.next();
				}

			}
		}

	}

	private static void testCandidate(TrajectoryPoint ant, TrajectoryPoint candidate, TrajectoryPoint suc,
			GraphImpl graph) {
		if(testNodeAdjacency(graph, ant, candidate) && testNodeAdjacency(graph, candidate, suc)){
			TrajectoryPoint point = new TrajectoryPoint(candidate.getCodeSensor(), candidate.getIdVehicle(), candidate.getSpeed(), candidate.getTimestamp());
			
		}
	}

	private static boolean testNodeAdjacency(GraphImpl graph, TrajectoryPoint from, TrajectoryPoint to) {
		try{
			Long nodeIdFrom = ReadSensorData.getNodeIdOfSensor(from.getCodeSensor());
			Long nodeIdTo = ReadSensorData.getNodeIdOfSensor(to.getCodeSensor());
			Edge edge = graph.getEdge(nodeIdFrom, nodeIdTo);
			
			
			if(edge!=null){
				System.out.println("Tem edge: "+edge);
				long totalTravelAnt = to.getTimestamp().getTime() - from.getTimestamp().getTime();
				if(totalTravelAnt<edge.getCosts()[0]){
					System.out.println("É candidato.");
					return true;
				}
			}
		}catch (Exception e) {
			// TODO: handle exception
		}
		
		return false;
	}

}
