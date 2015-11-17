package zkgbai.economy;

import com.springrts.ai.oo.AIFloat3;

/**
 * Created by haplo on 11/16/2015.
 */
public class DefenseTarget {
    public AIFloat3 position;
    public int frameIssued;
    public DefenseTarget(AIFloat3 pos, int frame){
        this.position = pos;
        this.frameIssued = frame;
    }
}
