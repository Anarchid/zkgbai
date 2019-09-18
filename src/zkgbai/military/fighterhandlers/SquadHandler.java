package zkgbai.military.fighterhandlers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.EconomyManager;
import zkgbai.economy.tasks.CombatReclaimTask;
import zkgbai.economy.tasks.ReclaimTask;
import zkgbai.graph.GraphManager;
import zkgbai.military.MilitaryManager;
import zkgbai.military.UnitClasses;
import zkgbai.military.unitwrappers.Fighter;
import zkgbai.military.unitwrappers.ShieldSquad;
import zkgbai.military.unitwrappers.Squad;

import java.util.*;

import static zkgbai.kgbutil.KgbUtil.*;

/**
 * Created by haplo on 1/4/2016.
 */
public class SquadHandler {
    ZKGraphBasedAI ai;
    MilitaryManager warManager;
    EconomyManager ecoManager;
    GraphManager graphManager;

    RetreatHandler retreatHandler;

    UnitClasses unitTypes;

    public java.util.Map<Integer, Fighter> fighters = new HashMap<Integer, Fighter>();
    public Squad nextSquad = null;
    public Squad nextRaidAssaultSquad = null;
    public Squad nextAirSquad = null;
    public Squad nextBrawlerSquad = null;
    public Queue<ShieldSquad> shieldSquads = new LinkedList<>();
    public List<Squad> squads = new ArrayList<Squad>();

    int frame = 0;
    int squadCounter = 0;

    public SquadHandler(){
        this.ai = ZKGraphBasedAI.getInstance();
        this.warManager = ai.warManager;
        this.ecoManager = ai.ecoManager;
        this.graphManager = ai.graphManager;

        this.retreatHandler = warManager.retreatHandler;
        this.unitTypes = UnitClasses.getInstance();
    }

    public void update(int frame){
        this.frame = frame;

        if (frame % 30 == 7){
            cleanUnits();
            for (Fighter f:fighters.values()){
                Unit u = f.getUnit();
                String defName = u.getDef().getName();
                if (f.squad == null && !retreatHandler.isRetreating(u)) {
                    if (unitTypes.airMobs.contains(defName)) {
	                    addAirMob(f);
                    }else if (defName.equals("hoverassault") || defName.equals("tankheavyraid")){
	                    addRaidAssault(f);
                    }else if (unitTypes.assaults.contains(defName)){
                    	addAssault(f);
                    }else{
                        addShieldMob(f);
                    }
                }
            }
            updateSquads();
        }
    }

    public void addAssault(Fighter f){
        fighters.put(f.id, f);

        // create a new squad if there isn't one
        if (nextSquad == null){
            nextSquad = new Squad();
            nextSquad.setTarget(warManager.getRallyPoint(f.getPos()));
            nextSquad.income = ecoManager.effectiveIncome/(1f + (ai.mergedAllies * graphManager.territoryFraction * graphManager.territoryFraction));
        }

        nextSquad.addUnit(f);

        if ((nextSquad.metalValue > nextSquad.income * 45f && nextSquad.metalValue > 1000f) || nextSquad.metalValue > 2500f){
            nextSquad.status = 'r';
            squads.add(nextSquad);
            nextSquad = null;
        }
    }

    public void addRaidAssault(Fighter f){
        fighters.put(f.id, f);

        // create a new squad if there isn't one
        if (nextRaidAssaultSquad == null){
            nextRaidAssaultSquad = new Squad();
            nextRaidAssaultSquad.setTarget(warManager.getRallyPoint(f.getPos()));
            nextRaidAssaultSquad.income = ecoManager.effectiveIncome/(1f + (ai.mergedAllies * graphManager.territoryFraction * graphManager.territoryFraction));
        }

        nextRaidAssaultSquad.addUnit(f);

        if ((nextRaidAssaultSquad.metalValue > nextRaidAssaultSquad.income * 45 && nextRaidAssaultSquad.metalValue > 1000) || nextRaidAssaultSquad.metalValue > 2500){
            nextRaidAssaultSquad.status = 'r';
            squads.add(nextRaidAssaultSquad);
            nextRaidAssaultSquad = null;
        }
    }

    public void addShieldMob(Fighter f){
        List<ShieldSquad> dead = new ArrayList<>();
        for (ShieldSquad s:shieldSquads){
            if (s.numFelons == 0){
                // disband felonless squads and repurpose the units as regular mobs until a new felon appears.
                List<Fighter> fit = s.disband();
                for (Fighter fi: fit){
                    addAssault(fi);
                    fi.moveTo(fi.squad.target);
                    retreatHandler.addCoward(fi.getUnit());
                }
            }
            if (s.isDead()) dead.add(s);
        }
        shieldSquads.removeAll(dead);

        if (f.getUnit().getDef().getName().equals("shieldfelon")) {
            // Create a new shieldball for each felon.
            retreatHandler.removeUnit(f.getUnit());
            ShieldSquad s = new ShieldSquad();
            s.setTarget(warManager.getRallyPoint(f.getPos()));
            s.status = 'a';
            shieldSquads.add(s);
            s.addUnit(f);
            callShieldMobs(s);
        }else{
            // For other units.
	        if (shieldSquads.isEmpty()) {
	        	// If there's no felon, treat shield mobs as regular assaults.
	            addAssault(f);
	        }else{
                // Add the unit to the squad with the lowest value.
	            retreatHandler.removeUnit(f.getUnit());
                ShieldSquad needed = null;
                float metalv = Float.MAX_VALUE;
                for (ShieldSquad s: shieldSquads){
                    if (s.metalValue < metalv){
                        needed = s;
                        metalv = s.metalValue;
                    }
                }
                needed.addUnit(f);
            }
        }
        fighters.put(f.id, f);
    }

    public void addAirMob(Fighter f){
        fighters.put(f.id, f);
        
        if (f.getUnit().getDef().getName().equals("gunshipheavyskirm")){
            // create a new squad if there isn't one
            if (nextBrawlerSquad == null) {
                nextBrawlerSquad = new Squad();
                nextBrawlerSquad.setTarget(warManager.getRallyPoint(f.getPos()));
                nextBrawlerSquad.income = ecoManager.effectiveIncome / (ai.mergedAllies == 0 ? 1 : 2);
                nextBrawlerSquad.isAirSquad = true;
            }
    
            nextBrawlerSquad.addUnit(f);
    
            if (nextBrawlerSquad.metalValue > nextBrawlerSquad.income * 45) {
                nextBrawlerSquad.status = 'r';
                squads.add(nextBrawlerSquad);
                nextBrawlerSquad.setTarget(graphManager.getAllyCenter());
                nextBrawlerSquad = null;
            }
        }else {
            // create a new squad if there isn't one
            if (nextAirSquad == null) {
                nextAirSquad = new Squad();
                nextAirSquad.setTarget(warManager.getRallyPoint(f.getPos()));
                nextAirSquad.income = ecoManager.effectiveIncome / (ai.mergedAllies == 0 ? 1f : 2f);
                nextAirSquad.isAirSquad = true;
            }
    
            nextAirSquad.addUnit(f);
    
            if (nextAirSquad.metalValue > nextAirSquad.income * 45f) {
                nextAirSquad.status = 'r';
                squads.add(nextAirSquad);
                nextAirSquad.setTarget(graphManager.getAllyCenter());
                nextAirSquad = null;
            }
        }
    }

    private void callShieldMobs(ShieldSquad s){
        // Call a flash mob when a felon appears.
        for (Fighter f: fighters.values()){
            if (!f.shieldMob || f.squad instanceof ShieldSquad || retreatHandler.isRetreating(f.getUnit())) continue;
            if (f.squad != null) {
                f.squad.removeUnit(f);
                f.squad = null;
            }
            retreatHandler.removeUnit(f.getUnit());
            s.addUnit(f);
        }
    }

    public void removeUnit(Unit u){
        if (fighters.containsKey(u.getUnitId())){
            Fighter f = fighters.get(u.getUnitId());
            if (f.squad != null) {
                f.squad.removeUnit(f);
            }
            fighters.remove(f.id);
        }
    }

    public void removeFromSquad(Unit u){
        if (fighters.containsKey(u.getUnitId())) {
            Fighter f = fighters.get(u.getUnitId());
            if (f.squad != null) {
                f.squad.removeUnit(f);
                f.squad = null;
            }
        }
    }

    void cleanUnits(){
        List<Integer> invalidFighters = new ArrayList<Integer>();
        for (Fighter f:fighters.values()){
            if (f.getUnit().getHealth() <= 0 || f.getUnit().getTeam() != ai.teamID){
                if (f.squad != null) {
                    f.squad.removeUnit(f);
                }
                invalidFighters.add(f.id);
            }else if (!retreatHandler.isRetreating(f.getUnit()) && f.isStuck(frame)){
                if (f.squad != null) {
	                f.squad.removeUnit(f);
                }
                invalidFighters.add(f.id);
                ai.ecoManager.combatReclaimTasks.add(new CombatReclaimTask(f.getUnit()));
                warManager.retreatHandler.removeUnit(f.getUnit());
            }
        }
        for (Integer key:invalidFighters){
            fighters.remove(key);
        }
    }

    private void updateSquads(){
        // set the rally point for the next forming squad for defense
        if (nextSquad != null && !nextSquad.isDead() && squadCounter == 0) {
            AIFloat3 nspos = nextSquad.getPos();
            if (warManager.getThreat(nspos) > 2f * warManager.getTotalFriendlyThreat(nspos) || warManager.getPorcThreat(nspos) > 0 || warManager.getPorcThreat(nextSquad.target) > 0) {
                nextSquad.retreatTo(graphManager.getClosestHaven(nextSquad.getPos()));
            }else {
                nextSquad.setTarget(warManager.getRallyPoint(nextSquad.getPos()));
            }
        }else if (nextAirSquad != null && !nextAirSquad.isDead() && squadCounter == 1) {
            AIFloat3 target = warManager.getAirRallyPoint(nextAirSquad.getPos());
            if (distance(nextAirSquad.getPos(), target) > 1800f){
                nextAirSquad.retreatTo(target);
            }else {
                nextAirSquad.setTarget(target);
            }
        }else if (nextBrawlerSquad != null && !nextBrawlerSquad.isDead() && squadCounter == 2) {
            AIFloat3 target = warManager.getAirRallyPoint(nextBrawlerSquad.getPos());
            if (distance(nextBrawlerSquad.getPos(), target) > 1800f){
                nextBrawlerSquad.retreatTo(target);
            }else {
                nextBrawlerSquad.setTarget(target);
            }
        }else if (nextRaidAssaultSquad != null && !nextRaidAssaultSquad.isDead() && squadCounter == 3) {
	        AIFloat3 target = warManager.getAirRallyPoint(nextRaidAssaultSquad.getPos());
	        if (distance(nextRaidAssaultSquad.getPos(), target) > 1800f){
		        nextRaidAssaultSquad.retreatTo(target);
	        }else {
		        nextRaidAssaultSquad.setTarget(target);
	        }
        }
        squadCounter++;
        if (squadCounter > 3){
            squadCounter = 0;
        }
    
        if (!shieldSquads.isEmpty()) {
            // shields require special handling.
        
            //First kill off any dead shield squads.
            List<ShieldSquad> dead = new ArrayList<>();
            for (ShieldSquad s:shieldSquads){
                if (s.numFelons == 0){
                    // disband felonless squads and repurpose the units as regular mobs until a new felon appears.
                    List<Fighter> fi = s.disband();
                    for (Fighter f: fi){
                        addAssault(f);
                        f.moveTo(f.squad.target);
                        retreatHandler.addCoward(f.getUnit());
                    }
                }
                if (s.isDead()) dead.add(s);
            }
            shieldSquads.removeAll(dead);
        
            // Then process any living squads, of which there are only ever at most 4.
            ShieldSquad nextShieldSquad = shieldSquads.poll();
            if (nextShieldSquad.getHealth() < 0.85
                      || nextShieldSquad.getShields() < 0.35f
                      || (nextShieldSquad.leader != null && nextShieldSquad.leader.getUnit().getRulesParamFloat("disarmed", 0) > 0)
                      || (nextShieldSquad.leader != null && nextShieldSquad.leader.getUnit().getHealth() / nextShieldSquad.leader.getUnit().getMaxHealth() < 0.75)) {
                nextShieldSquad.retreatTo(graphManager.getClosestHaven(nextShieldSquad.getAvgPos()));
            } else if (nextShieldSquad.metalValue > 1300f && nextShieldSquad.leader.getUnit().getDef().getName().equals("shieldfelon")) {
                AIFloat3 target = warManager.getArtyTarget(nextShieldSquad.getPos(), true);
                nextShieldSquad.setTarget(target);
            } else if (warManager.getEffectiveThreat(nextShieldSquad.getPos()) > 0f || warManager.getPorcThreat(nextShieldSquad.getPos()) > 0f) {
                nextShieldSquad.retreatTo(graphManager.getClosestHaven(nextShieldSquad.getAvgPos()));
            } else {
                nextShieldSquad.setTarget(warManager.getRallyPoint(nextShieldSquad.getPos()));
            }
            shieldSquads.add(nextShieldSquad);
        }

        List<Squad> deadSquads = new ArrayList<Squad>();
        boolean assigned = false;

        for (Squad s: squads){
            s.cutoff();
            if (s.isDead()){
                deadSquads.add(s);
                continue;
            }
            
            AIFloat3 pos = s.getPos();

            // set rallying for squads that are finished forming and gathering to attack
            if (s.status == 'r' && !s.assigned){
                assigned = true;
                s.assigned = true;
                s.setTarget(warManager.getRallyPoint(pos));
                if (s.isRallied(frame)){
                    s.status = 'a';
                }
                break;
            }else if (s.status == 'a' && !s.assigned){
                if (!s.isAirSquad && warManager.getEffectiveThreat(pos) > 0 && (!graphManager.isAllyTerritory(pos) || warManager.getPorcThreat(pos) > 0)){
                    s.retreatTo(graphManager.getClosestHaven(pos));
                    assigned = true;
                    s.assigned = true;
                    break;
                }else if (s.isAirSquad && warManager.getEffectiveAAThreat(pos) > 0){
                    s.retreatTo(graphManager.getClosestAirHaven(pos));
                    assigned = true;
                    s.assigned = true;
                    break;
                }
                
                Unit leader = s.getLeader();
                if (leader == null){
                    deadSquads.add(s);
                    continue;
                }

                AIFloat3 target;
                if (s.isAirSquad) {
                    target = warManager.getAirTarget(leader, true);
                } else {
                    target = warManager.getTarget(leader, true);
                }
                
                assigned = true;
                s.assigned = true;
                if (s.isAirSquad && distance(target, pos) > 1800f){
                    s.retreatTo(target);
                }else {
                    s.setTarget(target);
                }
                break;
                
            }
        }

        squads.removeAll(deadSquads);
        if (!assigned){
            for (Squad s: squads){
                s.assigned = false;
            }
        }
    }
}
