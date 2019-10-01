package zkgbai.kgbutil;

import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.UnitDef;
import zkgbai.ZKGraphBasedAI;
import zkgbai.graph.GraphManager;
import zkgbai.graph.Link;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aeonios on 12/5/2015.
 */
public class TerrainAnalyzer {
    ZKGraphBasedAI ai;
    OOAICallback callback;
    GraphManager graphManager;
    List<String> initialFacList;
    Pathfinder path;

    UnitDef vehPath;
	UnitDef botPath;
	UnitDef spiderPath;
	UnitDef hoverPath;
	UnitDef amphPath;
	UnitDef boatPath;
    static String taMsg = "Terrain Analysis: ";

    public TerrainAnalyzer(){
        this.ai = ZKGraphBasedAI.getInstance();
        this.callback = ai.getCallback();
        this.graphManager = ai.graphManager;
        this.initialFacList = new ArrayList<String>();
        this.path = Pathfinder.getInstance();

        vehPath = callback.getUnitDefByName("vehcon");
        botPath = callback.getUnitDefByName("cloakcon");
        spiderPath = callback.getUnitDefByName("spidercon");
        hoverPath = callback.getUnitDefByName("hovercon");
        amphPath = callback.getUnitDefByName("amphcon");
        boatPath = callback.getUnitDefByName("shipcon");

        populateFacList();
    }

    private void populateFacList(){
        log(taMsg + "Checking Veh Pathability..");
        PathResult veh = checkPathing(vehPath, 1.3f);
        if (veh.result){
            log(taMsg + "Veh path check succeeded, enabling veh!");
            initialFacList.add("factoryveh");
            //if (veh.avgCostRatio < 1.2f) initialFacList.add("factorytank"); // fuck tanks. Kodachi nerf and dart buff have made them unplayable.
            initialFacList.add("factoryhover");
        }

        if (!veh.result){
            log(taMsg + "Checking Hover Pathability..");
            PathResult hover = checkPathing(hoverPath, 1.3f);
            if (hover.result){
                log(taMsg + "Hover path check succeeded, enabling hovers!");
                initialFacList.add("factoryhover");
            }
        }

        log(taMsg + "Checking Bot Pathability..");
        PathResult bot = checkPathing(botPath, 1.35f);
        if (bot.result
	              /*&& (!veh.result || bot.avgCostRatio < veh.avgCostRatio - 0.05f)
	              || ai.mergedAllies > 3*/){
            log(taMsg + "Bot path check succeeded, enabling bots!");
            initialFacList.add("factorycloak");
            initialFacList.add("factoryshield");
            initialFacList.add("factoryamph");
	        //initialFacList.add("factoryjump");
        }else if (veh.result && bot.avgCostRatio >= veh.avgCostRatio - 0.05f) {
            log(taMsg + "Bots not cost competitive, skipping!");
        }

        log(taMsg + "Checking Spider Pathability..");
        PathResult spider = checkPathing(spiderPath, 1.35f);
        if (spider.result &&
	              (((!bot.result || (spider.avgCostRatio < bot.avgCostRatio - 0.02f)) && (!veh.result || (spider.avgCostRatio < veh.avgCostRatio - 0.05f)))
		                 || ai.mergedAllies > 7)){
            log(taMsg + "Spider path check succeeded, enabling spiders and jumps!");
            initialFacList.add("factoryspider");
            if (!initialFacList.contains("factoryjump")) {
                //initialFacList.add("factoryjump");
            }
        } else if (spider.avgCostRatio >= bot.avgCostRatio - 0.02f || spider.avgCostRatio >= veh.avgCostRatio - 0.05f) {
            log(taMsg + "Spiders not cost competitive, skipping!");
        }

        log(taMsg + "Checking Boat Pathability..");
        PathResult boat = checkPathing(boatPath, 5f);
        if (boat.result){
            log(taMsg + "Boat path check succeeded, enabling boats!");
            //initialFacList.add("factoryship");
        }

        if (!bot.result && !veh.result) {
	        log(taMsg + "Checking Amph Pathability..");
	        PathResult amph = checkPathing(amphPath, 5f);
	        if (amph.result) {
		        log(taMsg + "Amph path check succeeded, stopping!");
		        initialFacList.add("factoryamph");
	        }
        }

        if (ai.allies.size() > 2){
            log(taMsg + "Allies detected, enabling air starts!");
            initialFacList.add("factorygunship");
        }

        if (initialFacList.size() < 3 || initialFacList.size() < ai.mergedAllies + 1) {
            log(taMsg + "Terrain Analysis Failed (or team was too big)! Enabling random factories.");
            if (!initialFacList.contains("factorycloak")) {
                initialFacList.add("factorycloak");
                initialFacList.add("factoryshield");
            }
            if (!initialFacList.contains("factoryamph")) {
                initialFacList.add("factoryamph");
            }
            if (!initialFacList.contains("factoryveh")) {
                initialFacList.add("factoryveh");
                initialFacList.add("factorytank");
            }
            if (!initialFacList.contains("factoryhover")) {
                initialFacList.add("factoryhover");
            }
            if (initialFacList.size() < ai.mergedAllies + 1){
                initialFacList.add("factoryplane");
            }
            if (initialFacList.size() < ai.mergedAllies + 1 && !initialFacList.contains("factoryspider")) {
                initialFacList.add("factoryspider");
	            //initialFacList.add("factoryjump");
            }
        }
    }

    private PathResult checkPathing(UnitDef pathType, float maxRelCost){
        float avgRelCost = 0.0f;
        boolean success = true;
        List<Link> links = graphManager.getLinks();
        float fail = 0;
        for (Link l:links){
            float linkCost = path.getPathCost(l.v0.getPos(), l.v1.getPos(), pathType);
            if (linkCost < 0f) linkCost = path.getPathCost(l.v1.getPos(), l.v0.getPos(), pathType);
            if (linkCost < 0f){
                success = false;
                fail++;
            }else{
            	linkCost /= l.length;
	            avgRelCost += linkCost;
            }
        }
        avgRelCost /= links.size() - fail;
        if (!success){
            log(taMsg + "Path Check Failed: unreachable mexes.");
        }
        log(taMsg + "Average Relative Path Cost: " + avgRelCost);
        if (success && avgRelCost > maxRelCost){
            log(taMsg + "Path Check Failed: high path costs.");
            success = false;
        }
        return new PathResult(success, avgRelCost);
    }

    public List<String> getInitialFacList(){
        if (initialFacList.isEmpty()){
            populateFacList();
        }
        List<String> facList = new ArrayList<String>();
        facList.addAll(initialFacList);
        return facList;
    }

    private void log(String s) {
        callback.getLog().log(s);
    }

}
