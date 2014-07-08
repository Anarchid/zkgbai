package zkgbai;

import java.util.HashMap;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.CommandDescription;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import com.springrts.ai.oo.clb.WeaponDef;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.vecmath.Tuple3f;

import zkgbai.Module;
import zkgbai.economy.EconomyManager;
import zkgbai.graph.GraphManager;
import zkgbai.los.LosManager;

public class ZKGraphBasedAI extends com.springrts.ai.oo.AbstractOOAI {
    private OOAICallback callback;
    private List<Module> modules = new LinkedList<Module>();
    public HashMap<Integer, float[]> startBoxes;
    public int teamID;
    public int allyTeamID;
    
	@Override
    public int init(int teamId, OOAICallback callback) {
        this.callback = callback;
        this.teamID = teamId;
        this.allyTeamID = callback.getGame().getMyAllyTeam();
        startBoxes = new HashMap<Integer, float[]>();
        parseStartScript();
        
        callback.getCheats().setEventsEnabled(false);
        callback.getCheats().setEnabled(false);

        LosManager losManager = new LosManager(this);
        GraphManager graphManager = new GraphManager(this);
        EconomyManager ecoManager = new EconomyManager(this);
        
        graphManager.setLosManager(losManager);
        ecoManager.setGraphManager(graphManager);
        
        modules.add(losManager);
        modules.add(graphManager);
        modules.add(ecoManager);
        
        for (Module module : modules) {
        	try {
        		module.init(teamId, callback);
        		debug(module.getModuleName() + " initialized");
	    	} catch (Exception e) {
	    		printException(e);
	    	}
        }
        return 0;
    }
	
    @Override
    public int luaMessage(java.lang.String inData){
	    for (Module module : modules) {
	    	try {
	            module.luaMessage(inData);
	    	} catch (Exception e) {
	    		printException(e);
	    	}
        }	        
    	return 0; //signaling: OK
    }
    
    @Override
    public int update(int frame) {
        for (Module module : modules) {
        	try {
        		module.update(frame);
	    	} catch (Exception e) {
	    		printException(e);
	    	}
        }	    
        return 0; // signaling: OK
    }

    @Override
    public int message(int player, String message) {
	    for (Module module : modules) {
	    	try {
	            module.message(player, message);
	    	} catch (Exception e) {
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
	    	} catch (Exception e) {
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
	    	} catch (Exception e) {
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
	    	} catch (Exception e) {
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
	    	} catch (Exception e) {
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
	    	} catch (Exception e) {
	    		printException(e);
	    	}
        } 
        return 0; // signaling: OK
    }

    @Override
    public int unitDestroyed(Unit unit, Unit attacker) {  
        try {
        	//this.unitManager.deRegisterUnit(unit);
    	} catch (Exception e) {
    		printException(e);
    	}
	    for (Module module : modules) {
	    	try {
	            module.unitDestroyed(unit, attacker);
	    	} catch (Exception e) {
	    		printException(e);
	    	}
	    }	    
        return 0; // signaling: OK
    }

    @Override
    public int unitGiven(Unit unit, int oldTeamId, int newTeamId) {    
        try {
        	//this.unitManager.registerUnit(unit);
    	} catch (Exception e) {
    		printException(e);
    	}
        for (Module module : modules) {
        	try {
        		module.unitGiven(unit, oldTeamId, newTeamId);
	    	} catch (Exception e) {
	    		printException(e);
	    	}
        }	  
        return 0; // signaling: OK
    }

    @Override
    public int unitCaptured(Unit unit, int oldTeamId, int newTeamId) {  
        try {
        	int myTeamId = callback.getGame().getMyTeam(); 
            if (myTeamId == newTeamId) {
            	//this.unitManager.registerUnit(unit);
            } else if (myTeamId == oldTeamId) {
            	//this.unitManager.registerUnit(unit);
            }
    	} catch (Exception e) {
    		printException(e);
    	}
        for (Module module : modules) {
        	try {
            	module.unitCaptured(unit, oldTeamId, newTeamId);
	    	} catch (Exception e) {
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
	    	} catch (Exception e) {
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
	    	} catch (Exception e) {
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
	    	} catch (Exception e) {
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
	    	} catch (Exception e) {
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
	    	} catch (Exception e) {
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
	    	} catch (Exception e) {
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
	    	} catch (Exception e) {
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
	    	} catch (Exception e) {
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
	    	} catch (Exception e) {
	    		printException(e);
	    	}
        }	    
        return 0; // signaling: OK
    }
    
    private void parseStartScript(){
    	String script = callback.getGame().getSetupScript();
    	Pattern p = Pattern.compile("\\[allyteam(\\d)\\]\\s*\\{([^\\}]*)\\}");
    	Matcher m = p.matcher(script);
    	 while (m.find()) {
    		 int allyTeamId = Integer.parseInt(m.group(1));
    		 String teamDefBody = m.group(2);
    		 Pattern sbp = Pattern.compile("startrect\\w+=(\\d+(\\.\\d+)?);");
    		 Matcher mb = sbp.matcher(teamDefBody);
    		     		 
    		 float[] startbox = new float[4];
    		 int i = 0;
        	 while (mb.find()) {
        		 startbox[i] = Float.parseFloat(mb.group(1));
        		 i++;
        	 }
        	 startBoxes.put(allyTeamId, startbox);
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
    
    public ArrayList<AIFloat3> getEnemyPositions(int enemyAllyTeam){
        float[] sb = startBoxes.get(enemyAllyTeam);
        if(sb == null){
        	return null;
        }else{    	

	        float z = (sb[0]+sb[3]) / 2;
	        float x = (sb[1]+sb[2]) / 2;
	        
	        int w8 = 8*callback.getMap().getWidth();
	        int h8 = 8*callback.getMap().getHeight();
	        
	        z = z * h8;
	        x = x * w8;
        	
	        AIFloat3 center = new AIFloat3(x,0,z);
	        AIFloat3 topLeft = new AIFloat3(sb[3]*w8,0,sb[1]*h8);
	        AIFloat3 topRight = new AIFloat3(sb[3]*w8,0,sb[2]*h8);
	        AIFloat3 bottomLeft = new AIFloat3(sb[0]*w8,0,sb[1]*h8);
	        AIFloat3 bottomRight = new AIFloat3(sb[0]*w8,0,sb[2]*h8);
	        
	        drawLine(topLeft, topRight);
	        drawLine(topLeft, bottomLeft);
	        drawLine(bottomRight, topRight);
	        drawLine(bottomRight, bottomLeft);
	        
	        ArrayList<AIFloat3> positions = new ArrayList<AIFloat3>();
	        
	        positions.add(center);
	        positions.add(topLeft);
	        positions.add(topRight);
	        positions.add(bottomLeft);
	        positions.add(bottomRight);


	        return positions;
        }
    }
    
    
    public void printException(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        debug("exception(" + sw.toString().replace("\n", " ") + ") " + ex);
    }
}
