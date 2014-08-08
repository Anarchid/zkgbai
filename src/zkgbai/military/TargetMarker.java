package zkgbai.military;

import com.springrts.ai.oo.AIFloat3;

public class TargetMarker {
	AIFloat3 position;
	int frame;
	
	TargetMarker(AIFloat3 position, int frame){
		this.position = position;
		this.frame = frame;
	}
	
	@Override
	public int hashCode(){
		return position.hashCode() + frame;
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof TargetMarker){
			TargetMarker that = (TargetMarker)other;
			return this.position.equals(that.position) && this.frame == that.frame;	
		}
		return false;
	}
}
