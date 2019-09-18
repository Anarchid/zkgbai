package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.Weapon;
import zkgbai.kgbutil.Pathfinder;

import java.util.Queue;

import static zkgbai.kgbutil.KgbUtil.*;


public class Fighter {
    public float metalValue;
    public int id;
    public int index;
    int stuck = 0;
    public AIFloat3 target;
    public Squad squad;
    public boolean shieldMob = false;
    public boolean outOfRange = false;
    protected Unit unit;
    static Pathfinder pathfinder = null;
    protected static final short OPTION_SHIFT_KEY = 32; //  32

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
    
    public Queue<AIFloat3> getFightPath(AIFloat3 pos){
        return pathfinder.findPath(unit, pos, pathfinder.ASSAULT_PATH);
    }

    public void fightTo(AIFloat3 pos){
        Queue<AIFloat3> path = pathfinder.findPath(unit, getRadialPoint(pos, 150f), pathfinder.ASSAULT_PATH);
        unit.fight(path.poll(), (short) 0, Integer.MAX_VALUE);
        
        // Add one extra waypoint
        if (path.size() > 1) path.poll(); // skip every other waypoint since they're close together.
        if (!path.isEmpty()) unit.fight(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
    }

    public void moveTo(AIFloat3 pos){
        Queue<AIFloat3> path = pathfinder.findPath(unit, getRadialPoint(pos, 100f), pathfinder.AVOID_ENEMIES);
    
        if (unit.getDef().isAbleToFly() && path.size() > 1) path.poll();
        unit.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE);
    
        // Add one extra waypoint
        if (path.size() > 1) path.poll(); // skip every other waypoint since they're close together.
        if (unit.getDef().isAbleToFly() && path.size() > 1) path.poll();
        if (!path.isEmpty()) unit.moveTo(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
    }
    
    public boolean isStuck(int frame){
	    if (unit.getHealth() <= 0 || target == null) return false;
	
	    float speed = getSpeed(unit);
	    int lastWepFrame = 0;
	    for (Weapon w:unit.getWeapons()){
		    int reload = w.getReloadFrame();
		    if (reload > lastWepFrame) lastWepFrame = reload;
	    }
	    
        if (lastWepFrame < frame - 30 && speed < unit.getDef().getSpeed()/10f && !unit.isParalyzed()){
            stuck++;
            if (stuck >= 10) unit.moveTo(getAngularPoint(target, unit.getPos(), distance(target, unit.getPos()) + 100f), (short) 0, Integer.MAX_VALUE);
            if (stuck == 15) return true;
        }else{
            stuck = Math.max(0, stuck - 1);
        }
        return false;
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
