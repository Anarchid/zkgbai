package zkgbai.gui;

import javax.swing.JFrame;

import org.newdawn.slick.*;
import org.newdawn.slick.opengl.pbuffer.GraphicsFactory;
import zkgbai.ZKGraphBasedAI;


public class DebugView extends BasicGame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ZKGraphBasedAI ai;
	Image losImage;
	Image threatImage;
	Image mapTexture;
	ImageBuffer backbuffer;
	Graphics bufferGraphics;
	int mapWidth;
	int mapHeight;
	private Image graphImage;
	
	public DebugView(ZKGraphBasedAI parent){
		super("ZKGBAI");
		this.ai = parent;
		this.mapWidth  =  parent.getCallback().getMap().getWidth();
		this.mapHeight = parent.getCallback().getMap().getHeight();
		backbuffer = new ImageBuffer(mapWidth, mapHeight);
		try {
			bufferGraphics = backbuffer.getImage().getGraphics();
		}catch (Exception e){
			ai.printException(e);
			System.exit(0);
		}
	}
	
	@Override
	public void render(GameContainer gc ,Graphics g){
		Color threatColor = new Color(1f, 1f, 1f, 0.5f);
		Color graphColor = new Color(1f, 1f, 1f, 1f);

		int w = backbuffer.getWidth();
		int h = backbuffer.getHeight();

		bufferGraphics.setBackground(new Color(0,0,0,255));
		bufferGraphics.clear();
		bufferGraphics.setDrawMode(Graphics.MODE_ALPHA_BLEND);

		bufferGraphics.drawImage(losImage, 0, 0, graphColor);
		bufferGraphics.drawImage(threatImage, 0, 0, threatColor);
		bufferGraphics.drawImage(graphImage, 0, 0, graphColor);

		g.drawImage(backbuffer.getImage(), 0, 0);
	}

	@Override
	public void init(GameContainer gc){
		// required to implement the slick game interface
	}

	@Override
	public void update(GameContainer gc, int delta){
		// required to implement the slick game interface
	}
	
	public void setLosImage(Image bu){
		losImage = bu;
	}
	
	public void setMapTexture(Image bu){
		mapTexture = bu;
	}

	public void setThreatImage(Image threatMap) {
		threatImage = threatMap;
	}

	public void setGraphImage(Image graphImage) {
		this.graphImage = graphImage;
	}
}
