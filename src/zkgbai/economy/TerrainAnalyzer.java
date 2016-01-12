package zkgbai.economy;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Pathing;
import zkgbai.ZKGraphBasedAI;
import zkgbai.graph.GraphManager;
import zkgbai.graph.Link;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by aeonios on 12/5/2015.
 */
public class TerrainAnalyzer {
    ZKGraphBasedAI parent;
    OOAICallback callback;
    GraphManager graphManager;
    List<String> initialFacList;
    Pathing path;

    int vehPath;
    int botPath;
    int spiderPath;
    int hoverPath;
    int amphPath;
    int boatPath;
    static String taMsg = "Terrain Analysis: ";

    public TerrainAnalyzer(){
        this.parent = ZKGraphBasedAI.getInstance();
        this.callback = parent.getCallback();
        this.graphManager = parent.graphManager;
        this.initialFacList = new ArrayList<String>();
        this.path = callback.getPathing();

        vehPath = callback.getUnitDefByName("corned").getMoveData().getPathType();
        botPath = callback.getUnitDefByName("armrectr").getMoveData().getPathType();
        spiderPath = callback.getUnitDefByName("arm_spider").getMoveData().getPathType();
        hoverPath = callback.getUnitDefByName("corch").getMoveData().getPathType();
        amphPath = callback.getUnitDefByName("amphcon").getMoveData().getPathType();
        boatPath = callback.getUnitDefByName("shipcon").getMoveData().getPathType();

        populateFacList();
    }

    private void populateFacList(){
        if (parent.allies.size() > 2){
            debug(taMsg + "Allies detected, enabling air starts!");
            initialFacList.add("factorygunship");
            initialFacList.add("factoryplane");
        }

        debug(taMsg + "Checking Veh Pathability..");
        PathResult veh = checkPathing(vehPath, 1.35f);
        if (veh.result){
            debug(taMsg + "Veh path check succeeded, enabling veh!");
            initialFacList.add("factoryveh");
            initialFacList.add("factorytank");
            initialFacList.add("factoryhover");
        }

        if (!veh.result){
            debug(taMsg + "Checking Hover Pathability..");
            PathResult hover = checkPathing(hoverPath, 1.35f);
            if (hover.result){
                debug(taMsg + "Hover path check succeeded, enabling hovers!");
                initialFacList.add("factoryhover");
            }
        }

        debug(taMsg + "Checking Bot Pathability..");
        PathResult bot = checkPathing(botPath, 1.4f);
        if ((bot.avgCostRatio < veh.avgCostRatio - 0.05f || !veh.result) && bot.result){
            debug(taMsg + "Bot path check succeeded, enabling bots!");
            initialFacList.add("factorycloak");
            initialFacList.add("factoryshield");
            initialFacList.add("factoryamph");
        } else if (veh.result && bot.avgCostRatio >= veh.avgCostRatio - 0.05f) {
            debug(taMsg + "Bots not cost competitive, skipping!");
        }

        debug(taMsg + "Checking Spider Pathability..");
        PathResult spider = checkPathing(spiderPath, 5f);
        if (spider.avgCostRatio < bot.avgCostRatio - 0.02f && spider.avgCostRatio < veh.avgCostRatio - 0.05f && spider.result){
            debug(taMsg + "Spider path check succeeded, enabling spiders and jumps!");
            initialFacList.add("factoryspider");
            if (!initialFacList.contains("factoryjump")) {
                //initialFacList.add("factoryjump");
            }
            return;
        } else if (spider.avgCostRatio >= bot.avgCostRatio - 0.02f || spider.avgCostRatio >= veh.avgCostRatio - 0.05f) {
            debug(taMsg + "Spiders not cost competitive, skipping!");
            return;
        }

        debug(taMsg + "Checking Boat Pathability..");
        PathResult boat = checkPathing(boatPath, 5f);
        if (boat.result){
            debug(taMsg + "Boat path check succeeded, enabling boats!");
            //initialFacList.add("factoryship");
            //return;
        }

        debug(taMsg + "Checking Amph Pathability..");
        PathResult amph = checkPathing(amphPath, 5f);
        if (amph.result){
            debug(taMsg + "Amph path check succeeded, stopping!");
            initialFacList.add("factoryamph");
            return;
        }

        if (initialFacList.isEmpty()) {
            debug(taMsg + "Analysis Failed! Going air by default.");
            if (!initialFacList.contains("factorygunship")) {
                initialFacList.add("factorygunship");
                initialFacList.add("factoryplane");
            }
        }
    }

    private PathResult checkPathing(int pathType, float maxRelCost){
        float avgRelCost = 0.0f;
        boolean success = true;
        List<Link> links = graphManager.getLinks();
        for (Link l:links){
            float linkCost = path.getApproximateLength(l.v0.getPos(), l.v1.getPos(), pathType, 0f)/l.length;
            if (linkCost < 1.0f){
                success = false;
                linkCost = 5;
            }

            avgRelCost += linkCost/links.size();
        }
        if (!success){
            debug(taMsg + "Path Check Failed: unreachable mexes.");
        }
        debug(taMsg + "Average Relative Path Cost: " + avgRelCost);
        if (success && avgRelCost > maxRelCost){
            debug(taMsg + "Path Check Failed: high path costs.");
            success = false;
        }
        return new PathResult(success, avgRelCost);
    }

    public List<String> getInitialFacList(){
        if (initialFacList.isEmpty()){
            populateFacList();
        }
        return initialFacList;
    }

    private void debug(String s) {
        callback.getGame().sendTextMessage(s, 0);
    }

}
