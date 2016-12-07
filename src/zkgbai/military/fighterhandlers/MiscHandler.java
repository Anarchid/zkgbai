package zkgbai.military.fighterhandlers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.ZKGraphBasedAI;
import zkgbai.graph.GraphManager;
import zkgbai.graph.MetalSpot;
import zkgbai.military.MilitaryManager;
import zkgbai.military.tasks.DefenseTarget;
import zkgbai.military.unitwrappers.Fighter;
import zkgbai.military.unitwrappers.Raider;
import zkgbai.military.unitwrappers.Squad;
import zkgbai.military.unitwrappers.Strider;

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

    java.util.Map<Integer, Fighter> supports;
    java.util.Map<Integer, Unit> sappers;
    java.util.Map<Integer, Unit> berthas;
    java.util.Map<Integer, Unit> swifts;
    java.util.Map<Integer, Unit> activeSwifts;
    public java.util.Map<Integer, Fighter> loners;
    public java.util.Map<Integer, Strider> striders;

    int frame = 0;
    Resource m;

    static final int CMD_ONECLICK_WEAPON = 35000;

    public MiscHandler(){
        this.ai = ZKGraphBasedAI.getInstance();
        this.callback = ai.getCallback();
        this.warManager = ai.warManager;
        this.graphManager = ai.graphManager;

        this.retreatHandler = warManager.retreatHandler;
        this.squadHandler = warManager.squadHandler;

        this.m = callback.getResourceByName("Metal");

        this.supports = new HashMap<Integer, Fighter>();
        this.loners = new HashMap<Integer, Fighter>();
        this.sappers = new HashMap<Integer, Unit>();
        this.berthas = new HashMap<Integer, Unit>();
        this.swifts = new HashMap<Integer, Unit>();
        this.activeSwifts = new HashMap<Integer, Unit>();
        this.striders = new HashMap<Integer, Strider>();
    }

    public void update(int frame) {
        this.frame = frame;

        if(frame%15 == 0) {
            cleanUnits();
            updateSupports();
            updateSappers();
            updateSwifts();

            if (frame % 30 == 0) {
                for (Strider st : striders.values()) {
                    Unit u = st.getUnit();
                    if (!retreatHandler.isRetreating(u)) {
                        AIFloat3 target = warManager.getSoloTarget(st.getPos(), false);
                        st.fightTo(target, frame);
                    }
                }
            }

            if (frame % 45 == 0) {
                dgunStriders();
            }
        }

        if (frame % 150 == 0){
            for (Unit b: berthas.values()){
                AIFloat3 target = warManager.getBerthaTarget(b.getPos());
                b.attackArea(target, 0f, (short) 0, frame+300);
            }
        }

        if (frame % 300 == 0){
            for (Fighter l:loners.values()){
                Unit u = l.getUnit();
                if (!retreatHandler.isRetreating(u)){
                    AIFloat3 target = warManager.getSoloTarget(l.getPos(), true);
                    l.fightTo(target, frame);
                }
            }
        }
    }

    public void addLoner(Fighter f){
        loners.put(f.id, f);
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

    public void addBertha(Unit u){
        berthas.put(u.getUnitId(), u);
    }

    public void addSwift(Unit u){
        swifts.put(u.getUnitId(), u);
        if (swifts.size() > 4){
            activeSwifts.putAll(swifts);
            swifts.clear();
        }
    }

    public void removeUnit(Unit unit){
        if (loners.containsKey(unit.getUnitId())){
            loners.remove(unit.getUnitId());
        }

        if (supports.containsKey(unit.getUnitId())){
            supports.remove(unit.getUnitId());
        }

        if (striders.containsKey(unit.getUnitId())){
            striders.remove(unit.getUnitId());
        }

        if (sappers.containsKey(unit.getUnitId())){
            sappers.remove(unit.getUnitId());
        }

        if (swifts.containsKey(unit.getUnitId())){
            swifts.remove(unit.getUnitId());
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
    }

    private void updateSupports() {
        for (Fighter s : supports.values()) {
            if (!retreatHandler.isRetreating(s.getUnit())) {
                if (s.squad != null) {
                    if (!s.squad.isDead()) {
                        s.moveTo(s.squad.getPos(), frame);
                    } else {
                        s.squad = null;
                    }
                }

                if (s.squad == null) {

                    if (s.getUnit().getDef().getName().equals("spherecloaker") && !squadHandler.squads.isEmpty()) {
                        int rand = (int) (Math.random() * (squadHandler.squads.size() - 1));
                        Squad randomSquad = squadHandler.squads.get(rand);
                        if (!randomSquad.isAirSquad) {
                            s.squad = randomSquad;
                        }
                    }else if (s.getUnit().getDef().getName().equals("core_spectre") && squadHandler.nextShieldSquad != null && squadHandler.nextShieldSquad.getPos() != null) {
                        s.squad = squadHandler.nextShieldSquad;
                    }

                    if (s.squad != null) {
                        if (s.squad.status == 'f') {
                            s.moveTo(s.squad.target, frame);
                        } else if (s.squad.getPos() != null) {
                            s.moveTo(s.squad.getPos(), frame);
                        }
                    }else {
                        s.moveTo(warManager.getRallyPoint(s.getPos()), frame);
                    }
                }
            }
        }
    }

    void updateSappers(){
        for (Unit s : sappers.values()) {
            if (s.getHealth() <= 0 || !s.getCurrentCommands().isEmpty()){continue;}

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

            MetalSpot ms = null;
            if (s.getDef().getName().equals("blastwing")){
                ms = graphManager.getClosestEnemySpot(s.getPos());
                if (ms != null) {
                    s.fight(ms.getPos(), (short) 0, frame + 300);
                }
                continue;
            }else {
                ms = graphManager.getClosestFrontLineSpot(s.getPos());
            }

            if (ms == null){
                ms = graphManager.getClosestNeutralSpot(s.getPos());
            }

            if (ms != null && distance(s.getPos(), ms.getPos()) > 500) {
                s.fight(getDirectionalPoint(ms.getPos(), graphManager.getEnemyCenter(), 350f), (short) 0, frame + 300);
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
                u.moveTo(best.getPos(), (short) 0, frame + 300);
                if (distance(u.getPos(), graphManager.getEnemyCenter()) < distance(u.getPos(), graphManager.getAllyCenter())){
                    List<Float> params = new ArrayList<>();
                    u.executeCustomCommand(CMD_ONECLICK_WEAPON, params, (short) 0, frame + 300);
                }
                spots.remove(best);
            }
        }
    }

    private void dgunStriders(){
        for (Strider s:striders.values()){
            String defName = s.getUnit().getDef().getName();
            if (!defName.equals("dante") && !defName.equals("scorpion") && !defName.equals("armbanth")){
                continue;
            }
            Unit target = getDgunTarget(s);
            if (target != null && frame > s.lastDgunFrame + s.dgunReload){
                s.getUnit().dGun(target, (short) 0, frame + 3000);
                s.lastDgunFrame = frame;
            }
        }
    }

    private Unit getDgunTarget(Strider s){
        AIFloat3 pos = s.getPos();
        List<Unit> enemies = ai.getCallback().getEnemyUnitsIn(pos, 450f);
        Unit target = null;
        float bestScore = 0;

        if (s.getUnit().getDef().getName().equals("dante")) {
            for (Unit e : enemies) {
                float cost = e.getDef().getCost(m);
                if (e.getMaxSpeed() > 0 && (!e.getDef().isAbleToFly() || e.getDef().getName().equals("corcrw")) && cost > 200) {
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
        }else{
            // scorpion and bantha both have emp dguns, so they shouldn't shoot at unarmed crap.
            for (Unit e : enemies) {
                float cost = e.getDef().getCost(m);
                if (!e.getDef().isAbleToAttack()){
                    continue;
                }
                if (e.getMaxSpeed() > 0 && (!e.getDef().isAbleToFly() || e.getDef().getName().equals("corcrw")) && cost > 200) {
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
}
