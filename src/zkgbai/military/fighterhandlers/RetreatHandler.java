package zkgbai.military.fighterhandlers;

import com.springrts.ai.AICallback;
import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import com.springrts.ai.oo.clb.Weapon;
import zkgbai.ZKGraphBasedAI;
import zkgbai.graph.GraphManager;
import zkgbai.military.MilitaryManager;
import zkgbai.kgbutil.Pathfinder;
import zkgbai.military.UnitClasses;
import zkgbai.military.unitwrappers.Fighter;
import static zkgbai.kgbutil.KgbUtil.*;

import java.util.*;

/**
 * Created by haplo on 1/3/2016.
 */
public class RetreatHandler {
    ZKGraphBasedAI ai;
    MilitaryManager warManager;
    GraphManager graphManager;
    Pathfinder pathfinder;
    SquadHandler squadHandler;
    RaiderHandler raiderHandler;

    UnitClasses unitTypes = UnitClasses.getInstance();

    Map<Integer, Unit> cowardUnits = new HashMap<>();
    Map<Integer, Unit> retreatingUnits = new HashMap<>();
    Map<Integer, Unit> retreatedUnits = new HashMap<>();

    Map<Integer, Weapon> shields = new HashMap<>();

    Set<Integer> AAdefs = new HashSet<>();
    Set<Integer> lowManeuverability = new HashSet<>();
    Set<Integer> striders = new HashSet<>();
    Set<Integer> smallraiders = new HashSet<>();
    Set<Integer> planes = new HashSet<>();
    int halberdID = 0;
    int scytheID = 0;

    int frame;

    public static final short OPTION_SHIFT_KEY = (1 << 5); //  32
    private static final int CMD_FIND_PAD = 33411;

    public RetreatHandler(){
        this.ai = ZKGraphBasedAI.getInstance();
        this.warManager = ai.warManager;
        this.graphManager = ai.graphManager;
        this.pathfinder = Pathfinder.getInstance();

	    OOAICallback callback = ai.getCallback();
        for (UnitDef def: callback.getUnitDefs()){
        	if (def.getTooltip().contains("Anti-Air")){
        		AAdefs.add(def.getUnitDefId());
	        }else if (def.getName().startsWith("strider")){
        		striders.add(def.getUnitDefId());
	        }
        }

        for (String defName : unitTypes.smallRaiders){
        	smallraiders.add(callback.getUnitDefByName(defName).getUnitDefId());
        }

        for (UnitDef ud: callback.getUnitDefByName("factoryplane").getBuildOptions()){
        	if (ud.getBuildOptions().isEmpty()){
        		planes.add(ud.getUnitDefId());
	        }
        }

        halberdID = callback.getUnitDefByName("hoverassault").getUnitDefId();
        scytheID = callback.getUnitDefByName("cloakheavyraid").getUnitDefId();

        lowManeuverability.add(callback.getUnitDefByName("tankraid").getUnitDefId());
	    lowManeuverability.add(callback.getUnitDefByName("tankheavyraid").getUnitDefId());
	    lowManeuverability.add(callback.getUnitDefByName("tankriot").getUnitDefId());
	    lowManeuverability.add(callback.getUnitDefByName("hoverskirm").getUnitDefId());
	    lowManeuverability.add(callback.getUnitDefByName("hoverriot").getUnitDefId());
        lowManeuverability.add(callback.getUnitDefByName("vehriot").getUnitDefId());
        lowManeuverability.add(callback.getUnitDefByName("vehassault").getUnitDefId());
    }

    public void init(){
        this.squadHandler = warManager.squadHandler;
        this.raiderHandler = warManager.raiderHandler;
    }

    public void addCoward(Unit u){
        cowardUnits.put(u.getUnitId(), u);
        for (Weapon w:u.getWeapons()){
            if (w.getDef().getShield() != null && w.getDef().getShield().getPower() > 0){
                shields.put(u.getUnitId(), w);
                break;
            }
        }
    }

    public boolean isCoward(Unit u){
        return cowardUnits.containsKey(u.getUnitId());
    }

    public void checkUnit(Unit u, boolean onFire){
        float hp = u.getHealth() / u.getMaxHealth();
        int uid = u.getUnitId();
        int defID = u.getDef().getUnitDefId();

        if (u.getHealth() <= 0 || u.getTeam() != ai.teamID){
        	return;
        }else if (cowardUnits.containsKey(uid) && !retreatingUnits.containsKey(uid)
                && (hp < 0.5f || (AAdefs.contains(defID) && hp < 0.85f)
                        || ((u.getDef().isAbleToFly() || lowManeuverability.contains(defID)) && hp < 0.65f)
                        || (striders.contains(defID) && hp < 0.6f))) {
            if (onFire && smallraiders.contains(defID)){
                return;
            }
            if (defID == halberdID) u.setFireState(0, (short) 0, Integer.MAX_VALUE); // set halbs to hold fire when retreating.
            retreatingUnits.put(u.getUnitId(), u);
            squadHandler.removeFromSquad(u);
            raiderHandler.removeFromSquad(u);
        }
    }

    public void update(int frame){
        this.frame = frame;

        if (frame % 15 == 0) {
            if (cowardUnits.isEmpty()) return;
            cleanUnits();
            checkShields();
            retreatCowards();
        }
    }

    public boolean isRetreating(Unit u){
        // don't retreat scythes unless they're cloaked
        if (u.getDef().getUnitDefId() == scytheID){
            if (!u.isCloaked() && warManager.getEffectiveThreat(u.getPos()) <= 0){
                return false;
            }
        }
        if (retreatingUnits.containsKey(u.getUnitId())){
            return true;
        }
        return false;
    }

    public void removeUnit(Unit u){
        int uid = u.getUnitId();
        if (cowardUnits.containsKey(uid)) cowardUnits.remove(uid);
        if (retreatingUnits.containsKey(uid)) retreatingUnits.remove(uid);
        if (retreatedUnits.containsKey(uid)) retreatedUnits.remove(uid);
        if (shields.containsKey(uid)) shields.remove(uid);
    }

    private void retreatCowards(){
        boolean retreated = false;
        List<Unit> healedUnits = new ArrayList<Unit>();
        for (Unit u: retreatingUnits.values()){
            if (!shields.isEmpty() && shields.containsKey(u.getUnitId())){
                Weapon w = shields.get(u.getUnitId());
                if ((w.getShieldPower() > 0.5f * w.getDef().getShield().getPower() && u.getHealth() == u.getMaxHealth()) || u.getHealth() <= 0){
                    healedUnits.add(u);
                    continue;
                }
            }else if (u.getHealth() == u.getMaxHealth() || u.getHealth() <= 0){
                healedUnits.add(u);
                if (u.getDef().getUnitDefId() == halberdID) u.setFireState(2, (short) 0, Integer.MAX_VALUE); // return halbs to fire at will once healed.
                continue;
            }

            // don't retreat scythes unless they're cloaked
            if (u.getDef().getUnitDefId() == scytheID){
                if (!u.isCloaked() && warManager.getEffectiveThreat(u.getPos()) <= 0){
                    continue;
                }
            }

            if(!retreatedUnits.containsKey(u.getUnitId()) || warManager.getThreat(u.getPos()) > 0) {
                AIFloat3 position;
                if (!planes.contains(u.getDef().getUnitDefId())) {
                    if (u.getDef().isAbleToFly()){
                        position = graphManager.getClosestAirHaven(u.getPos());
                    }else {
                        position = graphManager.getClosestHaven(u.getPos());
                    }
                    Queue<AIFloat3> path = pathfinder.findPath(u, position, pathfinder.AVOID_ENEMIES);
                    u.moveTo(path.poll(), (short) 0, Integer.MAX_VALUE);
                    
                    int pathSize = Math.min(3, path.size());
                    int i = 0;
                    while (i < pathSize && !path.isEmpty()) { // queue up to the first 3 waypoints
                        // skip every other waypoint except the last, since they're not very far apart.
                        if (path.size() > 1) {
                            path.poll();
                            // skip two out of three waypoints for flying units, since they move quickly.
                            if (path.size() > 1 && u.getDef().isAbleToFly()) {
                                path.poll();
                            }
                        }
                        u.moveTo(path.poll(), OPTION_SHIFT_KEY, Integer.MAX_VALUE);
                        i++;
                    }
                }else{
                    List<Float> params = new ArrayList<Float>();

                    if (!graphManager.isEnemyTerritory(u.getPos())) {
                        u.executeCustomCommand(CMD_FIND_PAD, params, (short) 0, Integer.MAX_VALUE);
                    }else{
                        Fighter b = new Fighter(u, 0);
                        b.moveTo(graphManager.getAllyCenter()); // if in enemy territory, maneuver back to safety before finding an airpad.
                        b.getUnit().executeCustomCommand(CMD_FIND_PAD, params, OPTION_SHIFT_KEY,  Integer.MAX_VALUE);
                    }
                }

                retreatedUnits.put(u.getUnitId(), u);
                retreated = true;
            }
        }
        if (!retreated){
            retreatedUnits.clear();
        }
        
        for (Unit u: healedUnits){
            retreatingUnits.remove(u.getUnitId());
            retreatedUnits.remove(u.getUnitId());
        }
    }

    private void checkShields(){
        for (int uid:shields.keySet()){
            Weapon w = shields.get(uid);
            if (w.getShieldPower() < 0.25 * w.getDef().getShield().getPower()){
                Unit u = cowardUnits.get(uid);
                retreatingUnits.put(u.getUnitId(), u);
                squadHandler.removeFromSquad(u);
            }
        }
    }

    private void cleanUnits(){
	    List<Integer> invalidFighters = new ArrayList<Integer>();
    	for (Unit u: cowardUnits.values()){
    		if (u.getHealth() <= 0 || u.getTeam() != ai.teamID){
    			invalidFighters.add(u.getUnitId());
		    }
	    }
    	for (Integer key:invalidFighters){
    		cowardUnits.remove(key);
    		shields.remove(key);
    		retreatingUnits.remove(key);
    		retreatedUnits.remove(key);
	    }
    }
}
