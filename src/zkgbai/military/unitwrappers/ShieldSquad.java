package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aeonios on 11/29/2015.
 */
public class ShieldSquad extends Squad {
    static int CMD_ORBIT = 13923;
    static int CMD_ORBIT_DRAW = 13924;
    static int CMD_WANTED_SPEED = 38825;
    static short OPTION_SHIFT_KEY = (1 << 5);
    public Fighter leader;
    private int leaderWeight;
    public boolean hasFunnel = false;
    public float funnelValue = 0;

    public ShieldSquad(){
        super();
        this.leader = null;
        this.leaderWeight = 0;
    }

    @Override
    public void addUnit(Fighter f, int frame){
        f.squad = this;
        if (!f.getUnit().getDef().getName().equals("striderfunnelweb")) {
            metalValue += f.metalValue;
        }else{
            funnelValue += f.metalValue;
        }

        // for funnels
        if (getUnitWeight(f) == 1){
            hasFunnel = true;
            if (leader != null) {
                List<Float> moveparams = new ArrayList<>();
                moveparams.add(1f);
                leader.getUnit().executeCustomCommand(CMD_WANTED_SPEED, moveparams, (short) 0, frame+15);
            }
        }

        if (leader == null){
            f.getUnit().setMoveState(1, (short) 0, frame+300);
            leader = f;
            leaderWeight = getUnitWeight(f);
            f.fightTo(target, frame);
            if (hasFunnel){
                List<Float> moveparams = new ArrayList<>();
                moveparams.add(1f);
                f.getUnit().executeCustomCommand(CMD_WANTED_SPEED, moveparams, (short) 0, frame+15);
            }
        }else if (getUnitWeight(f) > leaderWeight){
            f.getUnit().setMoveState(1, (short) 0, frame+300);
            leader.getUnit().setMoveState(0, (short) 0, frame + 300);
            List<Float> moveparams = new ArrayList<>();
            moveparams.add(2f);
            f.getUnit().executeCustomCommand(CMD_WANTED_SPEED, moveparams, (short) 0, frame+15);
            fighters.add(leader);
            leader = f;
            leaderWeight = getUnitWeight(f);
            if (target != null) {
                leader.fightTo(target, frame);
            }
            List<Float> params = new ArrayList<>();
            List<Float> drawParams = new ArrayList<>();
            drawParams.add((float)leader.id);
            for (Fighter fi: fighters){
                params.add((float)leader.id);
                if (fi.getUnit().getDef().getName().equals("vehcapture")){
                    params.add(175f);
                }else {
                    params.add(75f);
                }
                fi.getUnit().executeCustomCommand(CMD_ORBIT, params, (short) 0, frame + 3000);
                fi.getUnit().executeCustomCommand(CMD_ORBIT_DRAW, drawParams, OPTION_SHIFT_KEY, frame + 3000);
                params.clear();
            }

            if (hasFunnel){
                moveparams.clear();
                moveparams.add(1f);
                f.getUnit().executeCustomCommand(CMD_WANTED_SPEED, moveparams, (short) 0, frame+15);
            }
        }else{
            f.getUnit().setMoveState(0, (short) 0, frame+300);
            fighters.add(f);
            List<Float> params = new ArrayList<>();
            List<Float> drawParams = new ArrayList<>();
            params.add((float)leader.id);
            drawParams.add((float)leader.id);
            if (f.getUnit().getDef().getName().equals("vehcapture")){
                params.add(175f);
            }else {
                params.add(75f);
            }
            f.getUnit().executeCustomCommand(CMD_ORBIT, params, (short) 0, frame + 3000);
            f.getUnit().executeCustomCommand(CMD_ORBIT_DRAW, drawParams, OPTION_SHIFT_KEY, frame + 3000);
        }
    }

    @Override
    public void removeUnit(Fighter f){
        if (leader != null && leader.equals(f)){
            if (!f.getUnit().getDef().getName().equals("striderfunnelweb")) {
                metalValue -= f.metalValue;
            }else{
                funnelValue -= f.metalValue;
            }
            leader = getNewLeader();
            if (leader == null){
                leaderWeight = 0;
                return;
            }

            if (hasFunnel){
                List<Float> moveparams = new ArrayList<>();
                moveparams.add(1f);
                f.getUnit().executeCustomCommand(CMD_WANTED_SPEED, moveparams, (short) 0, Integer.MAX_VALUE);
            }
            fighters.remove(leader);
            leaderWeight = getUnitWeight(leader);
            leader.getUnit().setMoveState(1, (short) 0, Integer.MAX_VALUE);
            target = null;
            List<Float> params = new ArrayList<>();
            List<Float> drawParams = new ArrayList<>();
            drawParams.add((float)leader.id);
            for (Fighter fi:fighters){
                params.add((float)leader.id);
                if (fi.getUnit().getDef().getName().equals("vehcapture")){
                    params.add(175f);
                }else {
                    params.add(75f);
                }
                fi.getUnit().executeCustomCommand(CMD_ORBIT, params, (short) 0, Integer.MAX_VALUE);
                fi.getUnit().executeCustomCommand(CMD_ORBIT_DRAW, drawParams, OPTION_SHIFT_KEY, Integer.MAX_VALUE);
                params.clear();
            }
        }else{
            super.removeUnit(f);
        }
    }

    @Override
    public void setTarget(AIFloat3 pos, int frame){
        // set a target for the squad to attack.
        target = pos;
        if (leader != null && leader.getUnit().getHealth() > 0) {
            leader.fightTo(target, frame);
        }
    }

    @Override
    public void retreatTo(AIFloat3 pos, int frame){
        // set a target for the squad to attack.
        target = pos;
        if (leader != null && leader.getUnit().getHealth() > 0) {
            leader.moveTo(target, frame);
        }
    }

    public AIFloat3 getAvgPos(){
        return super.getPos();
    }

    @Override
    public AIFloat3 getPos(){
        if (leader != null){
            return leader.getPos();
        }
        return target;
    }

    @Override
    public boolean isDead(){
        return (fighters.isEmpty() && leader == null);
    }

    public float getHealth(){
        if (leader == null){
            return 0;
        }

        float count = fighters.size() + 1;
        float health = 0;
        health += (leader.getUnit().getHealth()/leader.getUnit().getMaxHealth())/count;

        for (Fighter f:fighters){
            health += (f.getUnit().getHealth()/f.getUnit().getMaxHealth())/count;
        }

        return health;
    }

    Fighter getNewLeader(){
        Fighter bestFighter = null;
        int score = 0;
        for (Fighter f: fighters){
            int tmpscore = getUnitWeight(f);
            if (tmpscore > score){
                score = tmpscore;
                bestFighter = f;
            }
        }
        return bestFighter;
    }

    int getUnitWeight(Fighter f){
        String type = f.getUnit().getDef().getName();
        switch (type){
            case "striderfunnelweb": return 1; // funnels skimrmish from too large a range for other units to do any damage
            case "vehcapture": return 2;
            case "shieldriot": return 2; // outlaws are too fast for other units to keep up with
            case "shieldarty": return 3;
            case "shieldassault": return 4;
            case "shieldfelon": return 5;
        }
        return 0;
    }
}
