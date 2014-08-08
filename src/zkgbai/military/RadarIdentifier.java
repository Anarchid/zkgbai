package zkgbai.military;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.UnitDef;

public class RadarIdentifier {
	ArrayList<UnitDef> unitDefs;
	ArrayList<RadarDef> radarDefs;
	HashMap<Float,RadarDef> radarDefsBySpeed;
	OOAICallback callback;
	
	public RadarIdentifier(OOAICallback cb){

		this.unitDefs = (ArrayList<UnitDef>) cb.getUnitDefs();
		this.radarDefs = new ArrayList<RadarDef>();
		this.callback = cb;
		radarDefsBySpeed = new HashMap<Float,RadarDef>();
		
		Collections.sort(unitDefs, new Comparator<UnitDef>() {
	        @Override
	        public int compare(UnitDef  a, UnitDef  b)
	        {
	            if(a.getSpeed() > b.getSpeed()) return 1;
	            if(a.getSpeed() < b.getSpeed()) return -1;
	            return 0;
	        }
	    });
		
		Resource metal = cb.getResourceByName("Metal");

		float lastSpeed = 0;
		ArrayList<UnitDef> speedList = new ArrayList<UnitDef>();
		for(UnitDef ud:unitDefs){
			if(ud.getSpeed() - lastSpeed < 3){
				speedList.add(ud);
			}else{
				createRadarDef(lastSpeed/30, speedList, metal);
				speedList = new ArrayList<UnitDef>();
				speedList.add(ud);
				lastSpeed = ud.getSpeed();
			}
		}
		createRadarDef(lastSpeed, speedList, metal);
	}
	
	private RadarDef createRadarDef(float speed, List<UnitDef>speedList, Resource metal){
		RadarDef rd = new RadarDef(speedList,metal);
		radarDefs.add(rd);				
		return rd;
	}
	
	public RadarDef getDefBySpeed(float speed){
		RadarDef closestDef = null;
		float minDiff = Float.MAX_VALUE;
		for(RadarDef rd:radarDefs){
			float speedDiff = Math.abs(speed-rd.getSpeed());
			if( speedDiff< minDiff){
				closestDef = rd;
				minDiff = speedDiff;
			}
		}
		return closestDef;
	}
}
