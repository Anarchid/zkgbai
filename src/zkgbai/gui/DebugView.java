package zkgbai.gui;

import org.starfire.shine.Color;
import org.starfire.shine.Graphics;
import org.starfire.shine.Image;
import org.starfire.shine.ImageBuffer;
import zkgbai.ZKGraphBasedAI;


public class DebugView {
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
	Graphics display;
	int mapWidth;
	int mapHeight;
	private Image graphImage;
	private Boolean visible = false;
	
	public DebugView(ZKGraphBasedAI parent){
		this.ai = parent;
		this.mapWidth  =  parent.getCallback().getMap().getWidth();
		this.mapHeight = parent.getCallback().getMap().getHeight();

		this.display = new Graphics();
		backbuffer = new ImageBuffer(mapWidth, mapHeight);
		try {
			bufferGraphics = backbuffer.getImage().getGraphics();
		}catch (Exception e){
			ai.printException(e);
			System.exit(0);
		}
	}

	public void render(){
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
		bufferGraphics.flush();

		display.drawImage(backbuffer.getImage(), 0, 0);
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
