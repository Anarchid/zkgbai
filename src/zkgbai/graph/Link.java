package zkgbai.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import com.springrts.ai.oo.AIFloat3;

public class Link {
		int id;
		public ArrayList<Pylon> pylons;
		boolean owned = false;
		boolean connected = false;
		boolean contested = false;
		boolean populated = false;
		float weight;
		float length;
		MetalSpot v0;
		MetalSpot v1;
		AIFloat3 centerPos;
		int pathID=0;
		
		Link(MetalSpot v0,MetalSpot v1){
			this.pylons = new ArrayList<Pylon>();
			this.v0 = v0;
			this.v1 = v1;
			calcCenterPos();
			calcLength();
			this.weight = (v0.value+v1.value)/this.length+1; 
		}
		
		public void addPylon(Pylon p){
			pylons.add(p);
		}
		
		public void removePylon(Pylon p){
			pylons.remove(p);
		}
		
		private void calcCenterPos() {
			AIFloat3 pos1 = v0.position;
			AIFloat3 pos2 = v1.position;
			float x = (pos1.x+pos2.x)/2;
			float y = (pos1.y+pos2.y)/2;
			float z = (pos1.z+pos2.z)/2;
			centerPos = new AIFloat3(x,y,z);
		}
		
		private void calcLength() {
			AIFloat3 pos1 = v0.position;
			AIFloat3 pos2 = v1.position;
			float dx = (pos1.x-pos2.x);
			float dz = (pos1.z-pos2.z);
			length = (float) Math.sqrt(dx*dx+dz*dz);
		}
		
		public boolean checkConnected()
		{
			ArrayList<Pylon>visited = new ArrayList<Pylon>();
			Queue <Pylon>queue = new LinkedList<Pylon>();
			
			for(Pylon p:v0.pylons){
				queue.add(p);	
			}
			
			while(!queue.isEmpty()) {
				Pylon q = queue.remove();
				if(q.spots.contains(v1)){
					connected = true;
					return true;
				}else{
					visited.add(q);
					for(Pylon child:q.neighbours){
						if(!visited.contains(child)){
							queue.add(child);
						}
					}
				}
			}
			connected = false;
			return false;
		}
}
