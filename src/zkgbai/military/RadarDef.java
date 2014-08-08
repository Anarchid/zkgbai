package zkgbai.military;

import java.util.ArrayList;
import java.util.List;

import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.UnitDef;

public class RadarDef {
	float averageSpeed = 0;
	float averageValue = 0;
	float averageRange = 0;
	float averageDanger = 0;
	ArrayList<UnitDef> unitDefs;
	
	String sampleName;
	
	/* for later use ... */
	boolean canPassWater = false;
	boolean canFloat = false;
	boolean canWalk = false;
	boolean canSubmerge = false;
	boolean canClimb = false;
	
	public RadarDef(List<UnitDef> defs, Resource r){
		this.unitDefs = (ArrayList<UnitDef>) defs;
		
		float totalSpeed = 0;
		float totalValue = 0;
		float totalRange = 0;
		float totalDanger = 0;
		
		sampleName = defs.get(0).getName();
		
		for(UnitDef ud:unitDefs){
			totalSpeed += ud.getSpeed();
			totalValue += ud.getCost(r);
			totalDanger += Math.sqrt(ud.getPower() * Math.min(1, ud.getWeaponMounts().size()));
			totalRange += ud.getMaxWeaponRange();
		}
		
		int udSize = unitDefs.size(); 
		averageSpeed = totalSpeed / udSize / 30;
		averageValue = totalValue / udSize;
		averageRange = totalRange / udSize;
		averageRange = totalDanger / udSize;
	}
	
	public float getDanger(){
		return this.averageDanger;
	}
	
	public float getSpeed(){
		return this.averageSpeed;
	}
	
	public String getName(){
		return this.sampleName;
	}
	
	public float getRange(){
		return this.averageRange;
	}
	
	public float getValue(){
		return this.averageValue;
	}
}
