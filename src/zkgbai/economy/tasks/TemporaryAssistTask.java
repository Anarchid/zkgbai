package zkgbai.economy.tasks;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;

import zkgbai.economy.Worker;

public class TemporaryAssistTask extends WorkerTask{
	AIFloat3 target;
	int timeoutFrame = 0;
	public TemporaryAssistTask(Worker worker, AIFloat3 target, int timeoutFrame) {
		super(worker);
		this.timeoutFrame = timeoutFrame;
		this.target = target;
	}
	
	public int getTimeout(){
		return timeoutFrame;
	}
	
}