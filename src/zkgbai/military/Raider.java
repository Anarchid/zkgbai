package zkgbai.military;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.military.tasks.FighterTask;
import zkgbai.military.tasks.ScoutTask;

import java.util.Deque;


public class Raider extends Fighter {
    private ScoutTask task;
    public boolean scouting = true;
    public Raider(Unit u, float metal) {
        super(u, metal);
        this.task = null;
    }

    public void setTask(ScoutTask t){
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

    public ScoutTask getTask(){
        return task;
    }

    public void scout(Deque<AIFloat3> path, int frame){
        unit.stop((short) 0, frame);
        unit.setMoveState(2, (short) 0, frame + 10); // set to maneuver
        unit.fight(path.poll(), (short) 0, frame + 300); // skip first waypoint if target actually found to prevent stuttering

        if (path.isEmpty()){
            return; // pathing failed
        }else{
            unit.fight(path.poll(), (short) 0, frame + 300); // immediately move to first waypoint

            int pathSize = Math.min(5, path.size());
            int i = 0;
            while (i<pathSize && !path.isEmpty()){ // queue up to the first 5 waypoints
                unit.fight(path.poll(), OPTION_SHIFT_KEY, frame + 300); // queue the rest with shift.
                i++;
                // skip every other waypoint except the last, since they're not very far apart.
                if (path.size() > 1){
                    path.poll();
                }
            }
        }
    }

    public void raid(Deque<AIFloat3> path, int frame){
        scouting = false;
        unit.stop((short) 0, frame);
        unit.setMoveState(1, (short) 0, frame + 10); // set to maneuver
        unit.fight(path.poll(), (short) 0, frame + 300); // skip first waypoint if target actually found to prevent stuttering

        if (path.isEmpty()){
            return; // pathing failed
        }else{
            unit.fight(path.poll(), (short) 0, frame + 300); // immediately move to first waypoint

            int pathSize = Math.min(5, path.size());
            int i = 0;
            while (i<pathSize && !path.isEmpty()){ // queue up to the first 5 waypoints
                unit.fight(path.poll(), OPTION_SHIFT_KEY, frame + 300); // queue the rest with shift.
                i++;
                // skip every other waypoint except the last, since they're not very far apart.
                if (path.size() > 1){
                    path.poll();
                }
            }
        }
    }
    
    public void sneak(Deque<AIFloat3> path, int frame){
        scouting = false;
        unit.stop((short) 0, frame);
        unit.setMoveState(2, (short) 0, frame + 10); // set to maneuver
        unit.moveTo(path.poll(), (short) 0, frame + 300); // skip first waypoint if target actually found to prevent stuttering, otherwise use it.

        if (path.isEmpty()){
            return; // pathing failed
        }else{
            unit.moveTo(path.poll(), (short) 0, frame + 300); // immediately move to first waypoint

            int pathSize = Math.min(5, path.size());
            for(int i=0;i<pathSize;i++){ // queue up to the first 5 waypoints
                unit.moveTo(path.poll(), OPTION_SHIFT_KEY, frame+300); // queue the rest with shift.
            }
        }
    }

    @Override
    public void fightTo(AIFloat3 pos, int frame){
        scouting = true;
        unit.setMoveState(2, (short) 0, frame + 10);
        super.fightTo(pos, frame);
    }
}
