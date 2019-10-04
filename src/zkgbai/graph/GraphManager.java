package zkgbai.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;

import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.Worker;
import zkgbai.los.LosManager;
import zkgbai.kgbutil.ByteArrayGraphics;
import zkgbai.military.MilitaryManager;

import static zkgbai.kgbutil.KgbUtil.*;

public class GraphManager extends Module {
	private ZKGraphBasedAI ai;
	private OOAICallback callback;
	private MilitaryManager warManager;
	private ArrayList<MetalSpot> metalSpots;
	private ArrayList<Link> links;
    public int myTeamID;
	int frame = 0;
    private UnitDef mexDef;
    int mexDefID;
	private LosManager losManager;
	Resource e;
	Resource m;

	ByteArrayGraphics frontLineGraphics;
	ByteArrayGraphics allyTerritoryGraphics;
	ByteArrayGraphics enemyTerritoryGraphics;

	float avgMexValue = 0;
	boolean graphInitialized = false;
	boolean bigMap;
	
	public float territoryFraction = 0;
	public boolean eminentTerritory = false;

	static AIFloat3 nullpos = new AIFloat3(0,0,0);
	AIFloat3 allyCenter = nullpos;
	AIFloat3 enemyCenter = nullpos;
	AIFloat3 mapCenter;
	AIFloat3 allyBase = new AIFloat3(0f, 0f, 0f);
	AIFloat3 startPos = nullpos;
	public boolean isWaterMap = false;
	public MetalSpot nullspot;
	
	public HashMap<String, Integer> pylonDefs;
	int pylonCounter;
	//private Image graphImage;
	//private Graphics graphGraphics;
	
	public GraphManager(ZKGraphBasedAI ai){
		this.ai = ai;
		this.callback = ai.getCallback();

		this.metalSpots = new ArrayList<MetalSpot>();
		this.links = new ArrayList<Link>();
		this.mexDef = ai.getCallback().getUnitDefByName("staticmex");
		this.mexDefID = mexDef.getUnitDefId();
		this.m = callback.getResourceByName("Metal");
		this.e = callback.getResourceByName("Energy");

		// calculate the map center
		int x = callback.getMap().getWidth() * 4;
		int z = callback.getMap().getHeight() * 4;
		mapCenter = new AIFloat3(x, 0, z);
		nullspot = new MetalSpot(mapCenter.x, mapCenter.y, mapCenter.z, 0);
		
		// hardwired for now because of segfaults upon segfaults
		pylonDefs = new java.util.HashMap<String, Integer>();
		pylonDefs.put("energywind", 60);
		pylonDefs.put("energysolar", 100);
		pylonDefs.put("energypylon", 500);
		pylonDefs.put("energyfusion", 150);
		pylonDefs.put("energysingu", 150);
		
		final int width = callback.getMap().getWidth()/8;
		final int height = callback.getMap().getHeight()/8;

		this.frontLineGraphics = new ByteArrayGraphics(width, height);
		this.allyTerritoryGraphics = new ByteArrayGraphics(width, height);
		this.enemyTerritoryGraphics = new ByteArrayGraphics(width, height);

		ai.log("GraphManager: Parsing metal spots...");
		
		ArrayList<HashMap> grpMexes = parseMetalSpotsGRP();
		if(grpMexes.size() > 0){
			initializeGraph(grpMexes);
		}
		this.bigMap = (callback.getMap().getHeight() + callback.getMap().getWidth() > 1536);
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
		
		ArrayList<HashMap> data = new ArrayList<HashMap>();
		
		int numSpots = (int)g.getRulesParamFloat("mex_count", 0.0f);

		if (numSpots == 0){
			return data;
		}
		
		ai.log("GraphManager: Detected "+numSpots+" metal spots in GRP");
		
		
		if(numSpots < 0){
			return data;
		}
		
		for (int i=1;i<=numSpots;i++){
			HashMap<String, String> map = new HashMap<String, String>();
			try{
				map.put("x", Float.toString(g.getRulesParamFloat("mex_x"+i, 0.0f)));
				map.put("y", Float.toString(g.getRulesParamFloat("mex_y"+i, 0.0f)));
				map.put("z", Float.toString(g.getRulesParamFloat("mex_z"+i, 0.0f)));
				map.put("metal", Float.toString(g.getRulesParamFloat("mex_metal"+i, 0.0f)));
				data.add(map);
			}
			catch(NullPointerException e){
				ai.log("faulty GRP metal config; returning partial");
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
			ai.log("Parsed JSON metalmap with "+jsonData.size()+" spots");
			if(!graphInitialized){
				initializeGraph(jsonData);
			}
		}
		return 0; //signaling: OK
	}

	@Override
	public int unitCreated(Unit unit, Unit builder) {
		UnitDef def = unit.getDef();
		if (def.getUnitDefId() == mexDefID && (warManager.getTotalFriendlyThreat(unit.getPos()) < 0.5f || !ai.ecoManager.defendedFac)){
			MetalSpot ms = getClosestSpot(unit.getPos());
			setOwned(ms);
			ms.extractor = unit;
		}
		
		
		Integer radius = pylonDefs.get(def.getName());
		if(radius != null){
			Pylon p = new Pylon(unit, radius);

			for(MetalSpot m:metalSpots){
				if(distance(p.position, m.position)<p.radius+50){
					m.addPylon(p);
					p.addSpot(m);
				}
			}

			for(Link l:links){
				if(distance(p.position, l.centerPos) < (l.length/2) + p.radius){
					for(Pylon lp:l.pylons.values()){
						if(!p.equals(lp) && distance(p.position, lp.position) < p.radius+lp.radius){
							lp.addNeighbour(p);
							p.addNeighbour(lp);
						}
					}

					l.addPylon(p);
					l.checkConnected();
				}
			}
		}

		return 0;
	}
	
	@Override
	public int unitFinished(Unit unit){
		if (unit.getDef().getUnitDefId() == mexDefID){
			MetalSpot ms = getClosestSpot(unit.getPos());
			setOwned(ms);
			ms.extractor = unit;
		}
		return 0;
	}

	@Override
	public int unitDestroyed(Unit unit, Unit attacker) {
		UnitDef def = unit.getDef();
		if(pylonDefs.containsKey(def.getName())){
			Pylon deadPylon = new Pylon(unit, pylonDefs.get(def.getName()));
			for(Link l:links){
				if(distance(deadPylon.position, l.centerPos) < (l.length/2) + deadPylon.radius){
					Pylon p = l.pylons.get(unit.getUnitId());

					if (p != null) {
						deadPylon = p;
						break;
					}
				}
			}

			for(Pylon p:deadPylon.neighbours){
				p.removeNeighbour(deadPylon);
			}
			for(MetalSpot m:deadPylon.spots){
				m.pylons.remove(deadPylon);
			}
			for(Link l:deadPylon.links){
				l.removePylon(deadPylon);
				l.checkConnected();
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
					if (u.getDef().getUnitDefId() == mexDefID) {
						ms.hostile = false;
						hasMex = true;
						break;
					}
				}

				if (!hasMex) {
					for (Unit u : enemies) {
						if (u.getDef().getUnitDefId() == mexDefID) {
							setHostile(ms);
							hasMex = true;
							break;
						}
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

		// mark front line territory
		frontLineGraphics.clear();
		List<MetalSpot> frontLine = getFrontLineSpots();
		for (MetalSpot ms: frontLine){
			AIFloat3 pos = ms.getPos();
			int x = Math.round(pos.x/64f);
			int y = Math.round(pos.z/64f);

			frontLineGraphics.paintCircle(x, y, 10, 1);
		}

		calcCenters();
		
		territoryFraction = ((float) getOwnedSpots().size()/(float) getMetalSpots().size());
		eminentTerritory = (((float) getTouchedSpots().size()/(float) getMetalSpots().size() > 0.85f)
								|| (territoryFraction > 0.4f));

		return 0;
	}

	private void setHostile(MetalSpot ms){
		ms.hostile = true;
		ms.owned = false;
		ms.touched = true;
		ms.enemyShadowed = false;

		// paint territory circles
		AIFloat3 pos = ms.getPos();
		int x = Math.round(pos.x/64f);
		int y = Math.round(pos.z/64f);

		enemyTerritoryGraphics.paintCircle(x, y, 13, 1);

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
		ms.touched = true;
		ms.allyShadowed = false;

		// paint territory circles
		AIFloat3 pos = ms.getPos();
		int x = Math.round(pos.x/64f);
		int y = Math.round(pos.z/64f);

		allyTerritoryGraphics.paintCircle(x, y, 25, 1);

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
			ms.extractor = null;

			// unpaint territory circles
			AIFloat3 pos = ms.getPos();
			int x = Math.round(pos.x/64f);
			int y = Math.round(pos.z/64f);

			allyTerritoryGraphics.unpaintCircle(x, y, 25, 1);
		}
		if (ms.hostile){
			ms.enemyShadowed = true;

			// unpaint territory circles
			AIFloat3 pos = ms.getPos();
			int x = Math.round(pos.x/64f);
			int y = Math.round(pos.z/64f);

			enemyTerritoryGraphics.unpaintCircle(x, y, 13, 1);
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

		if (!hasAdjacentHostile && (hasAdjacentOwned || ms.owned)){
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
		for (MetalSpot ms: metalSpots){
			if (!isWaterMap && callback.getMap().getElevationAt(ms.getPos().x, ms.getPos().z) < 10f){
				this.isWaterMap = true;
			}

			String checkpos = "ai_is_valid_startpos:" + ms.getPos().x + "/" + ms.getPos().z;
			if (callback.getLua().callRules(checkpos, checkpos.length()).equals("1")){
				ms.allyShadowed = true;
			}

			checkpos = "ai_is_valid_enemy_startpos:" + ms.getPos().x + "/" + ms.getPos().z;
			if (callback.getLua().callRules(checkpos, checkpos.length()).equals("1")){
				setHostile(ms);
				setNeutral(ms);
			}
		}
	
		// calculate the center starting pos, for placing superweapons and things.
		List<MetalSpot> allySpots = getAllyTerritory();
		if (!allySpots.isEmpty()) {
			for (MetalSpot ms : allySpots) {
				AIFloat3 pos = ms.getPos();
				allyBase.x += pos.x;
				allyBase.z += pos.z;
			}
			allyBase.x /= (float) allySpots.size();
			allyBase.z /= (float) allySpots.size();
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

	public boolean isAllyTerritory(AIFloat3 position){
		int x = Math.round(position.x/64f);
		int y = Math.round(position.z/64f);

		return (allyTerritoryGraphics.getValue(x, y) > 0);
	}

	public boolean isEnemyTerritory(AIFloat3 position){
		int x = Math.round(position.x/64f);
		int y = Math.round(position.z/64f);

		return (enemyTerritoryGraphics.getValue(x, y) > 0);
	}

	public boolean isFrontLine(AIFloat3 position){
		int x = Math.round(position.x/64f);
		int y = Math.round(position.z/64f);

		return (frontLineGraphics.getValue(x, y) > 0);
	}

	public List<MetalSpot> getMetalSpots() {
		return metalSpots;
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
		List<MetalSpot> spots = getOwnedSpots();
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

		spots = getEnemySpots();
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
	
	public AIFloat3 getAllyBase(){
		return allyBase;
	}

	public AIFloat3 getMapCenter(){
		return mapCenter;
	}

	public AIFloat3 getEnemyCenter(){
		return enemyCenter;
	}

	public AIFloat3 getClosestHaven(AIFloat3 position){
		UnitDef building = callback.getUnitDefByName("factorygunship");
		AIFloat3 closest = null;
		float distance = Float.MAX_VALUE;
		for (Worker w: ai.ecoManager.workers.values()){
			if (!isAllyTerritory(w.getPos()) || warManager.getThreat(w.getPos()) > 0 || w.getPos().equals(position)) continue;
			float dist = distance(position, w.getPos())/warManager.getBP(w.getPos());
			if (dist < distance){
				distance = dist;
				closest = getDirectionalPoint(w.getPos(), allyCenter, 100f);
			}
		}
		if (closest != null) {
			return closest;
		}else{
			return getAllyCenter();
		}
	}
	
	public AIFloat3 getClosestRaiderHaven(AIFloat3 position){
		AIFloat3 closest = null;
		float distance = Float.MAX_VALUE;
		for(MetalSpot ms: metalSpots){
			if (warManager.getThreat(ms.getPos()) == 0){
				float tempdist = distance(position, ms.getPos());
				if (tempdist < distance){
					distance = tempdist;
					closest = ms.getPos();
				}
			}
		}
		if (closest == null) {
			return mapCenter;
		}
		return closest;
	}

	public AIFloat3 getClosestFrontLineLink(AIFloat3 position){
		AIFloat3 bestpos = null;
		float bestdist = Float.MAX_VALUE;
		for (Link l: links){
			if (l.v0.owned && l.v1.owned && isFrontLine(l.centerPos)){
				float dist = distance(position, l.centerPos);
				if (dist < bestdist){
					bestdist = dist;
					bestpos = l.centerPos;
				}
			}
		}
		return bestpos;
	}

	public AIFloat3 getClosestAirHaven(AIFloat3 position){
		UnitDef building = callback.getUnitDefByName("factorygunship");
		AIFloat3 closest = null;
		float distance = Float.MAX_VALUE;
		for (Worker w: ai.ecoManager.workers.values()){
			if (!isAllyTerritory(w.getPos()) || warManager.getThreat(w.getPos()) > 0 || warManager.getAAThreat(w.getPos()) > 0 || w.getPos().equals(position)) continue;
			float dist = distance(position, w.getPos())/warManager.getBP(w.getPos());
			if (dist < distance){
				distance = dist;
				closest = getDirectionalPoint(w.getPos(), allyCenter, 100f);
			}
		}
		if (closest != null) {
			closest = callback.getMap().findClosestBuildSite(building, closest, 600f, 3, 0);
		}else{
			return getAllyCenter();
		}
		return closest;
	}
	
	public AIFloat3 getClosestLeadingLink(AIFloat3 position){
		UnitDef building = callback.getUnitDefByName("factorygunship");
		AIFloat3 closest = null;
		float fthreat = warManager.getFriendlyThreat(position);
		float distance = Float.MAX_VALUE;
		for (Link l:links){
			if ((l.v0.allyShadowed || l.v1.allyShadowed) && !l.v0.hostile && !l.v1.hostile && warManager.getEffectiveThreat(l.getPos()) <= fthreat){
				float dist = distance(position, l.getPos());
				if (dist < distance){
					distance = dist;
					closest = l.getPos();
				}
			}
		}
		if (closest != null) {
			closest = callback.getMap().findClosestBuildSite(building, closest, 600f, 3, 0);
		}
		return closest;
	}
	
	public AIFloat3 getClosestAirLeadingLink(AIFloat3 position){
		UnitDef building = callback.getUnitDefByName("factorygunship");
		AIFloat3 closest = null;
		float distance = Float.MAX_VALUE;
		for (Link l:links){
			if ((l.v0.allyShadowed || l.v1.allyShadowed) && !l.v0.hostile && !l.v1.hostile && warManager.getAAThreat(l.getPos()) == 0){
				float dist = distance(position, l.getPos());
				if (dist < distance){
					distance = dist;
					closest = l.getPos();
				}
			}
		}
		if (closest != null) {
			closest = callback.getMap().findClosestBuildSite(building, closest, 600f, 3, 0);
		}
		return closest;
	}

	public AIFloat3 getClosestBattleFront(AIFloat3 position){
		AIFloat3 pos = null;
		float mindist = Float.MAX_VALUE;
		for (MetalSpot ms: metalSpots){
			if (!ms.owned && !isAllyTerritory(ms.getPos())){
				float dist = distance(ms.position, position);
				if (dist < mindist){
					mindist = dist;
					pos = ms.position;
				}
			}
		}
		return (pos != null) ? pos : enemyCenter != null ? enemyCenter : mapCenter;
	}

	public List<MetalSpot> getOwnedSpots(){
		// returns all metal spots not owned by allies.
		List<MetalSpot> spots = new ArrayList<MetalSpot>();
		for(MetalSpot ms:metalSpots){
			if(ms.owned) spots.add(ms);
		}
		return spots;
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

	public List<MetalSpot> getTouchedSpots(){
		ArrayList<MetalSpot> spots = new ArrayList<MetalSpot>();
		for(MetalSpot ms:metalSpots){
			if(ms.touched) spots.add(ms);
		}
		return spots;
	}
    
    public MetalSpot getClosestNeutralSpot(AIFloat3 position){
    	float minRange = Float.MAX_VALUE;
    	MetalSpot bestMS = nullspot;
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
		MetalSpot bestMS = nullspot;
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
		MetalSpot bestMS = nullspot;
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
		MetalSpot bestSpot = nullspot;
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

	public AIFloat3 getClosestPylonSpot(AIFloat3 position){
		// First find the nearest unconnected mex
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

		// then find the shortest unconnected link
		Link link = null;
		distance = Float.MAX_VALUE;
		for (Link l:best.links){
			if (!l.connected && l.isOwned() && l.v0.extractor.getHealth() > 0 && l.v1.extractor.getHealth() > 0 && l.v0.extractor.getRulesParamFloat("gridNumber", 0f) != l.v1.extractor.getRulesParamFloat("gridNumber", 0f)){
				if (l.length < distance){
					distance = l.length;
					link = l;
				}
			}
		}
		return link.getPos();
	}
    
    public AIFloat3 getOverdriveSweetSpot(AIFloat3 position, UnitDef pylon){
		float radius = 0.9f * pylonDefs.get(pylon.getName());
		float distance = Float.MAX_VALUE;
	
	    // First find the nearest unconnected mex
	    MetalSpot best = null;
	    for (MetalSpot ms:metalSpots){
		    if (!ms.isConnected()){
			    float dist = distance(position, ms.getPos());
			    if (dist < distance){
				    distance = dist;
				    best = ms;
			    }
		    }
	    }
	
	    if (best == null) {
		    best = getClosestSpot(new AIFloat3(0, 0, 0));
	    }
	
	    // then find the shortest unconnected link
	    Link link = null;
	    distance = Float.MAX_VALUE;
	    for (Link l:best.links){
		    if (!l.connected && l.isOwned() && (l.v0.extractor == null || l.v1.extractor == null || (l.v0.extractor.getHealth() > 0 && l.v1.extractor.getHealth() > 0 && l.v0.extractor.getRulesParamFloat("gridNumber", 0f) != l.v1.extractor.getRulesParamFloat("gridNumber", 0f)))){
			    if (l.length < distance){
				    distance = l.length;
				    link = l;
			    }
		    }
	    }

    	if(link != null){
    		Pylon p = link.getConnectionHead(position);
    		
    		if(p != null){
				radius += 0.9f * p.radius;
				if (distance(position, link.v0.getPos()) < distance(position, link.v1.getPos())){
					return getDirectionalPoint(p.position, link.v1.getPos(), radius);
				}else {
					return getDirectionalPoint(p.position, link.v0.getPos(), radius);
				}
    		}else{
				// if no pylons are present, start a new chain.
				if (distance(position, link.v0.getPos()) < distance(position, link.v1.getPos())){
					return getDirectionalPoint(link.v0.getPos(), link.getPos(), radius);
				}else {
					return getDirectionalPoint(link.v1.getPos(), link.getPos(), radius);
				}
			}
    	}else if (best != null){
			return best.getPos();
		}

		// if there are no unconnected owned links, return null
		return null;
    }
}
