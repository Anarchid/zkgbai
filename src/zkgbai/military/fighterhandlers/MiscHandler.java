package zkgbai.military.fighterhandlers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.*;
import zkgbai.ZKGraphBasedAI;
import zkgbai.graph.GraphManager;
import zkgbai.graph.MetalSpot;
import zkgbai.military.Enemy;
import zkgbai.military.MilitaryManager;
import zkgbai.military.tasks.DefenseTarget;
import zkgbai.military.unitwrappers.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static zkgbai.kgbutil.KgbUtil.*;

/**
 * Created by haplo on 1/4/2016.
 */
public class MiscHandler {
    ZKGraphBasedAI ai;
    OOAICallback callback;
    MilitaryManager warManager;
    GraphManager graphManager;

    RetreatHandler retreatHandler;
    SquadHandler squadHandler;

    java.util.Map<Integer, Fighter> supports = new HashMap<Integer, Fighter>();
    java.util.Map<Integer, Unit> sappers = new HashMap<Integer, Unit>();
    java.util.Map<Integer, Unit> berthas = new HashMap<Integer, Unit>();
    java.util.Map<Integer, Unit> swifts = new HashMap<Integer, Unit>();
    java.util.Map<Integer, Unit> activeSwifts = new HashMap<Integer, Unit>();
    public java.util.Map<Integer, Fighter> loners = new HashMap<Integer, Fighter>();
    public java.util.Map<Integer, Strider> striders = new HashMap<Integer, Strider>();
    public java.util.Map<Integer, Fighter> arties = new HashMap<Integer, Fighter>();
    public java.util.Map<Integer, Krow> krows = new HashMap<Integer, Krow>();
    java.util.Map<Integer, Raider> ultis = new HashMap<Integer, Raider>();

    int frame = 0;
    Resource m;
    
    Unit nuke = null;
    Unit zenith = null;
    Unit derp = null;
    
    int lastNukeFrame = 0;

    boolean scouted = false;

    static final int CMD_ONECLICK_WEAPON = 35000;

    public MiscHandler(){
        this.ai = ZKGraphBasedAI.getInstance();
        this.callback = ai.getCallback();
        this.warManager = ai.warManager;
        this.graphManager = ai.graphManager;

        this.retreatHandler = warManager.retreatHandler;
        this.squadHandler = warManager.squadHandler;

        this.m = callback.getResourceByName("Metal");
    }

    public void update(int frame) {
        this.frame = frame;

        if(frame % 15 == 10) {
            cleanUnits();
            updateSupports();
            updateSappers();
            updateSwifts();

            if (frame % 30 == 10) {
                for (Raider ulti: ultis.values()){
                    if (!retreatHandler.isRetreating(ulti.getUnit())) {
                        Enemy target = warManager.getUltiTarget(ulti.getPos());
                        if (target != null) {
                            if (distance(ulti.getPos(), target.position) > 300f) {
                                ulti.sneak(target.position, frame);
                            } else {
                                ulti.getUnit().attack(target.unit, (short) 0, Integer.MAX_VALUE);
                            }
                        } else {
                            try {
                                AIFloat3 tgt = graphManager.getClosestHaven(graphManager.getClosestEnemySpot(ulti.getPos()).getPos());
                                if (distance(tgt, ulti.getPos()) > 300f) {
                                    ulti.sneak(tgt, frame);
                                }
                            } catch (Exception e) {} // because unpacking all the nulls that can occur here is incredibly stupid.
                        }
                    }
                }
                
                for (Strider st : striders.values()) {
                    Unit u = st.getUnit();
                    if (!retreatHandler.isRetreating(u)) {
                        AIFloat3 target;
                        if (u.getRulesParamFloat("disarmed", 0f) > 0 || warManager.getEffectiveThreat(st.getPos()) > 0){
                            target = graphManager.getClosestHaven(u.getPos());
                        }else {
                            target = warManager.getTarget(u, false);
                        }
                        st.fightTo(target);
                    }
                }
            }
    
            dgunStriders();
            dgunKrows();
            
            if (frame % 90 == 10){
                // assign krows
                for (Krow st:krows.values()){
                    Unit u = st.getUnit();
                    if (!retreatHandler.isRetreating(u)) {
                        AIFloat3 target;
                        if (u.getRulesParamFloat("disarmed", 0f) > 0){
                            target = graphManager.getClosestAirHaven(u.getPos());
                        }else {
                            target = warManager.getKrowTarget(u.getPos());
                        }
                        st.flyTo(target, frame);
                    }
                }
    
                // assign loners
                for (Fighter l:loners.values()){
                    Unit u = l.getUnit();
                    if (!retreatHandler.isRetreating(u)){
                        AIFloat3 target;
                        if ((u.getRulesParamFloat("disarmed", 0f) > 0 || warManager.getEffectiveThreat(l.getPos()) > 0) && !l.getUnit().getDef().getName().equals("spidercrabe")){
                            target = graphManager.getClosestHaven(u.getPos());
                            l.moveTo(target);
                        }else {
                            target = warManager.getTarget(u, false);
                            l.fightTo(target);
                        }
                    }
                }
    
                // assign arties
                for (Fighter a:arties.values()){
                    Unit u = a.getUnit();
                    if (!retreatHandler.isRetreating(u)){
                        AIFloat3 target;
                        if (u.getRulesParamFloat("disarmed", 0f) > 0 || (warManager.getEffectiveThreat(a.getPos()) > 0 && u.getDef().getName().equals("amphassault"))){
                            target = graphManager.getClosestHaven(u.getPos());
                            a.moveTo(target);
                        }else if (u.getDef().getName().equals("vehheavyarty")){
                            target = warManager.getArtyTarget(u.getPos(), false);
                            target = getRadialPoint(graphManager.getClosestHaven(target), 350f);
                            a.moveTo(target);
                        } else {
                            target = warManager.getArtyTarget(u.getPos(), false);
                            a.fightTo(target);
                        }
                    }
                }
            }
            
            if (frame % 200 == 10){
                // assign berthas
                for (Unit b: berthas.values()){
                    //AIFloat3 target = warManager.getBerthaTarget(b.getPos());
                    AIFloat3 target = warManager.getSuperWepTarget(b, false);
                    if (target != null) {
                        b.attackArea(target, 0f, (short) 0, Integer.MAX_VALUE);
                    }else{
                        b.stop((short) 0, Integer.MAX_VALUE);
                    }
                }
            }
            
            if (frame % 300 == 10){
                // use nukes
                if (nuke != null && !warManager.enemyHasAntiNuke && nuke.getStockpile() > 0 && frame - lastNukeFrame > 1800){
                    AIFloat3 target = warManager.getSuperWepTarget(nuke, true);
                    if (target != null){
                        nuke.attackArea(target, 0f, (short) 0, Integer.MAX_VALUE);
                        lastNukeFrame = frame;
                    }
                }
            }
    
            if (frame % 450 == 10){
                if (zenith != null){
                    int meteors = (int) zenith.getRulesParamFloat("meteorsControlled", 0f);
                    if (meteors > 150) {
                        AIFloat3 target = warManager.getSuperWepTarget(zenith, true);
                        if (target != null) {
                            zenith.attackArea(target, 0f, (short) 0, Integer.MAX_VALUE);
                        }else{
                            zenith.stop((short) 0, Integer.MAX_VALUE);
                        }
                    }else{
                        zenith.stop((short) 0, Integer.MAX_VALUE);
                    }
                }
            }
    
            if (frame % 900 == 10){
                if (derp != null){
                    AIFloat3 target = warManager.getSuperWepTarget(derp, false);
                    if (target != null){
                        derp.attackArea(target, 0f, (short) 0, Integer.MAX_VALUE);
                    }else{
                        derp.stop((short) 0, Integer.MAX_VALUE);
                    }
                }
            }
        }
    }

    public void addLoner(Fighter f){
        loners.put(f.id, f);
    }
    
    public void addArty(Fighter f){
        arties.put(f.id, f);
    }

    public void addSupport(Fighter f){
        supports.put(f.id, f);
    }

    public void addSapper(Unit u){
        sappers.put(u.getUnitId(), u);
    }

    public void addStrider(Strider st){
        striders.put(st.id, st);
    }
    
    public void addKrow(Krow kr){
        krows.put(kr.id, kr);
    }
    
    public void addUlti(Raider r){
        ultis.put(r.id, r);
    }

    public void addBertha(Unit u){
        berthas.put(u.getUnitId(), u);
        u.setFireState(0, (short) 0, Integer.MAX_VALUE);
    }
    
    public void addNuke(Unit u){
        nuke = u;
    }
    
    public void addZenith(Unit u){
        zenith = u;
        u.setFireState(0, (short) 0, Integer.MAX_VALUE);
    }
    
    public void addDRP(Unit u){
        derp = u;
        u.setFireState(0, (short) 0, Integer.MAX_VALUE);
        u.setTrajectory(1, (short) 0, Integer.MAX_VALUE);
    
        AIFloat3 target = warManager.getSuperWepTarget(derp, true);
        if (target != null){
            derp.attackArea(target, 0f, (short) 0, Integer.MAX_VALUE);
        }else{
            derp.stop((short) 0, Integer.MAX_VALUE);
        }
    }

    public void addSwift(Unit u){
        swifts.put(u.getUnitId(), u);
        if (swifts.size() > 4 || !scouted){
            scouted = true;
            activeSwifts.putAll(swifts);
            swifts.clear();
        }
    }

    public void removeUnit(Unit unit){
        if (nuke != null && nuke.getUnitId() == unit.getUnitId()){
            nuke = null;
        }
    
        if (zenith != null && zenith.getUnitId() == unit.getUnitId()){
            zenith = null;
        }
    
        if (derp != null && derp.getUnitId() == unit.getUnitId()){
            derp = null;
        }
        
        if (loners.containsKey(unit.getUnitId())){
            loners.remove(unit.getUnitId());
        }

        if (supports.containsKey(unit.getUnitId())){
            Fighter f = supports.get(unit.getUnitId());
            if (f.getUnit().getDef().getName().equals("shieldshield")){
                // decrement aspis count
                if (f.squad != null){
                    ShieldSquad sq = (ShieldSquad) f.squad;
                    sq.numAspis--;
                }
            }
            supports.remove(unit.getUnitId());
        }

        if (striders.containsKey(unit.getUnitId())){
            striders.remove(unit.getUnitId());
        }
    
        if (krows.containsKey(unit.getUnitId())){
            krows.remove(unit.getUnitId());
        }
    
        if (arties.containsKey(unit.getUnitId())){
            arties.remove(unit.getUnitId());
        }

        if (sappers.containsKey(unit.getUnitId())){
            sappers.remove(unit.getUnitId());
        }

        if (swifts.containsKey(unit.getUnitId())){
            swifts.remove(unit.getUnitId());
        }
    
        if (ultis.containsKey(unit.getUnitId())){
            ultis.remove(unit.getUnitId());
        }

        if (berthas.containsKey(unit.getUnitId())){
            berthas.remove(unit.getUnitId());
        }
    }

    void cleanUnits(){
        // remove dead/captured units because spring devs are stupid and call update before unitDestroyed.
        List<Integer> invalidFighters = new ArrayList<Integer>();

        for (Fighter f:loners.values()){
            if (f.getUnit().getHealth() <= 0 || f.getUnit().getTeam() != ai.teamID){
                invalidFighters.add(f.id);
            }
        }
        for (Integer key:invalidFighters){
            loners.remove(key);
        }
        invalidFighters.clear();

        for (Fighter f:supports.values()){
            if (f.getUnit().getHealth() <= 0 || f.getUnit().getTeam() != ai.teamID){
                invalidFighters.add(f.id);
            }
        }
        for (Integer key:invalidFighters){
            supports.remove(key);
        }
    
        for (Fighter f:striders.values()){
            if (f.getUnit().getHealth() <= 0 || f.getUnit().getTeam() != ai.teamID){
                invalidFighters.add(f.id);
            }
        }
        for (Integer key:invalidFighters){
            striders.remove(key);
        }
        invalidFighters.clear();
    
        for (Fighter f:krows.values()){
            if (f.getUnit().getHealth() <= 0 || f.getUnit().getTeam() != ai.teamID){
                invalidFighters.add(f.id);
            }
        }
        for (Integer key:invalidFighters){
            krows.remove(key);
        }
        invalidFighters.clear();
    
        for (Fighter f:arties.values()){
            if (f.getUnit().getHealth() <= 0 || f.getUnit().getTeam() != ai.teamID){
                invalidFighters.add(f.id);
            }
        }
        for (Integer key:invalidFighters){
            arties.remove(key);
        }
        invalidFighters.clear();
    
        for (Fighter f:ultis.values()){
            if (f.getUnit().getHealth() <= 0 || f.getUnit().getTeam() != ai.teamID){
                invalidFighters.add(f.id);
            }
        }
        for (Integer key:invalidFighters){
            ultis.remove(key);
        }
        invalidFighters.clear();
    
        for (Unit f:sappers.values()){
            if (f.getHealth() <= 0 || f.getTeam() != ai.teamID){
                invalidFighters.add(f.getUnitId());
            }
        }
        for (Integer key:invalidFighters){
            sappers.remove(key);
        }
        invalidFighters.clear();
    
        for (Unit f:swifts.values()){
            if (f.getHealth() <= 0 || f.getTeam() != ai.teamID){
                invalidFighters.add(f.getUnitId());
            }
        }
        for (Integer key:invalidFighters){
            swifts.remove(key);
        }
        invalidFighters.clear();
    
        for (Unit f:berthas.values()){
            if (f.getHealth() <= 0 || f.getTeam() != ai.teamID){
                invalidFighters.add(f.getUnitId());
            }
        }
        for (Integer key:invalidFighters){
            berthas.remove(key);
        }
    
        if (nuke != null && (nuke.getHealth() <= 0 || nuke.getTeam() != ai.teamID)){
            nuke = null;
        }
    
        if (zenith != null && (zenith.getHealth() <= 0 || zenith.getTeam() != ai.teamID)){
            zenith = null;
        }
    
        if (derp != null && (derp.getHealth() <= 0 || derp.getTeam() != ai.teamID)){
            derp = null;
        }
    }

    private void updateSupports() {
        for (Fighter s : supports.values()) {
            if (retreatHandler.isRetreating(s.getUnit())) continue;

	        if (s.squad != null && s.squad.isDead()) s.squad = null;

            if (s.squad == null) {
                if (!squadHandler.squads.isEmpty()) {
                	float maxValue = 0;
                	Squad best = null;
                	for (Squad sq: squadHandler.squads){
                		if (sq.isAirSquad) continue;
                		if (sq.metalValue > maxValue){
                			maxValue = sq.metalValue;
                			best = sq;
		                }
	                }
                    s.squad = best;
                }
                // if we still haven't found a squad, assign it to nextSquad.
                if (s.squad == null && squadHandler.nextSquad != null && !squadHandler.nextSquad.isDead()){
	                s.squad = squadHandler.nextSquad;
                }
            }

	        if (s.squad != null) {
	            AIFloat3 spos = s.squad.getPos();
	            s.moveTo(getDirectionalPoint(s.squad.target, spos, distance(spos, s.squad.target) + 125f));
	        }else{
		        s.moveTo(graphManager.getClosestHaven(s.getPos()));
	        }
        }
    }

    void updateSappers(){
        for (Unit s : sappers.values()) {
            if (s.getHealth() <= 0 || !s.getCurrentCommands().isEmpty()) continue;

            List<Unit> enemies = callback.getEnemyUnitsIn(s.getPos(), 350f);
            Unit enemy = null;
            for (Unit e:enemies){
                if (e.getDef() != null && e.getDef().getCost(m) > 100){
                    enemy = e;
                    break;
                }
            }
            if (enemy != null){
                s.attack(enemy, (short) 0, frame+300);
                continue;
            }

            MetalSpot ms;
            if (s.getDef().getName().equals("gunshipbomb")){
                ms = graphManager.getClosestEnemySpot(s.getPos());
                if (ms != null) {
                    s.fight(ms.getPos(), (short) 0, Integer.MAX_VALUE);
                }
                continue;
            }else {
                ms = graphManager.getClosestFrontLineSpot(s.getPos());
            }

            if (ms == null){
                ms = graphManager.getClosestNeutralSpot(s.getPos());
            }

            if (ms != null && distance(s.getPos(), ms.getPos()) > 500) {
                s.fight(getAngularPoint(ms.getPos(), graphManager.getEnemyCenter(), 350f), (short) 0, Integer.MAX_VALUE);
            }
        }
    }

    private void updateSwifts() {
        List<MetalSpot> spots = graphManager.getUnownedSpots();
        for (Unit u: activeSwifts.values()){
            if (u.getHealth() <= 0) continue;
            MetalSpot best = null;
            float lastSeen = 0;
            for (MetalSpot ms:spots){
                float tmpseen = frame - ms.getLastSeen();
                if (tmpseen > lastSeen){
                    best = ms;
                    lastSeen = ms.getLastSeen();
                }
            }

            if (best != null){
                u.moveTo(best.getPos(), (short) 0, Integer.MAX_VALUE);
                if (distance(u.getPos(), graphManager.getEnemyCenter()) < distance(u.getPos(), graphManager.getAllyCenter())){
                    List<Float> params = new ArrayList<>();
                    u.executeCustomCommand(CMD_ONECLICK_WEAPON, params, (short) 0, Integer.MAX_VALUE);
                }
                spots.remove(best);
            }
        }
    }

    private void dgunStriders(){
        for (Strider s:striders.values()){
            String defName = s.getUnit().getDef().getName();
            if (!defName.equals("striderdante") && !defName.equals("striderscorpion") && !defName.equals("striderbantha")){
                continue;
            }
            
            if (s.isDgunReady(frame)) {
                Unit target = getDgunTarget(s);
                if (target != null) {
                    s.getUnit().dGun(target, (short) 0, Integer.MAX_VALUE);
                }
            }
        }
    }

    private Unit getDgunTarget(Strider s){
        AIFloat3 pos = s.getPos();
        List<Unit> enemies = ai.getCallback().getEnemyUnitsIn(pos, 450f);
        Unit target = null;
        float bestScore = 0;

        if (s.getUnit().getDef().getName().equals("striderdante")) {
            for (Unit e : enemies) {
                float cost = e.getDef().getCost(m);
                if (e.getMaxSpeed() > 0 && !e.getDef().isAbleToFly() && cost > 200f) {
                    if (cost > bestScore) {
                        bestScore = cost;
                        target = e;
                    }
                } else if (e.getMaxSpeed() == 0 && !e.getDef().getName().equals("wolverine_mine") && !e.getDef().getName().equals("turretaalaser")) {
                    if (cost > bestScore) {
                        bestScore = cost;
                        target = e;
                    }
                }
            }
        }else{
            // scorpion and bantha both have emp dguns, so they shouldn't shoot at unarmed crap.
            for (Unit e : enemies) {
                float cost = e.getDef().getCost(m);
                if (!e.getDef().isAbleToAttack() || e.getDef().getTooltip().contains("Anti-Air")){
                    continue;
                }
                if (e.getMaxSpeed() > 0 && (!e.getDef().isAbleToFly() || e.getDef().getName().equals("gunshipkrow")) && cost > 200) {
                    if (cost > bestScore) {
                        bestScore = cost;
                        target = e;
                    }
                } else if (e.getMaxSpeed() == 0 && !e.getDef().getName().equals("wolverine_mine")) {
                    if (cost > bestScore) {
                        bestScore = cost;
                        target = e;
                    }
                }
            }
        }
        return target;
    }
    
    private void dgunKrows(){
        List<Float> params = new ArrayList<>();
        for (Krow s:krows.values()){
            if (s.isDgunReady(frame)) {
                List<Unit> enemies = ai.getCallback().getEnemyUnitsIn(s.getPos(), 175f);
                float cost = 0;
                for (Unit e : enemies) {
                    if (!e.getDef().isAbleToFly() && !e.getDef().getName().equals("wolverine_mine")){
                        if (e.getMaxSpeed() == 0){
                            cost += e.getDef().getCost(m) * 2f * (e.isBeingBuilt() ? e.getHealth()/e.getMaxHealth() : 1f);
                        }else {
                            cost += e.getDef().getCost(m) * (e.isBeingBuilt() ? e.getHealth()/e.getMaxHealth() : 1f);
                        }
                        if (cost > 300f) {
                            s.getUnit().executeCustomCommand(CMD_ONECLICK_WEAPON, params, (short) 0, Integer.MAX_VALUE);
                            break;
                        }
                    }
                }
            }
        }
    }
}
