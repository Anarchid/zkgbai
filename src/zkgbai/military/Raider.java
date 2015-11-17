package zkgbai.military;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.military.tasks.FighterTask;


public class Raider extends Fighter {
    private FighterTask task;
    public Raider(Unit u, float metal) {
        super(u, metal);
        this.task = null;
    }

    public void setTask(FighterTask t){
        task = t;
    }

    public void clearTask(){
        task = null;
    }

    public FighterTask getTask(){
        return task;
    }

    public void raid(AIFloat3 pos, int frame){
        unit.fight(pos, (short) 0, frame);
    }
}
