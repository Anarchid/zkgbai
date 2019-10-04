package zkgbai.military.unitwrappers;

import com.springrts.ai.AI;
import com.springrts.ai.oo.AIFloat3;
import zkgbai.ZKGraphBasedAI;
import zkgbai.military.MilitaryManager;

import java.util.ArrayList;
import java.util.Queue;
import java.util.List;
import static zkgbai.kgbutil.KgbUtil.*;

/**
 * Created by haplo on 1/8/2016.
 */
public class RaiderSquad {
    public List<Raider> raiders;
    public AIFloat3 target;
    public char status = 'f';
	// f = forming
	// r = rallying
	// a = attacking
    public Raider leader;
    private int index = 0;
    int firstRallyFrame = 0;
    public int lastAssignmentFrame = 0;
    public char type;

    public RaiderSquad(){
        this.raiders = new ArrayList<Raider>();
    }

    public void addUnit(Raider r, int frame) {
        raiders.add(r);
        r.squad = this;
        r.index = index;
        index++;

        if (leader == null){
            leader = r;
        }
    }

    public void removeUnit(Raider r){
    	r.squad = null;
        raiders.remove(r);
        if (leader.equals(r)){
            leader = getNewLeader();
        }
    }

    public void sneak(AIFloat3 pos, int frame){
        // set a target for the squad to attack.
        target = pos;
    
        float maxdist = 150f;
        float waydist = 75f;
        
        if (type == 's'){
            // Small raiders get independent paths, but if they're out of range of the group then they move to the leader
            // instead of the target. This works surprisingly well.
            for (Raider r : raiders) {
                float fdist = distance(leader.getPos(), r.getPos());
                if (fdist > maxdist) {
                    r.outOfRange = true;
                    if (fdist > 2f * maxdist){
                        r.sneak(getRadialPoint(leader.getPos(), waydist), frame);
                    }else{
                        r.getUnit().moveTo(getAngularPoint(leader.getPos(), r.getPos(), lerp(waydist, maxdist, Math.random())), (short) 0, Integer.MAX_VALUE);
                    }
                } else {
                    r.outOfRange = false;
                    r.sneak(pos, frame);
                }
            }
        }else {
            // Medium raiders are treated more like assaults. The group only gets one path that all units follow.
            // This keeps them mobbed up better so they can do maximum damage. Out of range units are still moved
            // to the leader to keep them dynamically rallied in case they get split up for whatever reason.
            Queue<AIFloat3> path = leader.getRaidPath(pos);
	        if (path.size() > 1) path.poll();
            AIFloat3 waypoint = path.poll();
    
            for (Raider r : raiders) {
                float fdist = distance(leader.getPos(), r.getPos());
                if (fdist > maxdist) {
                    r.outOfRange = true;
                    if (fdist > 2f * maxdist){
                        r.sneak(getAngularPoint(leader.getPos(), r.getPos(), lerp(waydist, maxdist, Math.random())), frame);
                    }else{
                        r.getUnit().moveTo(getAngularPoint(leader.getPos(), r.getPos(), lerp(waydist, maxdist, Math.random())), (short) 0, Integer.MAX_VALUE);
                    }
                } else {
                    r.outOfRange = false;
                    r.getUnit().moveTo(waypoint, (short) 0, Integer.MAX_VALUE);
                }
            }
        }
    }

    public void raid(AIFloat3 pos, int frame){
        // set a target for the squad to attack.
        target = pos;
    
        float maxdist = 150f;
        float waydist = 75f;
    
        if(type == 's'){
            // Small raiders get independent paths, but if they're out of range of the group then they move to the leader
            // instead of the target. This works surprisingly well.
            for (Raider r : raiders) {
                float fdist = distance(leader.getPos(), r.getPos());
                if (fdist > maxdist) {
                    r.outOfRange = true;
                    if (fdist > 2f * maxdist){
                        r.sneak(getRadialPoint(leader.getPos(), waydist), frame);
                    }else{
                        r.getUnit().moveTo(getAngularPoint(leader.getPos(), r.getPos(), lerp(waydist, maxdist, Math.random())), (short) 0, Integer.MAX_VALUE);
                    }
                } else {
                    r.outOfRange = false;
                    r.raid(pos, frame);
                }
            }
        }else {
            // Medium raiders are treated more like assaults. The group only gets one path that all units follow.
            // This keeps them mobbed up better so they can do maximum damage. Out of range units are still moved
            // to the leader to keep them dynamically rallied in case they get split up for whatever reason.
            Queue<AIFloat3> path = leader.getRaidPath(pos);
	        if (path.size() > 1) path.poll();
            AIFloat3 waypoint = path.poll();
    
            for (Raider r : raiders) {
                float fdist = distance(leader.getPos(), r.getPos());
                if (fdist > maxdist) {
                    r.outOfRange = true;
	                if (fdist > 2f * maxdist){
		                r.sneak(getAngularPoint(leader.getPos(), r.getPos(), lerp(waydist, maxdist, Math.random())), frame);
	                }else{
		                r.getUnit().moveTo(getAngularPoint(leader.getPos(), r.getPos(), lerp(waydist, maxdist, Math.random())), (short) 0, Integer.MAX_VALUE);
	                }
                } else {
                    r.outOfRange = false;
                    r.getUnit().fight(waypoint, (short) 0, Integer.MAX_VALUE);
                }
            }
        }
    }

    public AIFloat3 getPos(){
        return leader.getPos();
    }

    public boolean isRallied(int frame){
        if (firstRallyFrame == 0){
            firstRallyFrame = frame;
        }
        if (frame - firstRallyFrame > 900){
            return true;
        }
        
        for (Raider r: raiders){
            if (r.outOfRange){
                return false;
            }
        }
        return true;
    }

    public int getThreat(){
        float threat = 0;
        for (Raider r:raiders){
            threat += r.power + r.getUnit().getMaxHealth();
        }
        return (int) (threat / 10f);
    }
	
	public boolean isDead(){
		List<Raider> invalidRaiders = new ArrayList<>();
		for (Raider r: raiders){
			if (r.isDead()){
				invalidRaiders.add(r);
				r.squad = null;
			}
		}
		raiders.removeAll(invalidRaiders);
		
		leader = getNewLeader();
		return raiders.isEmpty();
	}

    private Raider getNewLeader(){
        Raider newLeader = null;
        int tmpindex = Integer.MAX_VALUE;
        for (Raider r:raiders){
        	int in = (r.getUnit().getDef().getName().contains("bomb") ? r.index + index : r.index);
            if (in < tmpindex){
                newLeader = r;
                tmpindex = r.index;
            }
        }
        return newLeader;
    }
}
