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
	
	public void setBuilding(Unit unit){
		this.building = unit;
	}
	
	public Unit getBuilding(){
		return building;
	}
}