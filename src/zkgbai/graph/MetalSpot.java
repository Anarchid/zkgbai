package zkgbai.graph;

import java.util.ArrayList;
import java.util.HashMap;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.economy.Worker;

public class MetalSpot {
	AIFloat3 position;
	float value = 0;
	float danger = 0;
	float protection = 0;
	boolean owned = false;
	boolean hostile = false;
	boolean connected = false;
	boolean visible = false;

	int lastSeen = 0;
	ArrayList<Worker> colonizers;
	public ArrayList<Pylon> pylons;
	public ArrayList<Link> links;
	Unit extractor;
	
	float shadowInfluence = 0;
	
	public float getShadowInfluence() {
		return shadowInfluence;
	}

	public void setShadowInfluence(float shadowInfluence) {
		this.shadowInfluence = shadowInfluence;
	}

	public boolean isShadowCaptured() {
		return isShadowCaptured;
	}

	public void setShadowCaptured(boolean isShadowCaptured) {
		this.isShadowCaptured = isShadowCaptured;
	}

	boolean isShadowCaptured = false;
	
	MetalSpot(float x, float y, float z, float m){
		colonizers = new ArrayList<Worker>();
		this.value = m;
		this.position = new AIFloat3(x,y,z);
		this.pylons = new ArrayList<Pylon>();
		this.links = new ArrayList<Link>();
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof MetalSpot){
			MetalSpot ms = (MetalSpot)other;
			return (ms.value == value && ms.position == position);
		}
		return false;
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
	
	public void addColonist(Worker w){
		colonizers.add(w);
	}
	
	public void removeColonist(Unit u){
		colonizers.remove(u);
	}
	
	public void clearColonists(){
		colonizers.clear();
	}
	
	public ArrayList<Worker> getColonists(){
		return colonizers;
	}
	
	public float getLastSeen(){
		return lastSeen;
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
