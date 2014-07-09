package zkgbai.economy.tasks;

import zkgbai.economy.Worker;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.UnitDef;

public class ConstructionTask extends ProductionTask {
	public AIFloat3 location;
	public ConstructionTask(Worker who, UnitDef what, int taskPriority, int builderPriority, AIFloat3 location){
		super(who, what, taskPriority, builderPriority);
		this.location = location;
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof ConstructionTask){
			ConstructionTask wt = (ConstructionTask)other;
			return (worker.getUnit().getUnitId() == wt.getWorker().getUnit().getUnitId() && buildType.getUnitDefId() == wt.buildType.getUnitDefId());
		}
		return false;
	}

	@Override
	public String toString(){
		return this.worker.getUnit().getDef().getName()+" to construct "+buildType.getName();
	}
	
	@Override
	public int hashCode(){
		return worker.getUnit().getUnitId()*43+buildType.getUnitDefId();
	}
}