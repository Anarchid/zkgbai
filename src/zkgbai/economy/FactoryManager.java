package zkgbai.economy;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
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

    boolean enemyHasAir = false;
    boolean enemyHasAmphs = false;

    public int numWorkers = 0;
    int numFighters = 0;
    int numSupports = 0;

    int numWarriors = 0;
    int numZeus = 0;
    int numRockos = 0;
    int numGlaives = 0;
    int numScythes = 0;

    int numMaces = 0;
    int numScalpels = 0;

    int numLevelers = 0;
    int numRavagers = 0;

    int numDarts = 0;

    int numFelons = 0;
    int numThugs = 0;
    int numLaws = 0;
    int numFunnels = 0;

    int numVenoms = 0;
    int numRedbacks = 0;

    int numRavens = 0;

    static int CMD_PRIORITY = 34220;

    public FactoryManager(ZKGraphBasedAI ai){
        this.ai = ai;
        this.callback = ai.getCallback();
        this.factories = new HashMap<Integer, Factory>();

        this.smallMap = (callback.getMap().getWidth() < 768 && callback.getMap().getHeight() < 768);
        this.bigMap = (callback.getMap().getHeight() > 896 || callback.getMap().getWidth() > 896);
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
            factories.put(fac.id, fac);
            assignFactoryTask(fac);
            unit.setMoveState(2, (short) 0, frame+300);
        }

        return 0;
    }

    @Override
    public int unitFinished(Unit unit) {
        UnitDef def = unit.getDef();
        String defName = def.getName();

        if (def.isBuilder() && unit.getMaxSpeed() > 0){
            numWorkers++;
        }else if (unit.getMaxSpeed() > 0 && !unit.getDef().getTooltip().contains("Anti-Air")){
            numFighters++;
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

        if (defName.equals("hoverriot")){
            numMaces++;
        }

        if (defName.equals("nsaclash")){
            numScalpels++;
        }

        if (defName.equals("corfav")){
            numDarts++;
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

        if (unit.getMaxSpeed() > 0 && unit.getDef().isAbleToAttack() && !unit.getDef().getTooltip().contains("Anti-Air")){
            numFighters--;
        }

        if (unit.getDef().isBuilder() && unit.getMaxSpeed() > 0){
            numWorkers--;
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

        if (defName.equals("hoverriot")){
            numMaces--;
        }

        if (defName.equals("nsaclash")){
            numScalpels--;
        }

        if (defName.equals("corfav")){
            numDarts--;
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
        if ((float) numWorkers < Math.floor(economyManager.effectiveIncomeMetal/3.5) + ((warManager.miscHandler.striders.size() + economyManager.fusions.size() + numFunnels + factories.size() - 1) * 3)
                && (numFighters > numWorkers || numWorkers == 0)) {
            return true;
        }else if (fac.raiderSpam >= 0 && numWorkers < 2){
            return true;
        }
        return false;
    }

    private String getCloaky(Factory fac) {
        if (needWorkers(fac)) {
            return "armrectr";
        }

        if (fac.raiderSpam < 0) {
            if ((numGlaives > 1 && numGlaives > numScythes * 4)) {
                fac.raiderSpam++;
                return "spherepole";
            } else {
                if (economyManager.effectiveIncome < 15) {
                    fac.raiderSpam++;
                }
                return "armpw";
            }
        }

        if (enemyHasAir && (Math.random() > 0.70 + (0.05 * ((float) factories.size()-1f)) || warManager.AAs.size() < 3)){
            return "armjeth";
        }

        if (economyManager.adjustedIncome > 45 && economyManager.energy > 100 && numSupports == 0 && warManager.squadHandler.squads.size() > 0 && Math.random() > 0.75){
            return "spherecloaker";
        }

        if (economyManager.adjustedIncome > 15 && Math.random() > 0.9){
            return "armtick";
        }

        if (economyManager.effectiveIncome < 30) {
            fac.raiderSpam--;
        }else{
            fac.raiderSpam--;
        }
        double rand = Math.random();
        if (economyManager.effectiveIncome < 30){
            if (numRockos < 2 * (numWarriors + numZeus)) {
                fac.raiderSpam++;
                return "armrock";
            } else if (rand > 0.15) {
                return "armzeus";
            } else {
                return "armwar";
            }
        } else {
            if (numRockos < 2 * (numWarriors + numZeus)) {
                fac.raiderSpam++;
                return "armrock";
            } else if (rand > 0.45) {
                return "armzeus";
            } else if (rand > 0.3) {
                return "armwar";
            } else {
                return "armsnipe";
            }
        }
    }

    private String getShields(Factory fac) {
        if (needWorkers(fac)) {
            return "cornecro";
        }

        if (fac.raiderSpam < 0){
            double rand = Math.random();
            if (economyManager.effectiveIncome > 15 && rand > 0.5){
                fac.raiderSpam += 2;
                return "corstorm";
            }else if (rand > 0.1){
                fac.raiderSpam++;
                return "corak";
            }
            return "corclog";
        }

        if (enemyHasAir && (Math.random() > 0.60 + (0.075 * ((float) factories.size()-1f)) || warManager.AAs.size() < 4)){
            return "corcrash";
        }

        fac.raiderSpam -= 3;

        if (numFelons < Math.min((economyManager.adjustedIncome/15)-2, 3) && numFelons * 4 < numThugs && Math.random() > 0.5){
            return "shieldfelon";
        }

        if (economyManager.adjustedIncome > 45 && economyManager.energy > 100 && numSupports == 0){
            return "core_spectre";
        }

        if (economyManager.adjustedIncome > 20 && Math.random() > 0.9){
            return "corroach";
        }

        double rand = Math.random();
        if (economyManager.adjustedIncome < 20) {
            if (numLaws == 0 || numThugs > 2 * numLaws) {
                return "cormak";
            }else {
                return "corthud";
            }
        }else {
            if (rand > 0.3) {
                if (numLaws == 0 || numThugs > 2 * numLaws) {
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

        if (fac.raiderSpam < 0) {
            fac.raiderSpam++;
            if (Math.random() > 0.1) {
                return "amphraider3";
            }else{
                return "amphraider2";
            }
        }

        if (enemyHasAir && (Math.random() > 0.80 + (0.05 * ((float) factories.size()-1f)) || warManager.AAs.size() < 2)){
            return "amphaa";
        }

        fac.raiderSpam -= 2;
        double rand = Math.random();
        if (economyManager.adjustedIncome < 35) {
            if (rand > 0.50) {
                return "amphfloater";
            } else {
                return "amphriot";
            }
        }else{
            if (rand > 0.5) {
                return "amphfloater";
            } else if (rand > 0.05) {
                return "amphriot";
            } else {
                return "amphassault";
            }
        }

    }

    private String getLV(Factory fac) {
        if (fac.raiderSpam < 0) {
            if ((economyManager.effectiveIncome > 15 && Math.random() > 0.7) || numDarts == 0){
                return "corfav";
            }
            fac.raiderSpam++;
            return "corgator";
        }

        if (needWorkers(fac)) {
            return "corned";
        }

        if (enemyHasAir && (Math.random() > 0.70 + (0.05 * ((float) factories.size()-1f)) || warManager.AAs.size() < 3)){
            return "vehaa";
        }

        if (economyManager.effectiveIncome < 35) {
            fac.raiderSpam -= 2;
        }else{
            fac.raiderSpam--;
        }
        double rand = Math.random();
        if (economyManager.effectiveIncome < 35) {
            if (rand > 0.3) {
                if (numRavagers > numLevelers * 2) {
                    return "corlevlr";
                }
                return "corraid";
            } else {
                //fac.raiderSpam++;
                return "cormist";
            }
        }else{
            if (rand > 0.35) {
                if (numRavagers > numLevelers * 2) {
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

        if (fac.raiderSpam < 0) {
            if (Math.random() > 0.5) {
                fac.raiderSpam++;
                return "hoverassault";
            }
            return "corsh";
        }

        if (enemyHasAir && (Math.random() > 0.70 + (0.05 * ((float) factories.size() - 1f)) || warManager.AAs.size() < 3)) {
            return "hoveraa";
        }

        fac.raiderSpam -= 3;
        if (numScalpels >= numMaces * 3 && numScalpels >= 2){
            return "hoverriot";
        }

        double rand = Math.random();
        if (economyManager.adjustedIncome < 35) {
            fac.raiderSpam += 3;
            return "nsaclash";
        } else {
            if (rand > 0.2) {
                fac.raiderSpam += 3;
                return "nsaclash";
            } else {
                return "armmanni";
            }
        }
    }

    private String getTanks(Factory fac) {
        if (needWorkers(fac)) {
            return "coracv";
        }

        if (fac.raiderSpam < 0) {
            if (economyManager.adjustedIncome > 20 && Math.random() > 0.5){
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

        fac.raiderSpam -= 3;
        double rand = Math.random();
        if (economyManager.adjustedIncome < 35) {
            if (rand > 0.35) {
                return "correap";
            } else {
                return "tawf114";
            }
        }else{
            if (rand > 0.40) {
                return "correap";
            } else if (rand > 0.1) {
                return "tawf114";
            }else {
                return "corgol";
            }
        }
    }

    private String getSpiders(Factory fac) {
        if (needWorkers(fac)) {
            return "arm_spider";
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

        fac.raiderSpam--;
        if (numVenoms == 0){
            return "arm_venom";
        }
        if (numRedbacks == 0){
            return "spiderriot";
        }

        double rand = Math.random();
        if (rand > 0.5) {
            return "spiderassault";
        } else if (rand > 0.25) {
            return "armsptk";
        } else if (rand > 0.2){
            return "spiderriot";
        } else if (rand > 0.10){
            return "arm_venom";
        } else {
            return "armcrabe";
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
            return "armca";
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
