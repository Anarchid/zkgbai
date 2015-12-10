package zkgbai.economy.tasks;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;

public class CombatReclaimTask extends WorkerTask {
	public Unit target;
	public CombatReclaimTask(Unit unit){
		super();
		this.target = unit;
		this.position = target.getPos();
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof CombatReclaimTask){
			CombatReclaimTask wt = (CombatReclaimTask)other;
			return (target.getUnitId() == wt.target.getUnitId());
		}
		return false;
	}
	
	@Override
	public String toString(){
		return " to reclaim " + target.getDef().getName();
	}

	@Override
	public AIFloat3 getPos(){
		return this.position;
	}
}