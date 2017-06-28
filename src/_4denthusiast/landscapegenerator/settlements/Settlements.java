package _4denthusiast.landscapegenerator.settlements;

import _4denthusiast.landscapegenerator.water.Water;
import _4denthusiast.landscapegenerator.IHeightMap;
import _4denthusiast.landscapegenerator.IPoint;

import java.util.Random;
import java.util.Map;
import java.util.Iterator;

public class Settlements{
	private IPoint p0;
	private Object population; //IField<double>
	private Water water;
	private IHeightMap heightMap;
	private Roads roads;
	private Random random;
	
	public Settlements(Map<String, Double> options, IPoint p0, IHeightMap heightMap, Water water){
		this.p0 = p0;
		this.population = p0.makeDoubleField();
		this.heightMap = heightMap;
		this.water = water;
		this.random = options.containsKey("seed")? new Random((long)(double)options.get("seed")) : new Random();
		for(Iterator<IPoint> it = p0.iterator(); it.hasNext(); ){
			IPoint p = it.next();
			double habitability = getHabitability(p);
			double pop = Math.log1p(habitability)*(1/random.nextDouble() - 1);
			if(random.nextDouble() * pop / habitability > 1)
				pop = 0;
			p.setDouble(population, pop);
		}
	}
	
	
	private double getHabitability(IPoint p){
		if(water.isLake(p))//The middle of a lake isn't a goood placse to live.
			return 0;
		double problems = 0;
		IPoint[] adj = p.getAdjacent();
		double wetness = water.getDrainage(p);
		for(int i=0; i<adj.length; i++)
			wetness = wetness + 0.2*water.getOutDrainage(adj[i]) + 0.002*water.getDrainage(adj[i]);
			//the outDrainage of the sea is nearly 0. Tiny lakes aren't very useful for trade, but larger lakes aren't that much more useful than largish ones.
		problems += 2.5/wetness; //Rivers and shores are good places to live.
		/*double steepness = 0;
		double height = heightMap.getHeight(p);
		for(int i=0; i<adj.length; i++)
			steepness += Math.pow(height-water.getWaterHeight(adj[i]), 2);
		problems += steepness;*/
		return 1/problems;
	}
	
	public double getPopulation(IPoint p){
		return p.getDouble(population);
	}
	
	public boolean hasRoads(){
		return roads != null;
	}
	
	public void generateRoads(){
		roads = new Roads(p0, heightMap, water, this, random);
	}
	
	public double getPathness(IPoint p){
		return roads.getPathness(p);
	}
	
	public boolean hasBorders(){
		return roads != null;
	}
	
	public double getBorderLevel(IPoint p, IPoint q){
	    return roads.getBorderLevel(p, q);
    }
	
	public boolean isOnBorder(IPoint p, double level){
		return roads.isOnBorder(p, level);
	}
	
	public int getKingdomColour(IPoint p, double level){
		return roads.getKingdomColour(p, level);
	}
}
