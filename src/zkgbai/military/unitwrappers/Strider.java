package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.Weapon;

import java.util.Queue;

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
    public void fightTo(AIFloat3 pos){
        AIFloat3 target;
        if (unit.getDef().getName().equals("striderscorpion")) {
            target = getDirectionalPoint(pos, getPos(), 250f);
        }else if (unit.getDef().getName().equals("striderbantha") || unit.getDef().getName().equals("striderdetriment")) {
            target = getDirectionalPoint(pos, getPos(), 350f);
        }else{ // dante
            target = getDirectionalPoint(pos, getPos(), 50f);
        }
    
        Queue<AIFloat3> path = pathfinder.findPath(unit, target, pathfinder.STRIDER_PATH);
        unit.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE);
        
        if (path.size() > 1) path.poll(); // skip every other waypoint except the last, since they're not very far apart.
        if (!path.isEmpty()) unit.moveTo(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
    }
}
