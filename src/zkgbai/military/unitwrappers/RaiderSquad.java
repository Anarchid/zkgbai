package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;

import java.util.ArrayList;
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

    public RaiderSquad(){
        this.raiders = new ArrayList<Raider>();
        this.status = 'r';
        // r = forming
        // r = rallying
        // a = attacking
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
        for (Raider r : raiders) {
            r.sneak(target, frame);
        }
    }

    public void raid(AIFloat3 pos, int frame){
        // set a target for the squad to attack.
        target = pos;
        for (Raider r : raiders) {
            r.raid(target, frame);
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
        AIFloat3 pos = getPos();
        boolean rallied = true;
        for (Raider r: raiders){
            if (distance(pos, r.getPos()) > 200){
                rallied = false;
                r.sneak(pos, frame);
            }
        }
        return rallied;
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
