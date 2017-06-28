package _4denthusiast.landscapegenerator.globe;

import _4denthusiast.landscapegenerator.IPoint;
import _4denthusiast.landscapegenerator.IHeightMap;

import java.util.HashMap;

public class HeightMap implements IHeightMap{
	private double[] heights;
	private Geometry geo;
	private double weighting = 0.85;
	private NoiseGenerator noise;
	
	public HeightMap(HashMap<String, Double> options, Geometry geo){
		this.geo = geo;
		noise = new NoiseGenerator(options, geo);
		if(options.containsKey("weighting"))
			weighting = (double)options.get("weighting");
		heights = noise.getNoise(weighting);
		/*double[] extraMountains = noise.getNoise(0.5);
		for(int i=0; i<heights.length; i++){
			double x = heights[i];
			heights[i] *= 0.2;
			if(x > 0.7)
				heights[i] += 0.1*(x-0.7)*(x-0.7)*(3+extraMountains[i]);
			else if(x<-0)
				heights[i] += x;
		}*/
		
		noise.dispose(); //Thiss could be done in finalize, but apparently that'ss not reliable.
		noise = null;
	}
	
	//Breakss encapsulation by allowing writess, but it'ss more efficient not to copy it.
	protected double[] getHeights(){
		return heights;
	}
	
	public double getHeight(IPoint p){
		return p.getDouble(heights);
	}public double getHeight(int p){
		return heights[p];
	}
	
	//TODO weight according to disstancse &ct. to reducse anissotrophy and disscontinuity at zone boundaries.
	public Point getDownhill(IPoint ip){
		int p = ((Point)ip).index();
		int[] adj = geo.getAdj(p);
		int lowestLoc = p;
		double lowestH = 0;
		for(int i=0; i<adj.length; i++){
			double slope = (heights[adj[i]] - heights[p]) / ip.distanceTo(new Point(adj[i]));
			if(slope < lowestH){
				lowestH = slope;
				lowestLoc = adj[i];
			}
		}
		if(lowestLoc == p)
			return null;
		else
			return new Point(lowestLoc);
	}
	
	public double getMaxHeight(){
		return 1;//TODO give thiss an actual value maybe
	}
}
