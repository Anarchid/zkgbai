package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.Weapon;
import zkgbai.ZKGraphBasedAI;
import zkgbai.kgbutil.Pathfinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class Coward {
	Unit unit;
	public int id;
	float retreatLevel;
	public boolean smallRaider;
	public boolean isRetreating = false;
	public boolean isScythe;
	public boolean isPlane;
	public boolean isFlyer;
	private boolean holdFire;
	private boolean isStrider;
	public Weapon shield = null;
	
	static int team;
	
	static Pathfinder pathfinder;
	
	private static final short OPTION_SHIFT_KEY = 32;
	private static final int CMD_FIND_PAD = 33411;
	
	public Coward(Unit u, float rlvl){
		unit = u;
		id = u.getUnitId();
		retreatLevel = rlvl;
		String defName = unit.getDef().getName();
		holdFire = defName.equals("hoverassault");
		isFlyer = unit.getDef().isAbleToFly();
		isStrider = defName.startsWith("strider") || defName.equals("gunshipkrow") || defName.equals("tankassault") || defName.equals("tankheavyassault");
		for (Weapon w:u.getWeapons()){
			if (w.getDef().getShield() != null && w.getDef().getShield().getPower() > 0){
				shield = w;
				break;
			}
		}
		if (pathfinder == null) {
			pathfinder = Pathfinder.getInstance();
			team = ZKGraphBasedAI.getInstance().teamID;
		}
	}
	
	public void retreatTo(AIFloat3 pos){
		Queue<AIFloat3> path = pathfinder.findPath(unit, pos, pathfinder.AVOID_ENEMIES);
		if (path.size() > 1 && isFlyer) path.poll();
		unit.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE);
		
		int pathSize = Math.min(3, path.size());
		int i = 0;
		while (i < pathSize && !path.isEmpty()) { // queue up to the first 3 waypoints
			// skip every other waypoint except the last, since they're not very far apart.
			if (path.size() > 1) path.poll();
			if (path.size() > 1 && isFlyer) path.poll(); // skip two out of three waypoints for flying units, since they move quickly.
			
			unit.moveTo(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
			i++;
		}
	}
	
	public void findPad(){
		List<Float> params = new ArrayList<Float>();
		unit.executeCustomCommand(CMD_FIND_PAD, params, (short) 0, Integer.MAX_VALUE);
	}
	
	public boolean shouldRetreat(){
		boolean retreat = (unit.getHealth()/unit.getMaxHealth() < retreatLevel || 1f - unit.getCaptureProgress() < retreatLevel ||
			                     (isStrider && 1f - (unit.getParalyzeDamage()/unit.getHealth()) < retreatLevel));
		if (retreat && holdFire) unit.setFireState(0, (short) 0, Integer.MAX_VALUE);
		isRetreating = retreat;
		return retreat;
	}
	
	public boolean isHealed(){
		boolean healed = (unit.getHealth() == unit.getMaxHealth() && unit.getCaptureProgress() == 0 && unit.getParalyzeDamage() == 0 &&
			                    (shield == null || shield.getShieldPower() > 0.5f * shield.getDef().getShield().getPower()));
		if (healed && holdFire) unit.setFireState(2, (short) 0, Integer.MAX_VALUE);
		isRetreating = healed;
		return healed;
	}
	
	public boolean checkShield(){
		boolean retreat = shield.getShieldPower() < 0.25f * shield.getDef().getShield().getPower();
		isRetreating = retreat;
		return retreat;
	}
	
	public boolean isDead(){
		return (unit.getHealth() <= 0 || unit.getTeam() != team);
	}
	
	public AIFloat3 getPos(){
		return unit.getPos();
	}
	
	public Unit getUnit(){
		return unit;
	}
	
	public boolean isCloaked(){
		return unit.isCloaked();
	}
}
