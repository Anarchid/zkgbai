package zkgbai.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import com.springrts.ai.AICallback;

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
import com.springrts.ai.oo.clb.Map;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Team;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;

import zkgbai.Module;
import zkgbai.StartArea;
import zkgbai.StartBox;
import zkgbai.ZKGraphBasedAI;
import zkgbai.gui.DebugView;
import zkgbai.los.LosManager;
import zkgbai.military.Enemy;

public class GraphManager extends Module {
	private ZKGraphBasedAI parent;
	private OOAICallback callback;
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
	boolean graphInitialized;
	
	BufferedImage threatMap;

	AIFloat3 allyCenter = null;
	
	public HashMap<String, Integer> pylonDefs; 
	int pylonCounter;
	private BufferedImage graphImage;
	private Graphics2D graphGraphics;
	
	public GraphManager(ZKGraphBasedAI parent){
		graphInitialized = false;
		
		this.metalSpots = new ArrayList<MetalSpot>();
		this.links = new ArrayList<Link>();
		this.pylons = new ArrayList<Pylon>();
		this.parent = parent;
		this.callback = parent.getCallback();
		this.mexDef = parent.getCallback().getUnitDefByName("cormex");
		this.mexDefID = mexDef.getUnitDefId();
		this.m = parent.getCallback().getResourceByName("Metal");
		this.e = parent.getCallback().getResourceByName("Energy");
		
		// hardwired for now because of segfaults upon segfaults
		pylonDefs = new java.util.HashMap<String, Integer>();
		pylonDefs.put("armsolar", 100);
		pylonDefs.put("armestor", 500);
		pylonDefs.put("armfus", 150);
		pylonDefs.put("cafus", 150);
		
		int width = parent.getCallback().getMap().getWidth();
		int height = parent.getCallback().getMap().getHeight();
		
		this.graphImage = new BufferedImage(width, height,BufferedImage.TYPE_INT_ARGB_PRE);
		this.graphGraphics = graphImage.createGraphics();
		
		ArrayList<HashMap> grpMexes = parseMetalSpotsGRP();
		if(grpMexes.size() > 0){
			initializeGraph(grpMexes);
		}
		
	}
	
	@Override
	public String getModuleName() {
		return "GraphManager";
	}
	
	@SuppressWarnings("rawtypes")
	private ArrayList<HashMap> parseMetalSpotsGRP(){
		Game g = parent.getCallback().getGame();
		
		GameRulesParam mexCount = g.getGameRulesParamByName("mex_count");
		
		ArrayList<HashMap> data = new ArrayList<HashMap>();

		if (mexCount == null){
			return data;
		}
		
		int numSpots = (int)mexCount.getValueFloat();
		
		parent.debug("Detected "+numSpots+" metal spots in GRP");
		
		
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
				parent.debug("faulty GRP metal config; returning partial");
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
    		parent.debug("Parsed JSON metalmap with "+jsonData.size()+" spots");
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
				if(groundDistance(p.position,m.position)<p.radius+50){
					m.addPylon(p);
					p.addSpot(m);
				}
			}

			for(Link l:links){
				if(GraphManager.groundDistance(p.position,l.centerPos) < l.length/2){
					for(Pylon lp:l.pylons){
						if(GraphManager.groundDistance(p.position,lp.position) < p.radius+lp.radius){
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

    	calcAllyCenter();
    	paintGraph();

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

    public static float groundDistance(AIFloat3 v0, AIFloat3 v1){
    	float dx = v0.x-v1.x;
    	float dz = v0.z-v1.z;
    	return (float) Math.sqrt(dx*dx+dz*dz);
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
    	paintGraph();
    	graphInitialized = true;
    }
    
    private void doInitialInference(){
		Set<Integer> enemies = parent.getEnemyAllyTeamIDs();
		if(parent.startType == ZKGraphBasedAI.StartType.ZK_STARTPOS){
			// identify ally startbox ID's
			Set<Integer> allyBoxes = new HashSet<Integer>();
			for(Team a:parent.allies){
				int boxID = (int)a.getTeamRulesParamByName("start_box_id").getValueFloat();
				allyBoxes.add(boxID);
				parent.debug("team "+a.getTeamId()+" of allyteam "+parent.getCallback().getGame().getTeamAllyTeam(a.getTeamId())+" is ally with boxID "+boxID);
			}
			
			for(Entry<Integer, StartArea> s:parent.startBoxes.entrySet()){
				if(!allyBoxes.contains(s.getKey())){
					parent.debug(s.getKey()+" is an enemy startbox");
					for (MetalSpot ms:metalSpots){
						AIFloat3 pos = ms.position;
						if(s.getValue().contains(pos)){
							ms.enemyShadowed = true;
						}
					}
				}else{
					parent.debug(s.getKey()+" is an allied startbox");
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
				box = parent.getStartArea(enemy);
			
				if(box!=null){
					for (MetalSpot ms:metalSpots){
						AIFloat3 pos = ms.position;
						if(box.contains(pos)){
							ms.enemyShadowed = true;
						}
					}
				}
			}

			box = parent.getStartArea(parent.allyTeamID);
			for (MetalSpot ms:metalSpots){
				AIFloat3 pos = ms.position;
				if(box.contains(pos)){
					ms.allyShadowed = true;
				}
			}
		}

		for (MetalSpot ms: metalSpots){
			avgMexValue += ms.value / metalSpots.size();
		}
		for (MetalSpot ms: metalSpots){
			ms.weight = ms.value/avgMexValue;
		}
    }
    
	private void paintGraph(){
		
		int w = graphImage.getWidth();
		int h = graphImage.getHeight();
		
		graphGraphics.setBackground(new Color(0, 0, 0, 0));
		graphGraphics.clearRect(0,0, w,h);
		graphGraphics.setStroke(new BasicStroke(2f));
		
		Color spotOwned = new Color(0,255,0,255);
		Color spotUnowned = new Color(255, 247, 90,255);
		Color spotHostile = new Color(255,0,0,255);
		Color spotContested = new Color(239, 127, 11, 255);

		Color linkLinked = new Color(0,255,255,100);
		Color linkUnlinked = new Color(255,255,0,100);
		
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

		for (Link l:links){
			
			float phase = 0;
			if(l.connected){
				phase = parent.currentFrame/30;
			}

			final float[] dash = {10.0f};
			Color linkColor;
			graphGraphics.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, dash, phase));
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
	}
	
	private void paintCircleOutline(int x, int y, int r){
		graphGraphics.drawOval(x-r, y-r, 2*r, 2*r);
	}

	private void paintCircle(int x, int y, int r){
		graphGraphics.fillOval(x - r, y - r, 2 * r, 2 * r);
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
			if(ms.hostile || ms.enemyShadowed) spots.add(ms);
		}
		return spots;
	}

	public List<MetalSpot> getAllyTerritory(){
		// returns all metal spots not owned by allies.
		List<MetalSpot> spots = new ArrayList<MetalSpot>();
		for(MetalSpot ms:metalSpots){
			if(ms.owned || ms.allyShadowed) spots.add(ms);
		}
		return spots;
	}

	private void calcAllyCenter(){
		List<MetalSpot> spots = new ArrayList<MetalSpot>();
		for(MetalSpot ms:metalSpots){
			if(ms.owned || ms.allyShadowed) spots.add(ms);
		}

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
			allyCenter = null;
		}
	}

	public AIFloat3 getAllyCenter(){
		return allyCenter;
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
    			float dist = groundDistance(position,ms.position); 
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
				float dist = groundDistance(position,ms.position);
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

	public AIFloat3 getNearestUnconnectedLink(AIFloat3 position){
		AIFloat3 closest = null;
		float distance = Float.MAX_VALUE;
		for (Link l:links){
			if (!l.connected && l.length < 1500){
				float dist = groundDistance(position, l.getPos());
				if (dist < distance){
					distance = dist;
					closest = l.getPos();
				}
			}
		}
		return closest;
	}

	public AIFloat3 getNearestPylonSpot(AIFloat3 position){
		AIFloat3 closest = null;
		float distance = Float.MAX_VALUE;
		for (Link l:links){
			if (!l.connected && l.length > 900 && l.length < 1500){
				float dist = groundDistance(position, l.getPos());
				if (dist < distance){
					distance = dist;
					closest = l.getPos();
				}
			}
		}
		return closest;
	}
    
    public AIFloat3 getOverdriveSweetSpot(AIFloat3 position){
    	float radius = 190;
		float minWeight = Float.MAX_VALUE;
    	Link link = null;
    	for(Link l:links){
    		if(!l.connected){
    			float combinedValue = (float) (l.v0.value+l.v1.value+Math.sqrt(l.pylons.size())+0.001f);
    			float combinedCost = (float) (l.length + Math.pow(GraphManager.groundDistance(l.centerPos, position),2));
	    		float weight = combinedCost/combinedValue;
	    		if (weight < minWeight){
	    			link = l;
	    			minWeight = weight;
	    		}
    		}
    	}
    	if(link != null){
    		Pylon p = link.getConnectionHead();
    		
    		if(p != null){
				float dx=link.v1.position.x - p.position.x;
				float dz=link.v1.position.z - p.position.z;
				
				double d = Math.sqrt(dx*dx+dz*dz);
				float vx = (float) (dx/d);
				float vz = (float) (dz/d);
				
				float x = p.position.x + vx*radius;
				float z = p.position.z + vz*radius;
				AIFloat3 newpos = new AIFloat3(x,p.position.y,z);
				return newpos;
    		}
    	}	
    	
    	// if no unconnected links nearby, just pick a metal spot
    	minWeight = Float.MAX_VALUE;
    	MetalSpot spot = null;
    	for(MetalSpot ms:metalSpots){
    		if(ms.owned){
	    		float weight = (groundDistance(ms.position, position));
	    		weight += weight*Math.sqrt(ms.getPylonCount());
	    		if (weight < minWeight){
	    			spot = ms;
	    			minWeight = weight;
	    		}
    		}
    	}
    	if(spot != null){
			return spot.position;
    	}else{
    		return position;
    	}	
    }

    public BufferedImage getGraphImage(){
    	return this.graphImage;
    }
    
	public void setLosManager(LosManager losManager) {
		this.losManager = losManager;
	}
}
