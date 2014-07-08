package zkgbai.graph;

import java.util.ArrayList;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;

public class Pylon {
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
	
	@Override 
	public boolean equals(Object other) {
		if (other instanceof Pylon) {
			return (((Pylon) other).getUnitId() == this.unit.getUnitId());
		}
		return result;
	}
	
	@Override 
	public int hashCode() {
		return (position.x*position.y*position.z*unit.getUnitId());
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
	
	void addLink(Link l){
		links.add(l);
	}

	ArrayList<MetalSpot>getSpots(){
		return spots;
	}
	
	Unit getUnit(){
		return unit;
	}
}
