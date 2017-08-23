package zkgbai.military.fighterhandlers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.EconomyManager;
import zkgbai.graph.GraphManager;
import zkgbai.military.MilitaryManager;
import zkgbai.military.UnitClasses;
import zkgbai.military.unitwrappers.Fighter;
import zkgbai.military.unitwrappers.ShieldSquad;
import zkgbai.military.unitwrappers.Squad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    public java.util.Map<Integer, Fighter> fighters;
    public Squad nextSquad;
    public Squad nextAirSquad;
    public Squad nextBrawlerSquad;
    public ShieldSquad nextShieldSquad;
    public List<Squad> squads;

    int frame = 0;
    int squadCounter = 0;

    public SquadHandler(){
        this.ai = ZKGraphBasedAI.getInstance();
        this.warManager = ai.warManager;
        this.ecoManager = ai.ecoManager;
        this.graphManager = ai.graphManager;

        this.retreatHandler = warManager.retreatHandler;
        this.unitTypes = UnitClasses.getInstance();

        this.fighters = new HashMap<Integer, Fighter>();
        this.squads = new ArrayList<Squad>();
        this.nextSquad = null;
        this.nextShieldSquad = null;
        this.nextAirSquad = null;
        this.nextBrawlerSquad = null;
    }

    public void update(int frame){
        this.frame = frame;

        if (frame % 30 == 0){

            for (Fighter f:fighters.values()){
                Unit u = f.getUnit();
                if (f.squad == null && !retreatHandler.isRetreating(u)) {
                    if (unitTypes.airMobs.contains(u.getDef().getName())){
                        addAirMob(f);
                    }else if (unitTypes.assaults.contains(u.getDef().getName())){
                        addAssault(f);
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
            nextSquad.setTarget(warManager.getRallyPoint(f.getPos()), frame);
            nextSquad.income = ecoManager.effectiveIncome/(1f + ((float) ai.mergedAllies * graphManager.territoryFraction * graphManager.territoryFraction));
        }

        nextSquad.addUnit(f, frame);

        if ((nextSquad.metalValue > nextSquad.income * 45 && nextSquad.metalValue > 1000) || nextSquad.metalValue > 2500){
            nextSquad.status = 'r';
            squads.add(nextSquad);
            nextSquad = null;
        }
    }

    public void addShieldMob(Fighter f){
        fighters.put(f.id, f);

        // create a new squad if there isn't one
        if (nextShieldSquad == null){
            nextShieldSquad = new ShieldSquad();
            nextShieldSquad.setTarget(warManager.getRallyPoint(f.getPos()), frame);
            nextShieldSquad.status = 'a';
        }
        nextShieldSquad.addUnit(f, frame);
    }

    public void addAirMob(Fighter f){
        fighters.put(f.id, f);
        
        if (f.getUnit().getDef().getName().equals("gunshipheavyskirm")){
            // create a new squad if there isn't one
            if (nextBrawlerSquad == null) {
                nextBrawlerSquad = new Squad();
                nextBrawlerSquad.setTarget(warManager.getRallyPoint(f.getPos()), frame);
                nextBrawlerSquad.income = ecoManager.effectiveIncome / (ai.mergedAllies == 0 ? 1 : 2);
                nextBrawlerSquad.isAirSquad = true;
            }
    
            nextBrawlerSquad.addUnit(f, frame);
    
            if (nextBrawlerSquad.metalValue > nextBrawlerSquad.income * 45) {
                nextBrawlerSquad.status = 'r';
                squads.add(nextBrawlerSquad);
                nextBrawlerSquad.setTarget(graphManager.getAllyCenter(), frame);
                nextBrawlerSquad = null;
            }
        }else {
            // create a new squad if there isn't one
            if (nextAirSquad == null) {
                nextAirSquad = new Squad();
                nextAirSquad.setTarget(warManager.getRallyPoint(f.getPos()), frame);
                nextAirSquad.income = ecoManager.effectiveIncome / (ai.mergedAllies == 0 ? 1 : 2);
                nextAirSquad.isAirSquad = true;
            }
    
            nextAirSquad.addUnit(f, frame);
    
            if (nextAirSquad.metalValue > nextAirSquad.income * 45) {
                nextAirSquad.status = 'r';
                squads.add(nextAirSquad);
                nextAirSquad.setTarget(graphManager.getAllyCenter(), frame);
                nextAirSquad = null;
            }
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
            }
        }
        for (Integer key:invalidFighters){
            fighters.remove(key);
        }
    }

    private void updateSquads(){
        // set the rally point for the next forming squad for defense
        if (nextSquad != null && !nextSquad.isDead() && squadCounter == 0) {
            if (warManager.getEffectiveThreat(nextSquad.getPos()) > 0 || warManager.getEffectiveThreat(nextSquad.target) > 0) {
                nextSquad.retreatTo(graphManager.getClosestHaven(nextSquad.getPos()), frame);
            }else {
                nextSquad.setTarget(warManager.getRallyPoint(nextSquad.getPos()), frame);
            }
        }else if (nextAirSquad != null && !nextAirSquad.isDead() && squadCounter == 1) {
            nextAirSquad.setTarget(warManager.getAirRallyPoint(nextAirSquad.getPos()), frame);
        }else if (nextBrawlerSquad != null && squadCounter == 2) {
            nextBrawlerSquad.setTarget(warManager.getAirRallyPoint(nextBrawlerSquad.getPos()), frame);
        }else if (nextShieldSquad != null && !nextShieldSquad.isDead() && squadCounter == 3) {
            // shields only get one squad into which it dumps all of its mobs.
            if (nextShieldSquad.getHealth() < 0.85
                        || (nextShieldSquad.leader != null && nextShieldSquad.leader.getUnit().getRulesParamFloat("disarmed", 0) > 0)
                    || (nextShieldSquad.leader != null && nextShieldSquad.leader.getUnit().getHealth()/nextShieldSquad.leader.getUnit().getMaxHealth() < 0.75)){
                nextShieldSquad.retreatTo(graphManager.getClosestHaven(nextShieldSquad.getAvgPos()), frame);
            }else if (nextShieldSquad.metalValue > 2000 && nextShieldSquad.leader.getUnit().getDef().getName().equals("shieldfelon")){
                AIFloat3 target = warManager.getArtyTarget(nextShieldSquad.getPos(), true);
                // reduce redundant order spam.
                if (!target.equals(nextShieldSquad.target)) {
                    nextShieldSquad.setTarget(target, frame);
                }
            }else if (warManager.getEffectiveThreat(nextShieldSquad.getPos()) > 0f){
                nextShieldSquad.retreatTo(graphManager.getClosestHaven(nextShieldSquad.getAvgPos()), frame);
            } else {
                nextShieldSquad.setTarget(warManager.getRallyPoint(nextShieldSquad.getPos()), frame);
            }
        }
        squadCounter++;
        if (squadCounter > 3){
            squadCounter = 0;
        }

        List<Squad> deadSquads = new ArrayList<Squad>();
        boolean assigned = false;

        for (Squad s: squads){
            s.cutoff();
            if (s.isDead()){
                deadSquads.add(s);
                continue;
            }

            // set rallying for squads that are finished forming and gathering to attack
            if (s.status == 'r' && !s.assigned){
                assigned = true;
                s.assigned = true;
                s.setTarget(warManager.getRallyPoint(s.getPos()), frame);
                if (s.isRallied(frame)){
                    s.status = 'a';
                }
                break;
            }else if (s.status == 'a' && !s.assigned){
                if (!s.isAirSquad && warManager.getEffectiveThreat(s.getPos()) > 0 && distance(s.getPos(), s.target) > 1200){
                    if (s.isRallied(frame)) {
                        s.retreatTo(graphManager.getClosestHaven(s.getPos()), frame);
                        assigned = true;
                        s.assigned = true;
                        break;
                    }
                }else if (s.isAirSquad && warManager.getEffectiveAAThreat(s.getPos()) > 0){
                    s.retreatTo(graphManager.getClosestAirHaven(s.getPos()), frame);
                    assigned = true;
                    s.assigned = true;
                    break;
                }

                AIFloat3 target;
                if (s.isAirSquad) {
                    target = warManager.getAirTarget(s.getPos(), true);
                } else {
                    target = warManager.getTarget(s.getPos(), true);
                }

                // reduce redundant order spam.
                //if (!target.equals(s.target)) {
                    assigned = true;
                    s.assigned = true;
                    s.setTarget(target, frame);
                    break;
                //}
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
