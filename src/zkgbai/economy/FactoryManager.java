package zkgbai.economy;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
import zkgbai.graph.GraphManager;
import zkgbai.military.MilitaryManager;
import zkgbai.military.UnitClasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by aeonios on 12/8/2015.
 */
public class FactoryManager extends Module {
    ZKGraphBasedAI ai;
    EconomyManager economyManager;
    MilitaryManager warManager;
    GraphManager graphManager;
    private OOAICallback callback;
    UnitClasses unitTypes;

    public Map<Integer, Factory> factories;

    int frame = 0;

    boolean smallMap = false;
    boolean bigMap = false;

    boolean earlyWorker = false;

    boolean enemyHasAir = false;
    boolean enemyHasDrones = false;
    boolean enemyHasAmphs = false;
    boolean enemyHasHeavyPorc = false;

    boolean glaiveSpam = false;

    public int numWorkers = 0;
    float workerValue = 0;
    float fighterValue = 0;
    float artyValue = 0;
    float assaultValue = 0;
    int numErasers = 0;
    int numAspis = 0;

    int numWarriors = 0;
    int numZeus = 0;
    int numRockos = 0;
    int numGlaives = 0;
    int numScythes = 0;

    int numMaces = 0;
    int numScalpels = 0;

    int numReapers = 0;
    int numBanishers = 0;

    int numLevelers = 0;
    int numRavagers = 0;

    int numScouts = 0;

    int numInfis = 0;
    int numUltis = 0;

    int numFelons = 0;
    int numThugs = 0;
    int numLaws = 0;
    int numRackets = 0;
    public int numFunnels = 0;

    int numVenoms = 0;
    int numRedbacks = 0;
    int numRecluses = 0;
    int numHermits = 0;

    int numRavens = 0;

    Resource m;

    static int CMD_PRIORITY = 34220;

    public FactoryManager(ZKGraphBasedAI ai){
        this.ai = ai;
        this.callback = ai.getCallback();
        this.factories = new HashMap<Integer, Factory>();

        this.smallMap = (callback.getMap().getWidth() + callback.getMap().getHeight() < 1280);
        this.bigMap = (callback.getMap().getHeight() + callback.getMap().getWidth() > 1664);

        this.earlyWorker = bigMap;
        this.m = callback.getResourceByName("Metal");
    }

    @Override
    public int init(int AIID, OOAICallback cb){
        this.warManager = ai.warManager;
        this.economyManager = ai.ecoManager;
        this.graphManager = ai.graphManager;
        this.unitTypes = UnitClasses.getInstance();
        
        return 0;
    }

    @Override
    public String getModuleName() {
        // TODO Auto-generated method stub
        return "FactoryManager";
    }

    @Override
    public int update(int frame) {
        this.frame = frame;

        if (bigMap && frame % 30 == 0 && economyManager.effectiveIncome < 10){
            ArrayList<Float> params = new ArrayList<>();
            params.add((float) 3);
            for (Factory f:factories.values()){
                f.getUnit().executeCustomCommand(CMD_PRIORITY, params, (short) 0, frame + 60);
            }
        }else if (bigMap && frame % 30 == 0){
            ArrayList<Float> params = new ArrayList<>();
            params.add((float) 1);
            for (Factory f:factories.values()){
                f.getUnit().executeCustomCommand(CMD_PRIORITY, params, (short) 0, frame + 60);
            }
        }

        return 0;
    }

    @Override
    public int unitCreated(Unit unit,  Unit builder) {
        UnitDef def = unit.getDef();
        String defName = def.getName();

        if (defName.contains("hub")){
            unit.setMoveState(0, (short) 0, frame + 10);
        }
        if(defName.contains("factory") || defName.contains("hub")){
            Factory fac;
            if (factories.size() < 1 + ai.mergedAllies) {
                fac = new Factory(unit, true);
            }else{
                fac = new Factory(unit, false);
            }
            factories.put(fac.id, fac);
            assignFactoryTask(fac);
            unit.setMoveState(2, (short) 0, frame+300);
        }

        if (def.isBuilder() && unit.getMaxSpeed() > 0 && !defName.equals("armcsa")){
            numWorkers++;
            if (def.getBuildSpeed() < 8f) {
                workerValue += unit.getDef().getCost(m);
            }
        }else if (unit.getMaxSpeed() > 0 && !unitTypes.AAs.contains(defName)){
            fighterValue += Math.min(unit.getDef().getCost(m), 750f);
        }

        if (defName.equals("armfus") || defName.equals("cafus")){
            fighterValue += 250f;
        }

        if(defName.equals("cormart") || defName.equals("armham") || defName.equals("armmerl") || defName.equals("trem") || defName.equals("armmanni") || defName.equals("armcrabe")){
            artyValue += def.getCost(m);
        }

        if(defName.equals("hoverassault") || defName.equals("armzeus") || defName.equals("corraid")
                || defName.equals("amphfloater") || defName.equals("corthud") || defName.equals("correap")){
            assaultValue += def.getCost(m);
        }

        if(defName.equals("armwar")){
            numWarriors++;
        }

        if(defName.equals("armzeus")){
            numZeus++;
        }

        if(defName.equals("armrock")){
            numRockos++;
        }

        if(defName.equals("armpw")){
            numGlaives++;
        }

        if (defName.equals("spherepole")){
            numScythes++;
        }

        if(defName.equals("shieldfelon")){
            numFelons++;
        }

        if (defName.equals("corthud")){
            numThugs++;
        }

        if (defName.equals("cormak")){
            numLaws++;
        }

        if (defName.equals("shieldarty")){
            numRackets++;
        }

        if (defName.equals("funnelweb")){
            numFunnels++;
        }

        if (defName.equals("arm_venom")){
            numVenoms++;
        }

        if (defName.equals("spiderriot")){
            numRedbacks++;
        }

        if (defName.equals("armsptk")){
            numRecluses++;
        }

        if (defName.equals("spiderassault")){
            numHermits++;
        }

        if (defName.equals("hoverriot")){
            numMaces++;
        }

        if (defName.equals("nsaclash")){
            numScalpels++;
        }

        if (defName.equals("correap")){
            numReapers++;
        }

        if (defName.equals("tawf114")){
            numBanishers++;
        }

        if (defName.equals("corfav") || defName.equals("corsh")){
            numScouts++;
        }

        if (defName.equals("corraid")){
            numRavagers++;
        }

        if (defName.equals("corlevlr")){
            numLevelers++;
        }

        if (defName.equals("corshad") || defName.equals("corhurc2")){
            numRavens++;
        }

        if (defName.equals("armspy")){
            numInfis++;
        }

        if (defName.equals("armcomdgun")){
            numUltis++;
        }

        if (defName.equals("spherecloaker")){
            numErasers++;
        }

        if (defName.equals("core_spectre")){
            numAspis++;
        }

        return 0;
    }

    @Override
    public int unitFinished(Unit unit) {
        UnitDef def = unit.getDef();
        String defName = def.getName();

        if (defName.equals("dante") || defName.equals("scorpion") || defName.equals("armraven") || defName.equals("funnelweb")
                || defName.equals("armbanth") || defName.equals("dantearmorco") || defName.equals("armcomdgun")){
            // find striderhubs to manually reassign them because they're stupid.
            List<Unit> units = callback.getFriendlyUnitsIn(unit.getPos(), 500f);
            for (Unit u: units){
                if (u.getTeam() == ai.teamID && u.getDef().getName().equals("striderhub")){
                    Factory f = factories.get(u.getUnitId());
                    assignFactoryTask(f);
                    break;
                }
            }
        }

        return 0;
    }

    @Override
    public int unitDestroyed(Unit unit, Unit attacker) {
        UnitDef def = unit.getDef();
        String defName = def.getName();

        if (def.isBuilder() && unit.getMaxSpeed() > 0){
            numWorkers--;
            if (def.getBuildSpeed() < 8f) {
                workerValue -= unit.getDef().getCost(m);
            }
        }else if (unit.getMaxSpeed() > 0 && !unitTypes.AAs.contains(defName) /*&& !unitTypes.striders.contains(defName)*/){
            fighterValue -= Math.min(unit.getDef().getCost(m), 750f);
        }

        if (defName.equals("armfus") || defName.equals("cafus")){
            fighterValue -= 250f;
        }

        if(defName.equals("cormart") || defName.equals("armham") || defName.equals("armmerl") || defName.equals("trem") || defName.equals("armmanni") || defName.equals("armcrabe")){
            artyValue -= def.getCost(m);
        }

        if(defName.equals("hoverassault") || defName.equals("armzeus") || defName.equals("corraid")
                || defName.equals("amphfloater") || defName.equals("corthud") || defName.equals("correap")){
            assaultValue -= def.getCost(m);
        }

        if (defName.equals("spherecloaker")){
            numErasers--;
        }

        if (defName.equals("core_spectre")){
            numAspis--;
        }

        if(defName.equals("armwar")){
            numWarriors--;
        }

        if(defName.equals("armzeus")){
            numZeus--;
        }

        if(defName.equals("armrock")){
            numRockos--;
        }

        if (defName.equals("shieldfelon")){
            numFelons--;
        }

        if (defName.equals("corthud")){
            numThugs--;
        }

        if (defName.equals("cormak")){
            numLaws--;
        }

        if (defName.equals("shieldarty")){
            numRackets--;
        }

        if (defName.equals("funnelweb")){
            numFunnels--;
        }

        if (defName.equals("arm_venom")){
            numVenoms--;
        }

        if (defName.equals("spiderriot")){
            numRedbacks--;
        }

        if (defName.equals("armsptk")){
            numRecluses--;
        }

        if (defName.equals("spiderassault")){
            numHermits--;
        }

        if (defName.equals("hoverriot")){
            numMaces--;
        }

        if (defName.equals("nsaclash")){
            numScalpels--;
        }

        if (defName.equals("corfav") || defName.equals("corsh")){
            numScouts--;
        }

        if (defName.equals("correap")){
            numReapers--;
        }

        if (defName.equals("tawf114")){
            numBanishers--;
        }

        if (defName.equals("corraid")){
            numRavagers--;
        }

        if (defName.equals("corlevlr")){
            numLevelers--;
        }

        if (defName.equals("corshad") || defName.equals("corhurc2")){
            numRavens--;
        }

        if (defName.equals("armspy")){
            numInfis--;
        }

        if (defName.equals("armcomdgun")){
            numUltis--;
        }

        if (factories.containsKey(unit.getUnitId())){
            factories.remove(unit.getUnitId());
        }
        return 0; // signaling: OK
    }

    @Override
    public int unitCaptured(Unit unit, int oldTeamID, int newTeamID){
        if (oldTeamID == ai.teamID){
            return unitDestroyed(unit, null);
        }
        return 0;
    }

    @Override
    public int unitGiven(Unit unit, int oldTeamID, int newTeamID){
        if (newTeamID == ai.teamID){
            return unitCreated(unit, null);
        }
        return 0;
    }

    @Override
    public int unitIdle(Unit unit) {
        if (factories.containsKey(unit.getUnitId()) && !unit.getDef().getName().equals("striderhub") && !unit.getDef().getName().equals("armcsa")){
            Factory fac = factories.get(unit.getUnitId());
            assignFactoryTask(fac);
        }

        return 0; // signaling: OK
    }

    @Override
    public int enemyEnterLOS(Unit unit){
        if (unit.getDef().isAbleToFly() && !unit.getDef().getName().equals("attackdrone") && !unit.getDef().getName().equals("battledrone") && !unit.getDef().getName().equals("carrydrone")){
            enemyHasAir = true;
        }else if (unit.getDef().getName().equals("attackdrone") || unit.getDef().getName().equals("battledrone") || unit.getDef().getName().equals("carrydrone")){
            enemyHasDrones = true;
        }

        if (unit.getDef().getTooltip().contains("Amph")){
            enemyHasAmphs = true;
        }

        if (unit.getDef().getName().equals("corhlt") || unit.getDef().getName().equals("cordoom") || unit.getDef().getName().equals("armanni") || unit.getDef().getName().equals("armcrabe")){
            enemyHasHeavyPorc = true;
        }
        return 0;
    }

    void assignFactoryTask(Factory fac){
        fac.getUnit().stop((short) 0, frame+30);
        String defName = fac.getUnit().getDef().getName();
        UnitDef unit;
        if (defName.equals("factorycloak")){
            unit = callback.getUnitDefByName(getCloaky(fac));
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("factoryshield")){
            unit = callback.getUnitDefByName(getShields(fac));
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("factorygunship")){
            unit = callback.getUnitDefByName(getGunship(fac));
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("factoryplane")){
            unit = callback.getUnitDefByName(getPlanes(fac));
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("factoryamph")){
            unit = callback.getUnitDefByName(getAmphs(fac));
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("factoryveh")){
            unit = callback.getUnitDefByName(getLV(fac));
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("factoryhover")){
            unit = callback.getUnitDefByName(getHovers(fac));
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("factorytank")){
            unit = callback.getUnitDefByName(getTanks(fac));
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("factoryspider")){
            unit = callback.getUnitDefByName(getSpiders(fac));
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("striderhub")){
            unit = callback.getUnitDefByName(getStrider());
            AIFloat3 pos = callback.getMap().findClosestBuildSite(unit, fac.getPos(), 250f, 3, 0);
            fac.getUnit().build(unit, pos, (short) 0, (short) 0, frame+3000);
        }
    }

    private Boolean needWorkers(Factory fac){
        String facType = fac.getUnit().getDef().getName();
        float metalratio = 3.5f;
        float workerRatio;
        float reclaimValue = economyManager.getReclaimValue();
        float fv = fighterValue + reclaimValue/2f;
        float income = economyManager.effectiveIncomeMetal + ((reclaimValue * ((1f + ai.mergedAllies)/(1f + ai.allies.size())))/150f);
        float allymod = ai.unmergedAllies;

        if (facType.equals("factorytank") || facType.equals("factoryship") || facType.equals("factoryplane") || facType.equals("factorygunship")){
            if (bigMap) {
                workerRatio = 2f + allymod;
            }else{
                workerRatio = 2.5f + allymod;
            }
        }else if (bigMap){
            workerRatio = 1.5f + allymod + ((economyManager.staticIncome * allymod)/100f);
        }else{
            workerRatio = 2f + allymod + ((economyManager.staticIncome)/100f);
        }

        if ((float) graphManager.getOwnedSpots().size()/(float) graphManager.getMetalSpots().size() > 0.5){
            workerRatio *= 1.5f;
        }

        if (facType.equals("factorytank") || facType.equals("factoryship") || facType.equals("factoryplane") || facType.equals("factorygunship")){
            metalratio = 5f;
        }else if (facType.equals("factoryspider")){
            metalratio = 5f;
        }

        if ((float) numWorkers < Math.floor(income/metalratio) + warManager.miscHandler.striders.size() + numFunnels + ((economyManager.fusions.size() + factories.size() - 1) * 3)
                && (fv > workerValue * workerRatio || numWorkers == 0 || (economyManager.effectiveIncome > 15 && economyManager.metal/economyManager.maxStorage > 0.8 && economyManager.energy/economyManager.maxStorage > 0.5 && ai.mergedAllies == 0))
                || (earlyWorker && ai.mergedAllies < 4 && numWorkers < 2 + (2 * ai.mergedAllies) && !facType.equals("factoryplane") && !facType.equals("factorygunship"))
                /*|| (bigMap && numWorkers < 3 && !facType.equals("factorytank"))*/) {
            return true;
        }else if ((fac.raiderSpam >= 0 && numWorkers < 3 * (1 + ai.mergedAllies) && !facType.equals("factorytank") && !facType.equals("factoryplane") && !facType.equals("factorygunship"))
                || (fac.raiderSpam >= 0 && earlyWorker && facType.equals("factorytank") && numWorkers < 3 * (1 + ai.mergedAllies))){
            return true;
        }
        return false;
    }

    private String getCloaky(Factory fac) {
        if (needWorkers(fac)) {
            return "armrectr";
        }

        if (warManager.slasherSpam * 280 > assaultValue){
            return "armzeus";
        }

        if (fac.raiderSpam < 0) {
            if (fac.expensiveRaiderSpam < 0){
                fac.expensiveRaiderSpam++;
                return "spherepole";
            }
            if ((!glaiveSpam && economyManager.effectiveIncome > 15 && numGlaives > 1 && Math.random() > 0.9)) {
                fac.expensiveRaiderSpam -= (int) Math.min(4, Math.max(2, Math.floor(economyManager.adjustedIncome / 10))) - 1;
                return "spherepole";
            } else {
                if (warManager.raiderHandler.soloRaiders.size() > 1) {
                    fac.raiderSpam++;
                }
                return "armpw";
            }
        }

        if (glaiveSpam){
            glaiveSpam = false;
        }

        if (((enemyHasAir || enemyHasDrones) && warManager.AAs.size() < 3) ||
                (enemyHasAir && Math.random() > 0.70 + (0.05 * ((float) factories.size()-(1f + ai.mergedAllies))))){
            return "armjeth";
        }

        if (!economyManager.fusions.isEmpty() && numErasers < 1 + (ai.mergedAllies/2) && warManager.squadHandler.squads.size() > 0 && Math.random() > 0.75){
            return "spherecloaker";
        }

        if (economyManager.adjustedIncome > 15 && Math.random() > 0.9){
            return "armtick";
        }

        if (economyManager.effectiveIncome > 30 && Math.random() > 0.975){
            glaiveSpam = true;
            fac.raiderSpam -= Math.min(12, Math.max(6, (int) Math.floor(economyManager.adjustedIncome/5f)));
        }

        double rand = Math.random();
        if (economyManager.fusions.isEmpty()){
            if (numRockos < 2 * (numWarriors + numZeus)) {
                if (bigMap && economyManager.effectiveIncome < 30) {
                    fac.raiderSpam--;
                }
                return "armrock";
            } else if (rand > 0.6) {
                if (economyManager.effectiveIncome < 30) {
                    fac.raiderSpam -= 3;
                }else{
                    fac.raiderSpam -= 2;
                }
                return "armzeus";
            } else {
                if (economyManager.effectiveIncome < 30) {
                    fac.raiderSpam -= 2;
                }else{
                    fac.raiderSpam -= 1;
                }
                return "armwar";
            }
        } else {
            if (numRockos < 2 * (numWarriors + numZeus)) {
                return "armrock";
            } else if (rand > 0.6) {
                fac.raiderSpam -= 2;
                return "armzeus";
            } else if (rand > 0.2) {
                fac.raiderSpam -= 1;
                return "armwar";
            } else {
                fac.raiderSpam -= 3;
                return "armsnipe";
            }
        }
    }

    private String getShields(Factory fac) {
        if (needWorkers(fac)) {
            return "cornecro";
        }

        if (warManager.slasherSpam * 140 > assaultValue && warManager.slasherSpam * 140 > warManager.enemyPorcValue){
            return "corthud";
        }

        if (fac.raiderSpam < 0){
            double rand = Math.random();
            if (warManager.raiderHandler.soloRaiders.size() < 2 || economyManager.effectiveIncome < 15 || rand > 0.35){
                if (warManager.raiderHandler.soloRaiders.size() > 1) {
                    fac.raiderSpam++;
                }
                return "corak";
            }else{
                fac.raiderSpam++;
                return "corstorm";
            }
        }

        if (((enemyHasAir || enemyHasDrones) && warManager.AAs.size() < 4) ||
                (enemyHasAir && Math.random() > 0.60 + (0.075 * ((float) factories.size()-1f)))){
            return "corcrash";
        }

        fac.raiderSpam--;

        if (numFelons < Math.min((economyManager.adjustedIncome/15)-2, 3) && numFelons * 4 < numThugs && Math.random() > 0.5){
            fac.raiderSpam--;
            return "shieldfelon";
        }

        if (economyManager.adjustedIncome > 45 && economyManager.energy > 100 && numAspis == 0 && Math.random() > 0.75){
            fac.raiderSpam--;
            return "core_spectre";
        }

        if (economyManager.adjustedIncome > 20 && Math.random() > 0.9){
            return "corroach";
        }

        if (economyManager.adjustedIncome > 20 && numLaws > 0 && numThugs > 1 && numRackets * 3 < numThugs + numLaws) {
            fac.raiderSpam--;
            return "shieldarty";
        }else{
            if (numLaws == 0 || numThugs > numLaws * 2) {
                return "cormak";
            }else {
                return "corthud";
            }
        }
    }

    private String getAmphs(Factory fac) {
        if (needWorkers(fac)) {
            return "amphcon";
        }

        if (warManager.slasherSpam * 140 > assaultValue){
            return "amphfloater";
        }

        if (fac.raiderSpam < 0) {
            if (Math.random() > 0.1) {
                if (warManager.raiderHandler.soloRaiders.size() > 1) {
                    fac.raiderSpam++;
                }
                return "amphraider3";
            }else{
                fac.raiderSpam++;
                return "amphraider2";
            }
        }

        if (((enemyHasAir || enemyHasDrones) && warManager.AAs.size() < 2) ||
                (enemyHasAir && Math.random() > 0.80 + (0.05 * ((float) factories.size()-(1f + ai.mergedAllies))))){
            return "amphaa";
        }

        if (bigMap && economyManager.adjustedIncome < 25) {
            fac.raiderSpam -= 3;
        }else{
            fac.raiderSpam -= 1;
        }
        double rand = Math.random();
        if (economyManager.adjustedIncome < 35) {
            if (rand > 0.20) {
                return "amphfloater";
            } else {
                return "amphriot";
            }
        }else{
            if (rand > 0.25) {
                return "amphfloater";
            } else if (rand > 0.1) {
                return "amphriot";
            } else {
                fac.raiderSpam -= 2;
                return "amphassault";
            }
        }

    }

    private String getLV(Factory fac) {
        if (earlyWorker && needWorkers(fac)) {
            return "corned";
        }

        if (warManager.slasherSpam * 140 > assaultValue && warManager.slasherSpam * 140 > warManager.enemyPorcValue){
            return "corraid";
        }

        if (fac.raiderSpam < 0) {
            if ((economyManager.adjustedIncome > 15 && Math.random() > 0.7) || numScouts == 0 || (bigMap && numScouts < 2)){
                return "corfav";
            }
            fac.raiderSpam++;
            return "corgator";
        }

        if (needWorkers(fac)) {
            return "corned";
        }

        if (((enemyHasAir || enemyHasDrones) && warManager.AAs.size() < 3) ||
                (enemyHasAir && Math.random() > 0.70 + (0.05 * ((float) factories.size()-(1f + ai.mergedAllies))))){
            return "vehaa";
        }

        if (((!bigMap && economyManager.adjustedIncome > 30) || economyManager.adjustedIncome > 60)
                && warManager.enemyPorcValue > artyValue && Math.random() > 0.65){
            if (enemyHasHeavyPorc && Math.random() > 0.9) {
                return "armmerl";
            }else{
                return "corgarp";
            }
        }

        if (Math.random() > 0.85){
            fac.raiderSpam -= Math.min(8f, Math.max(4f, Math.floor(economyManager.adjustedIncome / 4f))) + 1;
        }

        double rand = Math.random();
        if (economyManager.effectiveIncome < 35) {
            if (rand > 0.1) {
                if (numRavagers > numLevelers * 3 || numLevelers == 0) {
                    return "corlevlr";
                }
                return "corraid";
            } else {
                //fac.raiderSpam++;
                return "cormist";
            }
        }else{
            if (rand > 0.35) {
                if (numRavagers > numLevelers * 3 || numLevelers == 0) {
                    return "corlevlr";
                }
                return "corraid";
            } else {
                return "corgarp";
            }
        }

    }

    private String getHovers(Factory fac) {
        if (needWorkers(fac)) {
            return "corch";
        }

        if (warManager.slasherSpam * 140 > assaultValue && warManager.slasherSpam * 140 > warManager.enemyPorcValue){
            return "hoverassault";
        }

        if (fac.raiderSpam < 0) {
            if (fac.expensiveRaiderSpam < 0 && warManager.raiderHandler.soloRaiders.size() > 1){
                fac.expensiveRaiderSpam++;
                return "hoverassault";
            }
            if ((economyManager.effectiveIncome > 15 && numScouts > 1 && Math.random() > 0.85)) {
                fac.expensiveRaiderSpam -= (int) Math.min(6, Math.max(2, Math.floor(economyManager.adjustedIncome / 10))) - 1;
                return "hoverassault";
            } else {
                if (warManager.raiderHandler.soloRaiders.size() > 1) {
                    fac.raiderSpam++;
                }
                return "corsh";
            }
        }

        if (((enemyHasAir || enemyHasDrones) && warManager.AAs.size() < 3) ||
                (enemyHasAir && Math.random() > 0.70 + (0.05 * ((float) factories.size() - 1f)))) {
            return "hoveraa";
        }

        if (((!bigMap && economyManager.adjustedIncome > 30) || economyManager.adjustedIncome > 60)
                && warManager.enemyPorcValue > artyValue && Math.random() > 0.85){
            return "armmanni";
        }

        if (numScalpels > numMaces * 4 && numScalpels > 1){
            if (economyManager.effectiveIncome < 30) {
                fac.raiderSpam -= 3;
            }else{
                fac.raiderSpam -= 2;
            }
            return "hoverriot";
        }

        if (bigMap && economyManager.effectiveIncome < 30) {
            fac.raiderSpam--;
        }
        if (economyManager.effectiveIncome > 30 && Math.random() > 0.95){
            return "armmanni";
        }
        return "nsaclash";
    }

    private String getTanks(Factory fac) {
        if (needWorkers(fac) || (fac.raiderSpam >= 0 && numWorkers < 2)) {
            return "coracv";
        }

        if (warManager.slasherSpam * 140 > warManager.enemyPorcValue || warManager.slasherSpam * 560 > assaultValue){
            fac.raiderSpam--;
            return "correap";
        }

        if (fac.raiderSpam < 0) {
            if (economyManager.adjustedIncome > 20 && Math.random() > 0.3){
                fac.raiderSpam++;
                return "panther";
            }else {
                if (economyManager.adjustedIncome < 20) {
                    fac.raiderSpam += 3;
                }else{
                    fac.raiderSpam++;
                }
                return "logkoda";
            }
        }

        if (((enemyHasAir || enemyHasDrones) && warManager.AAs.size() < 3) ||
                (enemyHasAir && Math.random() > 0.70 + (0.05 * ((float) factories.size()-(1f + ai.mergedAllies))))){
            return "corsent";
        }

        if (((!bigMap && economyManager.adjustedIncome > 30) || economyManager.adjustedIncome > 60)
                && enemyHasHeavyPorc && warManager.enemyPorcValue > artyValue && Math.random() > 0.5){
            return "cormart";
        }

        fac.raiderSpam -= 2;
        double rand = Math.random();
        if (economyManager.adjustedIncome < 35) {
            if (numBanishers * 2 > numReapers) {
                fac.raiderSpam--;
                return "correap";
            } else {
                return "tawf114";
            }
        }else{
            if (rand > 0.1) {
                if (numBanishers * 3 > numReapers){
                    fac.raiderSpam--;
                    return "correap";
                }else {
                    return "tawf114";
                }
            }else {
                fac.raiderSpam -= 2;
                return "corgol";
            }
        }
    }

    private String getSpiders(Factory fac) {
        if (needWorkers(fac)) {
            return "arm_spider";
        }

        if (warManager.slasherSpam * 140 > assaultValue){
            if (Math.random() > 0.5) {
                return "spiderassault";
            }else{
                return "armsptk";
            }
        }

        if (fac.raiderSpam < 0) {
            if (Math.random() > 0.5) {
                fac.raiderSpam++;
            }
            return "armflea";
        }

        if (((enemyHasAir || enemyHasDrones) && warManager.AAs.size() < 3) ||
                (enemyHasAir && Math.random() > 0.80 + (0.05 * ((float) factories.size()-(1f + ai.mergedAllies))))){
            return "spideraa";
        }

        if (((!bigMap && economyManager.adjustedIncome > 30) || economyManager.adjustedIncome > 60)
                && warManager.enemyPorcValue > artyValue && Math.random() > 0.75){
            fac.raiderSpam -= 4;
            return "armcrabe";
        }

        fac.raiderSpam -= 2;
        if (numRedbacks < numVenoms){
            return "spiderriot";
        }

        if (numRecluses < (numVenoms + numRedbacks)){
            return "armsptk";
        }

        if (numHermits < (numVenoms + numRedbacks)){
            return "spiderassault";
        }

        double rand = Math.random();
        if (rand > 0.9 || numVenoms == 0) {
            return "arm_venom";
        } else{
            return "armsptk";
        }
    }

    private String getGunship(Factory fac){
        if (fac.raiderSpam < 0){
            /*if (Math.random() > 0.8 || economyManager.effectiveIncome < 15) {
                fac.raiderSpam++;
                return "armkam";
            }*/
            if (Math.random() > 0.5){
                fac.raiderSpam++;
            }
            return "blastwing";
        }

        if(needWorkers(fac)) {
            return "gunshipcon";
        }

        if (((enemyHasAir || enemyHasDrones) && warManager.AAs.size() < 3) ||
                (enemyHasAir && Math.random() > 0.35)){
            if (Math.random() > 0.5) {
                return "gunshipaa";
            }
            return "gunshipsupport";
        }

        fac.raiderSpam--;
        if (economyManager.adjustedIncome < 30f){
            return "gunshipsupport";
        }
        double rand = Math.random();
        if (rand > 0.4){
            return "gunshipsupport";
        }else if (rand > 0.075) {
            return "armbrawl";
        }else{
            return "blackdawn";
        }
    }

    private String getPlanes(Factory fac){
        // note: planes do not have raiders and lichos are best AA.
        if(needWorkers(fac)) {
            return "armca";
        }

        if (fac.raiderSpam < 0){
            fac.raiderSpam++;
            return "fighter";
        }

        if (economyManager.adjustedIncome > 35) {
            fac.raiderSpam--;
        }
        if (numRavens <= (economyManager.baseIncome/(12 * (1 + (0.5 * ai.mergedAllies)))) * 1.5f || economyManager.adjustedIncome < 35 || Math.random() > 0.35) {
            if (Math.random() > 0.2) {
                return "corshad";
            }
            return "corhurc2";
        }
        fac.raiderSpam -= 3;
        return "armcybr";
    }

    private String getStrider(){
        if (economyManager.adjustedIncome > 60 && warManager.miscHandler.striders.size() > 1 && Math.random() > 0.5){
            if (Math.random() > 0.25 || warManager.miscHandler.striders.size() < 5) {
                return "armbanth";
            }else{
                return "armorco";
            }
        }

        double rand = Math.random();
        if (warManager.squadHandler.nextShieldSquad != null && (ai.mergedAllies == 0 || rand > 0.5)){
            return "funnelweb";
        }

        if(rand > 0.75){
            return  "scorpion";
        }else{
            return "dante";
        }
    }
}
