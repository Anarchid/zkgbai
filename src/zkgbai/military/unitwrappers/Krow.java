package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.Weapon;

import java.util.Deque;

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
			Deque<AIFloat3> path = pathfinder.findPath(unit, pos, pathfinder.ASSAULT_PATH);
			unit.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE); // skip first few waypoints if target actually found to prevent stuttering, otherwise use the first waypoint.
			if (path.size() > 2) {
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
