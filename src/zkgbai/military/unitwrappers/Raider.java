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
    public int lastTaskFrame;
    private AIFloat3 lastpos;
    public int index = 0;
    public boolean assigned = false;
    public RaiderSquad squad;

    public Raider(Unit u, float metal) {
        super(u, metal);
        this.task = null;
        this.lastTaskFrame = 0;
        this.lastpos = getPos();
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

    public void raid(AIFloat3 target, int frame) {
        Deque<AIFloat3> path = pathfinder.findPath(unit, getRadialPoint(target, 50f), pathfinder.RAIDER_PATH);
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

    public void sneak(AIFloat3 target, int frame) {
        Deque<AIFloat3> path = pathfinder.findPath(unit, getRadialPoint(target, 50f), pathfinder.RAIDER_PATH);
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
                unit.moveTo(unstickPoint, (short) 0, Integer.MAX_VALUE);
                clearTask(frame);
            }
            lastpos = unit.getPos();
        }
    }
}