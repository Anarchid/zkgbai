package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Weapon;
import com.springrts.ai.oo.clb.WeaponDef;
import zkgbai.ZKGraphBasedAI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static zkgbai.kgbutil.KgbUtil.*;

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
    public int numFelons = 0;
    public int numAspis = 0;
    private float maxShieldPower = 0f;
    private Map<Integer, Weapon> shields = new HashMap<>();
    
    private static int felonID;
    private static int aspisID;
    private static int domiID;
    private static boolean initialized = false;

    public ShieldSquad(){
        super();
        this.leader = null;
        this.leaderWeight = 0;
        if (!initialized) {
	        OOAICallback callback = ZKGraphBasedAI.getInstance().getCallback();
	        felonID = callback.getUnitDefByName("shieldfelon").getUnitDefId();
	        aspisID = callback.getUnitDefByName("shieldshield").getUnitDefId();
	        domiID = callback.getUnitDefByName("vehcapture").getUnitDefId();
        }
	    
    }

    @Override
    public void addUnit(Fighter f){
        f.squad = this;
        f.index = index;
        index++;
        metalValue += f.metalValue;
        int defID = f.getUnit().getDef().getUnitDefId();

        if (defID == felonID) numFelons++;

        if (defID != aspisID) {
            for (Weapon w : f.getUnit().getWeapons()) {
                if (w.getDef().getShield() != null && w.getDef().getShield().getPower() > 0) {
                    maxShieldPower += w.getDef().getShield().getPower();
                    shields.put(f.id, w);
                    break;
                }
            }
        }

        if (leader == null){
            f.getUnit().setMoveState(1, (short) 0, Integer.MAX_VALUE);
            leader = f;
            leaderWeight = getUnitWeight(f);
	        List<Float> moveparams = new ArrayList<>();
	        moveparams.add(45f);
	        f.getUnit().executeCustomCommand(CMD_WANTED_SPEED, moveparams, (short) 0, Integer.MAX_VALUE);
        }else if (getUnitWeight(f) < leaderWeight){
            f.getUnit().setMoveState(1, (short) 0, Integer.MAX_VALUE);
            leader.getUnit().setMoveState(0, (short) 0, Integer.MAX_VALUE);
	        // Set the wanted max speed for the new leader and remove the speed limit from the old leader.
            List<Float> moveparams = new ArrayList<>();
            moveparams.add(45f);
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
            collectStragglers();
        }else{
            f.getUnit().setMoveState(0, (short) 0, Integer.MAX_VALUE);
            fighters.add(f);
            List<Float> params = new ArrayList<>();
            List<Float> drawParams = new ArrayList<>();
            params.add((float)leader.id);
            drawParams.add((float)leader.id);
            if (f.getUnit().getDef().getUnitDefId() == domiID){
                params.add(175f);
            }else if (f.getUnit().getDef().getUnitDefId() == aspisID){
                params.add(40f);
            }else {
                params.add(75f);
            }
            f.getUnit().executeCustomCommand(CMD_ORBIT, params, (short) 0, Integer.MAX_VALUE);
            f.getUnit().executeCustomCommand(CMD_ORBIT_DRAW, drawParams, OPTION_SHIFT_KEY, Integer.MAX_VALUE);
        }
    }

    @Override
    public void removeUnit(Fighter f){
        if (f.getUnit().getDef().getUnitDefId() == felonID) numFelons--;

        if (shields.containsKey(f.id)){
            WeaponDef w = shields.get(f.getUnit().getUnitId()).getDef();
            maxShieldPower -= w.getShield().getPower();
            shields.remove(f.id);
        }

        metalValue -= f.metalValue;

        if (leader != null && leader.equals(f)){
            leader = getNewLeader();
            if (leader == null){
                leaderWeight = 0;
                return;
            }
	
	        fighters.remove(leader);
            leaderWeight = getUnitWeight(leader);
            leader.getUnit().setMoveState(1, (short) 0, Integer.MAX_VALUE);
	        List<Float> moveparams = new ArrayList<>();
	        moveparams.add(45f);
	        leader.getUnit().executeCustomCommand(CMD_WANTED_SPEED, moveparams, (short) 0, Integer.MAX_VALUE);
            target = null;
            collectStragglers();
        }else{
            fighters.remove(f);
        }
    }

    @Override
    public void setTarget(AIFloat3 pos){
        // set a target for the squad to attack.
        target = pos;
        collectStragglers();
        leader.fightTo(target);
    }

    @Override
    public void retreatTo(AIFloat3 pos){
        // set a target for the squad to attack.
        target = pos;
        if (leader != null && leader.getUnit().getHealth() > 0) {
        	collectStragglers();
            leader.moveTo(target);
        }
    }
    
    private void collectStragglers(){
    	for (Fighter f:fighters){
    		if (distance(leader.getPos(), f.getPos()) > 600f){
    			f.moveTo(getAngularPoint(leader.getPos(), f.getPos(), 400f)); // have units that haven't rallied yet avoid enemies.
			    List<Float> params = new ArrayList<>();
			    List<Float> drawParams = new ArrayList<>();
			    params.add((float)leader.id);
			    drawParams.add((float)leader.id);
			    if (f.getUnit().getDef().getName().equals("vehcapture")){
				    params.add(175f);
			    }else if (f.getUnit().getDef().getName().equals("shieldshield")){
				    params.add(40f);
			    }else {
				    params.add(75f);
			    }
			    f.getUnit().executeCustomCommand(CMD_ORBIT, params, OPTION_SHIFT_KEY, Integer.MAX_VALUE);
			    f.getUnit().executeCustomCommand(CMD_ORBIT_DRAW, drawParams, OPTION_SHIFT_KEY, Integer.MAX_VALUE);
		    }
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
    	numFelons = 0;
    	if (leader != null && leader.getUnit().getHealth() <= 0 || leader.getUnit().getTeam() != team){
    		leader.squad = null;
    		leader = null;
	    }else if (leader.getUnit().getDef().getUnitDefId() == felonID){
    		numFelons++;
	    }
	
	    List<Fighter> invalidFighters = new ArrayList<>();
	    for (Fighter f: fighters){
		    if (f.getUnit().getHealth() <= 0 || f.getUnit().getTeam() != team){
			    invalidFighters.add(f);
			    shields.remove(f.id);
			    f.squad = null;
		    }else if (f.getUnit().getDef().getUnitDefId() == felonID){
		    	numFelons++;
		    }
	    }
	    fighters.removeAll(invalidFighters);
	    if (leader == null) leader = getNewLeader();
    	return (fighters.isEmpty() && leader == null);
    }
    
    @Override
    public int getThreat(){
	    float threat = leader.getUnit().getPower() + leader.getUnit().getMaxHealth();
	    for (Fighter f: fighters){
		    threat += f.getUnit().getPower() + f.getUnit().getMaxHealth();
	    }
	    for (Weapon w:shields.values()){
	        threat += w.getDef().getShield().getPower();
	    }
	    return (int) (threat/8f);
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

    public float getShields(){
        if (maxShieldPower == 0) return 1f;
        float currentShields = 0;
        for (Weapon w: shields.values()){
            currentShields += w.getShieldPower();
        }
        return currentShields/maxShieldPower;
    }

    Fighter getNewLeader(){
        Fighter bestFighter = null;
        int score = Integer.MAX_VALUE;
        for (Fighter f: fighters){
            int tmpscore = getUnitWeight(f);
            if (tmpscore < score){
                score = tmpscore;
                bestFighter = f;
            }
        }
        return bestFighter;
    }

    int getUnitWeight(Fighter f){
        if (f.getUnit().getDef().getUnitDefId() == felonID) return f.index;
        return index + f.index;
    }

    public List<Fighter> disband(){
        List<Fighter> disbanded = new ArrayList<>();

        if (leader != null){
            List<Float> moveparams = new ArrayList<>();
            moveparams.add(leader.getUnit().getMaxSpeed() * 30f);
            leader.getUnit().executeCustomCommand(CMD_WANTED_SPEED, moveparams, (short) 0, Integer.MAX_VALUE);
            disbanded.add(leader);
            leader.squad = null;
            leader = null;
        }
        for (Fighter f: fighters){
            f.squad = null;
            f.getUnit().setMoveState(1, (short) 0, Integer.MAX_VALUE);
        }
        disbanded.addAll(fighters);
        fighters.clear();
        this.metalValue = 0;
        return disbanded;
    }
}
