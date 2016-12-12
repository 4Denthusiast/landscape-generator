package _4denthusiast.landscapegenerator.water;

import _4denthusiast.landscapegenerator.HeightMap;
import _4denthusiast.landscapegenerator.Point;

public abstract class IWater{
	
	public double getWetness(int i, int j){
		return getWetness(new Point(i,j));
	}
	public double getWetness(Point p){
		return getWetness(p.x, p.y);
	}
	//returns a value in [0,1] representing how wet (vaguely) the point is
	//As implementations may have differing internal representations, this method has no precise contract and should only be used for display purposes.
	
	public double getWaterHeight(int i, int j){
		return getWaterHeight(new Point(i,j));
	}
	
	public abstract double getWaterHeight(Point p);
	
	public abstract boolean isSaline(int i, int j);
	//I suppose this isn't necessarily always discrete, but never mind.
	
	public abstract double getErosion(int i, int j);
	
	public abstract void computeFlow();
}
