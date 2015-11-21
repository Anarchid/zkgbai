/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zkgbai.military;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.springrts.ai.oo.clb.Unit;
import zkgbai.graph.CostSupplier;

/**
 *
 * @author User
 */
public class Pathfinder extends Object {

    OOAICallback callback;
    float mwidth, mheight;
    int smwidth;
    float[] slopeMap;
    private final static int mapCompression = 8;
    private final static int originalMapRes = 16;
    private final static int mapRes = originalMapRes * mapCompression;

    public Pathfinder(MilitaryManager ai) {
    	this.ai = ai;
    	callback = ai.parent.getCallback();
        mwidth = callback.getMap().getWidth() * 8;
        mheight = callback.getMap().getHeight() * 8;
        smwidth = (int) (mwidth / mapRes);
        updateSlopeMap();
    }

    private void updateSlopeMap() {
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

    private Map<CostSupplier, float[]> costSupplierCosts = new HashMap<CostSupplier, float[]>();
    private Map<CostSupplier, int[]> costSupplierLastUpdate = new HashMap<CostSupplier, int[]>();
	private MilitaryManager ai;

    private float getCachedCost(CostSupplier supplier, float slope, float maxSlope, int pos) {
        if (!costSupplierCosts.containsKey(supplier)) {
            costSupplierCosts.put(supplier, new float[slopeMap.length]);
            costSupplierLastUpdate.put(supplier, new int[slopeMap.length]);
        }

        float[] costs = costSupplierCosts.get(supplier);
        int[] lastUpdate = costSupplierLastUpdate.get(supplier);
        if (ai.parent.currentFrame - lastUpdate[pos] > 15) {
            lastUpdate[pos] = ai.parent.currentFrame;
            costs[pos] = supplier.getCost(slope, maxSlope, toAIFloat3(pos));
        }
        return costs[pos];
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
        float maxSlope = 0;
        if (!u.getDef().isAbleToFly()) {
            maxSlope = u.getDef().getMoveData().getMaxSlope();
        }
        
        long time = System.currentTimeMillis();
        int startPos = (int) (target.z / mapRes) * smwidth + (int) (target.x / mapRes); //reverse to return in right order when traversing backwards
        int targetPos = (int) (start.z / mapRes) * smwidth + (int) (start.x / mapRes);
        int[] offset = new int[]{-1, 1, smwidth, -smwidth, smwidth + 1, smwidth - 1, -smwidth + 1, -smwidth - 1};
        float[] offsetCostMod = new float[]{1, 1, 1, 1, 1.42f, 1.42f, 1.42f, 1.42f};

        Deque<AIFloat3> result = new ArrayDeque<AIFloat3>();

        Comparator<pqEntry> pqComp = new Comparator<pqEntry>() {

            @Override
            public int compare(pqEntry t, pqEntry t1) {
                if (t == null && t1 == null) {
                    return 0;
                }
                if (t == null) {
                    return -1;
                }
                if (t1 == null) {
                    return 1;
                }
                return (int) Math.signum(t.cost - t1.cost);
            }

        };

        PriorityQueue<pqEntry> pq = new PriorityQueue<pqEntry>(1, pqComp);
        pq.add(new pqEntry(getHeuristic(startPos, targetPos), 0, startPos));

        float[] minCost = new float[slopeMap.length];
        int[] pathTo = new int[slopeMap.length];
        for (int i = 0; i < minCost.length; i++) {
            minCost[i] = Float.MAX_VALUE;
        }
        minCost[startPos] = 0;

        int pos;
        float cost;

        while (true) {
            do {
                if (pq.isEmpty()) {
                    result.add(target);
                    return result;
                }
                pos = pq.peek().pos;
                cost = pq.poll().realCost;
                //if (cost > 1e6f) command.mark(toAIFloat3(pos), "unreachable with " + maxSlope);
            } while (cost > minCost[pos] || cost > 1e6f);
            if (pos == targetPos) {//breaks but shouldnt
                
                    //ommand.debug("pathfinder reached target");
                    //command.mark(new AIFloat3(),"pathfinder reached target");
                break;
            }

            for (int i = 0; i < offset.length; i++) {
                if (pos % (smwidth) == 0 && offset[i] % smwidth != 0) {
                    //command.mark(toAIFloat3(pos), "stopping");
                    continue;
                }
                if ((pos + 1) % (smwidth) == 0 && offset[i] % smwidth != 0) {
                    //command.mark(toAIFloat3(pos), "stopping");
                    continue;
                }
                if (inBounds(pos + offset[i], minCost.length)
                        && cost + offsetCostMod[i] * getCachedCost(costs, slopeMap[pos + offset[i]], maxSlope, (pos + offset[i])) < minCost[pos + offset[i]]) {

                    pathTo[pos + offset[i]] = pos;
                    minCost[pos + offset[i]] = cost + offsetCostMod[i] * getCachedCost(costs, slopeMap[pos + offset[i]], maxSlope, (pos + offset[i]));
                    pq.add(new pqEntry(getHeuristic(pos + offset[i], targetPos) + minCost[pos + offset[i]],
                             minCost[pos + offset[i]], pos + offset[i]));
                    //command.mark(toAIFloat3(pos+offset[i]), "for " + (getHeuristic(pos + offset[i], targetPos) + minCost[pos + offset[i]]));
                }
            }
        }

        pos = targetPos;
        while (pos != startPos) {
            //clbk.getMap().getDrawer().addLine(toAIFloat3(pos), toAIFloat3(pathTo[pos]));
            result.add(toAIFloat3(pos));
            pos = pathTo[pos];
        }
        result.add(target);
        result.add(target);//add twice to confirm path
        time = System.currentTimeMillis() - time;
        if (time > 10) {
            ai.parent.debug("pathfinder took " + time + "ms");
        }
        
        return result;

    }

    private float getHeuristic(int start, int trg) {
        return (float) Math.sqrt((start % smwidth - trg % smwidth) * (start % smwidth - trg % smwidth) + (start / smwidth - trg / smwidth) * (start / smwidth - trg / smwidth));
    }

    private AIFloat3 toAIFloat3(int pos) {
        AIFloat3 ret = new AIFloat3(mapRes * (pos % (smwidth)), 0, mapRes * (pos / (smwidth)));
        ret.y = callback.getMap().getElevationAt(ret.x, ret.z);
        return ret;
    }

    /**
     * Fastest path to target (not shortest)
     */
    public final CostSupplier FAST_PATH = new CostSupplier() {

        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (slope > maxSlope) {
                return Float.MAX_VALUE;
            }
            return 10 * ((slope / maxSlope) + 1);
        }
    };
    /**
     * Fastest path to target while avoiding riot units
     */
    public final CostSupplier RAIDER_PATH = new CostSupplier() {

        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (slope > maxSlope) {
                return Float.MAX_VALUE;
            }
            return 10 * (2 * ai.getThreat(pos) + 0.5f * (slope/maxSlope));
        }
    };
    /**
     * Fastest path to target while  TODO
     */
    public final CostSupplier ASSAULT_PATH = new CostSupplier() {

        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (slope > maxSlope) {
                return Float.MAX_VALUE;
            }
            return 10 * (slope / maxSlope) + 1 * ai.getThreat(pos) + 1;
        }
    };
    /**
     * Fastest path to target while avoiding enemies that are able to attack
     */
    public final CostSupplier AVOID_ENEMIES = new CostSupplier() {

        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (slope > maxSlope) {
                return Float.MAX_VALUE;
            }
            return 10 * (slope / maxSlope) + 20 * ai.getThreat(pos);
        }
    };

    private boolean inBounds(int num, int max) {
        return num < max && num >= 0;
    }

    private class pqEntry {

        final float cost;
        final float realCost;
        final int pos;

        public pqEntry(float cost, float realCost, int pos) {
            this.cost = cost;
            this.pos = pos;
            this.realCost = realCost;
        }
    }
}