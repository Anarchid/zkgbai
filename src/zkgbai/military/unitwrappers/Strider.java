package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.Weapon;

import java.util.Deque;

import static zkgbai.kgbutil.KgbUtil.*;

/**
 * Created by aeonios on 11/14/2015.
 */
public class Strider extends Fighter {
    public Strider(Unit u, Float metal){
        super(u, metal);
    }
    
    public boolean isDgunReady(int frame){
        for (Weapon w:unit.getWeapons()){
            if (w.getDef().isManualFire()){
                if (w.getReloadFrame() <= frame){
                    return true;
                }
                break;
            }
        }
        return false;
    }

    @Override
    public void fightTo(AIFloat3 pos, int frame){
        AIFloat3 target;
        if (unit.getDef().getName().equals("striderscorpion")) {
            target = getDirectionalPoint(pos, getPos(), 250f);
        }else if (unit.getDef().getName().equals("striderbantha") || unit.getDef().getName().equals("striderdetriment")) {
            target = getDirectionalPoint(pos, getPos(), 350f);
        }else{ // dante
            target = getDirectionalPoint(pos, getPos(), 50f);
        }
    
        Deque<AIFloat3> path = pathfinder.findPath(unit, target, pathfinder.ASSAULT_PATH);
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
                // skip every two of three waypoints except the last, since they're not very far apart.
                if (path.size() > 2) {
                    path.poll();
                    path.poll();
                }
            }
        }
    }
}
