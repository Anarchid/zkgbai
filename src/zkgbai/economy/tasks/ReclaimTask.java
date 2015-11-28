package zkgbai.economy.tasks;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Feature;

public class ReclaimTask extends WorkerTask {
	public Feature target;
	public ReclaimTask(Feature feat){
		super();
		this.target = feat;
		this.position = target.getPosition();
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof ReclaimTask){
			ReclaimTask wt = (ReclaimTask)other;
			return (target.getFeatureId() == wt.target.getFeatureId());
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