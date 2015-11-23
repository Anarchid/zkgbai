package zkgbai;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Drawer;

public abstract class StartArea {
	public abstract boolean contains(AIFloat3 point);
	public abstract void draw(Drawer c);
}
