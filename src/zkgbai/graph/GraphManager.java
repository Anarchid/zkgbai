package zkgbai.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.poly2tri.Poly2Tri;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;
import org.poly2tri.triangulation.point.TPoint;
import org.poly2tri.triangulation.sets.PointSet;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;
import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Game;
import com.springrts.ai.oo.clb.GameRulesParam;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Team;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;

import zkgbai.Module;
import zkgbai.StartArea;
import zkgbai.ZKGraphBasedAI;
import zkgbai.los.LosManager;
import zkgbai.military.MilitaryManager;

import static zkgbai.kgbutil.KgbUtil.*;

public class GraphManager extends Module {
	private ZKGraphBasedAI ai;
	private OOAICallback callback;
	private MilitaryManager warManager;
	private ArrayList<MetalSpot> metalSpots;
	private ArrayList<Link> links;
	private ArrayList<Pylon> pylons;
    public int myTeamID;
	int frame = 0;
    private UnitDef mexDef;
    int mexDefID;
	private LosManager losManager;
	Resource e;
	Resource m;

	float avgMexValue = 0;
	boolean graphInitialized = false;

	static AIFloat3 nullpos = new AIFloat3(0,0,0);
	AIFloat3 allyCenter = nullpos;
	AIFloat3 enemyCenter = nullpos;
	AIFloat3 mapCenter;
	AIFloat3 startPos = nullpos;
	public boolean isWaterMap = false;
	
	public HashMap<String, Integer> pylonDefs; 
	int pylonCounter;
	//private Image graphImage;
	//private Graphics graphGraphics;
	
	public GraphManager(ZKGraphBasedAI ai){
		this.ai = ai;
		this.callback = ai.getCallback();

		this.metalSpots = new ArrayList<MetalSpot>();
		this.links = new ArrayList<Link>();
		this.pylons = new ArrayList<Pylon>();
		this.mexDef = ai.getCallback().getUnitDefByName("cormex");
		this.mexDefID = mexDef.getUnitDefId();
		this.m = callback.getResourceByName("Metal");
		this.e = callback.getResourceByName("Energy");

		// calculate the map center
		int x = callback.getMap().getWidth() * 4;
		int z = callback.getMap().getHeight() * 4;
		mapCenter = new AIFloat3(x, 0, z);
		
		// hardwired for now because of segfaults upon segfaults
		pylonDefs = new java.util.HashMap<String, Integer>();
		pylonDefs.put("armwin", 60);
		pylonDefs.put("armsolar", 100);
		pylonDefs.put("armestor", 500);
		pylonDefs.put("armfus", 150);
		pylonDefs.put("cafus", 150);
		
		final int width = callback.getMap().getWidth();
		final int height = callback.getMap().getHeight();

		ai.debug("GraphManager: Parsing metal spots...");
		
		ArrayList<HashMap> grpMexes = parseMetalSpotsGRP();
		if(grpMexes.size() > 0){
			initializeGraph(grpMexes);
		}
	}

	@Override
	public int init(int AIID, OOAICallback cb){
		this.losManager = ai.losManager;
		this.warManager = ai.warManager;
		return 0;
	}
	
	@Override
	public String getModuleName() {
		return "GraphManager";
	}
	
	@SuppressWarnings("rawtypes")
	private ArrayList<HashMap> parseMetalSpotsGRP(){
		Game g = ai.getCallback().getGame();
		
		GameRulesParam mexCount = g.getGameRulesParamByName("mex_count");
		
		ArrayList<HashMap> data = new ArrayList<HashMap>();

		if (mexCount == null){
			return data;
		}
		
		int numSpots = (int)mexCount.getValueFloat();
		
		ai.debug("GraphManager: Detected "+numSpots+" metal spots in GRP");
		
		
		if(numSpots < 0){
			return data;
		}
		
		for (int i=1;i<=numSpots;i++){
			HashMap<String, String> map = new HashMap<String, String>();
			try{
				map.put("x", Float.toString(g.getGameRulesParamByName("mex_x"+i).getValueFloat()));
				map.put("y", Float.toString(g.getGameRulesParamByName("mex_y"+i).getValueFloat()));
				map.put("z", Float.toString(g.getGameRulesParamByName("mex_z"+i).getValueFloat()));
				map.put("metal", Float.toString(g.getGameRulesParamByName("mex_metal"+i).getValueFloat()));
				data.add(map);
			}
			catch(NullPointerException e){
				ai.debug("faulty GRP metal config; returning partial");
				return data;
			}
		}
		
		return data;
	}
	
    @Override
    public int luaMessage(java.lang.String inData){        
    	if(inData.startsWith("METAL_SPOTS:")){
    		String json = inData.substring(12);
			JsonParserFactory factory=JsonParserFactory.getInstance();
			JSONParser parser=factory.newJsonParser();
			ArrayList<HashMap> jsonData=(ArrayList)parser.parseJson(json).values().toArray()[0];
    		ai.debug("Parsed JSON metalmap with "+jsonData.size()+" spots");
			if(!graphInitialized){
				initializeGraph(jsonData);	
			}
    	}
    	return 0; //signaling: OK
    }
    
    @Override
    public int unitFinished(Unit unit) {
    	UnitDef def = unit.getDef();
		Integer radius = pylonDefs.get(def.getName());
		if(radius != null){
			Pylon p = new Pylon(unit, radius.intValue());

			for(MetalSpot m:metalSpots){
				if(distance(p.position, m.position)<p.radius+50){
					m.addPylon(p);
					p.addSpot(m);
				}
			}

			for(Link l:links){
				if(distance(p.position, l.centerPos) < l.length/2){
					for(Pylon lp:l.pylons){
						if(distance(p.position, lp.position) < p.radius+lp.radius){
							lp.addNeighbour(p);
							p.addNeighbour(lp);
						}
					}

					l.addPylon(p);
					if(l.checkConnected()){

					}
				}
			}

			pylons.add(p);
		}

    	return 0;
    }

	@Override
    public int unitDestroyed(Unit unit, Unit attacker) {
    	UnitDef def = unit.getDef();
		if(!def.isBuilder() && def.getName().equals("armsolar")){
    		Pylon deadPylon = null;
    		for(Pylon p:pylons){
    			if(p.unit.equals(unit)){
    				deadPylon = p;
    				break;
    			}
    		}
    		
    		if(deadPylon != null){
    			for(Pylon p:deadPylon.neighbours){
    				p.neighbours.remove(deadPylon);
    			}
    			for(MetalSpot m:deadPylon.spots){
    				m.pylons.remove(deadPylon);
    			}
    			for(Link l:deadPylon.links){
    				l.pylons.remove(deadPylon);
    				if(!l.checkConnected()){
    				}
    			}
    		}
    		// destroy pylon
    	}
        return 0; // signaling: OK
    }

	@Override
	public int enemyEnterLOS(Unit enemy){
		// when enemy porc is seen, infer that the nearest metal spot is hostile
		if (enemy.getMaxSpeed() == 0 && enemy.getDef().isAbleToAttack()){
			MetalSpot spot = getClosestSpot(enemy.getPos());
			// unless it's already owned by allies
			if (!spot.hostile && !spot.owned){
				setHostile(spot);
			}
		}
		return 0;
	}
    
    // TODO: move los stuff to a separate handler
	@Override
	public int update(int uframe){
		this.frame = uframe;
		if (frame % 60 > 0){
			return 0;
		}

    	for(MetalSpot ms:metalSpots){
    		if(losManager.isInLos(ms.getPos())){
				ms.lastSeen = frame;
				ms.visible = true;

				List<Unit> friendlies = callback.getFriendlyUnitsIn(ms.getPos(), 50f);
				List<Unit> enemies = callback.getEnemyUnitsIn(ms.getPos(), 50f);

				boolean hasMex = false;
				for (Unit u:friendlies){
					if (u.getDef().getUnitDefId() == mexDefID){
						setOwned(ms);
						hasMex = true;
					}
				}

				for (Unit u: enemies){
					if (u.getDef().getUnitDefId() == mexDefID){
						setHostile(ms);
						hasMex = true;
					}
				}

				if (!hasMex){
					setNeutral(ms);
				}

				cleanShadows(ms);
    		}else{
    			if(ms.visible){
    				ms.visible = false;
    			}
				if (ms.owned){
					ms.owned = false;
				}
				if (frame - ms.lastSeen > 3600){
					ms.lastSeen = frame-1800;
				}
    		}
    		
    	}

    	calcCenters();

		return 0;
	}

	private void setHostile(MetalSpot ms){
		ms.hostile = true;
		ms.owned = false;
		ms.enemyShadowed = false;
		// set adjacent spots as enemyShadowed if they aren't already hostile
		for (Link l:ms.links){
			if (!l.v0.hostile){
				l.v0.enemyShadowed = true;
			}
			if (!l.v1.hostile){
				l.v1.enemyShadowed = true;
			}
		}
	}

	private void setOwned(MetalSpot ms){
		ms.hostile = false;
		ms.owned = true;
		ms.allyShadowed = false;
		// set adjacent spots as allyShadowed if they aren't already owned
		for (Link l:ms.links){
			if (!l.v0.owned){
				l.v0.allyShadowed = true;
			}
			if (!l.v1.owned){
				l.v1.allyShadowed = true;
			}
		}
	}

	private void setNeutral(MetalSpot ms){
		if (ms.owned){
			ms.allyShadowed = true;
		}
		if (ms.hostile){
			ms.enemyShadowed = true;
		}
		ms.hostile = false;
		ms.owned = false;
	}

	private void cleanShadows(MetalSpot ms){
		boolean hasAdjacentOwned = false;
		boolean hasAdjacentHostile = false;
		for (Link l:ms.links){
			if (l.v0.hostile || l.v1.hostile){
				hasAdjacentHostile = true;
			}
			if (l.v0.owned || l.v1.owned){
				hasAdjacentOwned = true;
			}
		}

		if ((!hasAdjacentHostile && frame - ms.lastSeen > 9001)
				|| (!hasAdjacentHostile && ms.owned)){
			ms.enemyShadowed = false;
		}
		if (!hasAdjacentOwned){
			ms.allyShadowed = false;
		}
	}
    
    private void initializeGraph(ArrayList<HashMap> jsonData){
    	List<TriangulationPoint> points = new ArrayList<TriangulationPoint>();
    	HashMap<TriangulationPoint,MetalSpot> mexes = new HashMap<TriangulationPoint,MetalSpot>();
    	
		for (HashMap s:jsonData){
			float x = Float.parseFloat((String)s.get("x"));
			float y = Float.parseFloat((String)s.get("y"));
			float z = Float.parseFloat((String)s.get("z"));
			float m = Float.parseFloat((String)s.get("metal"));
			
			TPoint p = new TPoint(x,z);
			MetalSpot ms = new MetalSpot(x,y,z,m);
			metalSpots.add(ms);
    		points.add(p);
    		mexes.put(p, ms);
		}
		
		PointSet ps = new PointSet(points);
    	Poly2Tri.triangulate(ps);
    	List<DelaunayTriangle> triangles = ps.getTriangles();
    	ArrayList<TriangulationEdge> edges = new ArrayList<TriangulationEdge>();
    	
    	for(DelaunayTriangle t:triangles){
    		edges.add(new TriangulationEdge(t.points[0],t.points[1]));
    		edges.add(new TriangulationEdge(t.points[0],t.points[2]));
    		edges.add(new TriangulationEdge(t.points[1],t.points[2]));
    	}
    	
		Set<TriangulationEdge> set = new HashSet<TriangulationEdge>();
    	for(TriangulationEdge edge:edges){
    		set.add(edge);
    	}
    	
    	edges = new ArrayList<TriangulationEdge>(set);
    	for(TriangulationEdge edge:edges){
    		MetalSpot m0 = mexes.get(edge.v0); 
    		MetalSpot m1 = mexes.get(edge.v1);
    		
    		Link l = new Link(m0,m1);
    		m0.addLink(l);
    		m1.addLink(l);
    		
    		links.add(l);
    	}
    	
    	doInitialInference();
    	graphInitialized = true;
    }
    
    private void doInitialInference(){
		Set<Integer> enemies = ai.getEnemyAllyTeamIDs();
		if(ai.startType == ZKGraphBasedAI.StartType.ZK_STARTPOS){
			// identify ally startbox ID's
			Set<Integer> allyBoxes = new HashSet<Integer>();
			for(Team a:callback.getAllyTeams()){
				int boxID = (int)a.getTeamRulesParamByName("start_box_id").getValueFloat();
				allyBoxes.add(boxID);
				ai.debug("team "+a.getTeamId()+" of allyteam "+ai.getCallback().getGame().getTeamAllyTeam(a.getTeamId())+" is ally with boxID "+boxID);
			}
			
			for(Entry<Integer, StartArea> s:ai.startBoxes.entrySet()){
				if(!allyBoxes.contains(s.getKey())){
					ai.debug(s.getKey()+" is an enemy startbox");
					for (MetalSpot ms:metalSpots){
						AIFloat3 pos = ms.position;
						if(s.getValue().contains(pos)){
							setHostile(ms);
							setNeutral(ms);
						}
					}
				}else{
					ai.debug(s.getKey()+" is an allied startbox");
					for (MetalSpot ms:metalSpots){
						AIFloat3 pos = ms.position;
						if(s.getValue().contains(pos)){
							ms.allyShadowed = true;
						}
					}
				}
			}
			
		}else{
			StartArea box = null;
			for(int enemy:enemies){
				box = ai.getStartArea(enemy);
			
				if(box!=null){
					for (MetalSpot ms:metalSpots){
						AIFloat3 pos = ms.position;
						if(box.contains(pos)){
							setHostile(ms);
							setNeutral(ms);
						}
					}
				}
			}

			box = ai.getStartArea(ai.allyTeamID);
			for (MetalSpot ms:metalSpots){
				AIFloat3 pos = ms.position;
				if(box.contains(pos)){
					ms.allyShadowed = true;
				}
			}
		}

		for (MetalSpot ms: metalSpots){
			if (callback.getMap().getElevationAt(ms.getPos().x, ms.getPos().z) < 10f){
				this.isWaterMap = true;
				break;
			}
		}
    }
    
	/*public void paintGraph(){

		graphGraphics.clear();
		
		Color spotOwned = new Color(0,255,0,255);
		Color spotUnowned = new Color(255, 247, 90,255);
		Color spotHostile = new Color(255,0,0,255);
		Color spotContested = new Color(239, 127, 11, 255);

		Color linkLinked = new Color(0,255,255,100);
		Color linkUnlinked = new Color(255,255,0,100);

		for (Link l:links){
			Color linkColor;
			graphGraphics.setLineWidth(5);
			if(l.connected){
				linkColor = linkLinked;
			}else{
				linkColor = linkUnlinked;
			}

			graphGraphics.setColor(linkColor);

			int x1 = (int) (l.v0.position.x / 8);
			int y1 = (int) (l.v0.position.z / 8);

			int x2 = (int) (l.v1.position.x / 8);
			int y2 = (int) (l.v1.position.z / 8);

			graphGraphics.drawLine(x1, y1, x2, y2);
		}

		for(MetalSpot ms:metalSpots){
			AIFloat3 position = ms.position;
			
			int x = (int) (position.x / 8);
			int y = (int) (position.z / 8);

			// draw solid dots for owned/hostile mexes
			if(ms.owned){
				graphGraphics.setColor(spotOwned);
				paintCircle(x,y,6);
			}else if (ms.hostile) {
				graphGraphics.setColor(spotHostile);
				paintCircle(x, y, 6);
			}else{
				graphGraphics.setColor(spotUnowned);
				paintCircle(x,y,4);
			}

			// draw hollow circles for shadow captured mexes
			if(ms.allyShadowed && !(ms.enemyShadowed)){
				graphGraphics.setColor(spotOwned);
				paintCircleOutline(x, y, 8);
			}else if (ms.enemyShadowed && !(ms.allyShadowed)) {
				graphGraphics.setColor(spotHostile);
				paintCircleOutline(x, y, 8);
			}else if (ms.allyShadowed && ms.enemyShadowed){
				graphGraphics.setColor(spotContested);
				paintCircleOutline(x, y, 8);
			}
		}
	}
	
	private void paintCircleOutline(int x, int y, int r){
		graphGraphics.drawOval(x-r, y-r, 2*r, 2*r);
	}

	private void paintCircle(int x, int y, int r){
		graphGraphics.fillOval(x - r, y - r, 2 * r, 2 * r);
	}*/

	public void setStartPos(AIFloat3 pos){
		startPos = pos;
	}

	public AIFloat3 getStartPos(){
		return startPos;
	}

	public List<Link> getLinks(){
		return links;
	}
    
    public List<MetalSpot> getEnemySpots(){
    	ArrayList<MetalSpot> spots = new ArrayList<MetalSpot>();
    	for(MetalSpot ms:metalSpots){
    		if(ms.hostile) spots.add(ms);
    	}
    	return spots;
    }

	public List<MetalSpot> getEnemyTerritory(){
		ArrayList<MetalSpot> spots = new ArrayList<MetalSpot>();
		for(MetalSpot ms:metalSpots){
			if((ms.hostile || ms.enemyShadowed) && !ms.owned) spots.add(ms);
		}
		return spots;
	}

	public List<MetalSpot> getAllyTerritory(){
		// returns all metal spots not owned by allies.
		List<MetalSpot> spots = new ArrayList<MetalSpot>();
		for(MetalSpot ms:metalSpots){
			if((ms.owned || ms.allyShadowed) && !ms.hostile) spots.add(ms);
		}
		return spots;
	}

	private void calcCenters(){
		List<MetalSpot> spots = getAllyTerritory();
		if (spots.size() > 0){
			AIFloat3 position = new AIFloat3();
			int count = spots.size();
			float x = 0;
			float z = 0;
			for (MetalSpot ms : spots) {
				x += (ms.getPos().x) / count;
				z += (ms.getPos().z) / count;
			}
			position.x = x;
			position.z = z;
			UnitDef factory = callback.getUnitDefByName("factorygunship");
			position = callback.getMap().findClosestBuildSite(factory, position, 600f, 3, 0);
			allyCenter = position;
		}else {
			allyCenter = nullpos;
		}

		spots = getEnemyTerritory();
		if (spots.size() > 0){
			AIFloat3 position = new AIFloat3();
			int count = spots.size();
			float x = 0;
			float z = 0;
			for (MetalSpot ms : spots) {
				x += (ms.getPos().x) / count;
				z += (ms.getPos().z) / count;
			}
			position.x = x;
			position.z = z;
			UnitDef factory = callback.getUnitDefByName("factorygunship");
			position = callback.getMap().findClosestBuildSite(factory, position, 600f, 3, 0);
			enemyCenter = position;
		}else{
			enemyCenter = nullpos;
		}
	}

	public AIFloat3 getAllyCenter(){
		return allyCenter;
	}

	public AIFloat3 getMapCenter(){
		return mapCenter;
	}

	public AIFloat3 getEnemyCenter(){
		return enemyCenter;
	}

	public AIFloat3 getClosestHaven(AIFloat3 position){
		UnitDef building = callback.getUnitDefByName("factorygunship");
		AIFloat3 closest = position; // returns position as a fallback
		float distance = Float.MAX_VALUE;
		for (Link l:links){
			if (l.isOwned() && warManager.getThreat(l.getPos()) == 0){
				float dist = distance(position, l.getPos());
				if (dist < distance){
					distance = dist;
					closest = l.getPos();
				}
			}
		}
		closest = callback.getMap().findClosestBuildSite(building, closest, 600f, 3, 0);
		return closest;
	}

	public AIFloat3 getClosestAirHaven(AIFloat3 position){
		UnitDef building = callback.getUnitDefByName("factorygunship");
		AIFloat3 closest = position; // returns position as a fallback
		float distance = Float.MAX_VALUE;
		for (Link l:links){
			if (l.isOwned() && warManager.getAAThreat(l.getPos()) == 0){
				float dist = distance(position, l.getPos());
				if (dist < distance){
					distance = dist;
					closest = l.getPos();
				}
			}
		}
		closest = callback.getMap().findClosestBuildSite(building, closest, 600f, 3, 0);
		return closest;
	}

	public List<MetalSpot> getUnownedSpots(){
		// returns all metal spots not owned by allies.
		List<MetalSpot> spots = new ArrayList<MetalSpot>();
		for(MetalSpot ms:metalSpots){
			if(!ms.owned) spots.add(ms);
		}
		return spots;
	}

    public List<MetalSpot> getNeutralSpots(){
    	ArrayList<MetalSpot> spots = new ArrayList<MetalSpot>();
    	for(MetalSpot ms:metalSpots){
    		if(!ms.owned && !ms.hostile) spots.add(ms);
    	}
    	return spots;
    }
    
    public MetalSpot getClosestNeutralSpot(AIFloat3 position){
    	float minRange = Float.MAX_VALUE;
    	MetalSpot bestMS = null;
    	for(MetalSpot ms:metalSpots){
    		if(!ms.owned && !ms.hostile){
    			float dist = distance(position, ms.position);
    			if(dist < minRange){
    				bestMS = ms;
    				minRange = dist;
    			}
    		}
    	}
    	
    	return bestMS;
    }

	public MetalSpot getClosestEnemySpot(AIFloat3 position){
		float minRange = Float.MAX_VALUE;
		MetalSpot bestMS = null;
		for(MetalSpot ms:metalSpots){
			if(ms.hostile){
				float dist = distance(position, ms.position);
				if(dist < minRange){
					bestMS = ms;
					minRange = dist;
				}
			}
		}

		return bestMS;
	}

	public MetalSpot getClosestSpot(AIFloat3 position){
		float minRange = Float.MAX_VALUE;
		MetalSpot bestMS = null;
		for(MetalSpot ms:metalSpots){
				float dist = distance(position, ms.position);
				if(dist < minRange){
					bestMS = ms;
					minRange = dist;
				}
		}

		return bestMS;
	}

	public List<MetalSpot> getFrontLineSpots(){
		List<MetalSpot> spots = new ArrayList<MetalSpot>();
		for (MetalSpot ms: metalSpots){
			if (ms.isFrontLine()){
				spots.add(ms);
			}
		}
		return spots;
	}

	public MetalSpot getClosestFrontLineSpot(AIFloat3 position){
		List<MetalSpot> spots = getFrontLineSpots();
		float distance = Float.MAX_VALUE;
		MetalSpot bestSpot = null;
		for (MetalSpot ms:spots){
			if (ms.owned) {
				float dist = distance(position, ms.getPos());
				if (dist < distance) {
					bestSpot = ms;
					distance = dist;
				}
			}
		}
		return bestSpot;
	}
    
    public AIFloat3 getOverdriveSweetSpot(AIFloat3 position, UnitDef pylon){
		float radius = 0.9f * pylonDefs.get(pylon.getName());

		// First find the nearest mex with less than 2 connected links
		MetalSpot best = null;
		float distance = Float.MAX_VALUE;
		for (MetalSpot ms:metalSpots){
			if (!ms.isConnected()){
				float dist = distance(position, ms.getPos());
				if (dist < distance){
					distance = dist;
					best = ms;
				}
			}
		}

		// then find the nearest unconnected link
		Link link = null;
		distance = Float.MAX_VALUE;
		for (Link l:best.links){
			if (!l.connected && l.length < 1500 && l.isOwned()){
				float dist = distance(position, l.getPos());
				if (dist < distance){
					distance = dist;
					link = l;
				}
			}
		}

    	if(link != null){
    		Pylon p = link.getConnectionHead();
    		
    		if(p != null){
				radius += 0.9f * p.radius;

				float dx=link.v1.position.x - p.position.x;
				float dz=link.v1.position.z - p.position.z;
				
				double d = Math.sqrt(dx*dx+dz*dz);
				float vx = (float) (dx/d);
				float vz = (float) (dz/d);
				
				float x = p.position.x + vx*radius;
				float z = p.position.z + vz*radius;
				AIFloat3 newpos = new AIFloat3(x,p.position.y,z);
				return newpos;
    		}else{
				// if no pylons are present, start a new chain.
				return getDirectionalPoint(link.v1.getPos(), link.getPos(), radius);
			}
    	}	

		// if there are no unconnected owned links, return null
		return null;
    }
}
