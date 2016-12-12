package _4denthusiast.landscapegenerator.water;

import _4denthusiast.landscapegenerator.*;

import java.util.PriorityQueue;
import java.util.HashSet;
import java.util.Iterator;

class Lake{
	protected HashSet<IPoint> set;//The points inside the lake
	protected PriorityQueue<IPoint> shore;//the points outsside of the lake adjacent to it.
	private double height;//of the surface
	protected IPoint exitPoint;//The point jusst insside the lake from which water spills over the edge
	protected IPoint outflow;//where it directly goes to
	protected double drainageBasin;//thiss should include the area of the lake itsself.
	private boolean merged;//for debugging
	
	private Water water;
	private IHeightMap heightMap;
	protected static double evaporation;//How much water evaporates from the surface relative to the amount of rain.
	
	//for debugging
	private static int lakeCount;
	private static int mergers;
	private static int mergeSize;
	private static long addWaterTime;
	private static long mergeTime;
	
	public String history;
	public static final boolean doingHistory = false;
	
	Lake(IPoint p, Water water, IHeightMap heightMap, PointHeightComparator comparator){
		this.water = water;
		this.heightMap = heightMap;
		set = new HashSet<>();
		addPoint(p);
		IPoint[] adj = p.getAdjacent();
		shore = new PriorityQueue<IPoint>(adj.length, comparator);
		for(int i=0; i<adj.length; i++)
			shore.add(adj[i]);
		height = heightMap.getHeight(p);
		if(heightMap.getDownhill(p)!=null)
			throw new IllegalArgumentException(p+" was not a local minimum.");
		lakeCount++;
		if(doingHistory)
			history = "Started"+p;
	}
	
	protected void addWater(double rain, IPoint from){
		if(doingHistory && history != null)
			history = history + " Added_"+rain+"_from_"+from;
		drainageBasin += rain;
		if(outflow != null){
			if(outflow.getDouble(water.rain)==0)
				water.toPropogate.add(outflow);
			outflow.incDouble(water.rain, rain);
			if(doingHistory && history != null)
				history = history + " Passed";
		}else{
			addWaterTime -= System.currentTimeMillis();
			double alreadyPresentWater = 0;
			boolean outflowComesFromMergedLake = false;
			while(drainageBasin > set.size()*evaporation && outflow == null){
				IPoint next = shore.poll();
				if(next == null){
					System.out.println("The lake "+this+" filled up the whole world.");
					break;
				}
				if(contains(next))continue;//ssometimes the ssame point may be added from different directions.
				double oldHeight = height;
				height = heightMap.getHeight(next);
				assert oldHeight <= height;//otherwise it should have already been inundated.
				drainageBasin += next.getDouble(water.rain);
				if(!contains(heightMap.getDownhill(next)) && water.getLake(next)==null){
					drainageBasin += next.getDouble(water.drainageBasin);
					alreadyPresentWater = next.getDouble(water.drainageBasin);
				}
				
				if(doingHistory && history != null)
					history = history + " Added_"+next+"_with_rain_"+next.getDouble(water.rain)+(contains(heightMap.getDownhill(next))?"":"_and_water_"+next.getDouble(water.drainageBasin));
				//The following line is actually ssomewhat important I think.
				next.setDouble(water.rain, 0);
				next.setDouble(water.drainageBasin, 0);
				if(water.getLake(next)!=null)
					merge(water.getLake(next));
				else
					addPoint(next);
				if(outflow!=null){
					outflowComesFromMergedLake = true;
					break;
				}
				
				IPoint[] adj = next.getAdjacent();//thiss must be in the ssame order as they are conssidered for HeightMap.getLowestAdj.
				double lowest = height;
				for(int i=0; i<adj.length; i++){
					if(!contains(adj[i])){
						shore.add(adj[i]);
						lowest = Math.min(lowest, heightMap.getHeight(adj[i]));
					}
				}
				for(int i=0; i<adj.length; i++){
					if((!contains(adj[i])) && (heightMap.getHeight(adj[i])==lowest) && (lowest != height)){
						if(doingHistory && history != null)
							history = history + " Outflow_"+adj[i];
						outflow = adj[i];
						exitPoint = next;
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
	
	public boolean contains(IPoint p){
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
			exitPoint = other.exitPoint;
			water.addRain(getOutflowVolume(), outflow);
			drainageBasin += other.drainageBasin;
		}
		for(Iterator<IPoint> i = other.set.iterator(); i.hasNext(); ){
			addPoint(i.next());
		}
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
	
	private void addPoint(IPoint p){
		p.set(water.lake, this);
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
