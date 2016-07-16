package zkgbai.economy;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
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
    private OOAICallback callback;
    UnitClasses unitTypes;

    public Map<Integer, Factory> factories;

    int frame = 0;

    boolean smallMap = false;
    boolean bigMap = false;

    boolean earlyWorker = false;

    boolean enemyHasAir = false;
    boolean enemyHasAmphs = false;

    public int numWorkers = 0;
    float workerValue = 0;
    float fighterValue = 0;
    float artyValue = 0;
    float assaultValue = 0;
    int numSupports = 0;

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

    int numFelons = 0;
    int numThugs = 0;
    int numLaws = 0;
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
        this.bigMap = (callback.getMap().getHeight() + callback.getMap().getWidth() > 1280);

        this.earlyWorker = bigMap;
        this.m = callback.getResourceByName("Metal");
    }

    @Override
    public int init(int AIID, OOAICallback cb){
        this.warManager = ai.warManager;
        this.economyManager = ai.ecoManager;
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
        String defName = unit.getDef().getName();
        if(defName.contains("factory") || defName.contains("hub")){
            Factory fac;
            if (factories.size() == 0) {
                fac = new Factory(unit, true);
            }else{
                fac = new Factory(unit, false);
            }
            if (defName.equals("factoryspider") || defName.equals("factoryamph")){
                earlyWorker = true;
            }
            factories.put(fac.id, fac);
            assignFactoryTask(fac);
            unit.setMoveState(2, (short) 0, frame+300);
        }

        if (defName.equals("armfus") || defName.equals("cafus")){
            fighterValue += 250f;
        }

        return 0;
    }

    @Override
    public int unitFinished(Unit unit) {
        UnitDef def = unit.getDef();
        String defName = def.getName();

        if (def.isBuilder() && unit.getMaxSpeed() > 0){
            numWorkers++;
            if (def.getBuildSpeed() < 8f) {
                workerValue += unit.getDef().getCost(m);
            }
        }else if (unit.getMaxSpeed() > 0 && !unitTypes.AAs.contains(defName) && !unitTypes.striders.contains(defName)){
            fighterValue += Math.min(unit.getDef().getCost(m), 500f);
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

        if (defName.equals("corshad")){
            numRavens++;
        }

        if (defName.equals("spherecloaker") || defName.equals("core_spectre")){
            numSupports++;
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
        }else if (unit.getMaxSpeed() > 0 && !unitTypes.AAs.contains(defName) && !unitTypes.striders.contains(defName)){
            fighterValue -= Math.min(unit.getDef().getCost(m), 500f);
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

        if (defName.equals("spherecloaker") || defName.equals("core_spectre")){
            numSupports--;
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

        if (defName.equals("corshad")){
            numRavens--;
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
            String defName = unit.getDef().getName();
            if(defName.contains("factory") || defName.contains("hub")){
                Factory fac = new Factory(unit, false);
                factories.put(fac.id, fac);
                assignFactoryTask(fac);
                unit.setMoveState(2, (short) 0, frame+300);
            }
        }
        return 0;
    }

    @Override
    public int unitIdle(Unit unit) {
        if (factories.containsKey(unit.getUnitId())){
            Factory fac = factories.get(unit.getUnitId());
            assignFactoryTask(fac);
        }

        return 0; // signaling: OK
    }

    @Override
    public int enemyEnterLOS(Unit unit){
        if (unit.getDef().isAbleToFly()){
            enemyHasAir = true;
        }

        if (unit.getDef().getTooltip().contains("Amph")){
            enemyHasAmphs = true;
        }
        return 0;
    }

    void assignFactoryTask(Factory fac){
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
        float metalratio = 4.0f;
        float workerRatio = 1f + (economyManager.effectiveIncome/100f);

        if (facType.equals("factorytank") || facType.equals("factoryamph") || facType.equals("factoryspider")){
            workerRatio = 1f + (economyManager.effectiveIncome/300f);
        }

        if (facType.equals("factorytank") || facType.equals("factoryship") || facType.equals("factoryplane") || facType.equals("factorygunship")){
            metalratio = 5f;
        }else if (facType.equals("factoryspider")){
            metalratio = 5f;
        }

        if ((float) numWorkers < Math.floor(economyManager.effectiveIncomeMetal/metalratio) + ((warManager.miscHandler.striders.size() + economyManager.fusions.size() + numFunnels + factories.size() - 1) * 3)
                && (fighterValue > workerValue * workerRatio || numWorkers == 0 || (economyManager.metal > 400 && economyManager.energy > 100))
                || (earlyWorker && numWorkers < 2)
                || (fac.getUnit().getDef().getName().equals("factoryshield") && !smallMap && numWorkers < 2)) {
            return true;
        }else if (fac.raiderSpam >= 0 && numWorkers < 2
                || (fac.raiderSpam >= 0 && earlyWorker && facType.equals("factorytank") && numWorkers < 3)){
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
            if ((economyManager.effectiveIncome > 15 && numGlaives > 1 && Math.random() > 0.9)) {
                fac.expensiveRaiderSpam -= (int) Math.min(4, Math.max(2, Math.floor(economyManager.effectiveIncome / 10))) - 1;
                return "spherepole";
            } else {
                if (warManager.raiderHandler.soloRaiders.size() > 1) {
                    fac.raiderSpam++;
                }
                return "armpw";
            }
        }

        if (enemyHasAir && (Math.random() > 0.70 + (0.05 * ((float) factories.size()-1f)) || warManager.AAs.size() < 3)){
            return "armjeth";
        }

        if (!economyManager.fusions.isEmpty() && numSupports == 0 && warManager.squadHandler.squads.size() > 0 && Math.random() > 0.75){
            return "spherecloaker";
        }

        if (economyManager.adjustedIncome > 15 && Math.random() > 0.9){
            return "armtick";
        }

        double rand = Math.random();
        if (economyManager.fusions.isEmpty()){
            if (numRockos < 2 * (numWarriors + numZeus)) {
                fac.raiderSpam--;
                return "armrock";
            } else if (rand > 0.6) {
                fac.raiderSpam -= 3;
                return "armzeus";
            } else {
                fac.raiderSpam -= 2;
                return "armwar";
            }
        } else {
            if (numRockos < 2 * (numWarriors + numZeus)) {
                fac.raiderSpam--;
                return "armrock";
            } else if (rand > 0.6) {
                fac.raiderSpam -= 3;
                return "armzeus";
            } else if (rand > 0.2) {
                fac.raiderSpam -= 2;
                return "armwar";
            } else {
                fac.raiderSpam -= 4;
                return "armsnipe";
            }
        }
    }

    private String getShields(Factory fac) {
        if (needWorkers(fac)) {
            return "cornecro";
        }

        if (warManager.slasherSpam * 140 > assaultValue){
            return "corthud";
        }

        if (fac.raiderSpam < 0){
            double rand = Math.random();
            if (economyManager.effectiveIncome > 15 && rand > 0.5){
                fac.raiderSpam++;
                return "corstorm";
            }else if (rand > 0.1){
                if (warManager.raiderHandler.soloRaiders.size() > 1) {
                    fac.raiderSpam++;
                }
                return "corak";
            }
            return "corclog";
        }

        if (enemyHasAir && (Math.random() > 0.60 + (0.075 * ((float) factories.size()-1f)) || warManager.AAs.size() < 4)){
            return "corcrash";
        }

        fac.raiderSpam--;

        if (numFelons < Math.min((economyManager.adjustedIncome/15)-2, 3) && numFelons * 4 < numThugs && Math.random() > 0.5){
            fac.raiderSpam--;
            return "shieldfelon";
        }

        if (economyManager.adjustedIncome > 45 && economyManager.energy > 100 && numSupports == 0){
            fac.raiderSpam--;
            return "core_spectre";
        }

        if (economyManager.adjustedIncome > 20 && Math.random() > 0.9){
            return "corroach";
        }

        double rand = Math.random();
        if (economyManager.adjustedIncome < 20) {
            if (numLaws == 0 || numThugs > numLaws) {
                return "cormak";
            }else {
                return "corthud";
            }
        }else {
            if (rand > 0.3) {
                if (numLaws == 0 || numThugs > numLaws) {
                    return "cormak";
                }else {
                    return "corthud";
                }
            }else{
                return "shieldarty";
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

        if (enemyHasAir && (Math.random() > 0.80 + (0.05 * ((float) factories.size()-1f)) || warManager.AAs.size() < 2)){
            return "amphaa";
        }

        if (economyManager.effectiveIncome < 25) {
            fac.raiderSpam -= 3;
        }else{
            fac.raiderSpam -= 1;
        }
        double rand = Math.random();
        if (economyManager.adjustedIncome < 35) {
            if (rand > 0.33) {
                return "amphfloater";
            } else {
                return "amphriot";
            }
        }else{
            if (rand > 0.4) {
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

        if (warManager.slasherSpam * 140 > assaultValue){
            return "corraid";
        }

        if (fac.raiderSpam < 0) {
            if ((economyManager.effectiveIncome > 15 && Math.random() > 0.7) || numScouts == 0 || (bigMap && numScouts < 2)){
                return "corfav";
            }
            fac.raiderSpam++;
            return "corgator";
        }

        if (!earlyWorker && needWorkers(fac)) {
            return "corned";
        }

        if (enemyHasAir && (Math.random() > 0.70 + (0.05 * ((float) factories.size()-1f)) || warManager.AAs.size() < 3)){
            return "vehaa";
        }

        if (((!bigMap && economyManager.adjustedIncome > 30) || economyManager.adjustedIncome > 60)
                && warManager.enemyPorcValue > artyValue && Math.random() > 0.75){
            return "armmerl";
        }

        if (bigMap) {
            if (economyManager.adjustedIncome < 50) {
                fac.raiderSpam -= 2;
            } else {
                fac.raiderSpam--;
            }
        }else{
            if (economyManager.adjustedIncome < 30) {
                fac.raiderSpam -= 2;
            } else {
                fac.raiderSpam--;
            }
        }

        double rand = Math.random();
        if (economyManager.effectiveIncome < 35) {
            if (rand > 0.3) {
                if (numRavagers > numLevelers * 3) {
                    return "corlevlr";
                }
                return "corraid";
            } else {
                fac.raiderSpam++;
                return "cormist";
            }
        }else{
            if (rand > 0.35) {
                if (numRavagers > numLevelers * 3) {
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

        if (warManager.slasherSpam * 280 > assaultValue){
            return "hoverassault";
        }

        if (fac.raiderSpam < 0) {
            if (fac.expensiveRaiderSpam < 0){
                fac.expensiveRaiderSpam++;
                return "hoverassault";
            }
            if ((economyManager.effectiveIncome > 15 && numScouts > 1 && Math.random() > 0.85)) {
                fac.expensiveRaiderSpam -= (int) Math.min(4, Math.max(2, Math.floor(economyManager.effectiveIncome / 10))) - 1;
                return "hoverassault";
            } else {
                if (warManager.raiderHandler.soloRaiders.size() > 1) {
                    fac.raiderSpam++;
                }
                return "corsh";
            }
        }

        if (enemyHasAir && (Math.random() > 0.70 + (0.05 * ((float) factories.size() - 1f)) || warManager.AAs.size() < 3)) {
            return "hoveraa";
        }

        if (((!bigMap && economyManager.adjustedIncome > 30) || economyManager.adjustedIncome > 60)
                && warManager.enemyPorcValue > artyValue && Math.random() > 0.75){
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

        fac.raiderSpam--;
        return "nsaclash";
    }

    private String getTanks(Factory fac) {
        if (needWorkers(fac)) {
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

        if (enemyHasAir && (Math.random() > 0.70 + (0.05 * ((float) factories.size()-1f)) || warManager.AAs.size() < 3)){
            return "corsent";
        }

        if (((!bigMap && economyManager.adjustedIncome > 30) || economyManager.adjustedIncome > 60)
                && warManager.enemyPorcValue > artyValue && Math.random() > 0.5){
            if (Math.random() > 0.25) {
                return "cormart";
            }else{
                return "trem";
            }
        }

        fac.raiderSpam -= 2;
        double rand = Math.random();
        if (economyManager.adjustedIncome < 35) {
            if (numBanishers > numReapers) {
                fac.raiderSpam--;
                return "correap";
            } else {
                return "tawf114";
            }
        }else{
            if (rand > 0.1) {
                if (numBanishers > numReapers){
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

        if (enemyHasAir && (Math.random() > 0.80 + (0.05 * ((float) factories.size()-1f)) || warManager.AAs.size() < 3)){
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
        // if air start, never ever spam banshees and blastwings
        if (fac.raiderSpam < -3){
            fac.raiderSpam = 0;
        }

        if (fac.raiderSpam < 0){
            if (Math.random() > 0.8) {
                fac.raiderSpam++;
                return "armkam";
            }
            return "blastwing";
        }

        if(needWorkers(fac)) {
            return "gunshipcon";
        }

        if (enemyHasAir && (Math.random() > 0.70 + (0.05 * ((float) factories.size()-1f)) || warManager.AAs.size() < 3)){
            return "gunshipaa";
        }

        fac.raiderSpam--;
        double rand = Math.random();
        if (rand > 0.60){
            return "gunshipsupport";
        }else if (rand > 0.01) {
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

        if (numRavens <= economyManager.effectiveIncome/12 || economyManager.adjustedIncome < 35) {
            return "corshad";
        }
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
        if (warManager.squadHandler.nextShieldSquad != null){
            return "funnelweb";
        }

        if(rand > 0.75){
            return  "scorpion";
        }else{
            return "dante";
        }
    }
}
