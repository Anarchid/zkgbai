package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by haplo on 1/8/2016.
 */
public class RaiderSquad {
    public List<Raider> raiders;
    AIFloat3 target;
    public char status;
    public float income;
    private Raider leader;
    private int index = 0;

    public RaiderSquad(){
        this.raiders = new ArrayList<Raider>();
        this.income = 0;
        this.status = 'f';
        // f = forming
        // r = rallying
        // a = attacking
    }
}
