package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.Weapon;
import com.springrts.ai.oo.clb.WeaponDef;
import com.springrts.ai.oo.clb.WeaponMount;
import zkgbai.ZKGraphBasedAI;
import zkgbai.kgbutil.Pathfinder;

import java.util.Map;
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
    public float power = 0f;
    protected Unit unit;
    static Pathfinder pathfinder = null;
    static int team;
    protected static final short OPTION_SHIFT_KEY = 32; //  32

    public Fighter(Unit u, float metal){
        this.unit = u;
        this.id = u.getUnitId();
        this.metalValue = metal;
	
	    for (WeaponMount w:u.getDef().getWeaponMounts()){
		    WeaponDef wd = w.getWeaponDef();
		    if (!wd.isWaterWeapon() && !wd.getName().contains("fake")) {
			    // take the max between burst damage and continuous dps as weapon power.
			    float wepShot = wd.getDamage().getTypes().get(1) * wd.getProjectilesPerShot() * wd.getSalvoSize();
			    for (Map.Entry<String, String> param: wd.getCustomParams().entrySet()){
				    if (param.getKey().equals("extra_damage")) wepShot += Float.parseFloat(param.getValue()); // emp
				    if (param.getKey().equals("impulse")) wepShot += Float.parseFloat(param.getValue())/10f; // impulse
				    if (param.getKey().equals("timeslow_damagefactor")) wepShot += Math.min((wepShot * Float.parseFloat(param.getValue())), wepShot/2f); // slow
				    if (param.getKey().equals("disarmDamageOnly")) wepShot /= 10f;
				    if (param.getKey().equals("setunitsonfire")) wepShot *= 2f;
			    }
			    wepShot *= 1f + wd.getAreaOfEffect()/100f;
			    power += wepShot/wd.getReload();
		    }
	    }

        if (pathfinder == null){
            pathfinder = Pathfinder.getInstance();
            team = ZKGraphBasedAI.getInstance().teamID;
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
	    if (path.size() > 1) path.poll(); // skip every other waypoint since they're close together.
	    if (unit.getDef().isAbleToFly() && path.size() > 1) path.poll();
        unit.fight(path.poll(), (short) 0, Integer.MAX_VALUE);
        
        // Add one extra waypoint
        if (path.size() > 1) path.poll(); // skip every other waypoint since they're close together.
	    if (unit.getDef().isAbleToFly() && path.size() > 1) path.poll();
        if (!path.isEmpty()) unit.fight(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
    }

    public void moveTo(AIFloat3 pos){
        Queue<AIFloat3> path = pathfinder.findPath(unit, getRadialPoint(pos, 100f), pathfinder.AVOID_ENEMIES);
    
        if (unit.getDef().isAbleToFly() && path.size() > 1) path.poll();
	    if (path.size() > 1) path.poll(); // skip every other waypoint since they're close together.
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
            stuck = Math.max(0, stuck - 2);
        }
        return false;
    }
    
    public boolean isDead(){
    	return unit.getHealth() <= 0 || unit.getTeam() != team;
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
