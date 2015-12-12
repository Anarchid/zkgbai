package zkgbai.military;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.military.tasks.FighterTask;
import zkgbai.military.tasks.ScoutTask;

import java.util.Deque;


public class Raider extends Fighter {
    private ScoutTask task;
    public boolean scouting = true;
    private int lastTaskFrame;
    private AIFloat3 lastpos;

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

    public void raid(Deque<AIFloat3> path, int frame) {
        scouting = false;
        unit.stop((short) 0, frame);
        unit.setMoveState(1, (short) 0, frame + 30); // set to maneuver
        unit.fight(path.poll(), (short) 0, frame + 3000); // skip first waypoint if target actually found to prevent stuttering


        if (path.isEmpty()) {
            return; // pathing failed
        } else {
            unit.fight(path.poll(), (short) 0, frame + 3000); // skip first waypoint if target actually found to prevent stuttering

            int pathSize = Math.min(5, path.size());
            int i = 0;
            while (i < pathSize && !path.isEmpty()) { // queue up to the first 5 waypoints
                unit.fight(path.poll(), OPTION_SHIFT_KEY, frame + 3000); // skip first waypoint if target actually found to prevent stuttering
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
        unit.moveTo(path.poll(), (short) 0, frame + 3000); // skip first waypoint if target actually found to prevent stuttering, otherwise use it.

        if (path.isEmpty()) {
            return; // pathing failed
        } else {
            unit.moveTo(path.poll(), (short) 0, frame + 3000); // immediately move to first waypoint

            int pathSize = Math.min(5, path.size());
            for (int i = 0; i < pathSize; i++) { // queue up to the first 5 waypoints
                unit.moveTo(path.poll(), OPTION_SHIFT_KEY, frame + 3000); // queue the rest with shift.
            }
        }
        lastTaskFrame = frame;
        lastpos = unit.getPos();
    }

    public void unstick(int frame) {
        if (task != null && frame - lastTaskFrame > 90) {
            float movedist = distance(unit.getPos(), lastpos);
            float jobdist = distance(unit.getPos(), task.spot.getPos());
            if (movedist < 50 && jobdist > 100) {
                AIFloat3 unstickPoint = getRadialPoint(unit.getPos(), 450f);
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

    protected float distance(AIFloat3 pos1, AIFloat3 pos2) {
        float x1 = pos1.x;
        float z1 = pos1.z;
        float x2 = pos2.x;
        float z2 = pos2.z;
        return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (z1 - z2) * (z1 - z2));
    }
}