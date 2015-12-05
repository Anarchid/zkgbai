package zkgbai.economy;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Pathing;
import zkgbai.graph.GraphManager;
import zkgbai.graph.Link;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by haplo on 12/5/2015.
 */
public class TerrainAnalyzer {
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

    public TerrainAnalyzer(OOAICallback callback, GraphManager graphManager){
        this.callback = callback;
        this.graphManager = graphManager;
        this.initialFacList = new ArrayList<String>();
        this.path = callback.getPathing();

        vehPath = callback.getUnitDefByName("corned").getMoveData().getPathType();
        botPath = callback.getUnitDefByName("armrectr").getMoveData().getPathType();
        spiderPath = callback.getUnitDefByName("arm_spider").getMoveData().getPathType();
        hoverPath = callback.getUnitDefByName("corch").getMoveData().getPathType();
        amphPath = callback.getUnitDefByName("amphcon").getMoveData().getPathType();
        boatPath = callback.getUnitDefByName("shipcon").getMoveData().getPathType();

        populateFacLists();
    }

    private void populateFacLists(){
        float mapZ = callback.getMap().getHeight();
        float mapX = callback.getMap().getWidth();
        debug("Map Height: " + mapZ + " Map Width: " + mapX);

        debug("Checking Veh Pathability..");
        if (checkPathing(vehPath, true)){
            debug("Veh pathability succeeded, going veh!");
            initialFacList.add("factoryveh");
            initialFacList.add("factorytank");
            initialFacList.add("factoryhover");
            return;
        }

        debug("Checking Bot Pathability..");
        if (checkPathing(botPath, true)){
            debug("Bot pathability succeeded, going bots!");
            initialFacList.add("factorycloak");
            initialFacList.add("factoryshield");
            initialFacList.add("factoryamph");
            initialFacList.add("factoryspider");
            initialFacList.add("factoryjump");
            return;
        }

        debug("Checking Spider/Jump Pathability..");
        if (checkPathing(spiderPath, false)){
            debug("Spider pathability succeeded, going spiders/jumps!");
            initialFacList.add("factoryspider");
            initialFacList.add("factoryjump");
            return;
        }

    }

    private boolean checkPathing(int pathType, boolean useAvgRelCost){
        float avgRelCost = 0.0f;
        List<Link> links = graphManager.getLinks();
        for (Link l:links){
            float linkCost = path.getApproximateLength(l.v0.getPos(), l.v1.getPos(), pathType, 100f)/l.length;
            if (linkCost < 1.0f){
                return false;
            }
            avgRelCost += linkCost/links.size();
        }
        debug("Average Relative Path Cost: " + avgRelCost);
        if (useAvgRelCost && avgRelCost > 1.5f){
            return false;
        }
        return true;
    }

    private void debug(String s) {
        callback.getGame().sendTextMessage(s, 0);
    }
}
