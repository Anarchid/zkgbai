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
import zkgbai.military.unitwrappers.Raider;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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

    List<Raider> raidQueue;
    List<Raider> mediumRaidQueue;
    public List<Raider> raiders;

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

        this.raiders = new ArrayList<Raider>();
        this.raidQueue = new ArrayList<Raider>();
        this.mediumRaidQueue = new ArrayList<Raider>();

        this.scoutTasks = new ArrayList<ScoutTask>();
    }

    public void addSmallRaider(Raider r){
        raidQueue.add(r);

        // move raiders from the holding squad into the main raider pool
        // either if there's a big enough mob or if there are no scouts.
        if (raidQueue.size() > max(4, (int) floor(ecoManager.effectiveIncome/5)) || raiders.size() < 2){
            raiders.addAll(raidQueue);
            raidQueue.clear();
        }
    }

    public void addMediumRaider(Raider r) {
        // move raiders from the holding squad into the main raider pool.
        mediumRaidQueue.add(r);
        if (mediumRaidQueue.size() >= (int) min(6, max(4, floor(ecoManager.effectiveIncome / 5)))){
            raiders.addAll(mediumRaidQueue);
            mediumRaidQueue.clear();
        }
    }

    public void addSoloRaider(Raider r){
        raiders.add(r);
    }

    public void removeUnit(Unit u){
        for (Raider r: raiders){
            if (r.id == u.getUnitId()){
                if (r.getTask() != null){
                    r.getTask().removeRaider(r);
                }
            }
        }
        Raider r = new Raider(u, 0);
        raiders.remove(r);
        raidQueue.remove(r);
        mediumRaidQueue.remove(r);
    }

    void cleanUnits(){

    }

    public void update(int frame){
        this.frame = frame;

        if(frame%15 == 0) {
            createScoutTasks();
            checkScoutTasks();

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
            cost /= 4;
        }else {
            if ((frame - task.spot.getLastSeen()) < 450) {
                cost += 3000;
            }else {
                // reduce cost relative to every 30 seconds since last seen for enemy shadowed spots
                cost /= ((frame - task.spot.getLastSeen()) / 900);
            }

            // disprefer scouting the same non-hostile spot with more than one raider.
            if (!task.assignedRaiders.isEmpty() && !task.assignedRaiders.contains(raider)){
                cost += 2000;
            }
        }

        cost += (500 * warManager.getThreat(task.target));

        if (warManager.getEffectiveThreat(task.target) > warManager.getFriendlyThreat(raider.getPos())){
            cost += 9001;
        }
        return cost;
    }

    private void assignRaiders(){
        boolean needUnstick = false;
        if (frame % 120 == 0){
            needUnstick = true;

            for (Raider r: raidQueue){
                if (retreatHandler.isRetreating(r.getUnit()) || r.getUnit().getHealth() <= 0){
                    continue;
                }

                AIFloat3 pos;
                boolean overThreat = (warManager.getEffectiveThreat(r.getPos()) > 0);
                if (!overThreat){
                    pos = warManager.getRaiderRally(r.getPos());
                    r.fightTo(pos, frame);
                }
            }

            for (Raider r: mediumRaidQueue){
                if (retreatHandler.isRetreating(r.getUnit()) || r.getUnit().getHealth() <= 0){
                    continue;
                }

                AIFloat3 pos;
                boolean overThreat = (warManager.getEffectiveThreat(r.getPos()) > 0);
                if (!overThreat){
                    pos = warManager.getRaiderRally(r.getPos());
                    r.fightTo(pos, frame);
                }
            }
        }

        // retreat overthreatened raiders from queues
        for (Raider r: raidQueue){
            AIFloat3 pos;
            boolean overThreat = (warManager.getEffectiveThreat(r.getPos()) > 0);
            if (overThreat){
                pos = graphManager.getClosestHaven(r.getPos());
                r.sneak(pos, frame);
            }
        }

        for (Raider r: mediumRaidQueue){
            AIFloat3 pos;
            boolean overThreat = (warManager.getEffectiveThreat(r.getPos()) > 0);
            if (overThreat){
                pos = graphManager.getClosestHaven(r.getPos());
                r.sneak(pos, frame);
            }
        }

        // assign main raid group
        for (Raider r: raiders){
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

                if (overThreat) {
                    r.sneak(bestTarget, frame);
                } else {
                    r.raid(bestTarget, frame);
                }
            }else if (bestTask != null && (overThreat || bestTask != r.getTask() || r.getUnit().getCurrentCommands().isEmpty())){
                if (overThreat) {
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
    }

    public void avoidEnemies(Unit h, Unit attacker, AIFloat3 dir){
        for (Raider r : raiders) {
            if (r.id == h.getUnitId() && h.getHealth() / h.getMaxHealth() < 0.8 && attacker != null && attacker.getMaxSpeed() > 0 && warManager.getEffectiveThreat(h.getPos()) <= 0
                    && r.scouting && !r.getUnit().getDef().getName().equals("corgator")) {
                float movdist = -100;
                if (r.getUnit().getDef().getName().equals("spherepole") || r.getUnit().getDef().getName().equals("corsh") || warManager.getEffectiveThreat(h.getPos()) > 0) {
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

        for (Raider r: raidQueue){
            if (r.id == h.getUnitId() && h.getHealth()/h.getMaxHealth() < 0.6){
                float x = -100*dir.x;
                float z = -100*dir.z;
                AIFloat3 pos = h.getPos();
                AIFloat3 target = new AIFloat3();
                target.x = pos.x+x;
                target.z = pos.z+z;
                h.moveTo(target, (short) 0, frame);
            }
        }
    }

}
