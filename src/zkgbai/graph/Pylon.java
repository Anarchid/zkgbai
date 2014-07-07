package zkgbai.graph;

import java.util.ArrayList;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;

public class Pylon {
	int id;
	int radius;
	float output;
	AIFloat3 position;
	public ArrayList<MetalSpot> spots;
	public ArrayList<Pylon> neighbours;
	public ArrayList<Link> links;
	Unit unit;
	
	Pylon(Unit unit, int radius){
		this.unit = unit;
		this.position = unit.getPos();
		this.radius = radius;
		
		this.spots = new ArrayList<MetalSpot>();
		this.neighbours = new ArrayList<Pylon>();
		this.links = new ArrayList<Link>();
	}
	
	void addNeighbour(Pylon p){
		neighbours.add(p);
	}
	
	void removeNeighbour(Pylon p){
		neighbours.remove(p);
	}
	
	void addSpot(MetalSpot s){
		spots.add(s);
	}
	
	ArrayList<MetalSpot>getSpots(){
		return spots;
	}
	
	void addLink(Link l){
		links.add(l);
	}
	
}
