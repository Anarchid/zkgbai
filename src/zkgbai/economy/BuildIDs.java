package zkgbai.economy;

import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.UnitDef;

import java.util.HashSet;
import java.util.Set;

public class BuildIDs {
	public int mexID;
	public int solarID;
	public int windID;
	public int nanoID;
	public int storageID;
	public int airpadID;
	public int pylonID;
	public int radarID;
	public Set<Integer> facIDs = new HashSet<>();
	public Set<Integer> expensiveIDs = new HashSet<>();

	public BuildIDs(OOAICallback callback){
		Resource m = callback.getResourceByName("Metal");
		mexID = callback.getUnitDefByName("staticmex").getUnitDefId();
		solarID = callback.getUnitDefByName("energysolar").getUnitDefId();
		windID = callback.getUnitDefByName("energywind").getUnitDefId();
		nanoID = callback.getUnitDefByName("staticcon").getUnitDefId();
		storageID = callback.getUnitDefByName("staticstorage").getUnitDefId();
		airpadID = callback.getUnitDefByName("staticrearm").getUnitDefId();
		pylonID = callback.getUnitDefByName("energypylon").getUnitDefId();
		radarID = callback.getUnitDefByName("staticradar").getUnitDefId();

		for (UnitDef ud: callback.getUnitDefs()){
			if (ud.getName().contains("factory") || ud.getName().contains("hub")){
				facIDs.add(ud.getUnitDefId());
			}else if (ud.getSpeed() == 0 && ud.getCost(m) > 200f && ud.isAbleToAttack() && !ud.getTooltip().contains("Anti-Air")){
				expensiveIDs.add(ud.getUnitDefId());
			}
		}
	}
}
