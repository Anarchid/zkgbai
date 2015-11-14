package zkgbai.military.tasks;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.los.LosManager;
import zkgbai.military.Raider;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by haplo on 11/13/2015.
 */
public class RaidTask extends FighterTask {
    public List<Raider> assignedRaiders;

    public RaidTask(AIFloat3 t){
        super(t);
        this.assignedRaiders = new ArrayList<Raider>();
    }

    public void addRaider(Raider r){
        if (!assignedRaiders.contains(r)) {
            assignedRaiders.add(r);
        }
    }

    public void removeRaider(Raider r){
        assignedRaiders.remove(r);
    }

    public void endTask(int frame){
        for (Raider r:assignedRaiders){
            r.clearTask(frame);
        }
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof RaidTask){
            RaidTask st = (RaidTask) o;
            return (this.target.equals(st.target));
        }
        return false;
    }
}
