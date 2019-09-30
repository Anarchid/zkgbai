package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.Weapon;

import java.util.Queue;

import static zkgbai.kgbutil.KgbUtil.*;

/**
 * Created by haplo on 1/7/2017.
 */
public class Krow extends Strider {
	public Krow(Unit u, Float metal){
		super(u, metal);
	}
	
	public boolean isDgunReadyOrFiring(int frame){
		for (Weapon w:unit.getWeapons()){
			if (w.getDef().isManualFire()){
				if (w.getReloadFrame() <= frame || w.getReloadFrame() - frame > 720){
					return true;
				}
				break;
			}
		}
		return false;
	}

	public void flyTo(AIFloat3 pos, int frame){
		if (distance(pos, getPos()) > 1200f) {
			Queue<AIFloat3> path = pathfinder.findPath(unit, pos, pathfinder.ASSAULT_PATH);
			if (path.size() > 1) path.poll(); // skip the first waypoint since flying units move pretty quick and the waypoints are close together.
			if (path.size() > 1) path.poll();
			unit.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE); // skip first few waypoints if target actually found to prevent stuttering, otherwise use the first waypoint.
			if (path.size() > 1) path.poll();
			if (path.size() > 1) path.poll();
			if (!path.isEmpty()) unit.moveTo(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
		}else{
			AIFloat3 target;
			if (isDgunReadyOrFiring(frame)) {
				target = getDirectionalPoint(getPos(), pos, distance(pos, getPos()) + 300f);
				unit.moveTo(target, (short) 0, Integer.MAX_VALUE);
			}else{
				target = getDirectionalPoint(pos, getPos(), 375f);
				unit.moveTo(target, (short) 0, Integer.MAX_VALUE);
			}
		}
	}
}
