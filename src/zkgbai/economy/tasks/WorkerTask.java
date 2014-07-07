package zkgbai.economy.tasks;

import zkgbai.economy.Worker;


public class WorkerTask {
	public int priority = 0;
	boolean completed = true;
	Worker worker;
	
	public WorkerTask(Worker worker){
		this.worker = worker;
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