package zkgbai.military;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;

public class Enemy {
	Unit unit;
	int unitID;
	AIFloat3 position;
	float threatRadius = 0;
	float value = 0;
	float danger = 0;
	float speed = 0;
	int lastSeen = 0;
	boolean visible = false;
	boolean isStatic = false;
	
	Enemy(Unit unit, float cost){
		this.unit = unit;
		this.unitID = unit.getUnitId();

		this.value = cost;
		this.position = unit.getPos();
		this.isStatic = unit.getMaxSpeed() > 0;
		
		if(unit.getDef().getWeaponMounts().size() > 0){
			this.danger =  unit.getPower();
			this.threatRadius = unit.getMaxRange();
		}
		
		this.speed = unit.getMaxSpeed();
	}
	
	@Override
	public boolean equals(Object other){
		if (other instanceof Enemy){
			return unitID == ((Enemy) other).unitID;
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return unitID;
	}
	
	void setLastSeen(int f){
		this.lastSeen = f;
	}
	
	int getLastSeen(){
		return lastSeen;
	}
	
}
