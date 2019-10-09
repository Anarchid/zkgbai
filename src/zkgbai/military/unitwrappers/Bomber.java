package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.ZKGraphBasedAI;
import zkgbai.kgbutil.Pathfinder;
import zkgbai.military.Enemy;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static zkgbai.kgbutil.KgbUtil.distance;

public class Bomber {
	public int id;
	protected Unit unit;
	static Pathfinder pathfinder = null;
	static int team;
	static final short OPTION_SHIFT_KEY = 32;
	static final int CMD_FIND_PAD = 33411;
	public Enemy target = null;
	
	public Bomber(Unit u){
		unit = u;
		id = u.getUnitId();
		
		if (pathfinder == null){
			pathfinder = Pathfinder.getInstance();
			team = ZKGraphBasedAI.getInstance().teamID;
		}
	}
	
	public void bomb(){
		if (distance(unit.getPos(), target.position) > 1000f){
			flyTo(target.position);
		}else{
			if (target.isStatic){
				unit.attackArea(target.position, 0, (short) 0, Integer.MAX_VALUE);
			}else{
				if (target.unit.getPos() != null){
					unit.attack(target.unit, (short) 0, Integer.MAX_VALUE);
				}else{
					flyTo(target.position);
				}
			}
		}
	}
	
	public void flyTo(AIFloat3 pos){
		Queue<AIFloat3> path = pathfinder.findPath(unit, pos, pathfinder.AIR_PATH);
		if (path.size() > 1) path.poll(); // skip several waypoints since they're close together and bombers are extra fast.
		if (path.size() > 1) path.poll();
		if (path.size() > 1) path.poll();
		if (path.size() > 1) path.poll();
		unit.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE);
		
		// Add the full path, since the command may not be repeated
		while(!path.isEmpty()) {
			if (path.size() > 1) path.poll(); // skip several waypoints since they're close together and bombers are extra fast.
			if (path.size() > 1) path.poll();
			if (path.size() > 1) path.poll();
			if (path.size() > 1) path.poll();
			if (!path.isEmpty()) unit.moveTo(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
		}
	}
	
	public void findPad(){
		if (unit.getHealth() < unit.getMaxHealth() || unit.getRulesParamFloat("noammo", 0.0f) > 0) {
			List<Float> params = new ArrayList<Float>();
			unit.executeCustomCommand(CMD_FIND_PAD, params, (short) 0, Integer.MAX_VALUE);
		}
	}
	
	public boolean targetMissing(){
		return (target.isDead || (distance(unit.getPos(), target.position) < 500f && target.unit.getPos() == null));
	}
	
	public Unit getUnit(){
		return unit;
	}
	
	public AIFloat3 getPos(){
		return unit.getPos();
	}
	
	public boolean isDead(){
		return unit.getHealth() <= 0 || unit.getTeam() != team;
	}
}
