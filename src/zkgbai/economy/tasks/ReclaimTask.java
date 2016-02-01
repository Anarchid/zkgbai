package zkgbai.economy.tasks;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Feature;
import com.springrts.ai.oo.clb.FeatureDef;

import java.beans.FeatureDescriptor;

public class ReclaimTask extends WorkerTask {
	public Feature target;
	public FeatureDef def;
	public float metalValue;
	public ReclaimTask(Feature feat, float metal){
		super();
		this.target = feat;
		this.def = feat.getDef();
		this.position = feat.getPosition();
		this.metalValue = metal;
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