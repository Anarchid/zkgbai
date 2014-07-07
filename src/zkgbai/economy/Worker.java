package zkgbai.economy;

import zkgbai.economy.tasks.WorkerTask;

import com.springrts.ai.oo.clb.Unit;

public class Worker {
	private Unit unit;
	private WorkerTask task;
	
	Worker(Unit unit){
		this.unit = unit;
		this.task = new WorkerTask(this);
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
}
