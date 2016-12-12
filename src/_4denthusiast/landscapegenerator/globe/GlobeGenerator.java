package _4denthusiast.landscapegenerator.globe;

import _4denthusiast.landscapegenerator.water.Water;
import _4denthusiast.landscapegenerator.settlements.Settlements;

import java.util.HashMap;

public class GlobeGenerator{
	Geometry geo;
	GUI gui;
	DisplayManager display;
	HeightMap heightMap;
	Water water;
	Settlements settlements;
	
	public GlobeGenerator(HashMap<String, Double> args){
		geo = new Geometry(args);
		gui = new GUI();
		heightMap = new HeightMap(args, geo);
		water = new Water(args, heightMap, new Point(0));
		settlements = new Settlements(args, new Point(0), heightMap, water);
		settlements.generateRoads();
		gui.initDisplayManager(geo, heightMap, water, settlements);
	}
	
	public void run(){
		gui.run();
	}
}
