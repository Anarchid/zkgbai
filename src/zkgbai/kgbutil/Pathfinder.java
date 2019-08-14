/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zkgbai.kgbutil;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;

import java.util.*;

import com.springrts.ai.oo.clb.Unit;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.poly2tri.triangulation.point.FloatBufferPoint;
import zkgbai.ZKGraphBasedAI;
import zkgbai.graph.CostSupplier;
import zkgbai.military.MilitaryManager;

/**
 *
 * @author User
 */
public class Pathfinder extends Object {
    ZKGraphBasedAI ai;
    MilitaryManager warManager;
    OOAICallback callback;
    float mwidth, mheight;
    int smwidth;
    float[] slopeMap;
    float[] minCosts;
    float[] cachedCosts;
    int[] pathTo;
    private final static int mapCompression = 8;
    private final static int originalMapRes = 16;
    private final static int mapRes = originalMapRes * mapCompression;
    
    private static Pathfinder instance = null;
    
    private Pathfinder() {
        instance = this;
        this.ai = ZKGraphBasedAI.getInstance();
        this.callback = ai.getCallback();
        this.warManager = ai.warManager;
        mwidth = callback.getMap().getWidth() * 8;
        mheight = callback.getMap().getHeight() * 8;
        smwidth = (int) (mwidth / mapRes);
        updateSlopeMap();
        
        this.minCosts = new float[slopeMap.length];
        this.pathTo = new int[slopeMap.length];
        this.cachedCosts = new float[slopeMap.length];
    }
    
    public static Pathfinder getInstance(){
        if (instance == null){
            instance = new Pathfinder();
        }
        return instance;
    }
    
    public void updateSlopeMap() {
        List<Float> map = callback.getMap().getSlopeMap();
        slopeMap = new float[map.size() / mapCompression / mapCompression];
        for (int x = 0; x < mwidth / originalMapRes; x++) {
            for (int y = 0; y < mheight / originalMapRes; y++) {
                int cx = x / mapCompression;
                int cy = y / mapCompression;
                slopeMap[cy * smwidth + cx] = Math.max(slopeMap[cy * smwidth + cx], map.get(y * (smwidth * mapCompression) + x));
            }
        }
    }
    
    public boolean isReachable(Unit u, AIFloat3 target){
        return findPath(u, target, FAST_PATH).size() > 1;
    }
    /**
     * Finds the cheapest path between two arbitrary positions using the A*
     * algorithm.
     *
     * @param u the unit to find a path for
     * @param target the target location
     * @param costs Class implementing CostSupplier
     * @return Path as List of AIFloat3. If list.size() &lt; 2 no valid path was
     * @see #FAST_PATH
     * @see #RAIDER_PATH
     * @see #AVOID_ENEMIES found.
     *
     */
    
    public Deque<AIFloat3> findPath(Unit u, AIFloat3 target, CostSupplier costs) {
        AIFloat3 start = u.getPos();
        Deque<AIFloat3> result = new ArrayDeque<AIFloat3>();
        
        if (start == null){
            return result;
        }
        
        // bounds check, needed because unit positions can sometimes be nonsensical for various reasons.
        if (start.x < 0 || start.x > mwidth || start.z < 0 || start.z > mheight || target.x < 0 || target.x > mwidth || target.z < 0 || target.z > mheight){
            result.add(target);
            return result;
        }
        
        boolean flyer = true;
        float flyheight = 0;
        float maxSlope = 1;
        if (u.getDef().getMoveData() != null) {
            maxSlope = u.getDef().getMoveData().getMaxSlope();
            flyer = false;
            if (maxSlope == callback.getUnitDefByName("arm_spider").getMoveData().getMaxSlope()){
                // use special pathing for spiders, since they favor hills.
                if (u.getDef().getName().equals("armflea") || u.getDef().getName().equals("armspy")){
                    costs = SPIDER_RAIDER_PATH;
                }else {
                    costs = SPIDER_PATH;
                }
            }
        }else{
            costs = AIR_PATH;
            flyheight = u.getDef().getHeight() - callback.getMap().getElevationAt(u.getPos().x, u.getPos().y);
        }
        
        int startPos = (int) (target.z / mapRes) * smwidth + (int) (target.x / mapRes); //reverse to return in right order when traversing backwards
        int targetPos = (int) (start.z / mapRes) * smwidth + (int) (start.x / mapRes);
        int[] offset = new int[]{-1, 1, smwidth, -smwidth, smwidth + 1, smwidth - 1, -smwidth + 1, -smwidth - 1};
        float[] offsetCostMod = new float[]{1, 1, 1, 1, 1.42f, 1.42f, 1.42f, 1.42f};
        
        Comparator<PathNode> pnComp = new Comparator<PathNode>() {
            @Override
            public int compare(PathNode t, PathNode t1) {
                if (t == null && t1 == null) {
                    return 0;
                }
                if (t == null) {
                    return -1;
                }
                if (t1 == null) {
                    return 1;
                }
                
                if (t.totalCost == t1.totalCost){
                    return (int) Math.signum(-(t.index - t1.index));
                }
                return (int) Math.signum(t.totalCost - t1.totalCost);
            }
        };
        
        int pqIndex = 0;
        PriorityQueue<PathNode> openSet = new PriorityQueue<PathNode>(1, pnComp);
        Set<Integer> openRecord = new HashSet<Integer>(); // needed because 'contains' checks on priority queues are O(n)
        Set<Integer> closedSet = new HashSet<Integer>();
        openSet.add(new PathNode(startPos, getHeuristic(startPos, targetPos), 0, pqIndex));
        openRecord.add(startPos);
        
        
        for (int i = 0; i < minCosts.length; i++) {
            minCosts[i] = Float.MAX_VALUE;
            cachedCosts[i] = Float.MAX_VALUE;
        }
        minCosts[startPos] = 0;
        
        int pos;
        
        while (true) {
            // if the open set is empty and we haven't reached targetPos, it indicates that the target is unreachable.
            if (openSet.isEmpty()) {
                result.add(target);
                return result;
            }
            
            PathNode current = openSet.poll();
            pos = current.pos;
            
            closedSet.add(pos);
            
            // if we reach target pos, we have a complete path.
            if (pos == targetPos){
                break;
            }
            
            
            for (int i = 0; i < offset.length; i++) {
                if (!inBounds(pos, offset[i])){
                    continue;
                }
                
                int neighbor = pos + offset[i];
                
                // don't explore the same node twice
                if (closedSet.contains(neighbor)){
                    continue;
                }
                
                float costMod = cachedCosts[neighbor];
                if (costMod == Float.MAX_VALUE){
                    // calculate the cost if it isn't cached already
                    costMod = costs.getCost(slopeMap[neighbor], maxSlope, toAIFloat3(neighbor));
                    cachedCosts[neighbor] = costMod;
                }
                
                // don't explore unpathable nodes, as they do not form valid paths
                if (costMod == -1){
                    continue;
                }
                
                float templength = current.pathLength + (offsetCostMod[i] * costMod);
                
                if (templength < minCosts[neighbor]) {
                    pathTo[neighbor] = pos;
                    minCosts[neighbor] = templength;
                    pqIndex++;
                    PathNode newNode = new PathNode(neighbor, templength + getHeuristic(neighbor, targetPos), templength, pqIndex);
                    
                    // if a node is already in the open set, replace it with the new, lower cost node.
                    if (openRecord.contains(neighbor)){
                        openSet.remove(newNode);
                    }else{
                        openRecord.add(neighbor);
                    }
                    openSet.add(newNode);
                }
            }
        }
        
        pos = targetPos;
        while (pos != startPos) {
            if (flyer) {
                result.add(toAIFloat3(pos, flyheight));
            }else{
                result.add(toAIFloat3(pos));
            }
            pos = pathTo[pos];
        }
        result.add(target);
        
        return result;
        
    }
    
    private float getHeuristic(int start, int target) {
        double st = (double) start;
        double trg = (double) target;
        return (float) Math.sqrt((start % smwidth - target % smwidth) * (start % smwidth - target % smwidth) + (st / smwidth - trg / smwidth) * (st / smwidth - trg / smwidth));
    }
    
    private AIFloat3 toAIFloat3(int pos) {
        AIFloat3 ret = new AIFloat3(mapRes * (pos % (smwidth)), 0, mapRes * (pos / (smwidth)));
        ret.y = callback.getMap().getElevationAt(ret.x, ret.z);
        return ret;
    }
    
    private AIFloat3 toAIFloat3(int pos, float height) {
        AIFloat3 ret = new AIFloat3(mapRes * (pos % (smwidth)), 0, mapRes * (pos / (smwidth)));
        ret.y = callback.getMap().getElevationAt(ret.x, ret.z) + height;
        return ret;
    }
    
    /**
     * Fastest path to target (not shortest)
     */
    public final CostSupplier FAST_PATH = new CostSupplier() {
        
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (slope > maxSlope) {
                return -1;
            }
            return 10 * ((slope / maxSlope) + 1);
        }
    };
    /**
     * Fastest path to target while avoiding AA
     */
    public final CostSupplier AIR_PATH = new CostSupplier() {
        
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (slope > maxSlope) {
                return -1;
            }
            return 10 * (1 + (4 * warManager.getEffectiveAAThreat(pos)));
        }
    };
    /**
     * Best path to target while preferring steep slopes.
     */
    public final CostSupplier SPIDER_RAIDER_PATH = new CostSupplier() {
        
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (slope > maxSlope) {
                return -1;
            }
            // use inverse slope-cost relation for spiders, because they actually benefit from hills!
            return 10 * ((1f - slope) + (4f * warManager.getThreat(pos)) + 1f);
        }
    };
    /**
     * Best path to target while preferring steep slopes.
     */
    public final CostSupplier SPIDER_PATH = new CostSupplier() {
        
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (slope > maxSlope) {
                return -1;
            }
            // use inverse slope-cost relation for spiders, because they actually benefit from hills!
            return 10 * ((1f - slope) + warManager.getThreat(pos) + 1f);
        }
    };
    /**
     * Fastest path to target while avoiding riot units
     */
    public final CostSupplier RAIDER_PATH = new CostSupplier() {
        
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (slope > maxSlope) {
                return -1;
            }
            return 10 * (1 + (4 * warManager.getThreat(pos)) + (slope/maxSlope));
        }
    };
    /**
     * Fastest path to target while  TODO
     */
    public final CostSupplier ASSAULT_PATH = new CostSupplier() {
        
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (slope > maxSlope) {
                return -1;
            }
            return 10 * ((slope / maxSlope) + warManager.getThreat(pos) + 1);
        }
    };
    /**
     * Fastest path to target while avoiding enemies that are able to attack
     */
    public final CostSupplier AVOID_ENEMIES = new CostSupplier() {
        
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (slope > maxSlope) {
                return -1;
            }
            
            return 10 * ((4 * warManager.getThreat(pos)) + (slope/maxSlope) + 1);
        }
    };
    
    private boolean inBounds(int pos, int offset) {
        return !(pos+offset > slopeMap.length - 1 || pos+offset < 0
                         || ((pos % (smwidth) == 0 || pos == 0) && (offset + (2 * smwidth) + 1) % smwidth == 0) // left map-edge check
                         || ((pos + 1) % (smwidth) == 0 && (offset + (2 * smwidth) - 1) % smwidth == 0)); // right map-edge check
    }
    
    private class PathNode {
        final int pos;
        final float totalCost;
        final float pathLength;
        final int index; // used for tie breaking, in which case we default to LIFO.
        
        public PathNode(int pos, float tCost, float pLength, int ind) {
            this.pos = pos;
            this.totalCost = tCost;
            this.pathLength = pLength;
            this.index = ind;
        }
        
        @Override
        public boolean equals(Object o){
            if (o instanceof PathNode){
                PathNode pn = (PathNode) o;
                return (this.pos == pn.pos);
            }
            return false;
        }
    }
}