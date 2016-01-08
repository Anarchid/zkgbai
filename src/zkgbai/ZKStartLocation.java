package zkgbai;

import java.awt.Color;
import java.util.ArrayList;

import zkgbai.graph.GraphManager;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Drawer;

import static zkgbai.kgbutil.KgbUtil.distance;

public class ZKStartLocation extends StartArea {

	ArrayList<AIFloat3> locations;
	int id;
	
	ZKStartLocation(ArrayList<AIFloat3> locations){
		this.locations = locations;
	}
	
	@Override
	public boolean contains(AIFloat3 point) {
		for(AIFloat3 l:locations){
			if (distance(point, l) < 512){
				return true;
			}
		}
		return false;
	}

	@Override
	public void draw(Drawer c) {
		for(AIFloat3 l:locations){
			c.addPoint(l, "STARTPOS");
		}
	}

}
