package zkgbai.economy.tasks;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;

public class ConstructionTask extends WorkerTask {
	public UnitDef buildType;
	public int facing;
	public Unit target;
	public Unit ctTarget = null;
	public int frameIssued = 0;
	public boolean facPlop = false;
	public int assignedPlop;
	public boolean facDef = false;

	public ConstructionTask(UnitDef def, AIFloat3 pos, int h) {
		super();
		this.position = pos;
		this.buildType = def;
		this.facing = h;
	}

	public ConstructionTask(UnitDef def, AIFloat3 pos, int h, boolean isPlop) {
		super();
		this.position = pos;
		this.buildType = def;
		this.facing = h;
		this.facPlop = isPlop;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ConstructionTask) {
			ConstructionTask wt = (ConstructionTask) other;
			return (buildType.getUnitDefId() == wt.buildType.getUnitDefId() && position.x == wt.position.x && position.z == wt.position.z && facing == wt.facing);
		}
		return false;
	}

	@Override
	public String toString() {
		return " to build " + this.buildType.getName() + " at " + "x:" + position.x + " z:" + position.z;
	}
}