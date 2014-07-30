package zkgbai.military;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Map;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import com.springrts.ai.oo.clb.WeaponDef;

import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
import zkgbai.graph.GraphManager;
import zkgbai.graph.MetalSpot;
import zkgbai.los.LosManager;

public class MilitaryManager extends Module {
	
	ZKGraphBasedAI parent;
	GraphManager graphManager;
	
	java.util.Map<Integer,Enemy> targets;
	HashSet<Unit> soldiers;
	List<Unit> cowardUnits;
	List<Unit> retreatingUnits;
	List<Unit> havens;
	List<Squad> squads;
	
	int maxUnitPower = 0;
	BufferedImage threatmap;
	Graphics2D threatGraphics;
	
	static AIFloat3 nullpos = new AIFloat3(0,0,0);
	
	int nano;

	private LosManager losManager;
	
	@Override
	public String getModuleName() {
		return "MilitaryManager";
	}
	
	public void setGraphManager(GraphManager gm){
		this.graphManager = gm;
	}
	
	public void setLosManager(LosManager losManager) {
		this.losManager = losManager;
	}
	
	public MilitaryManager(ZKGraphBasedAI parent){
		this.parent = parent;
		this.targets = new HashMap<Integer,Enemy>();
		this.soldiers = new HashSet<Unit>();
		this.squads = new ArrayList<Squad>();
		this.cowardUnits = new ArrayList<Unit>();
		this.retreatingUnits = new ArrayList<Unit>();
		this.havens = new ArrayList<Unit>();
		this.nano = parent.getCallback().getUnitDefByName("armnanotc").getUnitDefId();
		
		int width = parent.getCallback().getMap().getWidth();
		int height = parent.getCallback().getMap().getHeight();
		
		this.threatmap = new BufferedImage(width, height,BufferedImage.TYPE_INT_ARGB_PRE);
		this.threatGraphics = threatmap.createGraphics();
	}
	
	private void paintThreatMap(){
		
		int w = threatmap.getWidth();
		int h = threatmap.getHeight();
		
		threatGraphics.setBackground(new Color(255, 255, 255, 0));
        threatGraphics.clearRect(0,0, w,h);

		for(Enemy t:targets.values()){
			int effectivePower = (int) Math.min(255,t.danger / 10);
			int effectiveValue = (int) Math.min(255,(t.value / 3) +100);

			AIFloat3 position = t.position;

			threatGraphics.setColor(new Color(255, 0, 0, effectivePower)); //Red
			
			int x = (int) (position.x / 8);
			int y = (int) (position.z / 8);
			int r = (int) (t.threatRadius / 8);
			
			if(t.speed > 0){
				paintCircle(x,y,r);
				threatGraphics.setColor(new Color(255, 0, 0, effectivePower/4)); //Red
				paintCircle(x,y,(int) (r*(1.1)+r*t.speed));
			}else{
				paintCircle(x,y,r);
			}
			
			threatGraphics.setColor(new Color(0,0, 255, effectiveValue/4));
			paintCircle(x,y,2);
		}
		
		threatGraphics.setColor(new Color(0,255, 0, 255));
		List<Unit> units = parent.getCallback().getTeamUnits();
		for(Unit u:units){
			AIFloat3 position = u.getPos();
			int x = (int) (position.x / 8);
			int y = (int) (position.z / 8);
			paintCircle(x,y,2);
		}
	}
	
	private void paintCircle(int x, int y, int r){
		threatGraphics.fillOval(x-r, y-r, 2*r, 2*r);
	}
	
	public BufferedImage getThreatMap(){
		return this.threatmap;
	}
	
	public float getThreat(AIFloat3 position){
		int x = (int) (position.x/8);
		int y = (int) (position.z/8);
		Color c = new Color(threatmap.getRGB(x,y));
		return c.getRed();
	}
    
    public AIFloat3 getTarget(AIFloat3 origin){
    	List<MetalSpot> ms = graphManager.getEnemySpots();
    	AIFloat3 targetMex = null;
    	float bestMexScore = 0;
    	if(ms.size() > 0){
    		for (MetalSpot m:ms){
    			float score = m.getValue()*(parent.currentFrame - m.getLastSeen()) / (getThreat(m.getPosition()) * GraphManager.groundDistance(origin, m.getPosition()));
    			
        		Iterator<Enemy> enemies = targets.values().iterator();
        		while(enemies.hasNext()){
        			Enemy e = enemies.next();

        			if(GraphManager.groundDistance(e.position, origin)<600 && e.danger == 0){
        				score += e.value;
        			}
        		}
    			
    			if (score > bestMexScore){
    				targetMex = m.getPosition();
    				bestMexScore = score;
    			}
    		}
    	}
    	AIFloat3 enemyTarget = null;
    	float bestEnemyScore = 0;
    	if(targets.size() > 0){
    		bestEnemyScore = 0;
    		Iterator<Enemy> enemies = targets.values().iterator();
    		while(enemies.hasNext()){
    			Enemy e = enemies.next();
    			float score = e.value;
    			if(e.danger == 0){
    				score /= e.danger;
    			}
    			
    			if(!e.visible){
    				score /= Math.sqrt(parent.currentFrame - e.lastSeen);
    			}
    			
    			score /= Math.sqrt(GraphManager.groundDistance(e.position, origin));
    			if(score>bestEnemyScore){
    				bestEnemyScore = score;
    				enemyTarget = e.position;
    			}
    		}
    	}
    	
    	if(targetMex != null){
    		if(enemyTarget== null){
    			return targetMex;
    		}else{
    			if (bestMexScore > bestEnemyScore){
    				return targetMex;
    			}else{
    				return enemyTarget;
    			}
    		}
    	}else{
    		if(enemyTarget!= null){
    			return enemyTarget;
    		}
    	}
    	
    	ms = graphManager.getNeutralSpots();
    	if(!ms.isEmpty()){
    		AIFloat3 target = ms.get((int) Math.floor(Math.random()*ms.size())).getPosition();
        	return target;
    	}
    	
    	float x = (float) (parent.getCallback().getMap().getWidth()*8 * Math.random());
    	float z = (float) (parent.getCallback().getMap().getWidth()*8 * Math.random());
    	
    	AIFloat3 target = new AIFloat3(x,0,z);
    	
    	return target;
    }
    
    @Override
    public int update(int frame) {
    	if(frame%30 == 0){
    		
    		for(Enemy t:targets.values()){
    			AIFloat3 tpos = t.unit.getPos();
    			if(tpos != null && !tpos.equals(nullpos)){
    				t.position = tpos;
    			}
    		}
    		
    		for(Unit s:soldiers){
    			if(s.getCurrentCommands().isEmpty() && (!(s.getMaxHealth() > 1000 && s.getHealth()/s.getMaxHealth() < 0.8))){
    				retreatingUnits.remove(s);
    				AIFloat3 t = getTarget(s.getPos());
    				if(t!=null){
    					s.fight(getTarget(s.getPos()), (short)0, frame+5);
    				}
    			}
    		}
    		
    		ArrayList<Integer> deadEnemies = new ArrayList<Integer>();
    		for(Enemy e:targets.values()){
    			if(!e.visible){
    				if(e.isStatic){
    					if (losManager.isInLos(e.position,1) && frame-e.lastSeen>300){
    						deadEnemies.add(e.unitID);
    					}
    				}else{ 
	    				if(frame - e.lastSeen > 5000){
	    					deadEnemies.add(e.unitID);
	    				}
    				}
    			}
    		}
    		for(int i:deadEnemies){
    			targets.remove(i);
    		}
    		paintThreatMap();	
    	}
        return 0; // signaling: OK
    }
	
    @Override
    public int enemyEnterLOS(Unit enemy) {
    	if(targets.containsKey(enemy.getUnitId())){
    		targets.get(enemy.getUnitId()).visible = true;
    	}else{
    		Enemy e = new Enemy(enemy, enemy.getDef().getCost(parent.getCallback().getResourceByName("Metal")));
    		targets.put(enemy.getUnitId(),e);
    		e.visible = true;
    	}
    	
        return 0; // signaling: OK
    }

    @Override
    public int enemyLeaveLOS(Unit enemy) {
    	if(targets.containsKey(enemy.getUnitId())){
			targets.get(enemy.getUnitId()).visible = false;
    	}	
        return 0; // signaling: OK
    }

    @Override
    public int enemyEnterRadar(Unit enemy) {
    	if(targets.containsKey(enemy.getUnitId())){
    		targets.get(enemy.getUnitId()).visible = true;
    	}else{
    		if(enemy.getDef() != null){
	    		Enemy e = new Enemy(enemy, enemy.getDef().getCost(parent.getCallback().getResourceByName("Metal")));
	    		targets.put(enemy.getUnitId(),e);
	    		e.visible = true;
    		}
    	}
    	
        return 0; // signaling: OK
    }

    @Override
    public int enemyLeaveRadar(Unit enemy) {
    	if(targets.containsKey(enemy.getUnitId())){
			targets.get(enemy.getUnitId()).visible = false;
    	}
        return 0; // signaling: OK
    }
    
    @Override
    public int enemyDestroyed(Unit unit, Unit attacker) {  
        if(targets.containsKey(unit.getUnitId())){
        	targets.remove(unit.getUnitId());
        }	    
        return 0; // signaling: OK
    }
    
    @Override
    public int unitFinished(Unit unit) {      
    	
    	if(unit.getDef().getUnitDefId() == nano){
    		havens.add(unit);
    	}
    	
    	if(unit.getMaxRange()>0 && unit.getMaxSpeed() > 0 && unit.getDef().getBuildOptions().isEmpty()){
    		soldiers.add(unit);
    		unit.setFireState(3, (short)0, parent.currentFrame);
    		unit.setMoveState(3, (short)0, parent.currentFrame);
    	}
    	
    	if(unit.getMaxSpeed()>0 && (unit.getDef().getBuildOptions().size() > 0 || unit.getMaxHealth() > 1000)){
    		cowardUnits.add(unit);
    	}
        return 0; // signaling: OK
    }
    
    @Override
    public int unitDestroyed(Unit unit, Unit attacker) {  
        soldiers.remove(unit);
        cowardUnits.remove(unit);
        havens.remove(unit);
        return 0; // signaling: OK
    }
    
    @Override
    public int unitDamaged(Unit h, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
    	if(cowardUnits.contains(h) || h.getDef().isBuilder()){
			if(h.getHealth()/h.getMaxHealth() < 0.6 || h.getDef().isBuilder()){
				if(!retreatingUnits.contains(h)){
    				retreatingUnits.add(h);
	            	float distance = Float.MAX_VALUE;
	            	AIFloat3 position = null;
	            	for(Unit thing:havens){
	            		if(thing.getDef().getUnitDefId() == nano){
	            			AIFloat3 pos = thing.getPos();
	            			float dist = GraphManager.groundDistance(pos, h.getPos()); 
	            			if(dist < distance){
	            				distance = dist; 
	            				position = pos;
	            			}
	            		}
	            	}
	            	if(position != null){
	                	float buildDist = 200;
	                	double angle = Math.random()*2*Math.PI;
	                	
	                	double vx = Math.cos(angle);
	                	double vz = Math.sin(angle);
	                	
	                	position.x += buildDist*vx;
	                	position.z += buildDist*vz;
	            		
	            		h.moveTo(position, (short)0, parent.currentFrame);
	            	}
				}
			}
		}
		return 0;
    }
}
