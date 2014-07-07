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
}