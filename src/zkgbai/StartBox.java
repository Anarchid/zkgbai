package zkgbai;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Drawer;

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
	
	public AIFloat3 getCenter(){
		return new AIFloat3((left+right)/2,0,(top+bottom)/2);
	}

	@Override
	public void draw(Drawer c) {
		c.addLine(new AIFloat3(top,0,left), new AIFloat3(top,0,right));
		c.addLine(new AIFloat3(top,0,right), new AIFloat3(bottom,0,right));
		c.addLine(new AIFloat3(bottom,0,right), new AIFloat3(bottom,0,left));
		c.addLine(new AIFloat3(bottom,0,left), new AIFloat3(top,0,left));
	}
}
