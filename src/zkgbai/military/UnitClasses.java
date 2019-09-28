package zkgbai.military;

import com.springrts.ai.AICallback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public List<String> heavies = new ArrayList<String>();

    public List<String> noRetreat = new ArrayList<String>();
    
    public Set<Integer> porcWeps = new HashSet<>();

    private static UnitClasses instance = null;

    private UnitClasses(){
        // raiders
        smallRaiders.add("cloakraid");
        smallRaiders.add("shieldraid");
        smallRaiders.add("amphraid");
	    smallRaiders.add("amphbomb");
        smallRaiders.add("hoverraid");

        mediumRaiders.add("vehraid");
        mediumRaiders.add("gunshipraid");
        mediumRaiders.add("cloakheavyraid");
	    mediumRaiders.add("hoverassault");
	    mediumRaiders.add("tankheavyraid");

        soloRaiders.add("spiderscout");
        soloRaiders.add("vehscout");
        soloRaiders.add("shieldscout");
        soloRaiders.add("gunshipbomb");

        // assaults; stuff that attacks in mobs
        assaults.add("cloakassault");
        assaults.add("cloakriot");
        assaults.add("cloakskirm");
        assaults.add("amphfloater");
        assaults.add("amphriot");
	    assaults.add("amphimpulse");
        assaults.add("vehriot");
        assaults.add("vehassault");
        assaults.add("hoverskirm");
        assaults.add("hoverriot");
        assaults.add("spiderassault");
        assaults.add("spiderskirm");
        assaults.add("spideremp");
        assaults.add("spiderriot");
        assaults.add("tankriot");
        assaults.add("tankassault");
        assaults.add("shieldskirm");
	    assaults.add("gunshipskirm");
	    assaults.add("gunshipheavyskirm");
	    assaults.add("gunshipassault");

        // Shield mobs
        shieldMobs.add("shieldassault");
        shieldMobs.add("shieldriot");
        shieldMobs.add("shieldarty");
        shieldMobs.add("shieldfelon");
        shieldMobs.add("vehcapture");
        shieldMobs.add("shieldshield");

        // mobSupport: things that increase the strength of mobs
        mobSupports.add("cloakjammer");


        // striders; stuff that can dgun
        striders.add("striderdante");
        striders.add("striderscorpion");
        striders.add("striderbantha");
        striders.add("striderdetriment");

        // loners; strider-like stuff that does better on its own than in mobs
        loners.add("vehsupport");
        loners.add("tankheavyassault");
        loners.add("spidercrabe");
	    loners.add("amphassault");
        
        // arties; stuff that has long range and can attack high threat areas without suiciding
        arties.add("cloaksnipe");
        arties.add("cloakarty");
        arties.add("veharty");
        arties.add("vehheavyarty");
        arties.add("hoverarty");
        arties.add("tankarty");
        arties.add("tankheavyarty");
        arties.add("striderarty");

        // Anti-air units
        AAs.add("cloakaa");
        AAs.add("gunshipaa");
        AAs.add("amphaa");
        AAs.add("hoveraa");
        AAs.add("jumpaa");
        //AAs.add("planeheavyfighter");
        //AAs.add("planefighter");
        AAs.add("shieldaa");
        AAs.add("shipaa");
        AAs.add("spideraa");
        AAs.add("tankaa");
        AAs.add("vehaa");

        // Planes
        planes.add("planefighter");
        planes.add("bomberdive");
        planes.add("bomberdisarm");
        planes.add("planeheavyfighter");
        planes.add("bomberprec");
        planes.add("bomberriot");
        planes.add("bomberheavy");
        planes.add("planescout");

        // Bombers
        bombers.add("bomberdive");
        bombers.add("bomberprec");
        bombers.add("bomberheavy");
        bombers.add("bomberriot");
        bombers.add("bomberdisarm");

        // Sappers
        sappers.add("cloakbomb");
        sappers.add("shieldbomb");
        sappers.add("jumpbomb");

        // Things that should not retreat
        noRetreat.add("spidercrabe");
        noRetreat.add("shieldshield");
        noRetreat.add("shieldfelon");
        noRetreat.add("bomberprec");
        noRetreat.add("gunshipbomb");
        noRetreat.add("planefighter");

        // List of things which can count higher towards fighter value.
        heavies.add("tankassault");
        heavies.add("tankheavyassault");
        heavies.add("spidercrabe");
        heavies.add("amphassault");

        // porc weps are set in root.
    }

    public static UnitClasses getInstance(){
        if (instance == null){
            instance = new UnitClasses();
        }
        return instance;
    }

    public static void release(){
    	instance = null;
    }
}
