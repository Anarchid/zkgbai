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
import java.util.List;

/**
 * Created by aeonios on 12/8/2015.
 */
public class FactoryManager extends Module {
    ZKGraphBasedAI ai;
    EconomyManager economyManager;
    MilitaryManager warManager;
    private OOAICallback callback;
    UnitClasses unitTypes;

    public List<Worker> factories;

    int frame = 0;

    boolean smallMap = false;

    boolean enemyHasAir = false;

    public int numWorkers = 0;
    int numFighters = 0;
    int numSupports = 0;
    int raiderSpam = 0;

    int numWarriors = 0;
    int numZeus = 0;
    int numRockos = 0;

    int numFelons = 0;
    int numThugs = 0;
    int numLaws = 0;
    int numFunnels = 0;

    int numVenoms = 0;
    int numRedbacks = 0;

    int numRavens = 0;

    public FactoryManager(ZKGraphBasedAI ai){
        this.ai = ai;
        this.callback = ai.getCallback();
        this.factories = new ArrayList<Worker>();

        this.raiderSpam = ((int) Math.ceil(Math.random()*9.0));
        raiderSpam = Math.max(4, raiderSpam)* -1;

        if (callback.getMap().getHeight() > 896 || callback.getMap().getWidth() > 896){
            raiderSpam = -9;
        }

        if (callback.getMap().getHeight() < 640 && callback.getMap().getWidth() < 640){
            raiderSpam /= 2;
        }

        this.smallMap = (callback.getMap().getWidth() < 768 && callback.getMap().getHeight() < 768);
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
            Worker fac = new Worker(unit);
            factories.add(fac);
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

        if (defName.equals("corshad")){
            numRavens--;
        }

        Worker deadWorker = null;
        for (Worker fac : factories) {
            if (fac.id == unit.getUnitId()) {
                deadWorker = fac;
            }
        }
        if (deadWorker != null){
            factories.remove(deadWorker);
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
                Worker fac = new Worker(unit);
                factories.add(fac);
                assignFactoryTask(fac);
                unit.setMoveState(2, (short) 0, frame+300);
            }
        }
        return 0;
    }

    @Override
    public int unitIdle(Unit unit) {
        for (Worker f: factories){
            if (f.id == unit.getUnitId()){
                assignFactoryTask(f);
            }
        }
        return 0; // signaling: OK
    }

    @Override
    public int enemyEnterLOS(Unit unit){
        if (unit.getDef().isAbleToFly()){
            enemyHasAir = true;
        }
        return 0;
    }

    void assignFactoryTask(Worker fac){
        String defName = fac.getUnit().getDef().getName();
        UnitDef unit;
        if (defName.equals("factorycloak")){
            unit = callback.getUnitDefByName(getCloaky());
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("factoryshield")){
            unit = callback.getUnitDefByName(getShields());
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("factorygunship")){
            unit = callback.getUnitDefByName(getGunship());
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("factoryplane")){
            unit = callback.getUnitDefByName(getPlanes());
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("factoryamph")){
            unit = callback.getUnitDefByName(getAmphs());
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("factoryveh")){
            unit = callback.getUnitDefByName(getLV());
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("factoryhover")){
            unit = callback.getUnitDefByName(getHovers());
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("factorytank")){
            unit = callback.getUnitDefByName(getTanks());
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("factoryspider")){
            unit = callback.getUnitDefByName(getSpiders());
            fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 3000);
        }else if (defName.equals("striderhub")){
            unit = callback.getUnitDefByName(getStrider());
            AIFloat3 pos = callback.getMap().findClosestBuildSite(unit, fac.getPos(), 250f, 3, 0);
            fac.getUnit().build(unit, pos, (short) 0, (short) 0, frame+3000);
        }

    }

    private Boolean needWorkers(){
        if ((float) numWorkers-1 < Math.floor(economyManager.effectiveIncomeMetal/5) + ((warManager.miscHandler.striders.size() + economyManager.fusions.size() + numFunnels + factories.size() - 1) * 4)
                && (numFighters > numWorkers || numWorkers == 0)
                && (economyManager.effectiveIncome > 9 || numWorkers == 0)) {
            return true;
        }
        return false;
    }

    private String getCloaky() {
        if (needWorkers()) {
            return "armrectr";
        }

        if (raiderSpam < 0) {
            if (economyManager.effectiveIncome > 15 && Math.random() > 0.8) {
                raiderSpam ++;
                return "spherepole";
            } else {
                if (smallMap || economyManager.effectiveIncome < 15) {
                    raiderSpam++;
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

        raiderSpam -= 2;

        if (numWarriors == 0){
            return "armwar";
        }

        double rand = Math.random();
        if (economyManager.adjustedIncome < 40){
            if (numRockos < 2 * (numWarriors + numZeus)) {
                raiderSpam += 2;
                return "armrock";
            } else if (rand > 0.3) {
                return "armzeus";
            } else {
                return "armwar";
            }
        } else {
            if (numRockos < 2 * (numWarriors + numZeus)) {
                raiderSpam += 2;
                return "armrock";
            } else if (rand > 0.4) {
                return "armzeus";
            } else if (rand > 0.1) {
                return "armwar";
            } else {
                return "armsnipe";
            }
        }
    }

    private String getShields() {
        if (needWorkers()) {
            return "cornecro";
        }

        if (raiderSpam < 0){
            double rand = Math.random();
            if (economyManager.effectiveIncome > 15 && rand > 0.5){
                raiderSpam += 2;
                return "corstorm";
            }else if (rand > 0.1){
                raiderSpam++;
                return "corak";
            }
            return "corclog";
        }

        if (enemyHasAir && (Math.random() > 0.60 + (0.075 * ((float) factories.size()-1f)) || warManager.AAs.size() < 4)){
            return "corcrash";
        }

        raiderSpam -= 3;

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

    private String getAmphs() {
        if (needWorkers()) {
            return "amphcon";
        }

        if (raiderSpam < 0) {
            raiderSpam++;
            if (Math.random() > 0.1) {
                return "amphraider3";
            }else{
                return "amphraider2";
            }
        }

        if (enemyHasAir && (Math.random() > 0.80 + (0.05 * ((float) factories.size()-1f)) || warManager.AAs.size() < 2)){
            return "amphaa";
        }

        raiderSpam -= 2;
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

    private String getLV() {
        if (raiderSpam < -4){
            raiderSpam = -4;
        }

        if (raiderSpam < 0) {
            if (Math.random() > 0.8){
                return "corfav";
            }
            raiderSpam++;
            return "corgator";
        }

        if (needWorkers()) {
            return "corned";
        }

        if (enemyHasAir && (Math.random() > 0.70 + (0.05 * ((float) factories.size()-1f)) || warManager.AAs.size() < 3)){
            return "vehaa";
        }

        raiderSpam -= 2;
        double rand = Math.random();
        if (economyManager.adjustedIncome < 35) {
            if (rand > 0.5) {
                return "corraid";
            } else if (rand > 0.3) {
                return "corlevlr";
            } else {
                return "cormist";
            }
        }else{
            if (rand > 0.5) {
                return "corraid";
            } else if (rand > 0.35) {
                return "corlevlr";
            } else {
                return "corgarp";
            }
        }

    }

    private String getHovers() {
        if (needWorkers()) {
            return "corch";
        }

        /*if (raiderSpam < -4 && callback.getMap().getHeight() < 1024 && callback.getMap().getWidth() < 1024){
            raiderSpam = -4;
        }*/

        if (raiderSpam < 0) {
            if (Math.random() > 0.5) {
                raiderSpam += 2;
                return "hoverassault";
            }

            raiderSpam++;
            return "corsh";
        }

        if (enemyHasAir && (Math.random() > 0.70 + (0.05 * ((float) factories.size() - 1f)) || warManager.AAs.size() < 3)) {
            return "hoveraa";
        }

        raiderSpam -= 3;
        double rand = Math.random();
        if (economyManager.adjustedIncome < 35) {
            if (rand > 0.2) {
                raiderSpam += 3;
                return "nsaclash";
            } else {
                return "hoverriot";
            }
        } else {
            if (rand > 0.35) {
                raiderSpam += 3;
                return "nsaclash";
            } else if (rand > 0.1) {
                return "hoverriot";
            } else {
                return "armmanni";
            }
        }
    }

    private String getTanks() {
        if (needWorkers() || (raiderSpam >= 0 && numWorkers == 1)) {
            return "coracv";
        }

        if (raiderSpam < 0) {
            if (economyManager.adjustedIncome > 20 && Math.random() > 0.5){
                raiderSpam++;
                return "panther";
            }else {
                if (economyManager.adjustedIncome < 20) {
                    raiderSpam += 3;
                }else{
                    raiderSpam++;
                }
                return "logkoda";
            }
        }

        if (enemyHasAir && (Math.random() > 0.70 + (0.05 * ((float) factories.size()-1f)) || warManager.AAs.size() < 3)){
            return "corsent";
        }

        raiderSpam -= 3;
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

    private String getSpiders() {
        if (needWorkers()) {
            return "arm_spider";
        }

        if (raiderSpam < 0) {
            if (Math.random() > 0.5) {
                raiderSpam++;
            }
            return "armflea";
        }

        if (enemyHasAir && (Math.random() > 0.80 + (0.05 * ((float) factories.size()-1f)) || warManager.AAs.size() < 3)){
            return "spideraa";
        }

        raiderSpam--;
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

    private String getGunship(){
        // if air start, never ever spam banshees and blastwings
        if (raiderSpam < -3){
            raiderSpam = 0;
        }

        if (raiderSpam < 0){
            if (Math.random() > 0.8) {
                raiderSpam++;
                return "armkam";
            }
            return "blastwing";
        }

        if(needWorkers()) {
            return "armca";
        }

        if (enemyHasAir && (Math.random() > 0.70 + (0.05 * ((float) factories.size()-1f)) || warManager.AAs.size() < 3)){
            return "gunshipaa";
        }

        raiderSpam--;
        double rand = Math.random();
        if (rand > 0.60){
            return "gunshipsupport";
        }else if (rand > 0.01) {
            return "armbrawl";
        }else{
            return "blackdawn";
        }
    }

    private String getPlanes(){
        // note: planes do not have raiders and lichos are best AA.
        if(needWorkers()) {
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
