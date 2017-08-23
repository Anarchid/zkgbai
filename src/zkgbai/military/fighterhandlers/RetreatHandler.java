package zkgbai.military.fighterhandlers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.ZKGraphBasedAI;
import zkgbai.graph.GraphManager;
import zkgbai.military.MilitaryManager;
import zkgbai.kgbutil.Pathfinder;
import zkgbai.military.UnitClasses;
import zkgbai.military.unitwrappers.Fighter;

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

    int frame;

    public static final short OPTION_SHIFT_KEY = (1 << 5); //  32
    private static final int CMD_FIND_PAD = 33411;

    public RetreatHandler(){
        this.ai = ZKGraphBasedAI.getInstance();
        this.warManager = ai.warManager;
        this.graphManager = ai.graphManager;
        this.pathfinder = Pathfinder.getInstance();
    }

    public void init(){
        this.squadHandler = warManager.squadHandler;
        this.raiderHandler = warManager.raiderHandler;
    }

    public void addCoward(Unit u){
        cowardUnits.put(u.getUnitId(), u);
    }

    public boolean isCoward(Unit u){
        return cowardUnits.containsKey(u.getUnitId());
    }

    public void checkUnit(Unit u){
        float hp = u.getHealth() / u.getMaxHealth();
        if (cowardUnits.containsKey(u.getUnitId()) && !retreatingUnits.containsKey(u.getUnitId())
                && (hp < 0.5f || (u.getDef().getTooltip().contains("Anti-Air") && hp < 0.95f)
                        || (u.getDef().isAbleToFly() && hp < 0.65f)
                        || (u.getDef().getTooltip().contains("Strider") && hp < 0.6f))) {
            String defName = u.getDef().getName();
            boolean onFire = u.getRulesParamFloat("on_fire", 0) > 0;
            if (onFire && unitTypes.smallRaiders.contains(defName)){
                return;
            }
            retreatingUnits.put(u.getUnitId(), u);
            squadHandler.removeFromSquad(u);
            raiderHandler.removeFromSquad(u);
        }
    }

    public void update(int frame){
        this.frame = frame;
        if (frame % 15 == 0) {
            retreatCowards();
        }
    }

    public boolean isRetreating(Unit u){
        if (retreatingUnits.containsKey(u.getUnitId())){
            return true;
        }
        return false;
    }

    public void removeUnit(Unit u){
        cowardUnits.remove(u);
        retreatingUnits.remove(u);
        retreatedUnits.remove(u);
    }

    private void retreatCowards(){
        boolean retreated = false;
        List<Unit> healedUnits = new ArrayList<Unit>();
        for (Unit u: retreatingUnits.values()){
            if (u.getHealth() == u.getMaxHealth() || u.getHealth() <= 0 ){
                healedUnits.add(u);
                continue;
            }

            if(!retreatedUnits.containsKey(u.getUnitId()) || warManager.getThreat(u.getPos()) > 0) {
                // don't retreat scythes unless they're cloaked
                if (u.getDef().getName().equals("cloakheavyraid")){
                    if (!u.isCloaked() && warManager.getEffectiveThreat(u.getPos()) <= 0){
                        continue;
                    }
                }

                AIFloat3 position;
                if (!unitTypes.bombers.contains(u.getDef().getName()) && !u.getDef().getName().equals("planefighter") && !u.getDef().getName().equals("planeheavyfighter")) {
                    if (u.getDef().isAbleToFly()){
                        position = graphManager.getClosestAirHaven(u.getPos());
                    }else {
                        position = graphManager.getClosestHaven(u.getPos());
                    }
                    Deque<AIFloat3> path = pathfinder.findPath(u, position, pathfinder.AVOID_ENEMIES);
                    u.moveTo(path.poll(), (short) 0, frame + 300); // skip first few waypoints if target actually found to prevent stuttering, otherwise use the first waypoint.
                    if (path.size() > 2){
                        path.poll();
                        path.poll();
                    }

                    if (path.isEmpty()) {
                        // pathing failed
                    } else {
                        u.moveTo(path.poll(), (short) 0, frame + 300); // immediately move to first non-redundant waypoint


                        int pathSize = Math.min(5, path.size());
                        int i = 0;
                        while (i < pathSize && !path.isEmpty()) { // queue up to the first 5 waypoints
                            u.moveTo(path.poll(), OPTION_SHIFT_KEY, frame + 3000);
                            i++;
                            // skip every other waypoint except the last, since they're not very far apart.
                            if (path.size() > 1) {
                                path.poll();
                                // skip two out of three waypoints for flying units, since they move quickly.
                                if (path.size() > 1 && u.getDef().isAbleToFly()) {
                                    path.poll();
                                }
                            }
                        }
                        // let the rest of the waypoints get handled the next time around.
                    }
                }else{
                    List<Float> params = new ArrayList<Float>();

                    if (!graphManager.isEnemyTerritory(u.getPos())) {
                        u.executeCustomCommand(CMD_FIND_PAD, params, (short) 0, frame + 300);
                    }else{
                        Fighter b = new Fighter(u, 0);
                        b.moveTo(graphManager.getAllyCenter(), frame); // if in enemy territory, maneuver back to safety before finding an airpad.
                        b.getUnit().executeCustomCommand(CMD_FIND_PAD, params, (short) 32, frame + 300);
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
            if (retreatingUnits.containsKey(u.getUnitId())) {
                retreatingUnits.remove(u.getUnitId());
            }
            if (retreatedUnits.containsKey(u.getUnitId())) {
                retreatedUnits.remove(u.getUnitId());
            }
        }
    }
}
