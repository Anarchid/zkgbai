package zkgbai.economy.tasks;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;

import zkgbai.economy.Worker;
import zkgbai.military.UnitClasses;

public class RepairTask extends WorkerTask{
	public Unit target;
	public Boolean isShieldMob = false;
	public Boolean isTank = false;

	public RepairTask(Unit target) {
		this.target = target;
		String defName = target.getDef().getName();

		UnitClasses unitTypes = UnitClasses.getInstance();
		if (unitTypes.shieldMobs.contains(target.getDef().getName()) && !target.getDef().getName().equals("striderfunnelweb")){
			this.isShieldMob = true;
		}

		if (defName.equals("tankassault") || defName.equals("tankriot") || defName.equals("tankheavyassault")){
			this.isTank = true;
		}
	}
	
	@Override
	public AIFloat3 getPos(){
		return target.getPos();
	}

	@Override
	public boolean equals(Object other){
		if(other instanceof RepairTask){
			RepairTask wt = (RepairTask)other;
			return (target.getUnitId() == wt.target.getUnitId());
		}
		return false;
	}

	@Override
	public String toString() {
		return " to repair " + target.getDef().getName();
	}
}