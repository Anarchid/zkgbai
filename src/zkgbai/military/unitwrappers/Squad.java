package zkgbai.military.unitwrappers;

import java.util.ArrayList;
import java.util.List;
import static zkgbai.kgbutil.KgbUtil.*;

import com.springrts.ai.oo.AIFloat3;

public class Squad {
	List<Fighter> fighters;
	public float metalValue;
	public char status;
	public float income;
	public boolean assigned;
	public boolean isAirSquad = false;
	private Fighter leader;
	private int index = 0;
	public AIFloat3 target;
	int firstRallyFrame = 0;
	
	public Squad(){
		this.fighters = new ArrayList<Fighter>();
		this.metalValue = 0;
		this.assigned = false;
		this.status = 'f';
		// f = forming
		// r = rallying
		// a = attacking
	}

	public void addUnit(Fighter f, int frame){
		fighters.add(f);
		f.squad = this;
		f.index = index;
		index++;
		metalValue = metalValue + f.metalValue;
		f.getUnit().setMoveState(1, (short) 0, frame+30);
		f.fightTo(target, frame);

		if (leader == null){
			leader = f;
		}
	}

	public void removeUnit(Fighter f){
		fighters.remove(f);
		if (leader != null && leader.equals(f)){
			leader = getNewLeader();
		}
		metalValue -= f.metalValue;
	}

	public void setTarget(AIFloat3 pos, int frame){
		// set a target for the squad to attack.
		target = pos;
		for (Fighter f:fighters){
			f.fightTo(target, frame);
		}
	}

	public void retreatTo(AIFloat3 pos, int frame){
		// set a target for the squad to attack.
		target = pos;
		for (Fighter f:fighters){
			f.moveTo(target, frame);
		}
	}

	public AIFloat3 getPos(){
		if (status == 'f' && leader != null && leader.getUnit().getHealth() > 0){
			return leader.getPos(); // if the squad is forming return the position of the oldest unit.
		}

		if (fighters.size() > 0){
			int count = fighters.size();
			float x = 0;
			float z = 0;
			for (Fighter f:fighters){
				x += (f.getPos().x)/count;
				z += (f.getPos().z)/count;
			}
			AIFloat3 pos = new AIFloat3();
			pos.x = x;
			pos.z = z;
			return pos; // otherwise return the average position of all its units
		}

		return target; // otherwise if the squad has no units, return its target
	}

	public boolean isRallied(int frame){
		if (firstRallyFrame == 0){
			firstRallyFrame = frame;
		}
		if (frame - firstRallyFrame > 900){
			return true;
		}
		boolean rallied = true;
		for (Fighter f: fighters){
			if (distance(target, f.getPos()) > 300 && distance(leader.getPos(), f.getPos()) > 300){
				f.moveTo(getRadialPoint(target, 50f), frame);
				rallied = false;
			}else{
				f.fightTo(target, frame);
			}
		}
		return rallied;
	}

	public boolean isDead(){
		if (fighters.size() == 0){
			return true;
		}
		return false;
	}

	public void cutoff(){
		List<Fighter> extraUnits = new ArrayList<Fighter>();
		if (fighters.size() < 4 && metalValue < 1000){
			for (Fighter f:fighters){
				f.squad = null;
			}
			extraUnits.addAll(fighters);
		}
		fighters.removeAll(extraUnits);
	}

	private Fighter getNewLeader(){
		Fighter newLeader = null;
		int tmpindex = Integer.MAX_VALUE;
		for (Fighter f:fighters){
			if (f.index < tmpindex){
				newLeader = f;
				tmpindex = f.index;
			}
		}
		return newLeader;
	}
}
