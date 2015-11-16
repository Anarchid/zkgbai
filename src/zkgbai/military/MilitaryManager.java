package zkgbai.military;

import java.awt.BasicStroke;
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
import com.springrts.ai.oo.clb.*;

import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.EconomyManager;
import zkgbai.graph.GraphManager;
import zkgbai.graph.MetalSpot;
import zkgbai.los.LosManager;
import zkgbai.military.tasks.FighterTask;
import zkgbai.military.tasks.RaidTask;
import zkgbai.military.tasks.ScoutTask;

public class MilitaryManager extends Module {
	
	ZKGraphBasedAI parent;
	GraphManager graphManager;
	
	java.util.Map<Integer,Enemy> targets;
	HashSet<Unit> soldiers;
	java.util.Map<Integer, Fighter> fighters;
	List<Unit> cowardUnits;
	List<Unit> retreatingUnits;
	List<Unit> havens;
	Squad nextSquad;
	List<Squad> squads;
	public List<Raider> raiders;
	List<Strider> striders;

	List<ScoutTask> scoutTasks;
	List<RaidTask> raidTasks;

	RadarIdentifier radarID;
	
	int maxUnitPower = 0;
	BufferedImage threatmap;
	Graphics2D threatGraphics;
	ArrayList<TargetMarker> targetMarkers;
	
	static AIFloat3 nullpos = new AIFloat3(0,0,0);
	
	int nano;

	private LosManager losManager;
	private EconomyManager ecoManager;
	private OOAICallback callback;
	private UnitClasses unitTypes;

	private Resource m;

	int frame = 0;

	static int CMD_DONT_FIRE_AT_RADAR = 38372;
	
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

	public void setEcoManager(EconomyManager ecoManager){this.ecoManager = ecoManager;}
	
	public MilitaryManager(ZKGraphBasedAI parent){
		this.parent = parent;
		this.callback = parent.getCallback();
		this.targets = new HashMap<Integer,Enemy>();
		this.fighters = new HashMap<Integer, Fighter>();
		this.soldiers = new HashSet<Unit>();
		this.raiders = new ArrayList<Raider>();
		this.striders = new ArrayList<Strider>();
		this.squads = new ArrayList<Squad>();
		this.nextSquad = null;
		this.cowardUnits = new ArrayList<Unit>();
		this.retreatingUnits = new ArrayList<Unit>();
		this.havens = new ArrayList<Unit>();
		this.scoutTasks = new ArrayList<ScoutTask>();
		this.raidTasks = new ArrayList<RaidTask>();
		this.nano = parent.getCallback().getUnitDefByName("armnanotc").getUnitDefId();
		this.unitTypes = new UnitClasses();
		this.m = callback.getResourceByName("Metal");
		
		targetMarkers = new ArrayList<TargetMarker>();
		int width = parent.getCallback().getMap().getWidth();
		int height = parent.getCallback().getMap().getHeight();
		
		this.threatmap = new BufferedImage(width, height,BufferedImage.TYPE_INT_ARGB_PRE);
		this.threatGraphics = threatmap.createGraphics();
		
		try{
			radarID = new RadarIdentifier(parent.getCallback());
		}catch(Exception e){
			parent.printException(e);;
		}
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
			
			threatGraphics.setColor(new Color(0,0, 255, Math.max(255,100+effectiveValue/4)));
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

		final float[] dash = {5.0f};

		float phase = 0;
		ArrayList<TargetMarker> deadMarkers = new ArrayList<TargetMarker>();
		for(TargetMarker tm:targetMarkers){
			int age = parent.currentFrame - tm.frame; 
			if(age < 255){
				phase = age;
				threatGraphics.setColor(new Color(0,255,255, 255-age));
				threatGraphics.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5f, dash, phase));
				AIFloat3 position = tm.position;				
				int x = (int) (position.x / 8);
				int y = (int) (position.z / 8);
				
				paintHollowCircle(x,y,5+age/50);

			}else{
				deadMarkers.add(tm);
			}
		}
		
		for(TargetMarker tm:deadMarkers){
			targetMarkers.remove(tm);
		}
		
	}
	
	private void paintCircle(int x, int y, int r){
		threatGraphics.fillOval(x-r, y-r, 2*r, 2*r);
	}
	
	private void paintHollowCircle(int x, int y, int r){
		threatGraphics.drawOval(x-r, y-r, 2*r, 2*r);
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

	private void createScoutTasks(){
		List<MetalSpot> unscouted = graphManager.getUnownedSpots();
		for (MetalSpot ms: unscouted){
			ScoutTask st = new ScoutTask(ms.getPos(), ms);
			if (!scoutTasks.contains(st) && (frame - ms.getLastSeen() > 240 || ms.getLastSeen() == 0)){
				scoutTasks.add(st);
			}
		}
	}

	private void checkScoutTasks(){
		List<ScoutTask> finished = new ArrayList<ScoutTask>();
		for (ScoutTask st: scoutTasks){
			if (st.spot.visible){
				st.endTask();
				finished.add(st);
			}
		}
		scoutTasks.removeAll(finished);
	}

	private void createRaidTasks(){
		for (Enemy e:targets.values()){
			if (e.isStatic && e.threatRadius == 0 && getThreat(e.position) < 0.5){
				RaidTask rt = new RaidTask(e.position);
				if (!raidTasks.contains(rt)){
					raidTasks.add(rt);
				}
			}
		}
	}

	private void checkRaidTasks(){
		List<RaidTask> finished = new ArrayList<RaidTask>();
		for (RaidTask rt: raidTasks){
			if (losManager.isInLos(rt.target)){
				List<Unit> enemies = callback.getEnemyUnitsIn(rt.target, 200f);
				if (enemies.size() == 0 || getThreat(rt.target) > 0.5){
					rt.endTask();
					finished.add(rt);
				}
			}
		}
		raidTasks.removeAll(finished);
	}

	private void assignRaiders(){
		for (Raider r: raiders){
			FighterTask bestTask = null;
			float cost = Float.MAX_VALUE;

			if (r.getTask() != null){
				bestTask = r.getTask();
				if (r.getTask() instanceof ScoutTask){
					ScoutTask st = (ScoutTask) r.getTask();
					cost = getScoutCost(st, r);
				}
				if (r.getTask() instanceof RaidTask){
					RaidTask rt = (RaidTask) r.getTask();
					cost = getRaidCost(rt, r);
				}
			}

			for (ScoutTask s:scoutTasks){
				float tmpcost = getScoutCost(s, r);
				if (tmpcost < cost){
					cost = tmpcost;
					bestTask = s;
				}
			}
			for (RaidTask rt:raidTasks){
				float tmpcost = getRaidCost(rt, r);
				if (tmpcost < cost){
					cost = tmpcost;
					bestTask = rt;
				}
			}

			if (bestTask != null && (!bestTask.equals(r.getTask()) || r.getUnit().getCurrentCommands().size() == 0)){
				if (bestTask instanceof ScoutTask){
					r.fightTo(bestTask.target, frame);
					r.setTask(bestTask);
					((ScoutTask) bestTask).addRaider(r);
				}
				if (bestTask instanceof RaidTask){
					r.fightTo(bestTask.target, frame);
					r.setTask(bestTask);
					((RaidTask) bestTask).addRaider(r);
				}
			}
		}
	}

	private float getScoutCost(ScoutTask task, Raider raider){
		float cost = graphManager.groundDistance(task.target, raider.getPos());
		// reduce cost relative to every 15 seconds since last seen
		cost = cost/(1+((frame - task.spot.getLastSeen())/900));
		if (task.spot.enemyShadowed || task.spot.hostile){
			cost /= 4;
		}
		return cost;
	}

	private float getRaidCost(RaidTask task, Raider raider){
		float cost = graphManager.groundDistance(task.target, raider.getPos());
		cost /= 8;
		return cost;
	}

	private void addToSquad(Fighter f){
		// create a new squad if there isn't one
		if (nextSquad == null){
			nextSquad = new Squad();
			nextSquad.setTarget(getRallyPoint(f.getPos()), frame);
			nextSquad.income = ecoManager.effectiveIncome;
		}

		nextSquad.addUnit(f, frame);

		if (nextSquad.metalValue > nextSquad.income * 60){
			nextSquad.status = 'r';
			squads.add(nextSquad);
			nextSquad = null;
		}
	}

	private void updateSquads(){
		List<Squad> deadSquads = new ArrayList<Squad>();
		for (Squad s: squads){
			if (s.status == 'r'){
				if (s.isRallied(frame)){
					s.status = 'a';
				}
			}else{
				s.setTarget(getTarget(s.getPos()), frame);
				if (s.isDead()){
					deadSquads.add(s);
				}
			}
		}
		squads.removeAll(deadSquads);
	}
    
    private AIFloat3 getTarget(AIFloat3 origin){
    	List<MetalSpot> ms = graphManager.getEnemySpots();
    	AIFloat3 target = null;
    	float cost = Float.MAX_VALUE;
    	if(ms.size() > 0){
    		for (MetalSpot m:ms){
    			float tmpcost = GraphManager.groundDistance(m.getPos(), origin);
				tmpcost *= 1+getThreat(m.getPos());
    			tmpcost /= (frame-m.getLastSeen())/600;
				if (m.allyShadowed) {
					tmpcost /= 4;
				}
    			
    			if (tmpcost < cost){
    				target = m.getPos();
    				cost = tmpcost;
    			}
    		}
    	}

    	if(targets.size() > 0){
    		Iterator<Enemy> enemies = targets.values().iterator();
    		while(enemies.hasNext()){
    			Enemy e = enemies.next();
    			float tmpcost = graphManager.groundDistance(origin, e.position);
				tmpcost /= getThreat(e.position);
				tmpcost /= e.value/250;
    			
    			if(tmpcost < cost){
    				cost = tmpcost;
    				target = e.position;
    			}
    		}
    	}
    	
    	if (target != null){
			TargetMarker tm = new TargetMarker(target, frame);
			targetMarkers.add(tm);
			return target;
		}

		// if no targets are available
    	ms = graphManager.getUnownedSpots();
    	for (MetalSpot m:ms){
			if (m.enemyShadowed){
				float tmpcost = GraphManager.groundDistance(m.getPos(), origin);
				tmpcost /= 1+getThreat(m.getPos());
				tmpcost /= (frame-m.getLastSeen())/600;

				if (tmpcost < cost) {
					target = m.getPos();
					cost = tmpcost;
				}
			}
		}
		return target;
    }

	private void dgunStriders(){
		for (Strider s:striders){
			List<Unit> enemies = callback.getEnemyUnitsIn(s.getPos(), 450f);
			if (enemies.size() > 0 && frame > s.lastDgunFrame + s.dgunReload){
				s.getUnit().dGunPosition(enemies.get(0).getPos(), (short) 0, frame + 300);
				s.lastDgunFrame = frame;
			}
		}
	}

	private AIFloat3 getRallyPoint(AIFloat3 pos){
		List<MetalSpot> mexes = graphManager.getOwnedSpots();
		AIFloat3 bestMex = null;
		float bestScore = 0;

		for (MetalSpot ms: mexes){
			float score = graphManager.groundDistance(pos, ms.getPos());
			score *= 2*(1+getThreat(ms.getPos()));
			if (score > bestScore){
				bestScore = score;
				bestMex = ms.getPos();
			}
		}
		return bestMex;
	}

	private void retreatCowards(){
		for (Unit u: retreatingUnits){
			float distance = Float.MAX_VALUE;
			AIFloat3 position = null;
			for(Unit thing:havens){
				if(thing.getDef().getUnitDefId() == nano){
					AIFloat3 pos = thing.getPos();
					float dist = GraphManager.groundDistance(pos, u.getPos());
					if(dist < distance){
						distance = dist;
						position = pos;
					}
				}
			}
			if(position != null){
				UnitDef building = callback.getUnitDefByName("factorygunship");
				position = callback.getMap().findClosestBuildSite(building, position, 600f, 3, 0);
				u.moveTo(position, (short)0, frame+300);
			}
		}
	}
    
    @Override
    public int update(int frame) {
    	this.frame = frame;

		if(frame%15 == 0) {
			createRaidTasks();
			createScoutTasks();

			checkRaidTasks();
			checkScoutTasks();

			assignRaiders();
			updateSquads();

			for (Enemy t : targets.values()) {
				AIFloat3 tpos = t.unit.getPos();
				if (tpos != null && !tpos.equals(nullpos)) {
					if (!t.getIdentified()) {

						float speed = GraphManager.groundDistance(t.position, tpos) / 30;

						if (speed > t.maxObservedSpeed) {
							RadarDef rd = radarID.getDefBySpeed(speed);
							t.maxObservedSpeed = speed;
							if (rd != null) {
								t.danger = rd.getDanger();
								t.speed = speed;
								t.threatRadius = rd.getRange();
								t.value = rd.getValue();
							}
						}
					}
					t.position = tpos;
				}
			}

			retreatCowards();

			Iterator<Fighter> iter = fighters.values().iterator();
			while (iter.hasNext()) {
				Fighter f = iter.next();
				Unit u = f.getUnit();
				if (retreatingUnits.contains(u) && u.getHealth() / u.getMaxHealth() > 0.95) {
					retreatingUnits.remove(u);
					addToSquad(f);
				}
			}

			dgunStriders();

			ArrayList<Integer> deadEnemies = new ArrayList<Integer>();
			for (Enemy e : targets.values()) {
				if (!e.visible) {
					if (e.isStatic) {
						if (losManager.isInLos(e.position, 1) && frame - e.lastSeen > 300) {
							deadEnemies.add(e.unitID);
						}
					} else {
						if (losManager.isInLos(e.position, 1) && frame - e.lastSeen > 300) {
							deadEnemies.add(e.unitID);
						}
					}
				}
			}
			for (int i : deadEnemies) {
				targets.remove(i);
			}
			paintThreatMap();
		}
        return 0; // signaling: OK
    }
	
    @Override
    public int enemyEnterLOS(Unit enemy) {
    	Resource metal = parent.getCallback().getResourceByName("Metal");
    	if(targets.containsKey(enemy.getUnitId())){
    		Enemy e = targets.get(enemy.getUnitId()); 
    		e.visible = true;
    		e.setIdentified();
    		e.updateFromUnitDef(enemy.getDef(), enemy.getDef().getCost(metal));
    	}else{
    		Enemy e = new Enemy(enemy, enemy.getDef().getCost(metal));
    		targets.put(enemy.getUnitId(),e);
    		e.visible = true;
    		e.setIdentified();
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
    			e.isRadarVisible = true;
    		}else{
    			Enemy e = new Enemy(enemy, 50);
        		targets.put(enemy.getUnitId(),e);
    			e.isRadarVisible = true;
    		}
    	}
    	
        return 0; // signaling: OK
    }

    @Override
    public int enemyLeaveRadar(Unit enemy) {
    	if(targets.containsKey(enemy.getUnitId())){
			targets.get(enemy.getUnitId()).isRadarVisible = false;
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

		String defName = unit.getDef().getName();

		// enable snipers to shoot radar dots
		if (defName.equals("armsnipe")){
			ArrayList<Float> params = new ArrayList<>();
			params.add((float) 0);
			unit.executeCustomCommand(CMD_DONT_FIRE_AT_RADAR, params, (short) 0, frame+60);
		}

		if (unitTypes.striders.contains(defName)){
			Strider st = new Strider(unit, unit.getDef().getCost(m));
			striders.add(st);
			fighters.put(st.id ,st);
			addToSquad(st);
		}else if (unitTypes.raiders.contains(defName)) {
			Raider r = new Raider(unit, unit.getDef().getCost(m));
			raiders.add(r);
			unit.setMoveState(2, (short) 0, frame + 10);
		}else if (unitTypes.assaults.contains(defName)){
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			fighters.put(f.id, f);
			addToSquad(f);
		}else if(unit.getDef().isAbleToAttack() && unit.getMaxSpeed() > 0 && unit.getDef().getBuildOptions().isEmpty()){
    		soldiers.add(unit);
    		unit.setFireState(3, (short)0, parent.currentFrame);
    		unit.setMoveState(3, (short)0, parent.currentFrame);
    	}
    	
    	if(unit.getMaxHealth() > 3000 && unit.getDef().getBuildOptions().size() == 0){
    		cowardUnits.add(unit);
    	}
        return 0; // signaling: OK
    }
    
    @Override
    public int unitDestroyed(Unit unit, Unit attacker) {  
        soldiers.remove(unit);
        cowardUnits.remove(unit);
        havens.remove(unit);
		retreatingUnits.remove(unit);

		if (fighters.containsKey(unit.getUnitId())){
			Fighter f = fighters.get(unit.getUnitId());
			f.squad.removeUnit(f);
			fighters.remove(f.id);
		}

		Fighter dead = null;
		for (Raider r:raiders){
			if (r.id == unit.getUnitId()){
				dead = r;
				if (r.getTask() instanceof ScoutTask){
					ScoutTask st = (ScoutTask) r.getTask();
					st.removeRaider(r);
				}
				if (r.getTask() instanceof RaidTask){
					RaidTask st = (RaidTask) r.getTask();
					st.removeRaider(r);
				}
			}
		}
		for (Strider s:striders){
			if (s.id == unit.getUnitId()){
				dead = s;
			}
		}
		raiders.remove(dead);
		striders.remove(dead);
        return 0; // signaling: OK
    }
    
    @Override
    public int unitDamaged(Unit h, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
    	if(cowardUnits.contains(h)){
			if(h.getHealth()/h.getMaxHealth() < 0.35){
				if(!retreatingUnits.contains(h)){
					retreatingUnits.add(h);
					if (fighters.containsKey(h.getUnitId())){
						Fighter f = fighters.get(h.getUnitId());
						f.squad.removeUnit(f);
					}
				}
			}
		}
		return 0;
    }
}
