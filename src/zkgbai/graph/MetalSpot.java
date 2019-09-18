package zkgbai.graph;

import java.util.ArrayList;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;

public class MetalSpot {
	AIFloat3 position;
	float value = 0;
	public boolean visible = false;

	public boolean owned = false;
	public boolean touched = false;
	public boolean hostile = false;
	public boolean enemyShadowed = false;
	public boolean allyShadowed = false;
	public Unit extractor = null;

	int lastSeen = 0;
	public ArrayList<Pylon> pylons;
	public ArrayList<Link> links;
	
	MetalSpot(float x, float y, float z, float m){
		this.value = m;
		this.position = new AIFloat3(x,y,z);
		this.pylons = new ArrayList<Pylon>();
		this.links = new ArrayList<Link>();
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof MetalSpot){
			MetalSpot ms = (MetalSpot)other;
			return (ms.value == value && ms.position.equals(position));
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
	
	public AIFloat3 getPos(){
		return position;
	}
	
	public float getLastSeen(){
		return lastSeen;
	}
	
	public float getValue(){
		return value;
	}

	public boolean isFrontLine(){
		if (!owned && !allyShadowed){
			return false;
		}

		if (enemyShadowed){
			return true;
		}
		
		for (Link l:links){
			if (l.isHostile()) {
				return true;
			}
		}

		return false;
	}

	public boolean isConnected(){
		int numConnected = 0;
		for (Link l:links){
			if (l.isOwned()){
				if (l.v0.extractor == null || l.v1.extractor == null){
					return false;
				}
				if (!l.connected && l.v0.extractor.getHealth() > 0 && l.v1.extractor.getHealth() > 0 && l.v0.extractor.getRulesParamFloat("gridNumber", 0f) != l.v1.extractor.getRulesParamFloat("gridNumber", 0f)){
					return false;
				}
				numConnected++;
			}
		}
		if (numConnected == 0){
			return false;
		}
		return true;
	}
}
