package zkgbai.economy;

import com.springrts.ai.oo.AIFloat3;
import zkgbai.economy.tasks.WorkerTask;

import com.springrts.ai.oo.clb.Unit;

public class Worker {
	private Unit unit;
	private WorkerTask task;
	public int id;
	public boolean isChicken;
	public boolean isIdle;
	public int chickenFrame;
	AIFloat3 lastpos = null;
	int lastTaskFrame = 0;
	
	Worker(Unit unit){
		this.unit = unit;
		this.task = null;
		this.id = unit.getUnitId();
		this.isChicken = false;
		this.isIdle = true;
		this.chickenFrame = 0;
		this.lastpos = unit.getPos();
	}
	
	public void setTask(WorkerTask task, int frame){
		this.task = task;
		this.isIdle = false;
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

	public void clearTask(int frame){
		this.task = null;
		this.isIdle = true;
		try {
			unit.stop((short) 0, frame+30);
		}catch (Exception e){

		}
	}

	public void unstick(int frame){
		if (task != null && frame - lastTaskFrame > 150){
			float movedist = distance(unit.getPos(), lastpos);
			float jobdist = distance(unit.getPos(), task.getPos());
			if (movedist < 50 && jobdist > unit.getDef().getBuildDistance()+20){
				AIFloat3 unstickPoint = getRadialPoint(unit.getPos(), 75f);
				unit.moveTo(unstickPoint, (short) 0, frame+60);
				clearTask(frame);
			}
			lastTaskFrame = frame;
			lastpos = unit.getPos();
		}
	}

	protected float distance( AIFloat3 pos1,  AIFloat3 pos2){
		float x1 = pos1.x;
		float z1 = pos1.z;
		float x2 = pos2.x;
		float z2 = pos2.z;
		return (float) Math.sqrt((x1-x2)*(x1-x2)+(z1-z2)*(z1-z2));
	}

	protected AIFloat3 getRadialPoint(AIFloat3 position, Float radius){
		// returns a random point lying on a circle around the given position.
		AIFloat3 pos = new AIFloat3();
		double angle = Math.random()*2*Math.PI;
		double vx = Math.cos(angle);
		double vz = Math.sin(angle);
		pos.x = (float) (position.x + radius*vx);
		pos.z = (float) (position.z + radius*vz);
		return pos;
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
