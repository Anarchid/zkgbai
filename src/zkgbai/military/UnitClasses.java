package zkgbai.military;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aeonios on 11/14/2015.
 */
public class UnitClasses {
    public List<String> smallRaiders = new ArrayList<String>();
    public List<String> mediumRaiders = new ArrayList<String>();
    public List<String> soloRaiders = new ArrayList<String>();
    public List<String> assaults = new ArrayList<String>();
    public List<String> arties = new ArrayList<String>();
    public List<String> striders = new ArrayList<String>();
    public List<String> airMobs = new ArrayList<String>();
    public List<String> shieldMobs = new ArrayList<String>();
    public List<String> mobSupports = new ArrayList<String>();
    public List<String> loners = new ArrayList<String>();
    public List<String> AAs = new ArrayList<String>();
    public List<String> planes = new ArrayList<String>();
    public List<String> bombers = new ArrayList<String>();
    public List<String> sappers = new ArrayList<String>();

    public List<String> noRetreat = new ArrayList<String>();
    
    public List<String> porcWeps = new ArrayList<String>();

    private static UnitClasses instance = null;

    private UnitClasses(){
        // raiders
        smallRaiders.add("armpw");
        smallRaiders.add("corak");
        smallRaiders.add("amphraider3");
        smallRaiders.add("amphraider2");
        smallRaiders.add("corsh");

        mediumRaiders.add("corgator");
        mediumRaiders.add("armkam");
        mediumRaiders.add("spherepole");
        mediumRaiders.add("hoverassault");
        mediumRaiders.add("panther");
        
        soloRaiders.add("logkoda");
        soloRaiders.add("armflea");
        soloRaiders.add("corfav");
        soloRaiders.add("corstorm");
        soloRaiders.add("corclog");
        soloRaiders.add("blastwing");

        // assaults; stuff that attacks in mobs
        assaults.add("armzeus");
        assaults.add("armwar");
        assaults.add("armrock");
        assaults.add("amphfloater");
        assaults.add("amphriot");
        assaults.add("corlevlr");
        assaults.add("corraid");
        assaults.add("nsaclash");
        assaults.add("hoverriot");
        assaults.add("spiderassault");
        assaults.add("armsptk");
        assaults.add("arm_venom");
        assaults.add("spiderriot");
        assaults.add("tawf114");
        assaults.add("correap");


        // Air mobs
        airMobs.add("gunshipsupport");
        airMobs.add("armbrawl");
        airMobs.add("blackdawn");

        // Shield mobs
        shieldMobs.add("cormak");
        shieldMobs.add("shieldarty");
        shieldMobs.add("corthud");
        shieldMobs.add("shieldfelon");
        shieldMobs.add("funnelweb");
        shieldMobs.add("capturecar");

        // mobSupport: things that increase the strength of mobs
        mobSupports.add("spherecloaker");
        mobSupports.add("core_spectre");

        // striders; stuff that can dgun
        striders.add("dante");
        striders.add("scorpion");
        striders.add("armbanth");
        striders.add("armorco");

        // loners; strider-like stuff that does better on its own than in mobs
        loners.add("cormist");
        loners.add("corgol");
        loners.add("armcrabe");
        
        // arties; stuff that has long range and can attack high threat areas without suiciding
        arties.add("armsnipe");
        arties.add("armham");
        arties.add("amphassault");
        arties.add("corgarp");
        arties.add("armmerl");
        arties.add("armmanni");
        arties.add("cormart");
        arties.add("trem");
        arties.add("armraven");

        // Anti-air units
        AAs.add("armjeth");
        AAs.add("gunshipaa");
        AAs.add("amphaa");
        AAs.add("hoveraa");
        AAs.add("armaak");
        //AAs.add("corvamp");
        //AAs.add("fighter");
        AAs.add("corcrash");
        AAs.add("shipaa");
        AAs.add("spideraa");
        AAs.add("corsent");
        AAs.add("vehaa");

        // Planes
        planes.add("fighter");
        planes.add("bomberdive");
        planes.add("armstiletto_laser");
        planes.add("corvamp");
        planes.add("corshad");
        planes.add("corhurc2");
        planes.add("armcybr");
        planes.add("corawac");

        // Bombers
        bombers.add("bomberdive");
        bombers.add("corshad");
        bombers.add("armcybr");
        bombers.add("corhurc2");
        bombers.add("armstiletto_laser");

        // Sappers
        sappers.add("armtick");
        sappers.add("corroach");
        sappers.add("corsktl");

        // Things that should not retreat
        noRetreat.add("armcrabe");
        noRetreat.add("core_spectre");
        noRetreat.add("corshad");
        noRetreat.add("blastwing");
        noRetreat.add("fighter");
        
        // a list of porc weapon def names, for identifying when things get attacked by porc that's out of los
        porcWeps.add("corllt_laser");
        porcWeps.add("corrl_armrl_missile");
        porcWeps.add("armpb_gauss");
        porcWeps.add("armdeva_armdeva_weapon");
        porcWeps.add("corhlt_laser");
        porcWeps.add("cordoom_plasma");
        porcWeps.add("armanni_ata");
        porcWeps.add("corbhmth_plasma");
    }

    public static UnitClasses getInstance(){
        if (instance == null){
            instance = new UnitClasses();
        }
        return instance;
    }
}
