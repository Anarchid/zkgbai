package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;

import java.util.Queue;

import static zkgbai.kgbutil.KgbUtil.*;

public class HoverSquad extends Squad {
	public HoverSquad(){
		super();
	}
	
	@Override
	public void setTarget(AIFloat3 pos){
		// set a target for the squad to attack.
		target = pos;
		
		float maxdist = 150f;
		float waydist = 75f;
		
		boolean farAway = distance(leader.getPos(), target) > 450f;
		
		Queue<AIFloat3> path = leader.getFightPath(pos);
		if (path.size() > 1) path.poll();
		if (path.size() > 1) path.poll();
		AIFloat3 waypoint = path.poll();
		
		for (Fighter f : fighters) {
			float fdist = distance(leader.getPos(), f.getPos());
			if (fdist > maxdist) {
				f.outOfRange = true;
				if (fdist > 2f * maxdist){
					f.moveTo(getAngularPoint(leader.getPos(), f.getPos(), lerp(waydist, maxdist, Math.random())));
				}else{
					f.getUnit().moveTo(getAngularPoint(leader.getPos(), f.getPos(), lerp(waydist, maxdist, Math.random())), (short) 0, Integer.MAX_VALUE);
				}
			} else {
				f.outOfRange = false;
				if (farAway){
					f.getUnit().moveTo(waypoint, (short) 0, Integer.MAX_VALUE);
				}else {
					f.getUnit().fight(waypoint, (short) 0, Integer.MAX_VALUE);
				}
			}
		}
	}
}
