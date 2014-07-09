package zkgbai.economy.tasks;

import zkgbai.economy.Worker;


public class WorkerTask {
	public int priority = 0;
	boolean completed = true;
	Worker worker;
	
	public WorkerTask(Worker worker){
		this.worker = worker;
	}
	
	
	@Override
	public String toString(){
		return this.worker.getUnit().getDef().getName()+" to get a fresh assignment";
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof WorkerTask){
			WorkerTask wt = (WorkerTask)other;
			return (worker.getUnit().getUnitId() == wt.getWorker().getUnit().getUnitId());
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return worker.getUnit().getUnitId()*43;
	}
	
	public boolean isCompleted(){
		return completed;
	}
	
	public void setCompleted(){
		completed = true;
	}
	
	public Worker getWorker(){
		return worker;
	}

}