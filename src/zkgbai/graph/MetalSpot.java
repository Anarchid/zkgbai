package zkgbai.graph;

import java.util.ArrayList;
import java.util.HashMap;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;

public class MetalSpot {
	AIFloat3 position;
	float value;
	boolean owned = false;
	boolean hostile = false;
	boolean connected = false;
	ArrayList<Unit> colonizers; 
	public ArrayList<Pylon> pylons;
	public ArrayList<Link> links;
	Unit extractor;
	
	MetalSpot(float x, float y, float z, float m){
		colonizers = new ArrayList<Unit>();
		this.value = m;
		this.position = new AIFloat3(x,y,z);
		this.pylons = new ArrayList<Pylon>();
		this.links = new ArrayList<Link>();
	}
	
	public void addPylon(Pylon p){
		pylons.add(p);
	}
	
	public void addLink(Link l){
		links.add(l);
	}
	
	public int getPylonCount(){
		return pylons.size();
	}
	
	public AIFloat3 getPosition(){
		return position;
	}
	
	public void addColonist(Unit u){
		colonizers.add(u);
	}
	
	public void removeColonist(Unit u){
		colonizers.remove(u);
	}
	
	public void clearColonists(){
		colonizers.clear();
	}
	
	public ArrayList<Unit> getColonists(){
		return colonizers;
	}
	
	public float getValue(){
		return value;
	}
	
	public void setExtractor(Unit u){
		extractor = u;
	}
	
	public Unit getExtractor(){
		return extractor;
	}
}
