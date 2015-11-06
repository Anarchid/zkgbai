package zkgbai.economy;

import com.springrts.ai.oo.AIFloat3;
import zkgbai.economy.tasks.WorkerTask;

import com.springrts.ai.oo.clb.Unit;

public class Worker {
	private Unit unit;
	private WorkerTask task;
	public int id;
	public boolean isChicken;
	
	Worker(Unit unit){
		this.unit = unit;
		this.task = null;
		this.id = unit.getUnitId();
		this.isChicken = false;
	}
	
	public void setTask(WorkerTask task){
		this.task = task;
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
