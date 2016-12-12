package _4denthusiast.landscapegenerator;

import _4denthusiast.landscapegenerator.water.Water;
import _4denthusiast.landscapegenerator.settlements.Settlements;

import java.util.HashMap;

public class LandscapeGenerator{
	private ControlPanel cPanel;//I'll need this for updating the progress bar.
	private Display display;
	private String compositionNo;
	private final int size;
	private HeightMap heightMap;
	private Water water;
	private Settlements settlements;
	
	private HashMap<String, Double> options;
	
	public static boolean extremeDebug = false;
	
	public LandscapeGenerator(HashMap<String, Double> args){
		this.options = args;
		if(args.containsKey("size"))
			size = (int)(double)args.get("size");
		else
			size = 512;
		if(size>2048 || Integer.bitCount(size)!=1){
			System.out.println("That size was way too big or a non power of 2.");
			System.exit(1);
		}
		if(size>64)
			extremeDebug = false;
		Point.size = size;
		compositionNo = String.valueOf(Math.random()*12);
		display = new Display(size, compositionNo);
		cPanel = new ControlPanel(this, display, compositionNo);
	}
	
	//Only this one is done in a seperate thread because it usually takes longest and all the mulithreading stuff just to get a loading bar working is a pain.
	HeightMap.FactoryThread heightMapFactory = null;
	public void generateHeightMap(){
		synchronized(this){
			if(heightMap == null && heightMapFactory == null){
				heightMapFactory = HeightMap.generate(size, options, this);
				cPanel.hideHeightButton();
			}
		}
	}
	
	protected void setHeightMap(HeightMap heightMap){
		if(this.heightMap == null){
			this.heightMap = heightMap;
			this.heightMapFactory = null;
			display.setHeightMap(heightMap);
			cPanel.enableHeight();
		}
	}
	
	public void generateWater(){
		if(water == null){
			generateHeightMap();
			if(heightMapFactory != null){
				try{
					heightMapFactory.get();
				}catch(Exception e){
					e.printStackTrace();
					return;
				}
			}
			water = new Water(options, heightMap, new Point(0,0));
			display.setWater(water);
			cPanel.enableWater();
		}
	}
	
	public void generateFlow(){
		/*generateWater();
		new Thread(){
			public void run(){
				if(water instanceof DefaultWater)
					((DefaultWater)water).computeFlow();
			}
		}.start();*/
	}
	
	public void generateSettlements(){
		if(settlements == null){
			generateWater();
			settlements = new Settlements(options, new Point(0,0), heightMap, water);
			display.setSettlements(settlements);
			cPanel.enableSettlements();
		}
	}
	
	public void generateRoads(){
		if(settlements == null || !settlements.hasRoads()){
			generateSettlements();
			settlements.generateRoads();
			cPanel.enableRoads();
			cPanel.enableBorders();
		}
	}
	
	public void setProgressDescription(String s){
		cPanel.setProgressDescription(s);
	}
	
	public void setProgress(double p){
		cPanel.setProgress(p);
	}
	
	public void clearProgressBar(){
		cPanel.clearProgressBar();
	}
}
