package zkgbai.military.tasks;

import com.springrts.ai.oo.AIFloat3;
import zkgbai.graph.MetalSpot;
import zkgbai.military.unitwrappers.Raider;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aeonios on 11/13/2015.
 */
public class ScoutTask extends FighterTask {
    public List<Raider> assignedRaiders;
    public MetalSpot spot;

    public ScoutTask(AIFloat3 t, MetalSpot s){
        super(t);
        this.spot = s;
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

    public void endTask(){
        for (Raider r:assignedRaiders){
            r.endTask();
        }
    }
    
    @Override
    public boolean equals(Object o){
        if (o instanceof ScoutTask){
            ScoutTask st = (ScoutTask) o;
            return (this.target.equals(st.target) && this.spot.equals(st.spot));
        }
        return false;
    }
}
