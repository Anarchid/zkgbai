package zkgbai.economy;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Weapon;
import zkgbai.economy.tasks.WorkerTask;
import static zkgbai.kgbutil.KgbUtil.*;

import com.springrts.ai.oo.clb.Unit;
import zkgbai.kgbutil.Pathfinder;

import java.util.Queue;

public class Worker {
	private Unit unit;
	private WorkerTask task;
	public int id;
	public boolean isCom = false;
	public boolean assignedPlop = false;
	public boolean isGreedy = false;
	public float bp;
	public int buildRange;
	public boolean hasShields = false;
	private Weapon shield;
	public boolean isChicken;
	public int chickenFrame;
	public int lastRetreatFrame;
	int lastTaskFrame = 0;
	int lastStuckFrame = Integer.MIN_VALUE/2;
	static Pathfinder pathfinder = null;
	protected static final short OPTION_SHIFT_KEY = (1 << 5);
	
	Worker(Unit unit){
		this.unit = unit;
		this.task = null;
		this.id = unit.getUnitId();
		this.isChicken = false;
		this.chickenFrame = 0;
		this.bp = unit.getDef().getBuildSpeed();
		this.buildRange = Math.round(unit.getDef().getBuildDistance()/64f);

		for (Weapon w: unit.getWeapons()){
			if (w.getDef().getShield() != null && w.getDef().getShield().getPower() > 0){
				shield = w;
				hasShields = true;
			}
		}
		
		if (pathfinder == null){
			pathfinder = Pathfinder.getInstance();
		}
	}
	
	public void setTask(WorkerTask task, int frame){
		this.task = task;
		task.addWorker(this);
		lastTaskFrame = frame;
	}
	
	public WorkerTask getTask(){
		return task;
	}
	
	public Unit getUnit(){
		return unit;
	}

	public AIFloat3 getPos(){
		return unit.getPos();
	}

	public void clearTask(){
		if (task != null) task.removeWorker(this);
		this.task = null;
		if (unit.getHealth() > 0) {
			unit.stop((short) 0, Integer.MAX_VALUE);
		}
	}
	
	public void endTask(){
		this.task = null;
		if (unit.getHealth() > 0) {
			unit.stop((short) 0, Integer.MAX_VALUE);
		}
	}

	public boolean unstick(int frame, Resource m, Resource e){
		boolean unstuck = false;
		if (frame - lastStuckFrame < 60) return true;
		if (task == null || frame - lastTaskFrame < 120) return false;
		
		float jobdist = distance(unit.getPos(), task.getPos());

		if ((getSpeed(unit) < unit.getDef().getSpeed()/10f && (jobdist > unit.getDef().getBuildDistance() || (unit.getResourceUse(m) == 0f && unit.getResourceUse(e) == 0f && unit.getResourceMake(m) == 0f)))
			      || unit.getCurrentCommands().isEmpty()){
			AIFloat3 unstickPoint = getAngularPoint(task.getPos(), unit.getPos(), distance(task.getPos(), unit.getPos()) + 50f);
			unit.moveTo(unstickPoint, (short) 0, Integer.MAX_VALUE);
			clearTask();
			unstuck = true;
			lastStuckFrame = frame;
		}
		lastTaskFrame = frame;
		
		return unstuck;
	}
	
	public void moveTo(AIFloat3 pos){
		Queue<AIFloat3> path = pathfinder.findPath(unit, getDirectionalPoint(pos, unit.getPos(), unit.getDef().getBuildDistance()), pathfinder.AVOID_ENEMIES);
		
		unit.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE); // skip first few waypoints if target actually found to prevent stuttering, otherwise use the first waypoint.
		while (!path.isEmpty()) {
			if (path.size() > 1) path.poll();
			if (!path.isEmpty()) unit.moveTo(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
		}
	}
	
	public void retreatTo(AIFloat3 pos, int frame){
		lastRetreatFrame = frame;
		Queue<AIFloat3> path = pathfinder.findPath(unit, pos, pathfinder.AVOID_ENEMIES);
		
		unit.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE); // skip first few waypoints if target actually found to prevent stuttering, otherwise use the first waypoint.
		while (!path.isEmpty()) {
			if (path.size() > 1) path.poll();
			if (!path.isEmpty()) unit.moveTo(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
		}
	}
	
	public boolean canReach(AIFloat3 pos){
		return pathfinder.isWorkerReachable(unit, pos);
	}

	public float getShields(){
		if (!hasShields) return 1f;
		return shield.getShieldPower()/shield.getDef().getShield().getPower();
	}
	
	public boolean hasPlop(){
		return unit.getRulesParamFloat("facplop", 0f) == 1f;
	}

	@Override
	public boolean equals(Object o){
		if (o instanceof Worker){
			Worker w = (Worker) o;
			return (w.id == this.id);
		}
		return false;
	}
}
