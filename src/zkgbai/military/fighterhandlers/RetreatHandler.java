package zkgbai.military.fighterhandlers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitRulesParam;
import zkgbai.ZKGraphBasedAI;
import zkgbai.graph.GraphManager;
import zkgbai.military.MilitaryManager;
import zkgbai.kgbutil.Pathfinder;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Created by haplo on 1/3/2016.
 */
public class RetreatHandler {
    ZKGraphBasedAI ai;
    MilitaryManager warManager;
    GraphManager graphManager;
    Pathfinder pathfinder;
    SquadHandler squadHandler;

    List<Unit> cowardUnits;
    List<Unit> retreatingUnits;
    List<Unit> retreatedUnits;

    int frame;

    public static final short OPTION_SHIFT_KEY = (1 << 5); //  32

    public RetreatHandler(){
        this.ai = ZKGraphBasedAI.getInstance();
        this.warManager = ai.warManager;
        this.graphManager = ai.graphManager;
        this.pathfinder = Pathfinder.getInstance();

        this.cowardUnits = new ArrayList<Unit>();
        this.retreatingUnits = new ArrayList<Unit>();
        this.retreatedUnits = new ArrayList<Unit>();
    }

    public void init(){
        this.squadHandler = warManager.squadHandler;
    }

    public void addCoward(Unit u){
        cowardUnits.add(u);
    }

    public boolean isCoward(Unit u){
        return cowardUnits.contains(u);
    }

    public void checkUnit(Unit u){
        if(cowardUnits.contains(u) && !retreatingUnits.contains(u) && (u.getHealth()/u.getMaxHealth() < 0.5 || (u.getDef().getTooltip().contains("Anti-Air") && u.getHealth()/u.getMaxHealth() < 0.75))){
            retreatingUnits.add(u);
            squadHandler.removeFromSquad(u);
        }
    }

    public void update(int frame){
        this.frame = frame;
        if (frame % 6 == 0) {
            retreatCowards();
        }
    }

    public boolean isRetreating(Unit u){
        if (retreatingUnits.contains(u)){
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
        for (Unit u: retreatingUnits){
            if (u.getHealth() == u.getMaxHealth() || u.getHealth() <= 0 ){
                healedUnits.add(u);
                continue;
            }

            if(!retreatedUnits.contains(u)) {
                // don't retreat scythes unless they're cloaked
                if (u.getDef().getName().equals("spherepole")){
                    if (!u.isCloaked() && warManager.getEffectiveThreat(u.getPos()) <= 0){
                        continue;
                    }
                }

                AIFloat3 position;
                if (!u.getDef().isAbleToFly()) {
                    position = graphManager.getClosestHaven(u.getPos());
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
                            }
                        }
                        // let the rest of the waypoints get handled the next time around.
                    }
                }else{
                    position = graphManager.getClosestAirHaven(u.getPos());
                    u.moveTo(position, (short) 0, frame + 300); // don't use pathing for air units.
                }

                retreatedUnits.add(u);
                retreated = true;
                break;
            }
        }
        if (!retreated){
            retreatedUnits.clear();
        }

        retreatingUnits.removeAll(healedUnits);
        retreatedUnits.removeAll(healedUnits);
    }
}
