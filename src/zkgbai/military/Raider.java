package zkgbai.military;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.military.tasks.FighterTask;

import java.util.Deque;


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

    public void raid(Deque<AIFloat3> path, int frame){
        unit.stop((short) 0, frame);
        AIFloat3 target = path.poll(); // skip first waypoint if target actually found to prevent stuttering

        if (path.isEmpty()){
            fightTo(target, frame);
        }else{
            unit.fight(path.poll(), (short) 0, frame + 300); // immediately move to first waypoint

            while(!path.isEmpty()){
                unit.fight(path.poll(), OPTION_SHIFT_KEY, frame+300); // queue the rest with shift.
            }
        }
    }
}
