package zkgbai.graph;

import java.util.*;

import static zkgbai.kgbutil.KgbUtil.*;

import com.springrts.ai.oo.AIFloat3;

public class Link {
		boolean connected = false;
		public float length;
		public MetalSpot v0;
		public MetalSpot v1;
		AIFloat3 centerPos;
		Map<Integer, Pylon> pylons;
		
		Link(MetalSpot v0,MetalSpot v1){
			this.v0 = v0;
			this.v1 = v1;
			calcCenterPos();
			calcLength();
			this.pylons = new HashMap<Integer, Pylon>();
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
			pylons.put(p.hashCode(), p);
		}

		public void removePylon(Pylon p){
			pylons.remove(p.hashCode());
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

		public boolean isOwned(){
			return (v0.owned && v1.owned) || ((v0.owned || v1.owned) && !v0.hostile && !v0.enemyShadowed && !v1.hostile && !v1.enemyShadowed);
		}
	
		public boolean isAllyShadowed(){
			return (v0.allyShadowed || v1.allyShadowed) && !v0.hostile && !v1.hostile;
		}
		
		public boolean isHostile(){
			return v0.hostile || v0.enemyShadowed || v1.hostile || v1.enemyShadowed;
		}
		
		public void checkConnected() {
			ArrayList<Pylon> visited = new ArrayList<Pylon>();
			Queue<Pylon> queue = new LinkedList<Pylon>();
			float minDistance = Float.MAX_VALUE;
			int i = 0;

			// check each pylon
			for(Pylon p:v1.pylons){
				queue.add(p);
			}

			while(!queue.isEmpty()) {
				if(i++>50) {
					connected = false;
					return;
				}// infinite loop guard
				Pylon q = queue.remove();
				visited.add(q);

				// iterate through each pylon starting at v1 and
				// progressively moving towards v0 until v0 is reached
				float distance = distance(q.position, v0.position);
				if (distance < minDistance){
					minDistance = distance;
					for(Pylon child:q.neighbours){
						if(!visited.contains(child)){
							queue.add(child);
						}
					}
				}

				if (q.spots.contains(v0)){
					connected = true;
					return;
				}
			}

			connected = false;
		}
		
		public Pylon getConnectionHead(AIFloat3 position){
			Pylon winner = null;
			float minDistance = Float.MAX_VALUE;
			int i = 0;

			//start from the closest end
			if (distance(position, v0.getPos()) < distance(position, v1.getPos())){
				if (connected || v0.pylons.size() == 0) {
					return null;
				}

				ArrayList<Pylon> visited = new ArrayList<Pylon>();
				Queue<Pylon> queue = new LinkedList<Pylon>();

				for (Pylon p : v0.pylons) {
					queue.add(p);
				}

				while (!queue.isEmpty()) {
					if (i++ > 50) return null; // infinite loop guard
					Pylon q = queue.remove();
					visited.add(q);

					float distance = distance(q.position, v1.position);
					if (distance < minDistance) {
						minDistance = distance;
						winner = q;

						for (Pylon child : q.neighbours) {
							if (!visited.contains(child)) {
								queue.add(child);
							}
						}
					}
				}

				return winner;
			}else {
				if (connected || v1.pylons.size() == 0) {
					return null;
				}

				ArrayList<Pylon> visited = new ArrayList<Pylon>();
				Queue<Pylon> queue = new LinkedList<Pylon>();

				for (Pylon p : v1.pylons) {
					queue.add(p);
				}

				while (!queue.isEmpty()) {
					if (i++ > 50) return null; // infinite loop guard
					Pylon q = queue.remove();
					visited.add(q);

					float distance = distance(q.position, v0.position);
					if (distance < minDistance) {
						minDistance = distance;
						winner = q;

						for (Pylon child : q.neighbours) {
							if (!visited.contains(child)) {
								queue.add(child);
							}
						}
					}
				}

				return winner;
			}
		}
}
