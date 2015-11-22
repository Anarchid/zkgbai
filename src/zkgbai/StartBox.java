package zkgbai;

import com.springrts.ai.oo.AIFloat3;

public class StartBox extends StartArea {
	
	float left;
	float right;
	float top;
	float bottom;
	
	StartBox(float left, float top, float right, float bottom){
		this.left = left;
		this.right = right;
		this.bottom = bottom;
		this.top = top;
	}
	
	@Override
	public boolean contains(AIFloat3 point) {
		return point.x <= right && point.x >= left && point.z >= top && point.z <=bottom;
	}
}
