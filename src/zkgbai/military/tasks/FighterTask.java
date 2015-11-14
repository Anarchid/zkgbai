package zkgbai.military.tasks;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import zkgbai.los.LosManager;
import zkgbai.military.Fighter;

import java.util.List;

/**
 * Created by haplo on 11/13/2015.
 */
public class FighterTask {
    public AIFloat3 target;

    public FighterTask(AIFloat3 t){
        this.target = t;
    }
}
