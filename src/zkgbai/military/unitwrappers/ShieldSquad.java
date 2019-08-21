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

    public ShieldSquad(){
        super();
        this.leader = null;
        this.leaderWeight = 0;
    }

    @Override
    public void addUnit(Fighter f){
        f.squad = this;
        metalValue += f.metalValue;

        if (leader == null){
            f.getUnit().setMoveState(1, (short) 0, Integer.MAX_VALUE);
            leader = f;
            leaderWeight = getUnitWeight(f);
	        List<Float> moveparams = new ArrayList<>();
	        moveparams.add(40f);
	        f.getUnit().executeCustomCommand(CMD_WANTED_SPEED, moveparams, (short) 0, Integer.MAX_VALUE);
            f.fightTo(target);
        }else if (getUnitWeight(f) > leaderWeight){
            f.getUnit().setMoveState(1, (short) 0, Integer.MAX_VALUE);
            leader.getUnit().setMoveState(0, (short) 0, Integer.MAX_VALUE);
	        // Set the wanted max speed for the new leader and remove the speed limit from the old leader.
            List<Float> moveparams = new ArrayList<>();
            moveparams.add(40f);
            f.getUnit().executeCustomCommand(CMD_WANTED_SPEED, moveparams, (short) 0, Integer.MAX_VALUE);
            moveparams.clear();
	        moveparams.add(leader.getUnit().getMaxSpeed() * 30f); // CMD_WANTED_SPEED uses elmos/sec while the game returns elmos/frame, thus MaxSpeed*30.
	        leader.getUnit().executeCustomCommand(CMD_WANTED_SPEED, moveparams, (short) 0, Integer.MAX_VALUE);
            fighters.add(leader);
            leader = f;
            leaderWeight = getUnitWeight(f);
            if (target != null) {
                leader.fightTo(target);
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
                fi.getUnit().executeCustomCommand(CMD_ORBIT, params, (short) 0, Integer.MAX_VALUE);
                fi.getUnit().executeCustomCommand(CMD_ORBIT_DRAW, drawParams, OPTION_SHIFT_KEY, Integer.MAX_VALUE);
                params.clear();
            }
        }else{
            f.getUnit().setMoveState(0, (short) 0, Integer.MAX_VALUE);
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
            f.getUnit().executeCustomCommand(CMD_ORBIT, params, (short) 0, Integer.MAX_VALUE);
            f.getUnit().executeCustomCommand(CMD_ORBIT_DRAW, drawParams, OPTION_SHIFT_KEY, Integer.MAX_VALUE);
        }
    }

    @Override
    public void removeUnit(Fighter f){
        if (leader != null && leader.equals(f)){
        	metalValue -= f.metalValue;

            leader = getNewLeader();
            if (leader == null){
                leaderWeight = 0;
                return;
            }

            fighters.remove(leader);
            leaderWeight = getUnitWeight(leader);
            leader.getUnit().setMoveState(1, (short) 0, Integer.MAX_VALUE);
	        List<Float> moveparams = new ArrayList<>();
	        moveparams.add(40f);
	        leader.getUnit().executeCustomCommand(CMD_WANTED_SPEED, moveparams, (short) 0, Integer.MAX_VALUE);
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
    public void setTarget(AIFloat3 pos){
        // set a target for the squad to attack.
        target = pos;
        if (leader != null && leader.getUnit().getHealth() > 0) {
            leader.fightTo(target);
        }
    }

    @Override
    public void retreatTo(AIFloat3 pos){
        // set a target for the squad to attack.
        target = pos;
        if (leader != null && leader.getUnit().getHealth() > 0) {
            leader.moveTo(target);
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
            case "vehcapture": return 1;
            case "shieldriot": return 2; // outlaws are too fast for other units to keep up with
            case "shieldarty": return 3;
            case "shieldassault": return 4;
            case "shieldfelon": return 5;
        }
        return 0;
    }
}
