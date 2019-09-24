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
import zkgbai.military.unitwrappers.Coward;
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

    Map<Integer, Coward> cowardUnits = new HashMap<>();
    Map<Integer, Coward> retreatingUnits = new HashMap<>();
    Set<Integer> retreatedUnits = new HashSet<>();

    Map<Integer, Coward> shields = new HashMap<>();

    Set<Integer> AAdefs = new HashSet<>();
    Set<Integer> lowManeuverability = new HashSet<>();
    Set<Integer> striders = new HashSet<>();
    Set<Integer> smallraiders = new HashSet<>();
    Set<Integer> planes = new HashSet<>();
    int halberdID = 0;
    int scytheID = 0;
    
    int lastDamagedID = 0;
    Coward lastDamagedCoward = null;

    int frame;

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
	    lowManeuverability.add(callback.getUnitDefByName("shieldriot").getUnitDefId());
    }

    public void init(){
        this.squadHandler = warManager.squadHandler;
        this.raiderHandler = warManager.raiderHandler;
    }

    public void addCoward(Unit u){
	    int defID = u.getDef().getUnitDefId();
	    float rlvl;
	    
	    if (AAdefs.contains(defID)){
	    	rlvl = 0.85f;
	    }else if (u.getDef().isAbleToFly() || lowManeuverability.contains(defID)){
	    	rlvl = 0.65f;
	    }else if (striders.contains(defID)){
	    	rlvl = 0.6f;
	    }else{
	    	rlvl = 0.5f;
	    }
	    
	    Coward c = new Coward(u, rlvl);
	    c.smallRaider = smallraiders.contains(defID);
	    c.isScythe = u.getDef().getUnitDefId() == scytheID;
	    c.isPlane = planes.contains(defID);
    	
        cowardUnits.put(c.id, c);
        if (c.shield != null) shields.put(c.id, c);
        
    }

    public boolean isCoward(Unit u){
        return cowardUnits.containsKey(u.getUnitId());
    }

    public void checkUnit(Unit u, boolean onFire){
    	if (lastDamagedID != u.getUnitId()){
		    lastDamagedID = u.getUnitId();
		    lastDamagedCoward = cowardUnits.get(u.getUnitId());
	    }
	    if (lastDamagedCoward == null || lastDamagedCoward.isRetreating || (onFire && lastDamagedCoward.smallRaider)) return;
	    if (lastDamagedCoward.shouldRetreat()){
		    retreatingUnits.put(u.getUnitId(), lastDamagedCoward);
		    squadHandler.removeFromSquad(u);
		    raiderHandler.removeFromSquad(u);
	    }
    }

    public void update(int frame){
        this.frame = frame;

        if (frame % 15 == ai.offset % 15) {
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
        cowardUnits.remove(uid);
        retreatingUnits.remove(uid);
        retreatedUnits.remove(uid);
        shields.remove(uid);
    }

    private void retreatCowards(){
        boolean retreated = false;
        List<Integer> healedUnits = new ArrayList<>();
        for (Coward c: retreatingUnits.values()){
            if (c.isHealed()){
                healedUnits.add(c.id);
                continue;
            }
            
            if (c.isDead()){
	            healedUnits.add(c.id);
	            cowardUnits.remove(c.id);
	            shields.remove(c.id);
            }

            // don't retreat scythes unless they're cloaked
            if (c.isScythe){
                if (!c.isCloaked() && warManager.getEffectiveThreat(c.getPos()) <= 0){
                    continue;
                }
            }

            if(!retreatedUnits.contains(c.id) || warManager.getThreat(c.getPos()) > 0) {
                AIFloat3 position;
                if (!c.isPlane) {
                    if (c.isFlyer){
                        position = graphManager.getClosestAirHaven(c.getPos());
                    }else {
                        position = graphManager.getClosestHaven(c.getPos());
                    }
                    c.retreatTo(position);
                }else{
                    if (!graphManager.isEnemyTerritory(c.getPos())) {
                        c.findPad();
                    }else{
                        c.retreatTo(graphManager.getAllyCenter()); // if in enemy territory, maneuver back to safety before finding an airpad.
                    }
                }

                retreatedUnits.add(c.id);
                retreated = true;
            }
        }
        if (!retreated){
            retreatedUnits.clear();
        }
        
        for (int id: healedUnits){
            retreatingUnits.remove(id);
            retreatedUnits.remove(id);
        }
    }

    private void checkShields(){
        for (Coward c:shields.values()){
            if (!c.isRetreating && c.checkShield()){
                retreatingUnits.put(c.id, c);
                squadHandler.removeFromSquad(c.getUnit());
            }
        }
    }

    private void cleanUnits(){
	    List<Integer> invalidFighters = new ArrayList<Integer>();
    	for (Coward c: cowardUnits.values()){
    		if (c.isDead()){
    			invalidFighters.add(c.id);
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
