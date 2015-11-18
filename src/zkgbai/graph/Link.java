package zkgbai.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import com.springrts.ai.oo.AIFloat3;

public class Link {
		public ArrayList<Pylon> pylons;
		boolean connected = false;
		float weight;
		float length;
		MetalSpot v0;
		MetalSpot v1;
		AIFloat3 centerPos;
		
		Link(MetalSpot v0,MetalSpot v1){
			this.pylons = new ArrayList<Pylon>();
			this.v0 = v0;
			this.v1 = v1;
			calcCenterPos();
			calcLength();
			this.weight = (v0.value+v1.value)/this.length+1; 
		}
		
		@Override
		public boolean equals(Object other){
			if(other instanceof Link){
				Link that = (Link) other;
				return (v1 == that.v1 && v0 == that.v0 || v0 == that.v1 && v1 == that.v0);
			}
			return false;
		}
		
		@Override
		public int hashCode(){
			return v0.position.hashCode() + v1.position.hashCode()*2 + centerPos.hashCode()*4;
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

		public AIFloat3 getPos(){
			return centerPos;
		}
		
		public boolean checkConnected()
		{
			int i = 0;
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
					if(i++>1000) return false;
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
		
		public Pylon getConnectionHead(){
			if(connected || pylons.size() == 0){
				return null;
			}
			
			Pylon winner = null;
			float minDistance = Float.MAX_VALUE;
			
			int i = 0;
			ArrayList<Pylon>visited = new ArrayList<Pylon>();
			Queue <Pylon>queue = new LinkedList<Pylon>();
			
			for(Pylon p:v0.pylons){
				queue.add(p);	
			}
			
			while(!queue.isEmpty()) {
				Pylon q = queue.remove();
				if(i++>1000) return null;
				visited.add(q);
				
				float distance = GraphManager.groundDistance(q.position, v1.position);
				if (distance < minDistance){
					minDistance = distance;
					winner = q;
				}
				
				for(Pylon child:q.neighbours){
					if(!visited.contains(child)){
						queue.add(child);
					}
				}
			}
			
			return winner;
		}
}
