package zkgbai.economy.tasks;

import com.springrts.ai.oo.AIFloat3;
import zkgbai.economy.Worker;
import java.util.ArrayList;
import java.util.List;


public class WorkerTask {
	public List<Worker> assignedWorkers;
	protected AIFloat3 position;

	public WorkerTask() {
		this.assignedWorkers = new ArrayList<Worker>();
		this.position = new AIFloat3();
	}

	public AIFloat3 getPos(){
		return this.position;
	}

	public void addWorker(Worker w){
		this.assignedWorkers.add(w);
	}

	public void removeWorker(Worker w){
		this.assignedWorkers.remove(w);
	}

	public List<Worker> stopWorkers(){
		for (Worker w: assignedWorkers){
			w.clearTask();
		}
		return assignedWorkers;
	}
}