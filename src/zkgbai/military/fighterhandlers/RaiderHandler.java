package zkgbai.military.fighterhandlers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.EconomyManager;
import zkgbai.graph.GraphManager;
import zkgbai.graph.MetalSpot;
import zkgbai.military.Enemy;
import zkgbai.military.MilitaryManager;
import zkgbai.kgbutil.Pathfinder;
import zkgbai.military.UnitClasses;
import zkgbai.military.tasks.ScoutTask;
import zkgbai.military.unitwrappers.Fighter;
import zkgbai.military.unitwrappers.Raider;
import zkgbai.military.unitwrappers.RaiderSquad;

import java.util.*;

import static zkgbai.kgbutil.KgbUtil.*;
import static java.lang.Math.*;

/**
 * Created by haplo on 1/3/2016.
 */
public class RaiderHandler {
    ZKGraphBasedAI ai;
    MilitaryManager warManager;
    RetreatHandler retreatHandler;
    GraphManager graphManager;
    EconomyManager ecoManager;

    RaiderSquad nextSmallRaiderSquad = null;
    RaiderSquad nextScorcherSquad = null;
    RaiderSquad nextBansheeSquad = null;
    RaiderSquad nextScytheSquad = null;
    RaiderSquad nextHalberdSquad = null;
    public List<Raider> soloRaiders;
    public Map<Integer, Raider> smallRaiders;
    public Map<Integer, Raider> mediumRaiders;
    List<RaiderSquad> raiderSquads;

    List<ScoutTask> scoutTasks;

    Pathfinder pathfinder;

    int frame;

    public RaiderHandler(){
        this.ai = ZKGraphBasedAI.getInstance();
        this.warManager = ai.warManager;
        this.graphManager = ai.graphManager;
        this.ecoManager = ai.ecoManager;
        this.pathfinder = Pathfinder.getInstance();
        this.retreatHandler = warManager.retreatHandler;

        this.soloRaiders = new ArrayList<Raider>();
        this.smallRaiders = new HashMap<Integer, Raider>();
        this.mediumRaiders = new HashMap<Integer, Raider>();
        this.raiderSquads = new ArrayList<RaiderSquad>();

        this.scoutTasks = new ArrayList<ScoutTask>();
    }

    public void addSmallRaider(Raider r){
        if (soloRaiders.size() < 2 && !smallRaiders.containsKey(r.id)){
            soloRaiders.add(r);
            return;
        }

        smallRaiders.put(r.id, r);

        if (nextSmallRaiderSquad == null){
            nextSmallRaiderSquad = new RaiderSquad();
            nextSmallRaiderSquad.raid(warManager.getRaiderRally(r.getPos()), frame);
        }
        nextSmallRaiderSquad.addUnit(r, frame);

        if (nextSmallRaiderSquad.raiders.size() >= min(12, max(6, (int) floor(ecoManager.effectiveIncome/5f)))){
            raiderSquads.add(nextSmallRaiderSquad);
            nextSmallRaiderSquad.status = 'r';
            nextSmallRaiderSquad = null;
        }
    }

    public void addMediumRaider(Raider r) {
        mediumRaiders.put(r.id, r);

        String defName = r.getUnit().getDef().getName();

        if (defName.equals("corgator")){
            //for scorchers
            if (nextScorcherSquad == null){
                nextScorcherSquad = new RaiderSquad();
                nextScorcherSquad.raid(warManager.getRaiderRally(r.getPos()), frame);
            }
            nextScorcherSquad.addUnit(r, frame);

            if (nextScorcherSquad.raiders.size() >= (int) min(8f, max(4f, floor(ecoManager.effectiveIncome / 4f)))){
                raiderSquads.add(nextScorcherSquad);
                nextScorcherSquad.status = 'r';
                nextScorcherSquad = null;
            }

        }else if (defName.equals("armkam")){
            // for banshees
            if (nextBansheeSquad == null){
                nextBansheeSquad = new RaiderSquad();
                nextBansheeSquad.raid(warManager.getRaiderRally(r.getPos()), frame);
            }
            nextBansheeSquad.addUnit(r, frame);

            if (nextBansheeSquad.raiders.size() >= (int) min(6, max(4, floor(ecoManager.effectiveIncome / 5)))){
                raiderSquads.add(nextBansheeSquad);
                nextBansheeSquad.status = 'r';
                nextBansheeSquad = null;
            }
        }else if (defName.equals("spherepole")) {
            // for scythes
            if (nextScytheSquad == null) {
                nextScytheSquad = new RaiderSquad();
                nextScytheSquad.raid(warManager.getRaiderRally(r.getPos()), frame);
            }
            nextScytheSquad.addUnit(r, frame);

            if (nextScytheSquad.raiders.size() >= (int) min(4, max(2, floor(ecoManager.effectiveIncome / 10)))) {
                raiderSquads.add(nextScytheSquad);
                nextScytheSquad.status = 'r';
                nextScytheSquad = null;
            }
        }else if (defName.equals("hoverassault")) {
            // for scythes
            if (nextHalberdSquad == null) {
                nextHalberdSquad = new RaiderSquad();
                nextHalberdSquad.raid(warManager.getRaiderRally(r.getPos()), frame);
            }
            nextHalberdSquad.addUnit(r, frame);

            if (nextHalberdSquad.raiders.size() >= (int) min(4, max(2, floor(ecoManager.effectiveIncome / 10)))) {
                raiderSquads.add(nextHalberdSquad);
                nextHalberdSquad.status = 'r';
                nextHalberdSquad = null;
            }
        }
    }

    public void addSoloRaider(Raider r){
        soloRaiders.add(r);
    }

    public void removeUnit(Unit u){
        for (Raider r: soloRaiders){
            if (r.id == u.getUnitId()){
                if (r.getTask() != null){
                    r.getTask().removeRaider(r);
                }
            }
        }
        Raider r = new Raider(u, 0);
        soloRaiders.remove(r);
        if (smallRaiders.containsKey(u.getUnitId())){
            Raider sr = smallRaiders.get(u.getUnitId());
            if (sr.squad != null){
                sr.squad.removeUnit(sr);
            }

            if (sr.getTask() != null){
                sr.getTask().removeRaider(r);
            }
            smallRaiders.remove(u.getUnitId());
        }else if (mediumRaiders.containsKey(u.getUnitId())){
            Raider mr = mediumRaiders.get(u.getUnitId());
            if (mr.squad != null){
                mr.squad.removeUnit(mr);
            }

            if (mr.getTask() != null){
                mr.getTask().removeRaider(r);
            }
            mediumRaiders.remove(u.getUnitId());
        }
    }

    public void removeFromSquad(Unit u){
        if (smallRaiders.containsKey(u.getUnitId())){
            Raider r = smallRaiders.get(u.getUnitId());
            if (r.squad != null){
                r.squad.removeUnit(r);
                r.squad = null;
            }

            if (r.getTask() != null){
                r.getTask().removeRaider(r);
                r.clearTask();
            }
        }else if (mediumRaiders.containsKey(u.getUnitId())){
            Raider r = mediumRaiders.get(u.getUnitId());
            if (r.squad != null){
                r.squad.removeUnit(r);
                r.squad = null;
            }

            if (r.getTask() != null){
                r.getTask().removeRaider(r);
                r.clearTask();
            }
        }
    }

    public void update(int frame){
        this.frame = frame;

        if(frame%15 == 0) {
            createScoutTasks();
            checkScoutTasks();

            for (Raider r:smallRaiders.values()){
                if (r.squad == null && !retreatHandler.isRetreating(r.getUnit())){
                    addSmallRaider(r);
                }
            }

            for (Raider r:mediumRaiders.values()){
                if (r.squad == null && !retreatHandler.isRetreating(r.getUnit())){
                    addMediumRaider(r);
                }
            }

            assignRaiders();
        }
    }

    private void createScoutTasks(){
        List<MetalSpot> unscouted = graphManager.getEnemyTerritory();
        if (unscouted.isEmpty() && scoutTasks.isEmpty()){
            unscouted = graphManager.getUnownedSpots(); // if enemy territory is not known, get all spots not in our own territory.
        }

        for (MetalSpot ms: unscouted){
            ScoutTask st = new ScoutTask(ms.getPos(), ms);
            if (!scoutTasks.contains(st)){
                scoutTasks.add(st);
            }
        }
    }

    private void checkScoutTasks(){
        List<ScoutTask> finished = new ArrayList<ScoutTask>();
        if (graphManager.getEnemyTerritory().isEmpty()){
            for (ScoutTask st: scoutTasks){
                if (st.spot.visible){
                    st.endTask(frame);
                    finished.add(st);
                }
            }
        }else {
            for (ScoutTask st : scoutTasks) {
                if (!st.spot.hostile && !st.spot.enemyShadowed) {
                    st.endTask(frame);
                    finished.add(st);
                }
            }
        }
        scoutTasks.removeAll(finished);
    }

    private float getScoutCost(ScoutTask task,  Raider raider){
        float cost = distance(task.target, raider.getPos());
        if (task.spot.hostile){
            cost /= 4f;
        }else {
            if ((frame - task.spot.getLastSeen()) < 450) {
                cost += 3000f;
            }else {
                // reduce cost relative to every 30 seconds since last seen for enemy shadowed spots
                cost /= ((frame - task.spot.getLastSeen()) / 900);
            }

            // disprefer scouting the same non-hostile spot with more than one raider.
            if ((!task.assignedRaiders.isEmpty() && !task.assignedRaiders.contains(raider))){
                cost += 2000f;
            }
        }

        cost += (1000f * warManager.getThreat(task.target));

        if (warManager.getEffectiveThreat(task.target) > warManager.getFriendlyThreat(raider.getPos())){
            cost += 9001f;
        }
        return cost;
    }

    private void assignRaiders(){
        boolean needUnstick = false;
        if (frame % 120 == 0){
            needUnstick = true;

            // assign rally points for new squads
            if (nextSmallRaiderSquad != null && !nextSmallRaiderSquad.raiders.isEmpty()){
                boolean overThreat = (warManager.getEffectiveThreat(nextSmallRaiderSquad.getPos()) > 0);
                if (!overThreat) {
                    nextSmallRaiderSquad.raid(warManager.getRaiderRally(nextSmallRaiderSquad.getPos()), frame);
                }else{
                    nextSmallRaiderSquad.sneak(graphManager.getClosestHaven(nextSmallRaiderSquad.getPos()), frame);
                }
            }

            if (nextScorcherSquad != null && !nextScorcherSquad.raiders.isEmpty()){
                boolean overThreat = (warManager.getEffectiveThreat(nextScorcherSquad.getPos()) > 0);
                if (!overThreat) {
                    nextScorcherSquad.raid(warManager.getRaiderRally(nextScorcherSquad.getPos()), frame);
                }else{
                    nextScorcherSquad.sneak(graphManager.getClosestHaven(nextScorcherSquad.getPos()), frame);
                }
            }

            if (nextBansheeSquad != null && !nextBansheeSquad.raiders.isEmpty()){
                boolean overThreat = (warManager.getEffectiveThreat(nextBansheeSquad.getPos()) > 0);
                if (!overThreat) {
                    nextBansheeSquad.raid(warManager.getRaiderRally(nextBansheeSquad.getPos()), frame);
                }else{
                    nextBansheeSquad.sneak(graphManager.getClosestHaven(nextBansheeSquad.getPos()), frame);
                }
            }

            if (nextScytheSquad != null && !nextScytheSquad.raiders.isEmpty()){
                boolean overThreat = (warManager.getEffectiveThreat(nextScytheSquad.getPos()) > 0);
                if (!overThreat) {
                    nextScytheSquad.raid(warManager.getRaiderRally(nextScytheSquad.getPos()), frame);
                }else{
                    nextScytheSquad.sneak(graphManager.getClosestHaven(nextScytheSquad.getPos()), frame);
                }
            }

            if (nextHalberdSquad != null && !nextHalberdSquad.raiders.isEmpty()){
                boolean overThreat = (warManager.getEffectiveThreat(nextHalberdSquad.getPos()) > 0);
                if (!overThreat) {
                    nextHalberdSquad.raid(warManager.getRaiderRally(nextHalberdSquad.getPos()), frame);
                }else{
                    nextHalberdSquad.sneak(graphManager.getClosestHaven(nextHalberdSquad.getPos()), frame);
                }
            }
        }

        // assign soloraid/scout group
        for (Raider r: soloRaiders){
            boolean overThreat = (warManager.getEffectiveThreat(r.getPos()) > 0);
            if (retreatHandler.isRetreating(r.getUnit()) || r.getUnit().getHealth() <= 0
                    || (r.getUnit().getDef().getName().equals("spherepole") && !r.getUnit().isCloaked() && !overThreat)){
                continue;
            }

            if (needUnstick){
                r.unstick(frame);
            }

            ScoutTask bestTask = null;
            AIFloat3 bestTarget = null;
            float cost = Float.MAX_VALUE;

            for (ScoutTask s:scoutTasks){
                if (warManager.getEffectiveThreat(s.target) > warManager.getFriendlyThreat(r.getPos())){
                    continue;
                }

                float tmpcost = getScoutCost(s, r);
                if (tmpcost < cost){
                    cost = tmpcost;
                    bestTask = s;
                }
            }

            for (Enemy e: warManager.getTargets()){
                if (warManager.getEffectiveThreat(e.position) > warManager.getFriendlyThreat(r.getPos()) || e.isRiot){
                    continue;
                }

                float tmpcost = distance(r.getPos(), e.position);
                if (!e.isArty && !e.isWorker && !e.isStatic){
                    tmpcost += 1000;
                }
                if (e.isWorker){
                    tmpcost = (tmpcost/4)-100;
                    tmpcost += 750f * warManager.getThreat(e.position);
                }
                if (e.isStatic && e.getDanger() > 0f){
                    tmpcost = (tmpcost/4)-100;
                    tmpcost += 500f * warManager.getThreat(e.position);
                }

                if (tmpcost < cost){
                    cost = tmpcost;
                    bestTarget = e.position;
                }
            }

            if (bestTarget != null){
                if (r.getTask() != null) {
                    r.getTask().removeRaider(r);
                    r.clearTask();
                }

                if (overThreat || distance(bestTarget, r.getPos()) > 500) {
                    r.sneak(bestTarget, frame);
                } else {
                    r.raid(bestTarget, frame);
                }
            }else if (bestTask != null && (overThreat || bestTask != r.getTask() || r.getUnit().getCurrentCommands().isEmpty())){
                if (overThreat || distance(bestTask.target, r.getPos()) > 500) {
                    r.sneak(bestTask.target, frame);
                } else {
                    r.raid(bestTask.target, frame);
                }

                if (r.getTask() == null || !r.getTask().equals(bestTask)) {
                    if (r.getTask() != null) {
                        r.getTask().removeRaider(r);
                    }
                    bestTask.addRaider(r);
                    r.setTask(bestTask);
                }
            }
        }

        // assign raider squads
        List<RaiderSquad> deadSquads = new ArrayList<RaiderSquad>();
        for (RaiderSquad rs:raiderSquads){
            if (rs.isDead()){
                deadSquads.add(rs);
                continue;
            }

            boolean overThreat = false;
            for (Raider r:rs.raiders){
                if (warManager.getEffectiveThreat(r.getPos()) > 0){
                    overThreat = true;
                }
            }

            if (rs.status == 'r'){
                // rally rallying squads
                if (overThreat){
                    rs.sneak(graphManager.getClosestHaven(rs.getPos()), frame);
                }else {
                    if (rs.isRallied(frame)) {
                        rs.status = 'a';
                    }
                }
            }else{
                // for ready squads, assign to a target
                ScoutTask bestTask = null;
                AIFloat3 bestTarget = null;
                float cost = Float.MAX_VALUE;

                for (ScoutTask s:scoutTasks){
                    if (warManager.getEffectiveThreat(s.target) > warManager.getFriendlyThreat(rs.getPos())){
                        continue;
                    }

                    float tmpcost = getScoutCost(s, rs.leader);
                    if (tmpcost < cost){
                        cost = tmpcost;
                        bestTask = s;
                    }
                }

                for (Enemy e: warManager.getTargets()){
                    if (warManager.getEffectiveThreat(e.position) > warManager.getFriendlyThreat(rs.getPos()) || e.isRiot){
                        continue;
                    }

                    float tmpcost = distance(rs.getPos(), e.position);
                    if (!e.isArty && !e.isWorker && !e.isStatic){
                        tmpcost += 1000;
                    }
                    if (e.isWorker){
                        tmpcost = (tmpcost/4)-100;
                    }

                    if (e.isStatic && e.getDanger() > 0f){
                        tmpcost = (tmpcost/5)-200;
                    }

                    if (tmpcost < cost){
                        cost = tmpcost;
                        bestTarget = e.position;
                    }
                }

                if (bestTarget != null){
                    if (rs.leader.getTask() != null) {
                        rs.leader.getTask().removeRaider(rs.leader);
                        rs.leader.clearTask();
                    }
                    if (overThreat || distance(bestTarget, rs.getPos()) > 500) {
                        rs.sneak(bestTarget, frame);
                    }else{
                        rs.raid(bestTarget, frame);
                    }
                }else if (bestTask != null && (overThreat || bestTask != rs.leader.getTask() || rs.leader.getUnit().getCurrentCommands().isEmpty())){
                    if (overThreat || distance(bestTask.target, rs.getPos()) > 500) {
                        rs.sneak(bestTask.target, frame);
                    }else{
                        rs.raid(bestTask.target, frame);
                    }

                    if (rs.leader.getTask() == null || !rs.leader.getTask().equals(bestTask)) {
                        if (rs.leader.getTask() != null) {
                            rs.leader.getTask().removeRaider(rs.leader);
                        }
                        bestTask.addRaider(rs.leader);
                        rs.leader.setTask(bestTask);
                    }
                }
            }
        }
        raiderSquads.removeAll(deadSquads);
    }

    public void avoidEnemies(Unit h, Unit attacker, AIFloat3 dir){
        if (smallRaiders.containsKey(h.getUnitId())) {
            Raider r = smallRaiders.get(h.getUnitId());
            if (h.getHealth() / h.getMaxHealth() < 0.8 && attacker != null && attacker.getMaxSpeed() > 0 && warManager.getEffectiveThreat(h.getPos()) <= 0) {
                float movdist = -100;
                if (r.getUnit().getDef().getName().equals("corsh")) {
                    movdist = -450;
                }
                float x = movdist * dir.x;
                float z = movdist * dir.z;
                AIFloat3 pos = h.getPos();
                AIFloat3 target = new AIFloat3();
                target.x = pos.x + x;
                target.z = pos.z + z;
                h.moveTo(target, (short) 0, frame);
            }
        }
    }

}
