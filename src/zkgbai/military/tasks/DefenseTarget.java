package zkgbai.military.tasks;

import com.springrts.ai.oo.AIFloat3;

/**
 * Created by aeonios on 11/16/2015.
 */
public class DefenseTarget {
    public AIFloat3 position;
    public int frameIssued;
    public float damage;
    public DefenseTarget(AIFloat3 pos, float dmg, int frame){
        this.position = pos;
        this.damage = dmg;
        this.frameIssued = frame;
    }
}
