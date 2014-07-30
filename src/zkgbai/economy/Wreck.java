package zkgbai.economy;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Feature;
import com.springrts.ai.oo.clb.FeatureDef;

public class Wreck {
	public String name;
	public AIFloat3 position;
	FeatureDef def;
	Feature feature;
	float reclaimValue;
	int featureID;
	
	public Wreck(Feature f, float value){
		this.feature = f;
		this.featureID = feature.getFeatureId();
		this.def = feature.getDef();
		this.position = feature.getPosition();
		this.reclaimValue = value;
		this.name = f.getDef().getName();
	}
	
	public boolean equals(Object other){
		if(other instanceof Wreck){
			return ((Wreck) other).feature.equals(feature);
		}
		return false;
	}
	
	public int hashCode(){
		return feature.hashCode();
	}
}
