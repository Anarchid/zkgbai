package zkgbai.kgbutil;

/**
 * Created by haplo on 12/6/2015.
 */
public class PathResult {
    public boolean result;
    public float avgCostRatio;

    public PathResult(boolean result, float cost){
        this.result = result;
        this.avgCostRatio = cost;
    }
}
