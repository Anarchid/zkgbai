package zkgbai.military.fighterhandlers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.EconomyManager;
import zkgbai.graph.GraphManager;
import zkgbai.graph.MetalSpot;
import zkgbai.kgbutil.Pathfinder;
import zkgbai.los.LosManager;
import zkgbai.military.Enemy;
import zkgbai.military.MilitaryManager;
import zkgbai.military.unitwrappers.Bomber;

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
    Pathfinder pathfinder;
	
	Map<Integer, Bomber> bombers = new HashMap<>();

    List<Bomber> unarmedBombers = new LinkedList<>();
    Queue<Bomber> readyBombers = new LinkedList<>();
    List<Bomber> activeBombers = new LinkedList<>();

    int frame;
    
    int minFlock = 8;

    private static final int CMD_FIND_PAD = 33411;

    public BomberHandler(){
        this.ai = ZKGraphBasedAI.getInstance();
        this.warManager = ai.warManager;
        this.graphManager = ai.graphManager;
        this.ecoManager = ai.ecoManager;
        this.losManager = ai.losManager;
        this.retreatHandler = warManager.retreatHandler;
        this.pathfinder = Pathfinder.getInstance();
    }

    public void addBomber(Bomber b){
        bombers.put(b.id, b);
        readyBombers.add(b);
    }

    public void removeUnit(Unit u) {
        Bomber f = bombers.get(u.getUnitId());
        if (f != null) {
            unarmedBombers.remove(f);
	        readyBombers.remove(f);
	        activeBombers.remove(f);
	        bombers.remove(u.getUnitId());
        }
    }

    public int getBomberSize(){
        return unarmedBombers.size() + readyBombers.size() + activeBombers.size();
    }

    public void update(int frame){
        this.frame = frame;

        if(frame % 60 == ai.offset % 60) {
            if (unarmedBombers.isEmpty() && readyBombers.isEmpty() && activeBombers.isEmpty()) {return;}
            cleanUnits();
            checkBombers();
	        assignBombers();
        }
    }

    private void checkBombers(){
        List<Bomber> swap = new ArrayList<>();

        for (Bomber b:unarmedBombers){
            boolean isUnarmed = false;
            if (b.getUnit().getHealth() < b.getUnit().getMaxHealth()){
                isUnarmed = true;
            }else {
                if (b.getUnit().getRulesParamFloat("noammo", 0.0f) > 0) {
                    isUnarmed = true;
                }
            }

            if (!isUnarmed){
	            readyBombers.add(b);
                swap.add(b);
            }
        }
        unarmedBombers.removeAll(swap);
    }

    private void assignBombers(){
        List<Bomber> swap = new ArrayList<>();
        for (Bomber b:activeBombers){
            // sift unarmed bombers back into the unarmed list.
            boolean isUnarmed = false;
            if (b.getUnit().getRulesParamFloat("noammo", 0.0f) > 0) {
                isUnarmed = true;
            }

            if (isUnarmed){
                swap.add(b);
                unarmedBombers.add(b);
                continue;
            }
			
            if (b.targetMissing()){
	            swap.add(b);
	            unarmedBombers.add(b);
	            b.flyTo(graphManager.getAllyCenter());
            }else{
                b.bomb();
            }
        }
        activeBombers.removeAll(swap);

        // have all unarmed bombers reload/heal themselves.
        for (Bomber b:unarmedBombers) {
            if (!graphManager.isEnemyTerritory(b.getPos())) {
                b.findPad();
            }else{
                b.flyTo(graphManager.getAllyCenter()); // if in enemy territory, maneuver back to safety before finding an airpad.
            }
        }
        
        if (unarmedBombers.isEmpty() && readyBombers.size() >= minFlock){
	        PriorityQueue<Enemy> targets = warManager.getBomberTargets();
	        if (!targets.isEmpty()) minFlock = Math.max(8, (int) (Math.ceil(targets.peek().ud.getHealth()/800f) + (targets.peek().isImportant ? 1 : 0)));
	        
	        while (!targets.isEmpty() && !readyBombers.isEmpty()){
	        	Enemy e = targets.poll();
	        	int neededBombers = (int) (Math.ceil(e.ud.getHealth()/800f) + (e.isImportant ? 1 : 0));
	        	if (neededBombers > readyBombers.size()) continue;
	        	while (neededBombers > 0){
	        		Bomber b = readyBombers.poll();
	        		b.target = e;
	        		activeBombers.add(b);
	        		neededBombers--;
		        }
	        }
        }
    }

    void cleanUnits(){
        List<Bomber> invalidBombers = new ArrayList<>();
        for (Bomber f:bombers.values()){
            if (f.isDead()) invalidBombers.add(f);
        }
        for (Bomber f: invalidBombers){
            unarmedBombers.remove(f);
            readyBombers.remove(f);
            activeBombers.remove(f);
            bombers.remove(f.id);
        }
    }

}
