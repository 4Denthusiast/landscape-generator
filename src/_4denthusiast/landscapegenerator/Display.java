package _4denthusiast.landscapegenerator;

import _4denthusiast.landscapegenerator.water.Water;
import _4denthusiast.landscapegenerator.settlements.Settlements;

import java.io.*;
import javax.imageio.*;
import java.util.NoSuchElementException;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.image.BufferedImage;

class Display extends JFrame{
	private final int size;
	
	//All of these things need to be drawn, so I'm storing references for convenience. Also, this way I don't actually need a reference to the main class.
	private HeightMap heightMap;
	private Water water;
	private Settlements settlements;
	
	private BufferedImage preBuffer;
	private ControlPanel cPanel;
	
	Display(int size, String compositionNo){
		super("Imaginary landscape number "+compositionNo);
		this.size = size;
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(new Dimension(size+getInsets().top, size));
	}
	
	protected void setControlPanel(ControlPanel cPanel){
		this.cPanel = cPanel;
	}
	
	protected void setHeightMap(HeightMap heightMap){
		this.heightMap = heightMap;
	}
	
	protected void setWater(Water water){
		this.water = water;
	}
	
	protected void setSettlements(Settlements settlements){
		this.settlements = settlements;
	}
	
	// Generate the image to display (possibly tiled) in the window
	private void drawBuffer(){
		preBuffer = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		for(int i=0; i<size; i++){
			for(int j=0; j<size; j++){
				preBuffer.setRGB(i, j, getColourAtPoint(i, j).getRGB());
			}
		}
	}
	
	// A simply downscaled version of the map to use as a toolbar icon. I'm not sure how well this will work with other OSs.
	private void makeIcon(int n){
		BufferedImage icon = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
		for(int i=0; i<n; i++){
			for(int j=0; j<n; j++){
				icon.setRGB(i, j, getColourAtPoint((i*size)/n, (j*size)/n).getRGB());
			}
		}
		setIconImage(icon);
	}
	
	@Override
	public void paint(Graphics g){
		int leftInset = getInsets().left;
		int topInset = getInsets().top;
		super.paint(g);
		for(int i=getInsets().left; i<getWidth(); i+=size){
			for(int j=getInsets().top; j<getHeight(); j+=size){
				g.drawImage(preBuffer, i, j, null);
			}
		}
		if(cPanel != null && cPanel.shouldDisplayDiagonal()){
			//Check for offsets in the coordinates used.
			g.setColor(Color.WHITE);
			g.drawLine(0,0,size,size);
		}
	}
	
	// Get colours for the map in the window
	public Color getColourAtPoint(int i, int j){
		Point p = new Point(i,j);
		if(cPanel != null && (cPanel.shouldDisplaySettlements() || cPanel.shouldDisplayRoads() || cPanel.shouldDisplayBorders())){
			float pop = cPanel.shouldDisplaySettlements()?(float) settlements.getPopulation(p):0f;
			pop = (float)Math.min(Math.sqrt(pop)/6, 1);
			float road = cPanel.shouldDisplayRoads()?(float) settlements.getPathness(p):0;
			road = (float)Math.min(road, 1);
			float border = (cPanel.shouldDisplayBorders() && settlements.isOnBorder(p, cPanel.getKingdomCutoff()))?0.5f:0;
			return new Color(pop, Math.max(pop,border), road);
		}
		float d, wet, wh;
		boolean salt = false;
		boolean doHeight = cPanel.shouldDisplayHeight();
		if(cPanel==null || cPanel.shouldDisplayWater()){
			d = doHeight?1 - (float) heightMap.normaliseHeight(water.getWaterHeight(p) - heightMap.getHeight(p)):1;
			wet = (float) water.getWetness(i,j);
			wh = doHeight?(float) water.normaliseHeight(water.getWaterHeight(p)):0.3f;
			salt = water.isSaline(p);
		}else{
			d = 1;
			wet = 0;
			wh = doHeight?(float) heightMap.normaliseHeight(heightMap.getHeight(p)):0.3f;
		}
		return new Color((spline(3,0,2,1,wh)*(1-wet) + (salt?wet/2:0))*d, (spline(1.5,0.5,2,1,wh)*(1-wet)+0.3f*wet)*d , (spline(0,0,1,1,wh)*(1-wet)+wet)*d);
		//return new Color(spline(3,0,2,1,wh)*(1-wet), spline(1.5,0.5,2,1,wh)*(1-wet) , spline(0,0,1,1,wh)*(1-wet)+wet);
	}
	
	// Cubic interpolation with given start & end values & gradients.
	private float spline(double dl, double l, double dr, double r, float x){
		return (float)(x*x*x*(-2*r+dl+dr+2*l) + x*x*(-2*dl-dr+3*r-3*l) + x*dl + l);
	}
	
	private float[] colour1;
	private float[] colour2;
	private Color lerp(Color c1, Color c2, float x){
		if(x>1)
			x = 1;
		else if(x<0)
			x = 0;
		colour1 = c1.getComponents(colour1);
		colour2 = c2.getComponents(colour2);
		float α2 = x * colour2[3];
		float α1 = (1-α2)*colour1[3];
		float α = α1+α2;
		return new Color((colour1[0]*α1 + colour2[0]*α2)/α, (colour1[1]*α1 + colour2[1]*α2)/α, (colour1[2]*α1 + colour2[2]*α2)/α, α);
	}
	
	public void reDraw(){
		drawBuffer();
		makeIcon(48);
		setVisible(false);
		cPanel.setVisible(false);
		setVisible(true);
		cPanel.setVisible(true);
		repaint();
	}
	
	public void save(File out){
		ImageWriter writer = null;
		try{
			if(out.exists()){
				System.out.println("Save directory already exists.");
				return;
			}
			out.mkdir();
			writer = ImageIO.getImageWritersBySuffix("png").next();
			for(int i=0; i<NUM_LAYERS; i++){
				writer.setOutput(ImageIO.createImageOutputStream(new File(out, i+":"+layerNames[i]+".png")));
				writer.write(getLayer(i));
			}
		}catch(NoSuchElementException e){
			System.out.println("No PNG encoder found.");
		}catch(IOException e){
			e.printStackTrace();
		}finally{
			if(writer != null)
				writer.dispose();
		}
	}
	
	// Generate map layers for the saved version.
	private static final String[] layerNames = {"base", "settlements", "roads", "borders", "kingdoms"};
	private static final int LAYER_BASE = 0;
	private static final int LAYER_SETTLEMENTS = 1;
	private static final int LAYER_ROADS = 2;
	private static final int LAYER_BORDERS = 3;
	private static final int LAYER_KINGDOMS = 4;
	private static final int NUM_LAYERS = 5;
	private BufferedImage getLayer(int layer){
		BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		for(int i=0; i<size; i++){
			for(int j=0; j<size; j++)
				result.setRGB(i, j, getLayerColourAtPoint(i, j, layer).getRGB());
		}
		return result;
	}
	
	private Color getLayerColourAtPoint(int i, int j, int layer){
		Point p = new Point(i, j);
		switch(layer){
			case LAYER_BASE:
				float d = 1 - (float) heightMap.normaliseHeight(water.getWaterHeight(p) - heightMap.getHeight(p));
				float wet = (float) water.getWetness(i,j);
				float wh = (float) water.normaliseHeight(water.getWaterHeight(p));
				boolean salt = water.isSaline(p);
				return new Color((spline(3,0,2,1,wh)*(1-wet) + (salt?wet/2:0))*d, (spline(1.5,0.5,2,1,wh)*(1-wet)+0.3f*wet)*d , (spline(0,0,1,1,wh)*(1-wet)+wet)*d);
			case LAYER_SETTLEMENTS:
				float pop = (float)Math.sqrt(settlements.getPopulation(p))/6;
				return new Color(1f, (float)Math.min(pop/3,1), (float)Math.min(pop/9,1), (float)Math.min(pop,1));
			case LAYER_ROADS:
				return new Color(0.4f, 0.4f, 0.6f, (float)Math.min(Math.pow(settlements.getPathness(p), 0.7), 1));
			case LAYER_BORDERS:
				double cutoff = cPanel.getKingdomCutoff();
				if(settlements.isOnBorder(p, cutoff*3))
					return new Color(0f,1f,0.7f,1f);
				else if(settlements.isOnBorder(p, cutoff))
					return new Color(0f,0.9f,0f,0.7f);
				else if(settlements.isOnBorder(p, cutoff/2))
					return new Color(0.8f,0.2f,0f,0.4f);
				else
					return new Color(0f,1f,0f,0f);
			case LAYER_KINGDOMS:
				cutoff = cPanel.getKingdomCutoff();
				return new Color(settlements.getKingdomColour(p, cutoff) | 0x60000000, true);
			default:
				assert false;
				return null;
		}
	}
}
