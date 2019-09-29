/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zkgbai.kgbutil;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Map;
import com.springrts.ai.oo.clb.OOAICallback;

import java.util.*;

import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import zkgbai.ZKGraphBasedAI;
import zkgbai.military.MilitaryManager;

import static zkgbai.kgbutil.KgbUtil.distance;

/**
 *
 * @author User
 */
public class Pathfinder extends Object {
    ZKGraphBasedAI ai;
    MilitaryManager warManager;
    OOAICallback callback;
    Map maputil;
    float mwidth, mheight;
    int smwidth;
    float[] slopeMap;
    float[] minCosts;
    float[] cachedCosts;
    int[] pathTo;
    private final static int mapCompression = 4;
    private final static int originalMapRes = 16;
    private final static int mapRes = mapCompression * originalMapRes;

    private int scytheID;
    
    // Uniforms
    private float maxThreat;
    private float minDepth;
    private float maxDepth;
    private float maxSlope;
    private boolean floater;
    private boolean amph;
    private UnitDef blocker;
    
    private static Pathfinder instance = null;
    
    private Pathfinder() {
        instance = this;
        this.ai = ZKGraphBasedAI.getInstance();
        this.callback = ai.getCallback();
        this.warManager = ai.warManager;
        this.maputil = callback.getMap();
        mwidth = callback.getMap().getWidth() * 8;
        mheight = callback.getMap().getHeight() * 8;
        smwidth = (int) (mwidth / mapRes);
        updateSlopeMap();
        
        this.minCosts = new float[slopeMap.length];
        this.pathTo = new int[slopeMap.length];
        this.cachedCosts = new float[slopeMap.length];

        blocker = callback.getUnitDefByName("striderdetriment");
        scytheID = callback.getUnitDefByName("cloakheavyraid").getUnitDefId();
    }
    
    public static Pathfinder getInstance(){
        if (instance == null){
            instance = new Pathfinder();
        }
        return instance;
    }

    public static void release(){
        instance = null;
    }
    
    public void updateSlopeMap() {
        List<Float> map = callback.getMap().getSlopeMap();
        slopeMap = new float[map.size() / mapCompression / mapCompression];
        for (int x = 0; x < mwidth / originalMapRes; x++) {
            for (int y = 0; y < mheight / originalMapRes; y++) {
                int cx = x / mapCompression;
                int cy = y / mapCompression;
                slopeMap[cy * smwidth + cx] = Math.max(slopeMap[cy * smwidth + cx], map.get(y * (smwidth * (int) mapCompression) + x));
            }
        }
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
    
    public Queue<AIFloat3> findPath(Unit u, AIFloat3 target, CostSupplier costs) {
        AIFloat3 start = u.getPos();
        Queue<AIFloat3> result = new LinkedList<>();
        
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
        maxSlope = 1;
        if (u.getDef().getMoveData() != null) {
            maxSlope = u.getDef().getMoveData().getMaxSlope();
            maxDepth = -u.getDef().getMaxWaterDepth();
	        minDepth = (u.getDef().getMinWaterDepth() > 0 ? -u.getDef().getMinWaterDepth() : Float.MAX_VALUE);
            flyer = false;
            if (costs != AVOID_ENEMIES && maxSlope == callback.getUnitDefByName("spidercon").getMoveData().getMaxSlope()){
                // use special pathing for spiders, since they favor hills.
                if (u.getDef().getName().equals("spiderscout")){
                    costs = SPIDER_RAIDER_PATH;
                }else if (u.getDef().getName().equals("spiderantiheavy")) {
                    costs = WIDOW_PATH;
                }else{
                    costs = SPIDER_PATH;
                }
            }
        }else{
            if (costs == AVOID_ENEMIES || u.getDef().getName().equals("gunshipbomb") || u.getDef().getName().equals("gunshipraid")){
                costs = AIR_RAIDER_PATH;
            }else {
                costs = AIR_PATH;
            }
            flyheight = u.getPos().y - getElev(u.getPos());
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
                    if (neighbor == targetPos){
                        // due to low slope map res it may consider the unit's position as unpathable,
                        // which prevents it from completing a path.
                        costMod = 1f;
                    }else {
                        continue;
                    }
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
        int i = 0;
        while (pos != startPos) {
        	if (i % 4 == 0){
	            if (flyer) {
	                result.add(toAIFloat3(pos, flyheight));
	            }else{
	                result.add(toAIFloat3(pos));
	            }
        	}
        	i++;
            pos = pathTo[pos];
        }
        result.add(target);
        if (result.size() > 1) result.poll(); // remove the unit's current location
        //if (result.size() > 1) result.poll(); // remove the first waypoint because it causes stuttering.
        
        return result;
        
    }
    
    public boolean isAssaultReachable(Unit u, AIFloat3 target, float maxThreat) {
        CostSupplier costs;
        this.maxThreat = maxThreat;
        if (u.getDef().getMoveData() == null){
            costs = AIR_CHECK;
        }else{
            costs = ASSAULT_CHECK;
        }
        return isReachable(u, target, costs);
    }
    
    public boolean isRaiderReachable(Unit u, AIFloat3 target, float maxThreat) {
        CostSupplier costs;
        this.maxThreat = maxThreat;
        if (u.getDef().getMoveData() == null){
            costs = AIR_RAIDER_CHECK;
        }else if (u.getDef().getUnitDefId() == scytheID){
            costs = SCYTHE_CHECK;
        }else{
            costs = RAIDER_CHECK;
        }
        return isReachable(u, target, costs);
    }

    public boolean isReachable(Unit u, AIFloat3 target, CostSupplier costs) {
        AIFloat3 start = u.getPos();
        
        if (start == null){
            return false;
        }

        // bounds check, needed because unit positions can sometimes be nonsensical for various reasons.
        if (start.x < 0 || start.x > mwidth || start.z < 0 || start.z > mheight || target.x < 0 || target.x > mwidth || target.z < 0 || target.z > mheight){
            return false;
        }
        
        maxSlope = 1f;
        if (u.getDef().getMoveData() != null) {
            maxSlope = u.getDef().getMoveData().getMaxSlope();
	        maxDepth = -u.getDef().getMaxWaterDepth();
	        minDepth = (u.getDef().getMinWaterDepth() > 0 ? -u.getDef().getMinWaterDepth() : Float.MAX_VALUE);
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
                return false;
            }

            PathNode current = openSet.poll();
            pos = current.pos;

            closedSet.add(pos);

            // if we reach target pos, we have a complete path.
            if (pos == targetPos){
                return true;
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
                    if (neighbor == targetPos){
                        // due to low slope map res it may consider the unit's position as unpathable,
                        // which prevents it from completing a path.
                        costMod = 1f;
                    }else {
                        continue;
                    }
                }

                float templength = current.pathLength + (offsetCostMod[i] * costMod);

                if (templength < minCosts[neighbor]) {
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
    }
	
	public boolean isWorkerReachable(Unit u, AIFloat3 target) {
		AIFloat3 start = u.getPos();
		CostSupplier costs = TEST_PATH;
		
		if (start == null){
			return false;
		}
		
		// bounds check, needed because unit positions can sometimes be nonsensical for various reasons.
		if (start.x < 0 || start.x > mwidth || start.z < 0 || start.z > mheight || target.x < 0 || target.x > mwidth || target.z < 0 || target.z > mheight){
			return false;
		}
		
		maxSlope = 1f;
		if (u.getDef().getMoveData() != null) {
			maxSlope = u.getDef().getMoveData().getMaxSlope();
			maxDepth = -u.getDef().getMaxWaterDepth();
			minDepth = (u.getDef().getMinWaterDepth() > 0 ? -u.getDef().getMinWaterDepth() : Float.MAX_VALUE);
		}else {
			return true;
		}
		
		int startPos = (int) (start.z / mapRes) * smwidth + (int) (start.x / mapRes); // DON'T reverse start and target since we're getting the closest reachable point.
		int targetPos = (int) (target.z / mapRes) * smwidth + (int) (target.x / mapRes);
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
		
		int bestPos = 0;
		float bestDist = Float.MAX_VALUE;
		
		int pos;
		
		while (true) {
			// if the open set is empty and we haven't reached targetPos, it indicates that the target is unreachable.
			if (openSet.isEmpty()) {
				// take the closest reachable point and see if it's within build range.
				return distance(toAIFloat3(bestPos), target) < u.getDef().getBuildDistance();
			}
			
			PathNode current = openSet.poll();
			pos = current.pos;
			
			closedSet.add(pos);
			
			// if we reach target pos, we have a complete path.
			if (pos == targetPos){
				return true;
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
				float dist = getHeuristic(neighbor, targetPos);
				
				if (dist < bestDist){
					bestDist = dist;
					bestPos = neighbor;
				}
				
				if (templength < minCosts[neighbor]) {
					minCosts[neighbor] = templength;
					pqIndex++;
					PathNode newNode = new PathNode(neighbor, templength + dist, templength, pqIndex);
					
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
	}
	
	public float getPathCost(AIFloat3 start, AIFloat3 target, UnitDef ud) {
		// bounds check, needed because unit positions can sometimes be nonsensical for various reasons.
		CostSupplier costs = TEST_PATH;
		if (start.x < 0 || start.x > mwidth || start.z < 0 || start.z > mheight || target.x < 0 || target.x > mwidth || target.z < 0 || target.z > mheight){
			return -1f;
		}
		
		maxSlope = 1f;
		if (ud.getMoveData() != null) {
			maxSlope = ud.getMoveData().getMaxSlope();
			maxDepth = -ud.getMaxWaterDepth();
			minDepth = (ud.getMinWaterDepth() > 0 ? -ud.getMinWaterDepth() : Float.MAX_VALUE);
			floater = ud.isFloater();
			amph = maxDepth < -1000f;
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
		
		int bestPos = 0;
		float bestDist = Float.MAX_VALUE;
		
		int pos;
		
		while (true) {
			// if the open set is empty and we haven't reached targetPos, it indicates that the target is unreachable.
			if (openSet.isEmpty()) {
				if (distance(toAIFloat3(bestPos), target) < ud.getBuildDistance()){
					return (minCosts[bestPos] * mapRes) + ud.getBuildDistance();
				}
				return -1f;
			}
			
			PathNode current = openSet.poll();
			pos = current.pos;
			
			closedSet.add(pos);
			
			// if we reach target pos, we have a complete path.
			if (pos == targetPos){
				return minCosts[targetPos] * mapRes;
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
					if (neighbor == targetPos){
						// due to low slope map res it may consider the unit's position as unpathable,
						// which prevents it from completing a path.
						costMod = 1f;
					}else {
						continue;
					}
				}
				
				float templength = current.pathLength + (offsetCostMod[i] * costMod);
				float dist = getHeuristic(neighbor, targetPos);
				
				if (dist < bestDist){
					bestDist = dist;
					bestPos = neighbor;
				}
				
				if (templength < minCosts[neighbor]) {
					minCosts[neighbor] = templength;
					pqIndex++;
					PathNode newNode = new PathNode(neighbor, templength + dist, templength, pqIndex);
					
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
	}
    
    private float getHeuristic(int start, int target) {
        double st = (double) start;
        double trg = (double) target;
        return (float) Math.sqrt((start % smwidth - target % smwidth) * (start % smwidth - target % smwidth) + (st / smwidth - trg / smwidth) * (st / smwidth - trg / smwidth));
    }
    
    private float getElev(AIFloat3 pos){
    	return maputil.getElevationAt(pos.x, pos.z);
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
    public final CostSupplier TEST_PATH = new CostSupplier() {
        
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
        	float elev = getElev(pos);
            if ((slope > maxSlope && (!floater || elev > 0f)) || elev < maxDepth || elev > minDepth) {
                return -1;
            }
            if (floater && elev < 0f) return 1f;
            float waterPenalty = amph || elev > 0 ? 0 : 5f;
            return 1f + waterPenalty + (slope / maxSlope);
        }
    };
    /**
     * Fastest path to target while avoiding AA
     */
    public final CostSupplier AIR_PATH = new CostSupplier() {
        
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            return 1f + (5f * warManager.getThreat(pos) + (10 * warManager.getAAThreat(pos)));
        }
    };
    /**
     * Fastest path to target while avoiding AA and riots
     */
    public final CostSupplier AIR_RAIDER_PATH = new CostSupplier() {
        
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            return 1f + (20 * Math.max(Math.max(warManager.getThreat(pos), warManager.getAAThreat(pos)), warManager.getRiotThreat(pos)));
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
            return 1f + (1f - slope) + (20f * Math.max(warManager.getThreat(pos), warManager.getRiotThreat(pos)));
        }
    };
    /**
     * Best path to target while preferring steep slopes.
     */
    public final CostSupplier WIDOW_PATH = new CostSupplier() {
        
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (slope > maxSlope) {
                return -1;
            }
            // use inverse slope-cost relation for spiders, because they actually benefit from hills!
            return 1f + (1f - slope) + (20f * warManager.getScytheThreat(pos));
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
            return 1f + (1f - slope) + (10f * warManager.getThreat(pos));
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
            return 1f + (slope/maxSlope) + (20f * Math.max(warManager.getThreat(pos), warManager.getRiotThreat(pos)));
        }
    };
    /**
     * Fastest path to target while avoiding decloaking
     */
    public final CostSupplier SCYTHE_PATH = new CostSupplier() {

        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (slope > maxSlope) {
                return -1;
            }
            return 1f + (slope/maxSlope) + (20f * Math.max(warManager.getScytheThreat(pos), warManager.getRiotThreat(pos)));
        }
    };
    /**
     * Fastest path to target while not suiciding too badly TODO
     */
    public final CostSupplier ASSAULT_PATH = new CostSupplier() {
        
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (slope > maxSlope) {
                return -1;
            }
            return 1f + (slope/maxSlope) + (5f * warManager.getThreat(pos));
        }
    };
	/**
	 * Fastest path to target while preventing getting stuck on buildings TODO
	 */
	public final CostSupplier STRIDER_PATH = new CostSupplier() {
		
		@Override
		public float getCost(float slope, float maxSlope, AIFloat3 pos) {
			if (slope > maxSlope || !maputil.isPossibleToBuildAt(blocker, pos, 0)) {
				return -1;
			}
			return 1f + (slope/maxSlope) + (5f * warManager.getThreat(pos));
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
            return 1f + (slope/maxSlope) + (20f * warManager.getThreat(pos));
        }
    };
    /**
     * For testing whether a raider or group of raiders can reach a destination in one piece.
     */
    public final CostSupplier RAIDER_CHECK = new CostSupplier() {
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
        	float threat = warManager.getThreat(pos) * (1f + warManager.getRiotThreat(pos));
            if (slope > maxSlope || threat > maxThreat) {
                return -1;
            }else {
                return 1f + (slope/maxSlope) + (20f * threat);
            }
        }
    };
    /**
     * For testing whether a scythe or group of scythes can reach a destination without decloaking.
     */
    public final CostSupplier SCYTHE_CHECK = new CostSupplier() {
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (slope > maxSlope || warManager.getRiotThreat(pos) > 0 || (warManager.getScytheThreat(pos) > 0 && warManager.getThreat(pos) > maxThreat)) {
                return -1;
            }else {
                return 1f + (slope/maxSlope);
            }
        }
    };
    /**
     * For testing whether a squad can reach a destination in one piece.
     */
    public final CostSupplier ASSAULT_CHECK = new CostSupplier() {
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            float threat = warManager.getThreat(pos);
            if (slope > maxSlope || threat > maxThreat) {
                return -1;
            }else {
                return 1f + (slope/maxSlope) + (5f * threat);
            }
        }
    };
    /**
     * For testing whether a flyer can reach a destination in one piece.
     */
    public final CostSupplier AIR_CHECK = new CostSupplier() {
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (warManager.getAAThreat(pos) > 0) {
                return -1;
            }else {
                float threat = warManager.getThreat(pos);
                if (threat > maxThreat) return -1;
                return 1f + (10f * threat);
            }
        }
    };
    /**
     * For testing whether a flying or group of flying raiders can reach a destination in one piece.
     */
    public final CostSupplier AIR_RAIDER_CHECK = new CostSupplier() {
        @Override
        public float getCost(float slope, float maxSlope, AIFloat3 pos) {
            if (warManager.getAAThreat(pos) > 0 || warManager.getRiotThreat(pos) > 0) {
                return -1;
            }else {
                float threat = warManager.getThreat(pos);
                if (threat > maxThreat) return -1;
                return 1f + (10f * threat);
            }
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