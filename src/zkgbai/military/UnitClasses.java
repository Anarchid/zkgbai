package zkgbai.military;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aeonios on 11/14/2015.
 */
public class UnitClasses {
    public List<String> smallRaiders;
    public List<String> mediumRaiders;
    public List<String> soloRaiders;
    public List<String> assaults;
    public List<String> arties;
    public List<String> striders;
    public List<String> airMobs;
    public List<String> shieldMobs;
    public List<String> mobSupports;
    public List<String> loners;
    public List<String> AAs;
    public List<String> planes;
    public List<String> sappers;

    public List<String> noRetreat;

    public UnitClasses(){
        this.smallRaiders = new ArrayList<String>();
        this.mediumRaiders = new ArrayList<String>();
        this.soloRaiders = new ArrayList<String>();
        this.assaults = new ArrayList<String>();
        this.arties = new ArrayList<String>();
        this.striders = new ArrayList<String>();
        this.airMobs = new ArrayList<String>();
        this.shieldMobs = new ArrayList<String>();
        this.mobSupports = new ArrayList<String>();
        this.loners = new ArrayList<String>();
        this.AAs = new ArrayList<String>();
        this.planes = new ArrayList<String>();
        this.sappers = new ArrayList<String>();
        this.noRetreat = new ArrayList<String>();

        // raiders
        smallRaiders.add("armpw");
        smallRaiders.add("corak");
        smallRaiders.add("corclog");
        smallRaiders.add("amphraider3");
        smallRaiders.add("amphraider2");

        mediumRaiders.add("corgator");
        mediumRaiders.add("armkam");
        mediumRaiders.add("corsh");

        soloRaiders.add("spherepole");
        soloRaiders.add("hoverassault");
        soloRaiders.add("logkoda");
        soloRaiders.add("panther");
        soloRaiders.add("armflea");
        soloRaiders.add("corfav");

        // assaults; stuff that attacks in mobs
        assaults.add("armzeus");
        assaults.add("armwar");
        assaults.add("armrock");
        assaults.add("amphfloater");
        assaults.add("amphriot");
        assaults.add("corlevlr");
        assaults.add("corraid");
        assaults.add("capturecar");
        assaults.add("nsaclash");
        assaults.add("hoverriot");
        assaults.add("correap");
        assaults.add("tawf114");
        assaults.add("spiderassault");
        assaults.add("armsptk");
        assaults.add("arm_venom");
        assaults.add("spiderriot");


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

        // mobSupport: things that increase the strength of mobs
        mobSupports.add("spherecloaker");
        mobSupports.add("core_spectre");

        // striders; stuff that can dgun
        striders.add("dante");
        striders.add("scorpion");
        striders.add("armbanth");
        striders.add("armorco");

        // loners; strider-like stuff that does better on its own than in mobs
        loners.add("armsnipe");
        loners.add("corstorm");
        loners.add("armham");
        loners.add("amphassault");
        loners.add("cormist");
        loners.add("corgarp");
        loners.add("armmerl");
        loners.add("armmanni");
        loners.add("cormart");
        loners.add("corgol");
        loners.add("armcrabe");

        // Anti-air units
        AAs.add("armjeth");
        AAs.add("gunshipaa");
        AAs.add("amphaa");
        AAs.add("hoveraa");
        AAs.add("armaak");
        AAs.add("corvamp");
        AAs.add("corcrash");
        AAs.add("shipaa");
        AAs.add("spideraa");
        AAs.add("corsent");
        AAs.add("vehaa");

        // Planes
        planes.add("bomberdive");
        planes.add("fighter");
        planes.add("armstiletto_laser");
        planes.add("corvamp");
        planes.add("corshad");
        planes.add("corhurc2");
        planes.add("armcybr");
        planes.add("corawac");

        // Sappers
        sappers.add("armtick");
        sappers.add("corroach");
        sappers.add("corsktl");
        sappers.add("blastwing");

        // Things that should not retreat
        //noRetreat.add("corsh");
        noRetreat.add("armcrabe");
        noRetreat.add("spherepole");
        noRetreat.add("core_spectre");
    }
}
