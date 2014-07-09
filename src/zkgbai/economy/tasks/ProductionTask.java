package zkgbai.economy.tasks;

import zkgbai.economy.Worker;

import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;

public class ProductionTask extends WorkerTask {
	public UnitDef buildType;
	public Unit building;
	public int builderPriority;
	
	public ProductionTask(Worker who,UnitDef what, int taskPriority, int builderPriority){
		super(who);
		this.completed = false;
		this.priority = taskPriority;
		this.buildType = what;
		this.builderPriority = builderPriority;
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof ProductionTask){
			ProductionTask wt = (ProductionTask)other;
			return (worker.getUnit().getUnitId() == wt.getWorker().getUnit().getUnitId() && buildType == wt.buildType);
		}
		return false;
	}
	
	@Override
	public String toString(){
		return this.worker.getUnit().getDef().getName()+" to produce "+buildType.getName();
	}
	
	@Override
	public int hashCode(){
		return worker.getUnit().getUnitId()*43+buildType.getUnitDefId();
	}
	
	public void setBuilding(Unit unit){
		this.building = unit;
	}
	
	public Unit getBuilding(){
		return building;
	}
}