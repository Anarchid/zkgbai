package zkgbai.kgbutil;

/*
 * Abducted from CSI
 */

import com.springrts.ai.oo.AIFloat3;

/**
 *
 * @author User
 */
public interface CostSupplier {
    float getCost(float slope, float maxSlope, AIFloat3 pos);
}