package zkgbai.graph;

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
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;

import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
import zkgbai.los.LosManager;

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
	
	public HashMap<String, Integer> pylonDefs; 
	int pylonCounter;
	
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
	    			
	    			for(Pylon p:pylons){
		    			if(groundDistance(p.position,ms.position)<p.radius+50){
		    				p.addSpot(ms);
		    			}
	    			}
	    			
	    			for(Link l:ms.links){
	    				if(l.v0.owned && l.v1.owned){
	    					l.owned = true;
	    					if(l.pylons.size() > 0){
	    	    				if(l.checkConnected()){
	    	    					parent.drawLine(l.v0.position, l.v1.position);
	    	    				}
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
	    				parent.drawLine(m.position, p.position);
	    			}
	    		}
	    		
	    		for(Link l:links){
	    			if(groundDistance(p.position,l.centerPos) < l.length/2){
	    				for(Pylon lp:l.pylons){
	    					if(groundDistance(p.position,lp.position) < p.radius+lp.radius){
	    						lp.addNeighbour(p);
	    						p.addNeighbour(lp);
	    					}
	    				}
	    				
	    				l.addPylon(p);
	    				if(l.checkConnected()){
	    					parent.drawLine(l.v0.position, l.v1.position);
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
    	if(unit.getDef().getUnitDefId() == mexDefID){
    		AIFloat3 unitpos = unit.getPos();
	    	for(MetalSpot ms:metalSpots){
	    		if(!ms.hostile && groundDistance(unitpos,ms.getPosition()) < 50){
	    			ms.owned = false;
	    			ms.hostile = true;
	    			ms.setExtractor(unit);
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
	    			parent.marker(ms.position,"MexDestroyed");
	    		}
	    	}
    	} else if(!def.isBuilder() && def.getMakesResource(e)>0){
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
    		if(ms.hostile){
    			if(losManager.isInLos(ms.getPosition())){
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
    				}
    			}
    		}
    	}

		return 0;
	}

    public float groundDistance(AIFloat3 v0, AIFloat3 v1){
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
    
    public MetalSpot getSpotToColonize(AIFloat3 position){
    	// arbitrary weighting for now, but can be tuned later
    	float minWeight = Float.MAX_VALUE;
    	MetalSpot spot = null;
    	for(MetalSpot ms:metalSpots){
    		if(!ms.owned && !ms.hostile){
	    		float weight = groundDistance(ms.position, position)/(ms.value+0.001f);
	    		weight += weight*ms.getColonists().size();
	    		if (weight < minWeight){
	    			spot = ms;
	    			minWeight = weight;
	    		}
    		}
    	}
		return spot;	
    }
    
    public AIFloat3 getOverdriveSweetSpot(AIFloat3 position){
    	// if no unconnected links nearby, just pick a metal spot
    	float minWeight = Float.MAX_VALUE;
    	MetalSpot spot = null;
    	for(MetalSpot ms:metalSpots){
    		if(ms.owned){
	    		float weight = groundDistance(ms.position, position)/(ms.value+0.001f);
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
    
	public void setLosManager(LosManager losManager) {
		this.losManager = losManager;
	}
}
