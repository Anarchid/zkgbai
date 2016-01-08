package zkgbai.military.fighterhandlers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitRulesParam;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.EconomyManager;
import zkgbai.graph.GraphManager;
import zkgbai.graph.MetalSpot;
import zkgbai.los.LosManager;
import zkgbai.military.Enemy;
import zkgbai.military.MilitaryManager;
import zkgbai.military.unitwrappers.Fighter;

import java.util.*;

import static zkgbai.kgbutil.KgbUtil.distance;
import static zkgbai.kgbutil.KgbUtil.getRadialPoint;

/**
 * Created by haplo on 1/3/2016.
 */
public class BomberHandler {
    ZKGraphBasedAI ai;
    MilitaryManager warManager;
    LosManager losManager;
    RetreatHandler retreatHandler;
    GraphManager graphManager;
    EconomyManager ecoManager;

    Map<Integer, Fighter> unarmedBombers;
    Map<Integer, Fighter> readyBombers;
    Map<Integer, Fighter> activeBombers;

    int frame;

    private static final int CMD_FIND_PAD = 33411;

    public BomberHandler(){
        this.ai = ZKGraphBasedAI.getInstance();
        this.warManager = ai.warManager;
        this.graphManager = ai.graphManager;
        this.ecoManager = ai.ecoManager;
        this.losManager = ai.losManager;
        this.retreatHandler = warManager.retreatHandler;

        this.unarmedBombers = new HashMap<Integer, Fighter>();
        this.readyBombers = new HashMap<Integer, Fighter>();
        this.activeBombers = new HashMap<Integer, Fighter>();
    }

    public void addBomber(Fighter b){
        readyBombers.put(b.id, b);
    }

    public void removeUnit(Unit u) {
        int key = u.getUnitId();
        if (unarmedBombers.containsKey(key)) {
            unarmedBombers.remove(key);
        }

        if (readyBombers.containsKey(key)) {
            readyBombers.remove(key);
        }

        if (activeBombers.containsKey(key)) {
            activeBombers.remove(key);
        }
    }

    public void update(int frame){
        this.frame = frame;

        if (unarmedBombers.isEmpty() && readyBombers.isEmpty() && activeBombers.isEmpty()) {return;}

        if(frame % 60 == 0) {
            checkBombers();
        }

        if (frame % 300 == 0){
            assignBombers();
        }
    }

    private void checkBombers(){
        List<Integer> swap = new ArrayList<Integer>();

        for (Fighter b:unarmedBombers.values()){
            boolean isUnarmed = false;
            if (b.getUnit().getHealth() < b.getUnit().getMaxHealth()){
                isUnarmed = true;
            }else {
                UnitRulesParam noammo = b.getUnit().getUnitRulesParamByName("noammo");
                if (noammo != null && noammo.getValueFloat() > 0) {
                    isUnarmed = true;
                }
            }

            if (!isUnarmed){
                swap.add(b.id);
                readyBombers.put(b.id, b);
            }
        }

        for (Integer id:swap){
            unarmedBombers.remove(id);
        }

        // Allow all bombers to sift themselves into readyBombers before sending them to attack.
        // Also ensure that there are enough bombers to do damage.
        if (activeBombers.isEmpty() && unarmedBombers.isEmpty() && readyBombers.size() > ecoManager.effectiveIncome/12){
            activeBombers.putAll(readyBombers);
            readyBombers.clear();
        }
    }

    private void assignBombers(){
        AIFloat3 target = warManager.getAirTarget(graphManager.getAllyCenter(), true);

        List<Integer> swap = new ArrayList<Integer>();
        for (Fighter b:activeBombers.values()){
            // sift unarmed bombers back into the unarmed list.
            boolean isUnarmed = false;
            UnitRulesParam noammo = b.getUnit().getUnitRulesParamByName("noammo");
            if (noammo != null && noammo.getValueFloat() > 0) {
                isUnarmed = true;
            }

            // don't attack with retreating units
            if (retreatHandler.isRetreating(b.getUnit())){isUnarmed = true;}

            if (isUnarmed){
                swap.add(b.id);
                unarmedBombers.put(b.id, b);
                continue;
            }

            b.getUnit().fight(getRadialPoint(target, 800f), (short) 0,  frame+300);
        }

        for (Integer id:swap){
            activeBombers.remove(id);
        }

        // have all unarmed bombers reload/heal themselves.
        List<Float> params = new ArrayList<Float>();
        for (Fighter b:unarmedBombers.values()) {
            b.getUnit().executeCustomCommand(CMD_FIND_PAD, params, (short) 0, frame+300);
        }
    }

}
