package zkgbai.economy;

import com.springrts.ai.oo.AIFloat3;
import zkgbai.economy.tasks.WorkerTask;
import static zkgbai.kgbutil.KgbUtil.*;

import com.springrts.ai.oo.clb.Unit;
import zkgbai.kgbutil.Pathfinder;

import java.util.Deque;

public class Worker {
	private Unit unit;
	private WorkerTask task;
	public int id;
	public boolean isChicken;
	public int chickenFrame;
	AIFloat3 lastpos = null;
	int lastTaskFrame = 0;
	static Pathfinder pathfinder = null;
	protected static final short OPTION_SHIFT_KEY = (1 << 5);
	
	Worker(Unit unit){
		this.unit = unit;
		this.task = null;
		this.id = unit.getUnitId();
		this.isChicken = false;
		this.chickenFrame = 0;
		this.lastpos = unit.getPos();
		
		if (pathfinder == null){
			pathfinder = Pathfinder.getInstance();
		}
	}
	
	public void setTask(WorkerTask task, int frame){
		this.task = task;
		this.lastpos = unit.getPos();
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
		this.task = null;
		if (unit.getHealth() > 0) {
			unit.stop((short) 0, Integer.MAX_VALUE);
		}
	}

	public boolean unstick(int frame){
		boolean unstuck = false;
		if (task != null && frame - lastTaskFrame > 150){
			float movedist = distance(unit.getPos(), lastpos);
			float jobdist = distance(unit.getPos(), task.getPos());

			if (movedist < 50 && jobdist > unit.getDef().getBuildDistance()){
				AIFloat3 unstickPoint = getRadialPoint(unit.getPos(), 75f);
				unit.moveTo(unstickPoint, (short) 0, frame+6000);
				task.removeWorker(this);
				clearTask();
				unstuck = true;
			}
			lastTaskFrame = frame;
			lastpos = unit.getPos();
		}
		return unstuck;
	}
	
	public void moveTo(AIFloat3 pos){
		Deque<AIFloat3> path = pathfinder.findPath(unit, getDirectionalPoint(pos, unit.getPos(), unit.getDef().getBuildDistance()), pathfinder.AVOID_ENEMIES);
		unit.stop((short) 0, Integer.MAX_VALUE);
		
		unit.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE); // skip first few waypoints if target actually found to prevent stuttering, otherwise use the first waypoint.
		if (path.size() > 2){
			path.poll();
			path.poll();
		}
		
		if (path.isEmpty()) {
			return; // pathing failed
		} else {
			unit.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE); // immediately move to first waypoint
			
			int pathSize = Math.min(5, path.size());
			int i = 0;
			while (i < pathSize && !path.isEmpty()) { // queue up to the first 5 waypoints
				unit.moveTo(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
				i++;
				// skip every two of three waypoints except the last, since they're not very far apart.
				if (path.size() > 2) {
					path.poll();
					path.poll();
				}
			}
		}
	}
	
	public void retreatTo(AIFloat3 pos){
		Deque<AIFloat3> path = pathfinder.findPath(unit, pos, pathfinder.AVOID_ENEMIES);
		unit.stop((short) 0, Integer.MAX_VALUE);
		
		unit.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE); // skip first few waypoints if target actually found to prevent stuttering, otherwise use the first waypoint.
		if (path.size() > 2){
			path.poll();
			path.poll();
		}
		
		if (path.isEmpty()) {
			return; // pathing failed
		} else {
			unit.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE); // immediately move to first waypoint
			
			int pathSize = Math.min(5, path.size());
			int i = 0;
			while (!path.isEmpty()) {
				unit.moveTo(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
				i++;
				// skip every two of three waypoints except the last, since they're not very far apart.
				if (path.size() > 2) {
					path.poll();
					path.poll();
				}
			}
		}
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
