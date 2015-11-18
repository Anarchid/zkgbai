package zkgbai.gui;


import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import zkgbai.ZKGraphBasedAI;


public class DebugView extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ZKGraphBasedAI ai;
	BufferedImage losImage;
	BufferedImage threatImage;
	BufferedImage valueImage;
	BufferedImage mapTexture;
	BufferedImage backbuffer;
	Graphics2D bufferGraphics;
	int mapWidth;
	int mapHeight;
	private BufferedImage graphImage;
	
	public DebugView(ZKGraphBasedAI parent){
		this.ai = parent;
		this.setVisible(true);
		this.setTitle("ZKGBAI");
		
		this.mapWidth  =  parent.getCallback().getMap().getWidth();
		this.mapHeight = parent.getCallback().getMap().getHeight();
		float aspect = mapHeight / mapWidth;
		this.setSize(600,(int) (600*aspect));
		backbuffer = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_RGB);
		bufferGraphics = backbuffer.createGraphics();
	}
	
	@Override
	public void paint(Graphics g){
		AlphaComposite losComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f);
		AlphaComposite valueComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
		AlphaComposite graphComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
		int w = backbuffer.getWidth();
		int h = backbuffer.getHeight();

		bufferGraphics.setComposite(graphComposite);
		bufferGraphics.drawImage(threatImage, 0, 0, w,h, null);
		bufferGraphics.setComposite(valueComposite);
		bufferGraphics.drawImage(valueImage, 0, 0, w,h, null);
		bufferGraphics.setComposite(graphComposite);
		bufferGraphics.drawImage(graphImage, 0, 0, w,h, null);
		bufferGraphics.setComposite(losComposite);
		bufferGraphics.drawImage(losImage, 0, 0, w,h, null);

		g.drawImage(backbuffer, 0, 0, getWidth(), getHeight(), null);
	}
	
	public void setLosImage(BufferedImage bu){
		losImage = bu;
	}
	
	public void setMapTexture(BufferedImage bu){
		mapTexture = bu;
	}

	public void setThreatImage(BufferedImage threatMap) {
		threatImage = threatMap;
	}

	public void setGraphImage(BufferedImage graphImage) {
		this.graphImage = graphImage;
	}
	public void setValueImage(BufferedImage valueImage){
		this.valueImage = valueImage;
	}
}
