package zkgbai.military;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by haplo on 11/14/2015.
 */
public class UnitClasses {
    public List<String> raiders;
    public List<String> assaults;
    public List<String> arties;
    public List<String> striders;

    public UnitClasses(){
        this.raiders = new ArrayList<String>();
        this.assaults = new ArrayList<String>();
        this.arties = new ArrayList<String>();
        this.striders = new ArrayList<String>();

        // raiders
        raiders.add("armpw");
        raiders.add("spherepole");
        raiders.add("armkam");
    }
}
