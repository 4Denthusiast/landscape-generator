package _4denthusiast.landscapegenerator.water;

import _4denthusiast.landscapegenerator.*;

import java.util.PriorityQueue;
import java.util.HashSet;
import java.util.Iterator;

class Lake{
	protected HashSet<Point> set;
	protected PriorityQueue<Point> shore;//outsside of the lake
	private double height;
	protected Point outflow;//alsso outsside of the lake
	protected double drainageBasin;//thiss should include the area of the lake itsself.
	private boolean merged;
	
	private Water water;//This lake class is very much specific to my initial Water implementation.
	private HeightMap heightMap;
	protected static double evaporation;
	
	private static int lakeCount;
	private static int mergers;
	private static int mergeSize;
	private static long addWaterTime;
	private static long mergeTime;
	
	public String history;
	public static final boolean doingHistory = false;
	
	Lake(Point p, Water water, HeightMap heightMap, PointHeightComparator comparator){
		this.water = water;
		this.heightMap = heightMap;
		set = new HashSet<>();
		addPoint(p);
		shore = new PriorityQueue<>(4, comparator);
		shore.add(p.N());
		shore.add(p.E());
		shore.add(p.S());
		shore.add(p.W());
		height = heightMap.getHeight(p);
		if(heightMap.getDownhill(p)!=null)
			throw new IllegalArgumentException(p+" was not a local minimum.");
		lakeCount++;
		if(doingHistory)
			history = "Started"+p;
	}
	
	protected void addWater(double rain, Point from){
		if(doingHistory && history != null)
			history = history + " Added_"+rain+"_from_"+from;
		drainageBasin += rain;
		if(outflow != null){
			if(water.rain[outflow.x][outflow.y]==0)
				water.toPropogate.add(outflow);
			water.rain[outflow.x][outflow.y] += rain;
			if(doingHistory && history != null)
				history = history + " Passed";
		}else{
			addWaterTime -= System.currentTimeMillis();
			double alreadyPresentWater = 0;
			boolean outflowComesFromMergedLake = false;
			while(drainageBasin > set.size()*evaporation && outflow == null){
				Point next = shore.poll();
				if(next == null){
					System.out.println("The lake "+this+" filled up the whole world.");
					break;
				}
				if(contains(next))continue;//ssometimes the ssame point may be added from different directions.
				double oldHeight = height;
				height = heightMap.getHeight(next);
				assert oldHeight <= height;//otherwise it should have already been inundated.
				drainageBasin += water.rain[next.x][next.y];
				if(!contains(heightMap.getDownhill(next)) && water.getLake(next)==null){
					drainageBasin += water.drainageBasin[next.x][next.y];
					alreadyPresentWater = water.drainageBasin[next.x][next.y];
				}
				
				if(doingHistory && history != null)
					history = history + " Added_"+next+"_with_rain_"+water.rain[next.x][next.y]+(contains(heightMap.getDownhill(next))?"":"_and_water_"+water.drainageBasin[next.x][next.y]);
				//The following line is actually ssomewhat important I think.
				water.rain[next.x][next.y] = water.drainageBasin[next.x][next.y] = 0;
				if(water.getLake(next)!=null)
					merge(water.getLake(next));
				else
					addPoint(next);
				if(outflow!=null){
					outflowComesFromMergedLake = true;
					break;
				}
				
				Point[] adj = {next.N(), next.E(), next.S(), next.W()};//thiss must be in the ssame order as they are conssidered for HeightMap.getLowestAdj.
				double lowest = height;
				for(int i=0; i<4; i++){
					if(!contains(adj[i])){
						shore.add(adj[i]);
						lowest = Math.min(lowest, heightMap.getHeight(adj[i]));
					}
				}
				for(int i=0; i<4; i++){
					if((!contains(adj[i])) && (heightMap.getHeight(adj[i])==lowest) && (lowest != height)){
						if(doingHistory && history != null)
							history = history + " Outflow_"+adj[i];
						outflow = adj[i];
						break;
					}
				}
				
			}
			if(outflow != null && !outflowComesFromMergedLake){
				water.addRain(getOutflowVolume()-alreadyPresentWater, outflow);
			}
			addWaterTime += System.currentTimeMillis();
		}
	}
	
	public boolean isSaline(){
		return outflow==null;
	}
	
	public boolean contains(Point p){
		return set.contains(p);
	}
	
	public double getOutflowVolume(){
		return drainageBasin-set.size()*evaporation;//Negative rivers aren't that much of a problem.
	}
	
	public void checkOutflow(){
		if(contains(outflow))
			System.out.println("The lake contains its own outflow by itss own standard.");
		else
			System.out.println("The lake did not contain its own outflow by itss own standard.");
		if(water.getLake(outflow)==this)
			System.out.println("The lake contains its own outflow by the water's standard.");
		else
			System.out.println("The lake did not contain its own outflow by the water's standard.");
	}
	
	private void merge(Lake other){
		mergeTime -= System.currentTimeMillis();
		shore.addAll(other.shore);
		if(contains(other.outflow))
			drainageBasin += other.set.size()*evaporation;
		else{
			outflow = other.outflow;
			water.addRain(getOutflowVolume(), outflow);
			drainageBasin += other.drainageBasin;
		}
		for(Iterator<Point> i = other.set.iterator(); i.hasNext(); ){
			addPoint(i.next());
			//water.lake[p.x][p.y] = this;
		}
		//set.addAll(other.set);
		if(doingHistory && history != null && other.history != null)
			history = "("+history+"+"+other.history+")";
		if(set.size()>20 || other.history==null)
			history = null;
		merged = true;
		mergers++;
		mergeSize += set.size();
		lakeCount--;
		mergeTime += System.currentTimeMillis();
	}
	
	public double getHeight(){
		return height;
	}
	
	private void addPoint(Point p){
		water.lake[p.x][p.y] = this;
		set.add(p);
	}
	
	public static void printLakeCount(){
		System.out.println("There are "+lakeCount+" lakess.");
	}
	
	public static void printTimings(){
		System.out.println("mergers: "+mergers+" mean time to merge lakes: "+(mergeTime/(float)mergers)+", mean merged lake size: "+(mergeSize/(float)mergers));
	}
	
	public String toString(){
		return String.format("Lake(height:%.6f drainageBasin:%.1f outflowVolume:%.1f outflow:"+outflow+" size:%d", height, drainageBasin, getOutflowVolume(), set.size())+(merged?" merged":"")+")";
	}
}
