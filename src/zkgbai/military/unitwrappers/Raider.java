package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.ZKGraphBasedAI;
import zkgbai.kgbutil.Pathfinder;
import zkgbai.military.RadarDef;
import zkgbai.military.tasks.ScoutTask;

import java.util.Deque;
import static zkgbai.kgbutil.KgbUtil.*;


public class Raider extends Fighter {
    private ScoutTask task;
    public boolean scouting = true;
    private int lastTaskFrame;
    private AIFloat3 lastpos;
    private static Pathfinder pathfinder = null;

    public Raider(Unit u, float metal) {
        super(u, metal);
        this.task = null;
        this.lastTaskFrame = 0;
        this.lastpos = getPos();

        if (pathfinder == null){
            pathfinder = Pathfinder.getInstance();
        }
    }

    public void setTask(ScoutTask t) {
        task = t;
    }

    public void clearTask() {
        task = null;
    }

    public void clearTask(int frame) {
        try {
            unit.stop((short) 0, frame);
        } catch (Exception e) {}
        task = null;
    }

    public ScoutTask getTask() {
        return task;
    }

    public void raid(Deque<AIFloat3> path, int frame) {
        scouting = false;
        unit.stop((short) 0, frame);
        unit.setMoveState(1, (short) 0, frame + 30); // set to maneuver
        unit.fight(path.poll(), (short) 0, frame + 3000); // skip first few waypoints if target actually found to prevent stuttering, otherwise use the first waypoint.
        if (path.size() > 2){
            path.poll();
            path.poll();
        }


        if (path.isEmpty()) {
            return; // pathing failed
        } else {
            unit.fight(path.poll(), (short) 0, frame + 3000); // immediately move to first waypoint

            int pathSize = Math.min(5, path.size());
            int i = 0;
            while (i < pathSize && !path.isEmpty()) { // queue up to the first 5 waypoints
                unit.fight(path.poll(), OPTION_SHIFT_KEY, frame + 3000);
                i++;
                // skip every other waypoint except the last, since they're not very far apart.
                if (path.size() > 1) {
                    path.poll();
                }
            }
        }
        lastTaskFrame = frame;
        lastpos = unit.getPos();
    }

    public void sneak(Deque<AIFloat3> path, int frame) {
        scouting = false;
        unit.stop((short) 0, frame);
        unit.setMoveState(2, (short) 0, frame + 10); // set to maneuver
        lastTaskFrame = frame;
        lastpos = unit.getPos();

        unit.moveTo(path.poll(), (short) 0, frame + 3000); // skip first few waypoints if target actually found to prevent stuttering, otherwise use the first waypoint.
        if (path.size() > 2){
            path.poll();
            path.poll();
        }

        if (path.isEmpty()) {
            return; // pathing failed
        } else {
            unit.moveTo(path.poll(), (short) 0, frame + 3000); // immediately move to first waypoint

            int pathSize = Math.min(5, path.size());
            int i = 0;
            while (i < pathSize && !path.isEmpty()) { // queue up to the first 5 waypoints
                unit.moveTo(path.poll(), OPTION_SHIFT_KEY, frame + 3000);
                i++;
                // skip every other waypoint except the last, since they're not very far apart.
                if (path.size() > 1) {
                    path.poll();
                }
            }
        }
    }

    public void unstick(int frame) {
        if (task != null && frame - lastTaskFrame > 90) {
            float movedist = distance(unit.getPos(), lastpos);
            float jobdist = distance(unit.getPos(), task.spot.getPos());
            if (movedist < 150 && jobdist > 100) {
                AIFloat3 unstickPoint = getRadialPoint(unit.getPos(), 350f);
                unit.moveTo(unstickPoint, (short) 0, frame + 6000);
                clearTask(frame);
            }
            lastpos = unit.getPos();
        }
    }

    @Override
    public void fightTo(AIFloat3 pos, int frame) {
        scouting = true;
        unit.setMoveState(2, (short) 0, frame + 10);
        lastTaskFrame = frame;
        lastpos = unit.getPos();
        super.fightTo(pos, frame);
    }
}