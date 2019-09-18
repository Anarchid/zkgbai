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
    AIFloat3 target;
    public char status;
    public Raider leader;
    private int index = 0;
    int firstRallyFrame = 0;
    public char type;
    MilitaryManager warManager;

    public RaiderSquad(){
        this.raiders = new ArrayList<Raider>();
        this.status = 'r';
        // r = forming
        // r = rallying
        // a = attacking
        this.warManager = ZKGraphBasedAI.getInstance().warManager;
    }

    public void addUnit(Raider r, int frame) {
        raiders.add(r);
        r.squad = this;
        r.index = index;
        index++;
        r.raid(target, frame);

        if (leader == null){
            leader = r;
        }
    }

    public void removeUnit(Raider r){
        raiders.remove(r);
        if (leader.equals(r)){
            leader = getNewLeader();
        }
    }

    public void sneak(AIFloat3 pos, int frame){
        // set a target for the squad to attack.
        target = pos;
        if (leader == null || leader.getUnit().getHealth() <= 0) leader = getNewLeader();
        if (leader == null) return;
    
        float maxdist = 150f;
        float waydist = 75f;
        
        /*if (type == 's'){
            // Small raiders get independent paths, but if they're out of range of the group then they move to the leader
            // instead of the target. This works surprisingly well.
            for (Raider r : raiders) {
                if (r.getUnit().getHealth() <= 0) continue;
                float fdist = distance(leader.getPos(), r.getPos());
                if (fdist > maxdist) {
                    r.outOfRange = true;
                    if (fdist > 2f * maxdist){
                        r.sneak(getRadialPoint(leader.getPos(), waydist), frame);
                    }else{
                        r.getUnit().moveTo(getRadialPoint(leader.getPos(), waydist), (short) 0, Integer.MAX_VALUE);
                    }
                } else {
                    r.outOfRange = false;
                    r.sneak(pos, frame);
                }
            }
        }else {*/
            // Medium raiders are treated more like assaults. The group only gets one path that all units follow.
            // This keeps them mobbed up better so they can do maximum damage. Out of range units are still moved
            // to the leader to keep them dynamically rallied in case they get split up for whatever reason.
            Queue<AIFloat3> path = leader.getRaidPath(pos);
            AIFloat3 waypoint = path.poll();
    
            for (Raider r : raiders) {
                if (r.getUnit().getHealth() <= 0) continue;
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
                    r.getUnit().moveTo(getFormationPoint(r.getPos(), leader.getPos(), waypoint), (short) 0, Integer.MAX_VALUE);
                }
            }
        //}
    }

    public void raid(AIFloat3 pos, int frame){
        // set a target for the squad to attack.
        target = pos;
        if (leader == null || leader.getUnit().getHealth() <= 0) leader = getNewLeader();
        if (leader == null) return;
    
        float maxdist = 150f;
        float waydist = 75f;
    
        /*if(type == 's'){
            // Small raiders get independent paths, but if they're out of range of the group then they move to the leader
            // instead of the target. This works surprisingly well.
            for (Raider r : raiders) {
                if (r.getUnit().getHealth() <= 0) continue;
                float fdist = distance(leader.getPos(), r.getPos());
                if (fdist > maxdist) {
                    r.outOfRange = true;
                    if (fdist > 2f * maxdist){
                        r.sneak(getRadialPoint(leader.getPos(), waydist), frame);
                    }else{
                        r.getUnit().moveTo(getRadialPoint(leader.getPos(), waydist), (short) 0, Integer.MAX_VALUE);
                    }
                } else {
                    r.outOfRange = false;
                    r.raid(pos, frame);
                }
            }
        }else {*/
            // Medium raiders are treated more like assaults. The group only gets one path that all units follow.
            // This keeps them mobbed up better so they can do maximum damage. Out of range units are still moved
            // to the leader to keep them dynamically rallied in case they get split up for whatever reason.
            Queue<AIFloat3> path = leader.getRaidPath(pos);
            AIFloat3 waypoint = path.poll();
    
            for (Raider r : raiders) {
                if (r.getUnit().getHealth() <= 0) continue;
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
                    r.getUnit().fight(getFormationPoint(r.getPos(), leader.getPos(), waypoint), (short) 0, Integer.MAX_VALUE);
                }
            }
        //}
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
            if (r.getUnit().getHealth() <= 0) continue;
            if (r.outOfRange){
                return false;
            }
        }
        return true;
    }

    public float getThreat(){
        float threat = 0f;
        for (Raider r:raiders){
            if (r.getUnit().getHealth() > 0) threat += ((r.getUnit().getPower() + r.getUnit().getMaxHealth()) / (type == 's' ? 16f : 12f));
        }
        return threat/500f;
    }

    public boolean isDead(){
        if (raiders.size() == 0){
            return true;
        }
        return false;
    }

    private Raider getNewLeader(){
        Raider newLeader = null;
        int tmpindex = Integer.MAX_VALUE;
        for (Raider r:raiders){
            if (r.index < tmpindex){
                newLeader = r;
                tmpindex = r.index;
            }
        }
        return newLeader;
    }
}
