package zkgbai.economy.tasks;

import zkgbai.economy.Worker;
import zkgbai.economy.Wreck;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Feature;
import com.springrts.ai.oo.clb.UnitDef;

public class ReclaimTask extends WorkerTask {
	public Wreck feature;
	public AIFloat3 location;
	public ReclaimTask(Worker who, Wreck what){
		super(who);
		this.location = what.position;
		this.feature = what;
		this.completed = false;
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof ReclaimTask){
			ReclaimTask wt = (ReclaimTask)other;
			return (worker.getUnit().getUnitId() == wt.getWorker().getUnit().getUnitId() && feature == wt.feature && wt.location == location);
		}
		return false;
	}
	
	@Override
	public String toString(){
		return this.worker.getUnit().getDef().getName()+" to reclaim "+feature.name;
	}
	
	@Override
	public int hashCode(){
		return worker.getUnit().getUnitId()*43+feature.hashCode()*location.hashCode();
	}
}