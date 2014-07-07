package zkgbai.economy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.tasks.ConstructionTask;
import zkgbai.economy.tasks.ProductionTask;
import zkgbai.economy.tasks.TemporaryAssistTask;
import zkgbai.economy.tasks.WorkerTask;
import zkgbai.graph.GraphManager;
import zkgbai.graph.Link;
import zkgbai.graph.MetalSpot;
import zkgbai.graph.Pylon;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Economy;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;

public class EconomyManager extends Module {
	ZKGraphBasedAI parent;
	List<Worker> workers;
	ArrayList<Unit> factories;
	List<ConstructionTask> factoryTasks;
	List<WorkerTask> workerTasks;
	
	float effectiveIncomeMetal = 0;
	float effectiveIncomeEnergy = 0;
	
	float totalIncomeMetal = 0;
	float totalIncomeEnergy = 0;
	float totalBuildpower = 0;
	
	float effectiveIncome = 0;
	float effectiveExpenditure = 0;
	
	float energyQueued = 0;
	float metalQueued = 0;
	float buildpowerQueued = 0;
	
	int frame = 0;
	
	Economy eco;
	Resource m;
	Resource e;
	
	private int myTeamID;
	private OOAICallback callback;
	private GraphManager graphManager;
	private ArrayList<String> attackers;
	
	public EconomyManager(ZKGraphBasedAI parent){
		this.parent = parent;
		this.callback = parent.getCallback();
		this.myTeamID = parent.teamID;
		this.workers = new ArrayList<Worker>();
		this.factories = new ArrayList<Unit>();
		this.factoryTasks = new ArrayList<ConstructionTask>();
		this.workerTasks = new ArrayList<WorkerTask>();

		this.eco = callback.getEconomy();
		
		this.m = callback.getResourceByName("Metal");
		this.e = callback.getResourceByName("Energy");
		
		this.attackers = new ArrayList<String>();
		attackers.add("armrock");
		attackers.add("armrock");
		attackers.add("armpw");
		attackers.add("armpw");
		attackers.add("armpw");
		attackers.add("armzeus");
	}
	
	@Override
	public String getModuleName() {
		// TODO Auto-generated method stub
		return "EconomyManager";
	}
	
	@Override
	public int update(int frame){
		this.frame = frame;
		
		effectiveIncomeMetal = eco.getIncome(m);
		effectiveIncomeEnergy = eco.getIncome(e);
		
		float expendMetal = eco.getUsage(m);
		float expendEnergy = eco.getUsage(e);
		
		effectiveIncome= Math.min(effectiveIncomeMetal, effectiveIncomeEnergy);
		effectiveExpenditure = Math.min(expendMetal, expendEnergy);
		
		if(frame%30 ==0){
			inventarizeWorkers();
		}
		
		for(Worker w:workers){
			WorkerTask wt = w.getTask();
			if(wt.isCompleted()){
				assignWorkerTask(w);
				workerTasks.remove(wt);
				if(factoryTasks.contains(wt)){
					factoryTasks.remove(wt);
				}
			}
		}
		
		for(WorkerTask wt:workerTasks){
			if(wt instanceof TemporaryAssistTask){
				TemporaryAssistTask at = (TemporaryAssistTask) wt;
				if(at.getTimeout() < frame){
					at.setCompleted();
				}
			}
		}
		
		
		return 0;
	}
	
	@Override
    public int init(int teamId, OOAICallback callback) {
        return 0;
    }
	
    @Override
    public int unitFinished(Unit unit) { 
    	
		if(unit.getDef().getName().equals("factorycloak")){
			factories.add(unit);
			int numTeams = callback.getGame().getTeams();
			
			for(int i=0;i<numTeams;i++){
				if (i == parent.teamID){
					continue;
				}else{
					int allyTeam = callback.getGame().getTeamAllyTeam(i);
					if(!callback.getGame().isAllied(parent.allyTeamID, allyTeam)){
						//Gotcha! ENEMIES!
						AIFloat3 enemyPos = parent.getEnemyPosition(allyTeam);
						if(enemyPos != null){
							parent.marker(enemyPos, "ENEMY HERE");
							unit.patrolTo(enemyPos, (short) 0, frame+10);
						}
					}	
				}
			}
			

		}
    	checkWorker(unit);
    	
    	for (WorkerTask wt:workerTasks){
    		if(wt instanceof ProductionTask){
    			ProductionTask ct = (ProductionTask)wt;
    			if (ct.building != null){
	    			if(ct.building.getUnitId() == unit.getUnitId()){
	    				ct.setCompleted();
	    			}
    			}
    		}
    	}
        return 0;
    }
    
    @Override
    public int unitDestroyed(Unit unit, Unit attacker) {
    	Worker deadWorker = null;
	    for (Worker worker : workers) {
	    	if(worker.getUnit().getUnitId() == unit.getUnitId()){
	    		deadWorker = worker;
	    		workerTasks.remove(worker.getTask());
	    	}
	    } 
	    if (deadWorker != null){
	    	workers.remove(deadWorker);
	    }
	    
	    if(unit.getDef().getName() == "factorycloak"){
	    	factories.remove(unit);
	    }
        return 0; // signaling: OK
    }
    

    @Override
    public int unitIdle(Unit unit) {
	    for (Worker worker : workers) {
	    	if(worker.getUnit().getUnitId() == unit.getUnitId()){
	    		worker.getTask().setCompleted();
	    		WorkerTask wt = new WorkerTask(worker); 
	    		worker.setTask(wt);
	    		workerTasks.add(wt);
	    	}
	    } 
        return 0; // signaling: OK
    }
    
    @Override
    public int unitCreated(Unit unit, Unit builder) {
    	if(builder != null){
			if(unit.isBeingBuilt()){
		    	for (WorkerTask wt:workerTasks){
		    		if(wt instanceof ProductionTask){
		    			ProductionTask ct = (ProductionTask)wt;
		    			if (ct.getWorker().getUnit().getUnitId() == builder.getUnitId()){
		    				ct.setBuilding(unit);
		    			}
		    		}
		    	}
			}else{	
		    	for (WorkerTask wt:workerTasks){
		    		if(wt instanceof ProductionTask){
		    			ProductionTask ct = (ProductionTask)wt;
		    			if (ct.getWorker().getUnit().getUnitId() == builder.getUnitId()){
		    				ct.setCompleted();
		    			}
		    		}
		    	}
			}
    	}
        return 0;
    }
    
    private String getRandomAttacker(){
    	int index = (int) Math.floor(Math.random()*attackers.size());
    	return attackers.get(index);
    }
    
    /**
     * Manually check for workers without orders
     */
    private void inventarizeWorkers(){
    	for(Worker w:workers){
    		Unit u = w.getUnit();
    		if (u.getCurrentCommands().size() == 0){
	    		w.getTask().setCompleted();
	    		WorkerTask wt = new WorkerTask(w); 
	    		w.setTask(wt);
	    		workerTasks.add(wt);
    		}
    	}
    }
    
    void checkWorker(Unit unit){
		UnitDef def = unit.getDef();
		if (def.isBuilder()){
			if (def.getBuildOptions().size()>0){
				workers.add(new Worker(unit));
			}else{
				AIFloat3 shiftedPosition = unit.getPos();
		    	shiftedPosition.x+=30;
		    	shiftedPosition.z+=30;
		        unit.patrolTo(shiftedPosition, (short)0, frame+900000);
		        unit.setRepeat(true, (short)0, 900000);
			}
			totalBuildpower += def.getBuildSpeed();
		}
    }
    
    void assignWorkerTask(Worker worker){
    	// factories get special treatment
    	if(worker.getUnit().getMaxSpeed() == 0){
    		if (worker.getUnit().getDef().getName() == "armnanotc"){
    			
    		}else{
	    		if(totalBuildpower < effectiveIncome  || effectiveExpenditure+2 < effectiveIncome){
	    			ProductionTask task = createUnitTask(worker, "armrectr");
	    			workerTasks.add(task);
	    			worker.setTask(task);
	    		}else{
	    			ProductionTask task = createUnitTask(worker, getRandomAttacker());
	    			workerTasks.add(task);
	    			worker.setTask(task);	
	    		}
    		}
    		return;
    	}
    	
    	// is there a factory? a queued one maybe?
    	if (factories.size() == 0){
    		if(factoryTasks.size() ==0){
    			ConstructionTask task = createFactoryTask(worker);
    			factoryTasks.add(task);
    			worker.setTask(task);
    			return;
    		}else{
    			// check if assisting thet  to assist this factory?
    		}
    	}

    	// is there sufficient energy to cover metal income?
    	if(effectiveIncomeMetal*1.1 +2 > effectiveIncomeEnergy){
			ConstructionTask task = createEnergyTask(worker);
			worker.setTask(task);
			return;
    	}
    	
    	// ponder building a nanoturret
    	if(effectiveExpenditure+2 < effectiveIncome){
    		for(Worker w:workers){
    			Unit u = w.getUnit();
    			if(u.getMaxSpeed() == 0 && u.getDef().getBuildOptions().size()>0){
    				float dist = graphManager.groundDistance(worker.getUnit().getPos(),u.getPos());
    				if (dist<1000){
    					ConstructionTask task = createNanoTurretTask(worker,u);
    					worker.setTask(task);
    					return;
    				}
    			}
    		}
    	}
    	
    	// are there uncolonized metal spots?   	
		MetalSpot ms = graphManager.getSpotToColonize(worker.getUnit().getPos());
		if (ms != null){
			ConstructionTask task = createColonizeTask(worker, ms);
			worker.setTask(task);
			ms.addColonist(worker.getUnit());
			return;
		}
		
    	// are there damaged nearby ally units?
    	// is it useful to assist?
		
		// final fallback: make even more energy
		ConstructionTask task = createEnergyTask(worker);
		worker.setTask(task);
		return;
    }
    
    ProductionTask createUnitTask(Worker worker, String unitname){
    	UnitDef requisite = callback.getUnitDefByName(unitname);
    	int priority = 100;
    	int constructionPriority = 3;
        worker.getUnit().build(requisite, worker.getUnit().getPos(), (short) Math.floor(Math.random()*4), (short) 0, frame + 1000);
        ProductionTask ct =  new ProductionTask(worker,requisite,priority,constructionPriority);
    	workerTasks.add(ct);
    	return ct;
    }
    
    ConstructionTask createFactoryTask(Worker worker){
    	UnitDef factory = callback.getUnitDefByName("factorycloak");
    	AIFloat3 position = callback.getMap().findClosestBuildSite(factory,worker.getUnit().getPos(),600f, 3, 0);
    	int priority = 100;
    	int constructionPriority = 3;
        worker.getUnit().build(factory, position, (short) Math.floor(Math.random()*4), (short) 0, frame + 1000);
    	ConstructionTask ct =  new ConstructionTask(worker,factory,priority,constructionPriority, position);
    	workerTasks.add(ct);
    	return ct;
    }
    
    ConstructionTask createColonizeTask(Worker worker, MetalSpot ms){
    	UnitDef mex = callback.getUnitDefByName("cormex");
    	AIFloat3 position = callback.getMap().findClosestBuildSite(mex,ms.getPosition(),600f, 3, 0);
    	int priority = 100;
    	int constructionPriority = 3;
        worker.getUnit().build(mex, position, (short) Math.floor(Math.random()*4), (short) 0, frame + 1000);
    	ConstructionTask ct =  new ConstructionTask(worker,mex,priority,constructionPriority, position);
    	workerTasks.add(ct);
    	return ct;
    }
    
    ConstructionTask createEnergyTask(Worker worker){
    	
    	// TODO: implement overdrive housekeeping in graphmanager, get buildpos from there 
    	
    	UnitDef solar = callback.getUnitDefByName("armsolar");
    	AIFloat3 position = graphManager.getOverdriveSweetSpot(worker.getUnit().getPos());
    	position = callback.getMap().findClosestBuildSite(solar,position,600f, 3, 0);
    	int priority = 100;
    	int constructionPriority = 3;
        worker.getUnit().build(solar, position, (short) Math.floor(Math.random()*4), (short) 0, frame + 1000);
    	ConstructionTask ct =  new ConstructionTask(worker,solar,priority,constructionPriority, position);
    	workerTasks.add(ct);
    	return ct;
    }
    
    ConstructionTask createNanoTurretTask(Worker worker, Unit target){ 	
    	UnitDef nano = callback.getUnitDefByName("armnanotc");
    	AIFloat3 position = target.getPos();
    	position.x += Math.random()*100f-50;
    	position.z += Math.random()*100f-50;
    	position = callback.getMap().findClosestBuildSite(nano,position,600f, 3, 0);
    	int priority = 100;
    	int constructionPriority = 3;
        worker.getUnit().build(nano, position, (short) Math.floor(Math.random()*4), (short) 0, frame + 1000);
    	ConstructionTask ct =  new ConstructionTask(worker,nano,priority,constructionPriority, position);
    	workerTasks.add(ct);
    	return ct;
    }
    
	public void setGraphManager(GraphManager graphManager) {
		this.graphManager = graphManager;
	}
}
