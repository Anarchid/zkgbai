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
import java.util.Queue;
import java.util.Set;

import org.poly2tri.Poly2Tri;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;
import org.poly2tri.triangulation.point.TPoint;
import org.poly2tri.triangulation.sets.PointSet;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;
import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Map;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;

import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
import zkgbai.gui.DebugView;
import zkgbai.los.LosManager;
import zkgbai.military.Enemy;

public class GraphManager extends Module {
	private ZKGraphBasedAI parent;
	private ArrayList<MetalSpot> metalSpots;
	private ArrayList<Link> links;
	private ArrayList<Pylon> pylons;
    public int myTeamID;
    private UnitDef mexDef;
    int mexDefID;
	private LosManager losManager;
	Resource e;
	Resource m;
	
	BufferedImage threatMap;
	
	public HashMap<String, Integer> pylonDefs; 
	int pylonCounter;
	private BufferedImage graphImage;
	private Graphics2D graphGraphics;
	
	public GraphManager(ZKGraphBasedAI parent){
		this.metalSpots = new ArrayList<MetalSpot>();
		this.links = new ArrayList<Link>();
		this.pylons = new ArrayList<Pylon>();
		this.parent = parent;
		this.mexDef = parent.getCallback().getUnitDefByName("cormex");
		this.mexDefID = mexDef.getUnitDefId();
		this.m = parent.getCallback().getResourceByName("Metal");
		this.e = parent.getCallback().getResourceByName("Energy");
		
		// hardwired for now because of segfaults upon segfaults
		pylonDefs = new java.util.HashMap<String, Integer>();
		pylonDefs.put("armsolar", 100);
		
		int width = parent.getCallback().getMap().getWidth();
		int height = parent.getCallback().getMap().getHeight();
		
		this.graphImage = new BufferedImage(width, height,BufferedImage.TYPE_INT_ARGB_PRE);
		this.graphGraphics = graphImage.createGraphics();
	}
	
	@Override
	public String getModuleName() {
		return "GraphManager";
	}
	
    @Override
    public int luaMessage(java.lang.String inData){        
    	if(inData.startsWith("METAL_SPOTS:")){
    		String json = inData.substring(12);
			JsonParserFactory factory=JsonParserFactory.getInstance();
			JSONParser parser=factory.newJsonParser();
			ArrayList<HashMap> jsonData=(ArrayList)parser.parseJson(json).values().toArray()[0];
			initializeGraph(jsonData);
    		parent.debug("Parsed JSON metalmap with "+metalSpots.size()+" spots and "+links.size()+" links");
    		
			Set<Integer> enemies = parent.getEnemyAllyTeamIDs();
			for(int enemy:enemies){
				float[] box = parent.getEnemyBox(enemy);
				if(box!=null){
					
		 	         // 0 -> bottom
		 	         // 1 -> left
		 	         // 2 -> right
		 	         // 3 -> top
					for (MetalSpot ms:metalSpots){
						AIFloat3 pos = ms.position;
						if(pos.z > box[3] && pos.z < box[0] && pos.x>box[1] && pos.x<box[2]){
							ms.hostile = true;
							ms.setShadowCaptured(true);
							for(Link l:ms.links){
								l.contested = true;
							}
						}
					}
				}
			}
    	}
    	return 0; //signaling: OK
    }
    
    @Override
    public int unitFinished(Unit unit) {
    	UnitDef def = unit.getDef();
    	if(def.getUnitDefId() == mexDefID){
    		AIFloat3 unitpos = unit.getPos();
	    	for(MetalSpot ms:metalSpots){
	    		if(groundDistance(unitpos,ms.getPosition())<50){
	    			ms.owned = true;
	    			ms.hostile = false;
	    			ms.setExtractor(unit);
	    			ms.setShadowCaptured(false);
	    			ms.setShadowInfluence(0);

	    			for(Pylon p:pylons){
		    			if(groundDistance(p.position,ms.position)<p.radius+50){
		    				p.addSpot(ms);
		    			}
	    			}
	    			
	    			for(Link l:ms.links){
	    				if(l.v0.owned && l.v1.owned){
	    					l.owned = true;
	    					if(l.pylons.size() > 0){
	    						l.checkConnected();
	    					}
	    				}else{
	    					l.owned = false;
	    				}
	    			}
	    		}
	    	}
    	} else{
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
    	}
    	return 0;
    }
    
    @Override
    public int enemyEnterLOS(Unit unit) {
    	UnitDef def = unit.getDef();
    	
    	if(def != null){
	    	if(def.getUnitDefId() == mexDefID){
	    		AIFloat3 unitpos = unit.getPos();
		    	for(MetalSpot ms:metalSpots){
		    		if(!ms.hostile && GraphManager.groundDistance(unitpos,ms.getPosition()) < 50){
		    			ms.owned = false;
		    			ms.hostile = true;
		    			ms.setExtractor(unit);
		    			ms.setShadowCaptured(false);
		    			ms.setShadowInfluence(0);
		    			
		    			for(Link l:ms.links){
		    				l.contested = true;
		    			}
		    		}
		    	}
	    	}
    	}
        return 0; // signaling: OK
    }
    
    public int unitDestroyed(Unit unit, Unit attacker) {
    	UnitDef def = unit.getDef();
    	if(def.getUnitDefId() == mexDefID){
	    	for(MetalSpot ms:metalSpots){
	    		if(ms.extractor != null && ms.extractor.getUnitId() == unit.getUnitId()){
	    			ms.owned = false;
	    			ms.hostile = false;

	    			ms.colonizers.clear();
	    			for(Link link:ms.links){
	    				link.owned = false;
	    			}
	    			
	    			ms.setExtractor(null);
	    		}
	    	}
    	} else if(!def.isBuilder() && def.getName().equals("armsolar")){
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
    public int enemyDestroyed(Unit unit, Unit attacker) {  
    	if(unit.getDef().getUnitDefId() == mexDefID){
	    	for(MetalSpot ms:metalSpots){
	    		if(ms.extractor != null && ms.extractor == unit){
	    			ms.owned = false;
	    			ms.hostile = false;
	    			ms.setExtractor(null);
	    			ms.setShadowCaptured(false);
	    			ms.setShadowInfluence(0);
	    			for(Link l:ms.links){
	    				if(!l.v0.hostile && !l.v1.hostile){
	    					l.contested = false;
	    				}
	    			}
	    		}
	    	}
    	}
        return 0; // signaling: OK
    }
    
    // TODO: move los stuff to a separate handler
	@Override
	public int update(int frame){
		if (frame % 60 > 0){
			return 0;
		}

    	for(MetalSpot ms:metalSpots){
    		if(losManager.isInLos(ms.getPosition())){
				ms.lastSeen = frame;
				ms.visible = true;
    			
    			if(ms.hostile){
    				boolean hasMex = false;
    				List<Unit> hostiles = parent.getCallback().getEnemyUnitsIn(ms.getPosition(), 50f);
    				for(Unit hostile:hostiles){
    					if(hostile.getDef().getUnitDefId() == mexDefID){
    						hasMex = true;
    						break;
    					}
    				}
    				if (!hasMex){
    	    			ms.owned = false;
    	    			ms.hostile = false;
    	    			ms.setExtractor(null);
    	    			ms.setShadowCaptured(false);
    	    			ms.setShadowInfluence(0);
    	    			for(Link l:ms.links){
    	    				if(!l.v0.hostile && !l.v1.hostile){
    	    					l.contested = false;
    	    				}
    	    			}
    				}
    			}
    		}else{
    			if(ms.visible){
    				ms.visible = false;
    			}
    		}
    		
    		if(!ms.hostile && !ms.owned){
				boolean hasMex = false;
				Unit extractor = null;
				List<Unit> allies = parent.getCallback().getFriendlyUnitsIn(ms.getPosition(), 50f);
				for(Unit ally:allies){
					if(ally.getDef().getUnitDefId() == mexDefID){
						hasMex = true;
						extractor = ally;
						break;
					}
				}
				if (hasMex){
	    			ms.owned = true;
	    			ms.hostile = false;
	    			ms.setExtractor(extractor);
	    			ms.setShadowCaptured(false);
	    			ms.setShadowInfluence(0);
	    			for(Link l:ms.links){
	    				if(!l.v0.hostile && !l.v1.hostile){
	    					l.contested = false;
	    				}
	    			}
				}
    		}
    		
    	}
    	
    	for(MetalSpot ms:metalSpots){
    		if(ms.hostile){
    			for(Link l:ms.links){
    				if(!l.v0.hostile){
    					l.v0.shadowInfluence += l.weight;
    					if(l.v0.shadowInfluence > 100){
    						l.v0.hostile = true;
    						l.v0.isShadowCaptured = true;
    					}
    				}
    				if(!l.v1.hostile){
    					l.v0.shadowInfluence += l.weight;
    					if(l.v0.shadowInfluence > 100){
    						l.v0.hostile = true;
    						l.v0.isShadowCaptured = true;
    					}
    				}
    			}
    		}
    	}

    	
    	paintGraph();

		return 0;
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
    }
    
	private void paintGraph(){
		
		int w = graphImage.getWidth();
		int h = graphImage.getHeight();
		
		graphGraphics.setBackground(new Color(255, 255, 255, 0));
		graphGraphics.clearRect(0,0, w,h);
		graphGraphics.setStroke(new BasicStroke(2f));
		
		Color spotOwned = new Color(0,255,0,255);
		Color spotLinked = new Color(0,255,255,255);
		Color spotUnowned = new Color(255,255,0,255);
		Color spotHostile = new Color(255,0,0,255);
		
		
		Color linkOwned = new Color(0,255,0,100);
		Color linkLinked = new Color(0,255,255,100);
		Color linkUnowned = new Color(255,255,0,100);
		Color linkHostile = new Color(255,120,0,100);
		
		for(MetalSpot ms:metalSpots){
			AIFloat3 position = ms.position;
			
			int x = (int) (position.x / 8);
			int y = (int) (position.z / 8);
			
			if(ms.owned){
				graphGraphics.setColor(spotOwned);
				paintCircle(x,y,4);
				if(ms.connected){
					graphGraphics.setColor(spotLinked);
					paintCircle(x,y,6);
				}		
			}else{
				graphGraphics.setColor(spotUnowned);
				paintCircle(x,y,4);

				if(ms.hostile){
					graphGraphics.setColor(spotHostile);
					paintCircle(x,y,6);
				}
			}
		}
		final float[] dash = {10.0f};
	
		Color linkColor = null;
		for (Link l:links){
			
			float phase = 0;
			if(l.contested){
				phase = parent.currentFrame/30;
			}
			
			graphGraphics.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, dash, phase));
			if(l.connected){
				linkColor = linkLinked;
			}else if(l.owned){
				linkColor = linkOwned;
			}else if (l.contested){
				linkColor = linkHostile;
			}else{
				linkColor = linkUnowned;
			}
			
			graphGraphics.setColor(linkColor);

			int x1 = (int) (l.v0.position.x / 8);
			int y1 = (int) (l.v0.position.z / 8);
			
			int x2 = (int) (l.v1.position.x / 8);
			int y2 = (int) (l.v1.position.z / 8);		
			
			graphGraphics.drawLine(x1, y1, x2, y2);
		}
	}
	
	private void paintCircle(int x, int y, int r){
		graphGraphics.drawOval(x-r, y-r, 2*r, 2*r);
	}
    
    public List<MetalSpot> getEnemySpots(){
    	ArrayList<MetalSpot> spots = new ArrayList<MetalSpot>();
    	for(MetalSpot ms:metalSpots){
    		if(ms.hostile) spots.add(ms);
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
    
    public MetalSpot getClosestMetalSpot(AIFloat3 position){
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
    
    public AIFloat3 getOverdriveSweetSpot(AIFloat3 position){
    	float minWeight = Float.MAX_VALUE;  	
    	Link link = null;
    	for(Link l:links){
    		if(l.owned && !l.connected){
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
				
				float x = p.position.x + vx*95;
				float z = p.position.z + vz*95;
				AIFloat3 newpos = new AIFloat3(x,p.position.y,z);
				return newpos;
    		}
    	}	
    	
    	// if no unconnected links nearby, just pick a metal spot
    	minWeight = Float.MAX_VALUE;
    	MetalSpot spot = null;
    	for(MetalSpot ms:metalSpots){
    		if(ms.owned){
	    		float weight = (float) (groundDistance(ms.position, position));
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
