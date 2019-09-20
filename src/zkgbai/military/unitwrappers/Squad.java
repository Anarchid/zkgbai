package zkgbai.military.unitwrappers;

import java.util.ArrayList;
import java.util.Queue;
import java.util.List;
import static zkgbai.kgbutil.KgbUtil.*;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.ZKGraphBasedAI;

public class Squad {
	List<Fighter> fighters = new ArrayList<Fighter>();
	public float metalValue = 0;
	public char status = 'f';
	// f = forming
	// r = rallying
	// a = attacking
	public float income;
	public boolean isAirSquad = false;
	Fighter leader;
	int index = 0;
	public AIFloat3 target;
	int firstRallyFrame = 0;
	static final short OPTION_SHIFT_KEY = 32;
	int team = ZKGraphBasedAI.getInstance().teamID;
	
	public Squad(){}

	public void addUnit(Fighter f){
		fighters.add(f);
		f.squad = this;
		f.index = index;
		index++;
		metalValue = metalValue + f.metalValue;
		f.getUnit().setMoveState(1, (short) 0, Integer.MAX_VALUE);

		if (leader == null) leader = f;
	}

	public void removeUnit(Fighter f){
		fighters.remove(f);
		if (leader != null && leader.equals(f)){
			leader = getNewLeader();
		}
		metalValue -= f.metalValue;
		f.squad = null;
	}
	
	public int getSize(){
		return fighters.size();
	}

	public void setTarget(AIFloat3 pos){
		// set a target for the squad to attack.
		target = pos;
		
		float maxdist = 200f;
		float waydist = 100f;
		if (isAirSquad){
			maxdist = 300f;
			waydist = 150f;
		}
		
		Queue<AIFloat3> path = leader.getFightPath(pos);
		if (isAirSquad && path.size() > 1) path.poll();
		AIFloat3 waypoint = path.poll();
		for (Fighter f:fighters){
			f.target = target;
			float fdist = distance(f.getPos(), leader.getPos());
			if (fdist > maxdist){
				f.outOfRange = true;
				if (fdist > 2f * maxdist){
					f.moveTo(getAngularPoint(leader.getPos(), f.getPos(), lerp(waydist, maxdist, Math.random())));
				}else{
					f.getUnit().moveTo(getAngularPoint(leader.getPos(), f.getPos(), lerp(waydist, maxdist, Math.random())), (short) 0, Integer.MAX_VALUE);
				}
			}else {
				f.outOfRange = false;
				f.getUnit().fight(getFormationPoint(f.getPos(), leader.getPos(), waypoint), (short) 0, Integer.MAX_VALUE);
			}
		}
		
		// add one extra waypoint.
		if (path.isEmpty()) return;
		if (path.size() > 1) path.poll();
		if (isAirSquad && path.size() > 1) path.poll();
		waypoint = path.poll();
		for (Fighter f:fighters){
			if (f.getUnit().getHealth() <= 0 || f.outOfRange) continue;
			f.getUnit().fight(getFormationPoint(f.getPos(), leader.getPos(), waypoint), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
		}
		
	}

	public void retreatTo(AIFloat3 pos){
		// set a target for the squad to attack.
		target = pos;
		for (Fighter f:fighters){
			f.moveTo(target);
			f.target = target;
		}
	}

	public AIFloat3 getPos(){
		float maxdist = isAirSquad ? 300f: 200f;

		if (fighters.size() > 0){
			int count = 0;
			float x = 0;
			float z = 0;
			for (Fighter f:fighters){
				if (distance(f.getPos(), leader.getPos()) > maxdist) continue;
				x += f.getPos().x;
				z += f.getPos().z;
				count++;
			}
			AIFloat3 pos = new AIFloat3();
			x = x/count;
			z = z/count;
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
		
		setTarget(target);
		
		for (Fighter f: fighters){
			if (f.outOfRange){
				return false;
			}
		}
		return true;
	}

	public boolean isDead(){
		List<Fighter> invalidFighters = new ArrayList<>();
		for (Fighter f: fighters){
			if (f.getUnit().getHealth() <= 0 || f.getUnit().getTeam() != team){
				invalidFighters.add(f);
				f.squad = null;
			}
		}
		fighters.removeAll(invalidFighters);
		if (status != 'f') cutoff();
		
		leader = getBestLeader();
		return (leader == null);
	}

	private void cutoff(){
		if (fighters.size() < 2 || metalValue < 1000f){
			for (Fighter f:fighters){
				f.squad = null;
			}
			fighters.clear();
		}
	}
	
	public Unit getLeader(){
		leader = getBestLeader();
		if (leader == null) return null;
		return leader.getUnit();
	}
	
	public int getThreat(){
		float threat = 0;
		for (Fighter f: fighters){
			threat += f.getUnit().getPower() + f.getUnit().getMaxHealth();
		}
		return (int) (threat/8f);
	}
	
	private Fighter getBestLeader(){
		if (leader == null || leader.getUnit().getHealth() <= 0 || leader.getUnit().getTeam() != team) leader = getNewLeader();
		if (leader == null) return null;
		Fighter newLeader = leader;
		float minspeed = leader.getUnit().getDef().getSpeed();
		for (Fighter f:fighters){
			if (distance(f.getPos(), leader.getPos()) > 300f) continue;
			float tmpspeed = f.getUnit().getSpeed();
			if (tmpspeed < minspeed){
				minspeed = tmpspeed;
				newLeader = f;
			}
		}
		return newLeader;
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
