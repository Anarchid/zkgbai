package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.Weapon;
import zkgbai.ZKGraphBasedAI;
import zkgbai.kgbutil.Pathfinder;
import zkgbai.military.RadarDef;
import zkgbai.military.tasks.ScoutTask;

import java.util.Queue;
import static zkgbai.kgbutil.KgbUtil.*;


public class Raider extends Fighter {
	private ScoutTask task;
	public int index = 0;
	public int lastTaskFrame = 0;
	public int lastRotationFrame = 0;
	int stuck = 0;
	public RaiderSquad squad;

	public Raider(Unit u, float metal) {
		super(u, metal);
		this.task = null;
	}

	public void setTask(ScoutTask t) {
		task = t;
	}

	public void clearTask() {
		if (task != null) task.removeRaider(this);
		if (unit.getHealth() > 0 && unit.getTeam() == team) unit.stop((short) 0, Integer.MAX_VALUE);
		task = null;
	}

	public void endTask() {
		if (unit.getHealth() > 0 && unit.getTeam() == team) unit.stop((short) 0, Integer.MAX_VALUE);
		task = null;
	}

	public ScoutTask getTask() {
		return task;
	}

	public Queue<AIFloat3> getRaidPath(AIFloat3 pos){
		return pathfinder.findPath(unit, pos, unit.isCloaked() ? pathfinder.SCYTHE_PATH : pathfinder.RAIDER_PATH);
	}
	
	public void raid(AIFloat3 target, int frame) {
		Queue<AIFloat3> path = pathfinder.findPath(unit, getRadialPoint(target, 100f), unit.isCloaked() ? pathfinder.SCYTHE_PATH : pathfinder.RAIDER_PATH);
		if (path.size() > 1) path.poll();
		unit.fight(path.poll(), (short) 0, Integer.MAX_VALUE);
		if (!path.isEmpty()) unit.fight(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
		
		this.target = target;
	}

	public void sneak(AIFloat3 target, int frame) {
		Queue<AIFloat3> path = pathfinder.findPath(unit, getRadialPoint(target, 100f), unit.isCloaked() ? pathfinder.SCYTHE_PATH : pathfinder.RAIDER_PATH);
		if (path.size() > 1) path.poll();
		unit.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE);
		if (!path.isEmpty()) unit.moveTo(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
		
		this.target = target;
	}
	
	public boolean isReloading(int frame){
		int lastWepFrame = 0;
		for (Weapon w:unit.getWeapons()){
			int reload = w.getReloadFrame();
			if (reload > lastWepFrame) lastWepFrame = reload;
		}
		return lastWepFrame > frame;
	}
	
	public boolean unstick(int frame) {
		if (unit.getHealth() <= 0) return false;
		
		float speed = getSpeed(unit);
		int lastWepFrame = 0;
		for (Weapon w:unit.getWeapons()){
			int reload = w.getReloadFrame();
			if (reload > lastWepFrame) lastWepFrame = reload;
		}
		
		if (lastWepFrame < frame - 30 && speed < unit.getDef().getSpeed()/10f && !unit.isParalyzed()) {
			stuck++;
			if (stuck == 15) return true;
			clearTask();
			unit.moveTo(getRadialPoint(unit.getPos(), 100f), (short) 0, Integer.MAX_VALUE);
		}else{
			stuck = Math.max(0, stuck - 2);
		}
		return false;
	}
}