package zkgbai.economy.tasks;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;

import zkgbai.economy.Worker;

public class RepairTask extends WorkerTask{
	public Unit target;

	public RepairTask(Unit target) {
		this.target = target;
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