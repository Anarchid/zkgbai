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

    public void clearTask(int frame){
        try{unit.stop((short) 0, frame);}
        catch(Exception e){}
        task = null;
    }

    public FighterTask getTask(){
        return task;
    }

    public void raid(Deque<AIFloat3> path, int frame){
        unit.stop((short) 0, frame);
        unit.fight(path.poll(), (short) 0, frame + 300); // skip first waypoint if target actually found to prevent stuttering

        if (path.isEmpty()){
            return; // pathing failed
        }else{
            unit.fight(path.poll(), (short) 0, frame + 300); // immediately move to first waypoint

            int pathSize = Math.min(5, path.size());
            for(int i=0;i<pathSize;i++){ // queue up to the first 5 waypoints
                unit.fight(path.poll(), OPTION_SHIFT_KEY, frame + 300); // queue the rest with shift.
                if(path.isEmpty()) break;
            }
        }
    }
    
    public void sneak(Deque<AIFloat3> path, int frame){
        unit.stop((short) 0, frame);
        unit.moveTo(path.poll(), (short) 0, frame + 300); // skip first waypoint if target actually found to prevent stuttering, otherwise use it.

        if (path.isEmpty()){
            return; // pathing failed
        }else{
            unit.moveTo(path.poll(), (short) 0, frame + 300); // immediately move to first waypoint

            int pathSize = Math.min(5, path.size());
            for(int i=0;i<pathSize;i++){ // queue up to the first 5 waypoints
                unit.moveTo(path.poll(), OPTION_SHIFT_KEY, frame+300); // queue the rest with shift.
                if(path.isEmpty()) break;
            }
        }
    }
}
