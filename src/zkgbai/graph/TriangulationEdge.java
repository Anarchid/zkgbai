package zkgbai.graph;

import org.poly2tri.triangulation.TriangulationPoint;

public class TriangulationEdge {
	TriangulationPoint v0;
	TriangulationPoint v1;
	boolean unique;
	
	public TriangulationEdge(TriangulationPoint v0,TriangulationPoint v1){
		this.v0 = v0;
		this.v1 = v1;
	}
	
	@Override
	public boolean equals(Object a){
		if (a instanceof TriangulationEdge){
			TriangulationEdge b = (TriangulationEdge) a;
			return ((this.v0 == b.v0 && this.v1 == b.v1) || this.v1 == b.v0 && this.v0 == b.v1);
		}else{
			return false;
		}
	}
	
	public int hashCode(){
		TriangulationPoint a;
		TriangulationPoint b;
		
		boolean gt = false;
		
		double dx = v0.getX() - v1.getX();
		if (dx != 0){
			gt = dx > 0;
		}else{
			double dy = v0.getY() - v1.getY();
			if (dy != 0){
				gt = dy > 0;
			}else{
				double dz = v0.getZ() - v1.getZ();
				if (dz != 0){
					gt = dz > 0;
				}
			}
		}
		
		if(gt){
			a = v0;
			b = v1;
		}else{
			a = v1;
			b = v0;
		}

	    return (int) (41 * (41 + a.getX()) + b.getY());

	}
}
