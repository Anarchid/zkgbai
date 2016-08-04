package zkgbai;

import java.util.*;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Game;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Team;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import com.springrts.ai.oo.clb.WeaponDef;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import zkgbai.economy.EconomyManager;
import zkgbai.economy.FactoryManager;
import zkgbai.economy.tasks.ConstructionTask;
import zkgbai.graph.GraphManager;
import zkgbai.graph.MetalSpot;
import zkgbai.gui.DebugView;
import zkgbai.los.LosManager;
import zkgbai.military.MilitaryManager;

public class ZKGraphBasedAI extends com.springrts.ai.oo.AbstractOOAI {
	private static ZKGraphBasedAI instance = null;

	private OOAICallback callback;
    private List<Module> modules = new LinkedList<Module>();
    public HashMap<Integer, StartArea> startBoxes;
	HashSet<Integer> enemyTeams = new HashSet<Integer>();
	HashSet<Integer> enemyAllyTeams = new HashSet<Integer>();
	public List<Integer> allies;
    public int teamID;
    public int allyTeamID;
    public int currentFrame = 0;
    //DebugView debugView;
    boolean debugActivated;
    
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
        this.teamID = callback.getGame().getMyTeam(); // teamID as passed by interface is broken 0_0
        this.allyTeamID = callback.getGame().getMyAllyTeam();
        startBoxes = new HashMap<Integer, StartArea>();
		this.allies = new ArrayList<Integer>();

        parseStartBoxes();
        identifyEnemyTeams();
		identifyAllyTeams();

		// load modules
        try {
			losManager = new LosManager(this);
			debug(losManager.getModuleName() + " initialized.");
		} catch (Throwable e){
			printException(e);
		}
		try{
			graphManager = new GraphManager(this);
			debug(graphManager.getModuleName() + " initialized.");
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
	    /*if (message.equals("kgbdebug")){
			//add debugging code here
		}*/

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
        //log.info("unitDamaged: " + unit.getDef().getName());
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
        try {
        	//this.unitManager.registerUnit(unit);
    	} catch (Throwable e) {
    		printException(e);
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
    
   private void parseStartBoxes(){
		int mapWidth  = 8* callback.getMap().getWidth();
		int mapHeight = 8* callback.getMap().getHeight();
			
		Game g = callback.getGame();

	   if(g.getRulesParamFloat("startbox_custom_shapes", 0.0f) == 1f){
		   startType = StartType.ZK_STARTPOS;
				
		   int maxBox = (int) g.getRulesParamFloat("startbox_max_n", 0.0f);
				
		   for(int i=0;i<=maxBox;i++){
			   ArrayList<AIFloat3> starts = new ArrayList<AIFloat3>();
			   int numStarts = (int) g.getRulesParamFloat("startpos_n_"+i, 0.0f);
			   for(int j = 1;j<=numStarts;j++){
				   float x = g.getRulesParamFloat("startpos_x_"+i+"_"+j, 0.0f);
				   float z = g.getRulesParamFloat("startpos_z_"+i+"_"+j, 0.0f);
				   starts.add(new AIFloat3(x,0,z));
				   debug("startbox "+i+" contains startpos <"+x+","+z+">");
			   }
			   startBoxes.put(i,new ZKStartLocation(starts));
		   }
	
		   return;
	   }

		String script = callback.getGame().getSetupScript();
    	
    	Pattern z = Pattern.compile("startboxes=return \\{(.*)\\};");
    	Matcher zkBoxes = z.matcher(script);
    	
    	if(zkBoxes.find()){
			startType = StartType.ZK_BOX;

        	Pattern zkBox = Pattern.compile(" \\[(\\d+)\\] = \\{ (\\d+(?:\\.\\d+)?)[,]? (\\d+(?:\\.\\d+)?), (\\d+(?:\\.\\d+)?), (\\d+(?:\\.\\d+)?) \\},");
    		Matcher b = zkBox.matcher(zkBoxes.group(1));
    		debug("ZK boxes detected");
    		while(b.find()){
    			int allyTeamID = Integer.parseInt(b.group(1));
    			
	    		float[] box = new float[4];

    			for(int i =0;i<4;i++){
    				// left top width height
    				box[i] = Float.parseFloat(b.group(i+2));
    			}
    			
    			StartBox startbox = new StartBox(
    				box[0] * mapWidth,
    				box[1] * mapHeight,
    				(box[0] + box[2]) * mapWidth, 
    				(box[1] + box[3]) * mapWidth
    			);
	        	
	        	startBoxes.put(allyTeamID, startbox);
    		}    	
    	}else{
    		
			startType = StartType.SPRING_BOX;

	    	Pattern p = Pattern.compile("\\[allyteam(\\d)\\]\\s*\\{([^\\}]*)\\}");
	    	Matcher m = p.matcher(script);
	    
	    	 while (m.find()) {
	    		 int allyTeamId = Integer.parseInt(m.group(1));
	    		 String teamDefBody = m.group(2);
	    		 Pattern sbp = Pattern.compile("startrect\\w+=(\\d+(\\.\\d+)?);");
	    		 Matcher mb = sbp.matcher(teamDefBody);
	    		     		 
	    		 float[] startbox = new float[4];
	    		 int i = 0;
	 	        
	 	         // 0 -> bottom
	 	         // 1 -> left
	 	         // 2 -> right
	 	         // 3 -> top
	        	 while (mb.find()) {
	        		 startbox[i] = Float.parseFloat(mb.group(1));
	        		 i++;
	        	 }
	        	 
	        	 startbox[0] *= mapHeight;
	        	 startbox[1] *= mapWidth;
	        	 startbox[2] *= mapWidth;
	        	 startbox[3] *= mapHeight;
	        	 
	        	 startBoxes.put(allyTeamId, new StartBox(
	        		startbox[1],
	        		startbox[3],
	        		startbox[2],
	        		startbox[0]
	        	 ));
	    	}    		 
    	}
    }
    
    private void identifyEnemyTeams(){
    	List<Team> enemies = callback.getEnemyTeams();
		
    	for(Team i:enemies){
    		int enemyTeam = i.getTeamId(); 
    		int allyTeam = callback.getGame().getTeamAllyTeam(enemyTeam);
    		
    		enemyTeams.add(enemyTeam);
			if(allyTeam != this.allyTeamID){
				if(!enemyAllyTeams.contains(allyTeam)){
					enemyAllyTeams.add(allyTeam);
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
    
    public StartArea getStartArea(int id){
    	return this.startBoxes.get(id);
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
		if (!spots.isEmpty()) {
			int rand = (int) Math.floor(Math.random() * spots.size());
			MetalSpot spot = spots.get(rand);
			callback.getGame().sendStartPosition(true, spot.getPos());
			graphManager.setStartPos(spot.getPos());
			debug("Start Position Selected!");
		}else{
			debug("chooseStartPos: Startbox inference failed, or there were no mexes within the startbox.");
		}
	}
}
