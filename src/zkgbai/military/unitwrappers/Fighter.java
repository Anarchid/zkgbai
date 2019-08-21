package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.kgbutil.Pathfinder;

import java.util.Deque;

import static zkgbai.kgbutil.KgbUtil.*;


public class Fighter {
    public float metalValue;
    public int id;
    public int index;
    public Squad squad;
    protected Unit unit;
    static Pathfinder pathfinder = null;
    protected static final short OPTION_SHIFT_KEY = (1 << 5); //  32

    public Fighter(Unit u, float metal){
        this.unit = u;
        this.id = u.getUnitId();
        this.metalValue = metal;

        if (pathfinder == null){
            pathfinder = Pathfinder.getInstance();
        }
    }

    public Unit getUnit(){
        return unit;
    }

    public AIFloat3 getPos(){
        return unit.getPos();
    }

    public void fightTo(AIFloat3 pos){
        Deque<AIFloat3> path = pathfinder.findPath(unit, getRadialPoint(pos, 200f), pathfinder.ASSAULT_PATH);
        unit.fight(path.poll(), (short) 0, Integer.MAX_VALUE); // skip first few waypoints if target actually found to prevent stuttering, otherwise use the first waypoint.
        if (path.size() > 2){
            path.poll();
            path.poll();
        }


        if (path.isEmpty()) {
            return; // pathing failed
        } else {
            unit.fight(path.poll(), (short) 0, Integer.MAX_VALUE); // immediately move to first waypoint

            int pathSize = Math.min(5, path.size());
            int i = 0;
            while (i < pathSize && !path.isEmpty()) { // queue up to the first 5 waypoints
                unit.fight(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
                i++;
                // skip every two of three waypoints except the last, since they're not very far apart.
                if (path.size() > 2) {
                    path.poll();
                    path.poll();
                }
            }
        }
    }

    public void moveTo(AIFloat3 pos){
        Deque<AIFloat3> path = pathfinder.findPath(unit, getRadialPoint(pos, 100f), pathfinder.AVOID_ENEMIES);

        unit.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE); // skip first few waypoints if target actually found to prevent stuttering, otherwise use the first waypoint.
        if (path.size() > 2){
            path.poll();
            path.poll();
        }

        if (path.isEmpty()) {
            return; // pathing failed
        } else {
            unit.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE); // immediately move to first waypoint

            int pathSize = Math.min(5, path.size());
            int i = 0;
            while (i < pathSize && !path.isEmpty()) { // queue up to the first 5 waypoints
                unit.moveTo(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
                i++;
                // skip every two of three waypoints except the last, since they're not very far apart.
                if (path.size() > 2) {
                    path.poll();
                    path.poll();
                }
            }
        }
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof Fighter){
            Fighter f = (Fighter) o;
            return (f.id == id);
        }
        return false;
    }
}
