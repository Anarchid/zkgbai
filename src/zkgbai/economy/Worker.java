package zkgbai.economy;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.*;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.tasks.ConstructionTask;
import zkgbai.economy.tasks.RepairTask;
import zkgbai.economy.tasks.WorkerTask;
import static zkgbai.kgbutil.KgbUtil.*;

import zkgbai.kgbutil.Pathfinder;

import java.util.Map;
import java.util.Queue;

public class Worker {
	private Unit unit;
	private WorkerTask task;
	public int id;
	public boolean isCom = false;
	public boolean assignedPlop = false;
	public boolean isGreedy = false;
	public float power = 0f;
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
	static int team;
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
		
		if (unit.getDef().getName().contains("dyn")){
			power = 155f;
		}else {
			for (WeaponMount w : unit.getDef().getWeaponMounts()) {
				WeaponDef wd = w.getWeaponDef();
				if (!wd.isWaterWeapon() && !wd.getName().contains("fake")) {
					// take the max between burst damage and continuous dps as weapon power.
					float wepShot = wd.getDamage().getTypes().get(0) * wd.getProjectilesPerShot() * wd.getSalvoSize();
					for (Map.Entry<String, String> param : wd.getCustomParams().entrySet()) {
						if (param.getKey().equals("extra_damage")) wepShot += Float.parseFloat(param.getValue()); // emp
						if (param.getKey().equals("impulse"))
							wepShot += Float.parseFloat(param.getValue()) / 10f; // impulse
						if (param.getKey().equals("timeslow_damagefactor"))
							wepShot += (wepShot * Float.parseFloat(param.getValue())) / 2f; // slow
						if (param.getKey().equals("disarmDamageOnly")) wepShot /= 10f;
						if (param.getKey().equals("setunitsonfire")) wepShot *= 2f;
					}
					wepShot *= 1f + wd.getAreaOfEffect() / 100f;
					power += Math.max(wepShot, wepShot / wd.getReload());
				}
			}
		}
		
		if (pathfinder == null){
			pathfinder = Pathfinder.getInstance();
			team = ZKGraphBasedAI.getInstance().teamID;
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
		if (unit.getHealth() > 0 && unit.getTeam() == team) {
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

		if ((getSpeed(unit) < unit.getDef().getSpeed()/10f && jobdist > unit.getDef().getBuildDistance() * 1.1f) || unit.getCurrentCommands().isEmpty()){
			AIFloat3 unstickPoint = getAngularPoint(task.getPos(), unit.getPos(), distance(task.getPos(), unit.getPos()) + 150f);
			unit.moveTo(unstickPoint, (short) 0, Integer.MAX_VALUE);
			clearTask();
			unstuck = true;
			lastStuckFrame = frame;
		}
		lastTaskFrame = frame;
		
		return unstuck;
	}
	
	public void moveTo(AIFloat3 pos){
		Queue<AIFloat3> path = pathfinder.findPath(unit, pos, pathfinder.AVOID_ENEMIES);
		if (distance(pos, path.peek()) >= unit.getDef().getBuildDistance()){
			unit.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE); // skip first few waypoints if target actually found to prevent stuttering, otherwise use the first waypoint.
		}else{
			unit.moveTo(getDirectionalPoint(pos, unit.getPos(), unit.getDef().getBuildDistance() * 0.95f), (short) 0, Integer.MAX_VALUE);
			return;
		}
		
		int i = 0;
		while (!path.isEmpty() && distance(pos, path.peek()) >= unit.getDef().getBuildDistance() && i < 2) {
			unit.moveTo(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
			i++;
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
	
	public boolean isWorkingOnPorc(){
		if (task == null) return false;
		UnitDef ud = null;
		if (task instanceof ConstructionTask){
			ConstructionTask ctask = (ConstructionTask) task;
			ud = ctask.buildType;
		}else if (task instanceof RepairTask){
			RepairTask rt = (RepairTask) task;
			ud = rt.target.getDef();
		}
		return (ud != null && ud.getSpeed() == 0 && ud.isAbleToAttack());
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
