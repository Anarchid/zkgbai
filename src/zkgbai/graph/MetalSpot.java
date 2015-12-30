package zkgbai.graph;

import java.util.ArrayList;

import com.springrts.ai.oo.AIFloat3;

public class MetalSpot {
	AIFloat3 position;
	float value = 0;
	boolean connected = false;
	public boolean visible = false;

	public boolean owned = false;
	public boolean hostile = false;
	public boolean enemyShadowed = false;
	public boolean allyShadowed = false;

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
		int numEnemy = 0;
		for (Link l:links){
			if (l.length < 1500) {
				if ((l.v0.enemyShadowed && !l.v0.owned)
						|| (l.v1.enemyShadowed && !l.v1.owned)) {
					numEnemy++;
				}
			}
		}
		if (numEnemy > 1){
			return true;
		}
		return false;
	}
}
