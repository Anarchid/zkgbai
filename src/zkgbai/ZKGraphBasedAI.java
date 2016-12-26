package zkgbai;

import java.util.*;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.*;

import java.io.PrintWriter;
import java.io.StringWriter;

import zkgbai.economy.EconomyManager;
import zkgbai.economy.FactoryManager;
import zkgbai.economy.tasks.ReclaimTask;
import zkgbai.graph.GraphManager;
import zkgbai.graph.MetalSpot;
import zkgbai.kgbutil.Pathfinder;
import zkgbai.los.LosManager;
import zkgbai.military.MilitaryManager;
import zkgbai.military.unitwrappers.Squad;

import static zkgbai.kgbutil.KgbUtil.*;

public class ZKGraphBasedAI extends com.springrts.ai.oo.AbstractOOAI {
	private static ZKGraphBasedAI instance = null;

	private OOAICallback callback;
    private List<Module> modules = new LinkedList<Module>();
	HashSet<Integer> enemyTeams = new HashSet<Integer>();
	HashSet<Integer> enemyAllyTeams = new HashSet<Integer>();
	public List<Integer> allies;
    public int teamID;
    public int allyTeamID;
    public int currentFrame = 0;
    //DebugView debugView;
    boolean debugActivated;
    boolean rsgn = false;

	private boolean slave = false;
	private int mergeTarget = Integer.MAX_VALUE;
	public int mergedAllies = 0;
	public int unmergedAllies = 0;
	public int mergedAllyID = 0;
	
	public boolean isFFA = false;
    
	public LosManager losManager;
	public GraphManager graphManager;
	public EconomyManager ecoManager;
	public MilitaryManager warManager;
	public FactoryManager facManager;
    
    public static enum StartType{
    	SPRING_BOX,
    	ZK_BOX,
    	ZK_STARTPOS,
    	SPRING_STARTPOS, // unsupported
    }
    
    public StartType startType;

	// Set singleton instance
	public ZKGraphBasedAI(){
		super();
		instance = this;
	}

	public static ZKGraphBasedAI getInstance(){
		return instance;
	}
    
	@Override
    public int init(int AIId, OOAICallback callback) {
		this.debugActivated = false;
        this.callback = callback;
        this.teamID = callback.getGame().getMyTeam();
        this.allyTeamID = callback.getGame().getMyAllyTeam();
		this.allies = new ArrayList<Integer>();

        identifyEnemyTeams();
		identifyAllyTeams();
		checkTeamsMerging();

		try{
			graphManager = new GraphManager(this);
			debug(graphManager.getModuleName() + " initialized.");
		} catch (Throwable e){
			printException(e);
		}

		if (slave){
			selectRandomCommander();
			chooseStartPos();
			debug("Entering merge mode! Discarding modules.");
			graphManager = null;
			return 0;
		}

		// load modules
        try {
			losManager = new LosManager(this);
			debug(losManager.getModuleName() + " initialized.");
		} catch (Throwable e){
			printException(e);
		}
		try {
        	ecoManager = new EconomyManager(this);
			debug(ecoManager.getModuleName() + " initialized.");
		} catch (Throwable e){
			printException(e);
		}
        try {
			warManager = new MilitaryManager(this);
			debug(warManager.getModuleName() + " initialized.");
		} catch (Throwable e){
			printException(e);
		}
		try {
			facManager = new FactoryManager(this);
			debug(facManager.getModuleName() + " initialized.");
		} catch (Throwable e){
			printException(e);
		}

		/*try{
			debugView = new DebugView(this);
			debugView.setLosImage(losManager.getImage());
			debugView.setThreatImage(warManager.getThreatMap());
			debugView.setGraphImage(graphManager.getGraphImage());
		}
		catch(Throwable e){
			debug(e);
		}*/


		modules.add(losManager);
        modules.add(graphManager);
        modules.add(ecoManager);
        modules.add(warManager);
		modules.add(facManager);

		try {
			for (Module m:modules){
				m.init(AIId, callback);
			}
		} catch (Throwable e){
			printException(e);
		}

		selectRandomCommander();
		chooseStartPos();
        
        return 0;
    }

	private void debug(Throwable e) {
		debug(e.getMessage());
		for(StackTraceElement ste:e.getStackTrace()){
			debug(ste.toString());
		}
	}

	private void selectRandomCommander(){
		List<UnitDef> unitdefs = callback.getUnitDefs();
		List<String> commanderNames = new ArrayList<String>();
		
		for(UnitDef ud:unitdefs){
			if(ud.getHumanName().contains("Engineer Trainer")){
				commanderNames.add(ud.getName());
			}
		}
		
		int index = (int) Math.floor(Math.random() * commanderNames.size());
		String name = commanderNames.get(index);
		callback.getLua().callRules("ai_commander:"+name, -1);
		debug("Selected Commander: " + callback.getUnitDefByName(name).getHumanName() + ".");
	}

    @Override
    public int luaMessage(java.lang.String inData){
	    for (Module module : modules) {
	    	try {
	            module.luaMessage(inData);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}
        }	        
	        
    	return 0; //signaling: OK
    }
    
    @Override
    public int update(int frame) {
		if (slave) {
			if (frame % 30 == 0) {
				// give away income from communism when merged with another AI instance.
				Resource metal = callback.getResources().get(0);
				Resource energy = callback.getResources().get(1);
				callback.getEconomy().sendResource(metal, Math.min(10, callback.getEconomy().getCurrent(metal)) + callback.getEconomy().getIncome(metal), mergeTarget);
				callback.getEconomy().sendResource(energy, Math.min(10, callback.getEconomy().getCurrent(energy)) + callback.getEconomy().getIncome(energy), mergeTarget);
			}
			return 0;
		}
		
		if (frame == 0){
			callback.getGame().sendTextMessage("/say <ZKGBAI> glhf!", 0);
		}

		if (frame % 1800 == 0){
			Pathfinder.getInstance().updateSlopeMap();
		}
		
		if (frame % 300 == 0){
			List<MetalSpot> espots = graphManager.getEnemySpots();
			List<MetalSpot> ospots = graphManager.getOwnedSpots();
			if (((enemyAllyTeams.size() == 1 && espots.size() > 2 * ospots.size()) || (ospots.size() == 0 && facManager.factories.isEmpty())) && frame > 18000){
				callback.getGame().sendTextMessage("/say <ZKGBAI> gg!", 0);
				callback.getGame().sendTextMessage("/say <ZKGBAI resigned>", 0);
				for (Unit u : callback.getFriendlyUnits()) {
					u.selfDestruct((short) 0, Integer.MAX_VALUE);
				}
			}
		}
		
		if (frame % 1350 == 0){
			if (graphManager.territoryFraction > 0.6f && frame > 18000 && (!rsgn || Math.random() > 0.5) && !graphManager.getEnemySpots().isEmpty()){
				rsgn = true;
				double rand = Math.random();
				if (rand > 0.75) {
					callback.getGame().sendTextMessage("/say <ZKGBAI> Do you know what time it is?", 0);
					callback.getGame().sendTextMessage("/say <ZKGBAI> It's the time to resign!", 0);
				}else if (rand > 0.5){
					if (enemyTeams.size() > 1) {
						callback.getGame().sendTextMessage("/say <ZKGBAI> Resign lobsters!", 0);
					}else {
						callback.getGame().sendTextMessage("/say <ZKGBAI> Resign lobster!", 0);
					}
				}else if (rand > 0.25){
					callback.getGame().sendTextMessage("/say <ZKGBAI> It's over, resign already.", 0);
				}else{
					callback.getGame().sendTextMessage("/say <ZKGBAI> Having trouble?", 0);
					callback.getGame().sendTextMessage("/say <ZKGBAI> Type \"!resign\" for help.", 0);
				}
			}
		}
		
    	currentFrame = frame;

		for (Module module : modules) {
			try {
				module.update(frame);
			} catch (Throwable e) {
				printException(e);
			}
		}
        return 0; // signaling: OK
    }

    @Override
    public int message(int player, String message) {
	    if (!slave && message.equals("kgbdebug")){
		    for (MetalSpot ms: graphManager.getFrontLineSpots()){
		    	callback.getMap().getDrawer().addPoint(ms.getPos(), "Front Line");
		    }
		}

		for (Module module : modules) {
	    	try {
	            module.message(player, message);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}
	    }
	    
        return 0; // signaling: OK
    }
    
    @Override
    public int unitCreated(Unit unit, Unit builder) {
		if (slave){
			List<Unit> l = new ArrayList();
			l.add(unit);
			callback.getEconomy().sendUnits(l, mergeTarget);
		}

	    for (Module module : modules) {
	    	try {
	            module.unitCreated(unit, builder);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}
	    } 
        return 0;
    }

    @Override
    public int unitFinished(Unit unit) {
	    for (Module module : modules) {
	        try {
	            module.unitFinished(unit);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}
	    }
        return 0; // signaling: OK
    }

    @Override
    public int unitIdle(Unit unit) {
	    for (Module module : modules) {
	    	try {
	    		module.unitIdle(unit);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}
	    } 
        return 0; // signaling: OK
    }

    @Override
    public int unitMoveFailed(Unit unit) {
	    for (Module module : modules) {
	    	try {
	            module.unitMoveFailed(unit);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}
	    }
        return 0; // signaling: OK
    }

    @Override
    public int unitDamaged(Unit unit, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
	    for (Module module : modules) {
        	try {
        		module.unitDamaged(unit, attacker, damage, dir, weaponDef, paralyzed);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}
        } 
        return 0; // signaling: OK
    }

    @Override
    public int unitDestroyed(Unit unit, Unit attacker) {
        try {
        	//this.unitManager.deRegisterUnit(unit);
    	} catch (Throwable e) {
    		printException(e);
    	}
	    for (Module module : modules) {
	    	try {
	            module.unitDestroyed(unit, attacker);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}
	    }	    
        return 0; // signaling: OK
    }

    @Override
    public int unitGiven(Unit unit, int oldTeamId, int newTeamId) {
		if (slave){
			List<Unit> l = new ArrayList();
			l.add(unit);
			callback.getEconomy().sendUnits(l, mergeTarget);
			return 0;
		}

        for (Module module : modules) {
        	try {
        		module.unitGiven(unit, oldTeamId, newTeamId);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}
        }	  
        return 0; // signaling: OK
    }

    @Override
    public int unitCaptured(Unit unit, int oldTeamId, int newTeamId) {
        for (Module module : modules) {
        	try {
            	module.unitCaptured(unit, oldTeamId, newTeamId);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}
        }
	    
        return 0; // signaling: OK
    }

    @Override
    public int enemyEnterLOS(Unit enemy) {
	    for (Module module : modules) {
	    	try {
	    		module.enemyEnterLOS(enemy);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}    	 
	    }	    
        return 0; // signaling: OK
    }

    @Override
    public int enemyLeaveLOS(Unit enemy) {
    	for (Module module : modules) {
            try {
            	module.enemyLeaveLOS(enemy);
	    	} catch (Throwable e) {
				printException(e);
	    	}
        }	    
        return 0; // signaling: OK
    }

	@Override
	public int enemyCreated(Unit enemy) {
		for (Module module : modules) {
			try {
				module.enemyCreated(enemy);
			} catch (Throwable e) {
				printException(e);
			}
		}
		return 0; // signaling: OK
	}

	@Override
	public int enemyFinished(Unit enemy) {
		for (Module module : modules) {
			try {
				module.enemyFinished(enemy);
			} catch (Throwable e) {
				printException(e);
			}
		}
		return 0; // signaling: OK
	}

    @Override
    public int enemyEnterRadar(Unit enemy) {
    	for (Module module : modules) {
    		try {
    			module.enemyEnterRadar(enemy);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}
        }	   
        return 0; // signaling: OK
    }

    @Override
    public int enemyLeaveRadar(Unit enemy) {
		for (Module module : modules) {
			try {
				module.enemyLeaveRadar(enemy);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}        
	    }	    
        return 0; // signaling: OK
    }

    @Override
    public int enemyDamaged(Unit enemy, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
    	for (Module module : modules) {
    		try {
    			module.enemyDamaged(enemy, attacker, damage, dir, weaponDef, paralyzed);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}
        }	    
        return 0; // signaling: OK
    }

    @Override
    public int enemyDestroyed(Unit enemy, Unit attacker) {
    	for (Module module : modules) {
    		try {
    			module.enemyDestroyed(enemy, attacker);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}
        }	    
        return 0; // signaling: OK
    }

    @Override
    public int weaponFired(Unit unit, WeaponDef weaponDef) {
    	for (Module module : modules) {
    		try {
    			module.weaponFired(unit, weaponDef);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}		
        }	    
        return 0; // signaling: OK
    }

    @Override
    public int commandFinished(Unit unit, int commandId, int commandTopicId) {
	    for (Module module : modules) {
	    	try {
	    		module.commandFinished(unit, commandId, commandTopicId);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}
	    }	    
        return 0; // signaling: OK
    }

    @Override
    public int seismicPing(AIFloat3 pos, float strength) {
    	for (Module module : modules) {
    		try {
    			module.seismicPing(pos, strength);
	    	} catch (Throwable e) {
	    		printException(e);
	    	}
        }	    
        return 0; // signaling: OK
    }
    
    private void identifyEnemyTeams(){
    	List<Team> enemies = callback.getEnemyTeams();
		
    	for(Team i:enemies){
    		int enemyTeam = i.getTeamId(); 
    		int allyTeam = callback.getGame().getTeamAllyTeam(enemyTeam);
    		if (!callback.getGame().getRulesParamString("allyteam_short_name_" + allyTeam, "").equals("Neutral")) {
			    enemyTeams.add(enemyTeam);
			    if (allyTeam != this.allyTeamID) {
				    if (!enemyAllyTeams.contains(allyTeam)) {
					    enemyAllyTeams.add(allyTeam);
				    }
			    }
		    }
    	}
    }

	// creates a list of teams that are allied with the AI.
	private void identifyAllyTeams(){
		int teamCount = callback.getGame().getTeams();
		for (int i = 0; i < teamCount; i++){
			if (callback.getGame().getTeamAllyTeam(i) == allyTeamID && i != teamID){
				allies.add(i);
			}
		}
	}
    
    public OOAICallback getCallback(){
    	return callback;
    }
    
    public void debug(String s) {
    	callback.getGame().sendTextMessage(s, 0);
    }
    
    public void marker(AIFloat3 position, String message){
    	callback.getMap().getDrawer().deletePointsAndLines(position);
    	callback.getMap().getDrawer().addPoint(position, message);
    }
    
    public void drawLine(AIFloat3 v0, AIFloat3 v1){
    	callback.getMap().getDrawer().addLine(v0,v1);
    }
    
    public Set<Integer> getEnemyAllyTeamIDs(){
    	return this.enemyAllyTeams;
    }
    
    public Set<Integer> getEnemyTeamIDs(){
    	return this.enemyTeams;
    }
    
    public void printException(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        debug("exception(" + sw.toString().replace("\n", " ") + ") " + ex);
    }

	public void printException(Throwable ex) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		debug("exception(" + sw.toString().replace("\n", " ") + ") " + ex);
	}

	private void chooseStartPos(){
		List<MetalSpot> spots = graphManager.getAllyTerritory();
		int priorityStarts = 0;

		for (int allyid: allies){
			if (allyid < teamID){
				priorityStarts++;
			}
		}

		if (!spots.isEmpty()) {
			if (allies.size() == 0 || (allies.size() > 1 && priorityStarts == 0)) { // when starting solo, pick a spot near map center
				MetalSpot best = null;
				float dist = Float.MAX_VALUE;
				for (MetalSpot ms : spots) {
					float distmod = 1f;
					for (MetalSpot m : graphManager.getMetalSpots()) {
						if (m != ms) {
							float msdist = distance(m.getPos(), ms.getPos());
							if (msdist < 400) {
								distmod++;
							}
						}
					}

					float tmpdist = distance(ms.getPos(), graphManager.getMapCenter()) / distmod;
					if (tmpdist < dist) {
						best = ms;
						dist = tmpdist;
					}
				}

				callback.getGame().sendStartPosition(false, best.getPos());
				graphManager.setStartPos(best.getPos());
				debug("Solo Mode: Start Position Selected!");
			}else{ // when playing on teams, generate a position based on relative teamID
				AIFloat3 startPos;
				List<AIFloat3> cachedPositions = new ArrayList<>();
				AIFloat3 mapCenter = graphManager.getMapCenter();

				MetalSpot best = null;
				float distance = 0;
				for (MetalSpot ms: spots){
					float tmpdist = distance(ms.getPos(), mapCenter);
					if (tmpdist > distance){
						distance = tmpdist;
						best = ms;
					}
				}

				startPos = best.getPos();
				cachedPositions.add(startPos);
				spots.remove(best);

				if (priorityStarts > 1 || (allies.size() == 1 && priorityStarts > 0)){
					for (int i = 0; i < Math.max(1, priorityStarts - 1); i++){
						if (spots.isEmpty()) break;
						best = null;
						distance = 0;
						for (MetalSpot ms: spots){
							// for each metal spot
							float tmpdist = Float.MAX_VALUE;
							for (AIFloat3 spos: cachedPositions){
								// for each previously produced startpos,
								// find the closest one to the current metal spot
								// and use its distance as cost.

								// basically we're looking for the metal spot with the largest distance
								// from the closest previously chosen spot to it.
								float tmpclose = distance(spos, ms.getPos());
								if (tmpclose < tmpdist){
									tmpdist = tmpclose;
								}
							}
							if (tmpdist > distance){
								distance = tmpdist;
								best = ms;
							}
						}
						startPos = best.getPos();
						cachedPositions.add(startPos);
						spots.remove(best);
						if (spots.isEmpty()) break;
					}
				}

				if (best != null) {
					callback.getGame().sendStartPosition(false, startPos);
					graphManager.setStartPos(startPos);
					debug("Teams Mode: Start Position Selected!");
				}else{
					debug("Teams Mode: No start positions available!");
				}
			}
		}else{
			debug("chooseStartPos: Startbox inference failed, or there were no mexes within the startbox.");
		}
	}

	private void checkTeamsMerging(){
		for (int tID : allies){
			if (isIngroup(tID) && tID < teamID){
				slave = true;
				mergedAllies++;
				if (tID < mergeTarget){
					mergeTarget = tID;
				}
			}else if (isIngroup(tID)) {
				mergedAllies++;
				mergedAllyID = tID;
			}else{
				unmergedAllies++;
			}
		}
	}

	private boolean isIngroup(int tId) {
		String[] script = callback.getGame().getSetupScript().split("\n");
		for (int line = 0; line < script.length; line++) {
			if (script[line].startsWith("team=" + tId) && (script[line - 1].startsWith("shortname=ZKGBAI") || script[line - 2].startsWith("shortname=ZKGBAI"))) {
				return true;
			}
		}
		return false;
	}
}
