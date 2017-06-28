package _4denthusiast.landscapegenerator.settlements;

import _4denthusiast.landscapegenerator.IPoint;
import _4denthusiast.landscapegenerator.IHeightMap;
import _4denthusiast.landscapegenerator.water.Water;

import java.util.*;
import javafx.util.Pair;

//This actually worked better than expected.
public class Roads{
	private final IHeightMap heightMap;
	private final Water water;
	private final Settlements settlements;
	private Object /*IField<WayPoint>*/ wayPoints;
	private Object /*IField<double>*/ paths;
	private PriorityQueue<WayPoint> qu;
	
	public Roads(IPoint p0, IHeightMap heightMap, Water water, Settlements settlements, Random rand){
		this.heightMap = heightMap;
		this.water = water;
		this.settlements = settlements;
		wayPoints = p0.makeField();
		qu = new PriorityQueue<>();
		for(Iterator<IPoint> it = p0.iterator(); it.hasNext(); ){
			IPoint p = it.next();
			WayPoint wp = new WayPoint(p, rand.nextInt(1<<24));
			p.set(wayPoints, wp);
			qu.add(wp);
		}
		paths = p0.makeDoubleField();
		while(qu.size()>0){
			WayPoint w = qu.poll();
			if(w.upgraded())
				qu.add(w);
			else
				w.link();
		}
		qu = null;
	}
	
	private WayPoint getWayPoint(IPoint p){
		return (WayPoint) p.get(wayPoints);
	}
	
	private void paveRoad(IPoint p, double weight){
		p.incDouble(paths, weight);
	}
	
	public double getPathness(IPoint p){
		return p.getDouble(paths);
	}
	
	public double getBorderLevel(IPoint p, IPoint q){
		return getWayPoint(p).getBorderLevel(getWayPoint(q));
	}
	
	public boolean isOnBorder(IPoint p, double level){
		WayPoint here = getWayPoint(p).getCapital(level);
		IPoint[] adj = p.getAdjacent();
		for(int i=0; i<adj.length; i++){
			if(p.compareTo(adj[i])<0 && here != getWayPoint(adj[i]).getCapital(level))
				return true;
		}
		return false;
	}
	
	public int getKingdomColour(IPoint p, double level){
		return getWayPoint(p).getCapital(level).getColour();
	}
	
	//Stores a point's navigation information, specifically the nearest 6 or so points which are at least as important and the distances (along roads) to them.
	//Importance is calculated according to population and amount of links.
	private class WayPoint implements Comparable<WayPoint>{
		private IPoint location;
		private double importance;
		//This point's importance last time it was added to the queue. By storing things thiss way I avoid having to ssearch for things in the queue to remove them.
		private double prevImportance;
		private HashMap<WayPoint, Pair<Double, WayPoint>> routes;
		private WayPoint parent;
		private int colour;
		
		//All waypoints which link to here, until link is called on this.
		private ArrayList<WayPoint> interested;
		
		
		public WayPoint(IPoint location, int colour){
			this.location = location;
			this.colour = colour;
			interested = new ArrayList<>();
			importance = settlements.getPopulation(location) + Math.random()*0.001;
			prevImportance = importance;
		}
		
		public void link(){
			//System.out.println("Importance: "+importance);
			routes = new HashMap<>();
			IPoint[] adj = location.getAdjacent();
			for(int i=0; i<adj.length; i++)
				routes.put(getWayPoint(adj[i]), new Pair<>(getElementaryDistance(location, adj[i]), null));
			for(int i=0; i<interested.size(); i++){
				WayPoint w = interested.get(i);
				double dist = w.getDistanceTo(this);
				for(Map.Entry<WayPoint, Pair<Double, WayPoint>> next: w.routes.entrySet()){
					double fullDist = dist + next.getValue().getKey();
					if(fullDist < getDistanceTo(next.getKey()))
						routes.put(next.getKey(), new Pair<>(fullDist, w));
				}
			}
			interested = null;
			Iterator<Map.Entry<WayPoint, Pair<Double, WayPoint>>> it = routes.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<WayPoint, Pair<Double, WayPoint>> entry = it.next();
				if(entry.getKey().importance < this.importance)
					it.remove();
			}
			if(routes.size()>0){
				PriorityQueue<Map.Entry<WayPoint, Pair<Double, WayPoint>>> sorted = new PriorityQueue<>(routes.size(),
					(e1, e2) -> e1.getValue().getKey().compareTo(e2.getValue().getKey())
				);
				sorted.addAll(routes.entrySet());
				parent = sorted.peek().getKey();
				for(int i=0; i<4; i++){
					Map.Entry<WayPoint, Pair<Double, WayPoint>> entry = sorted.poll();
					if(entry == null)
						break;
					entry.getKey().registerInterest(this);
				}
			}else
				System.out.println("Orphan: "+importance);
		}
		
		private static final double shoreWeightingFactor = 16;
		//TODO adjusst thiss to take into account different edge lengthss on the ssphere.
		public double getElementaryDistance(IPoint p1, IPoint p2){
			if(water.isLake(p1)){
				if(water.getLake(p1) == water.getLake(p2))
					return 0.4+1.5*(water.getWaterHeight(p1)-heightMap.getHeight(p1));
					//It'ss not actually deep water that's the problem, it'ss the remotenesss & sstorms. If I ever implement weather thiss should be changed to reflect that.
				else
					return shoreWeightingFactor;
			} else if(water.isLake(p2))
				return shoreWeightingFactor;
			else{
				double height1 = heightMap.getHeight(p1);
				double height2 = heightMap.getHeight(p2);
				double maxHeight = Math.max(height1, height2);
				double minHeight = height1+height2-maxHeight;
				return 1+4*(maxHeight-minHeight);
			}
		}
		
		//Only gives distances to directly connected things.
		public double getDistanceTo(WayPoint other){
			if(other == this)
				return 0;
			Pair<Double, WayPoint> way = routes.get(other);
			if(way == null)
				return Double.POSITIVE_INFINITY;
			return way.getKey();
		}
		
		private void registerInterest(WayPoint other){
			interested.add(other);
			importance += 0.1;
			other.paveRoadTo(this, importance);
		}
		
		private void paveRoadTo(WayPoint other, double weight){
			assert(routes.containsKey(other));
			WayPoint mid = routes.get(other).getValue();
			if(mid == null){
				paveRoad(location, weight/2);
				paveRoad(other.location, weight/2);
			}else{
				mid.paveRoadTo(this, weight);
				mid.paveRoadTo(other, weight);
			}
		}
		
		public int compareTo(WayPoint other){
			return ((Double)prevImportance).compareTo(other.prevImportance);
		}
		
		public boolean upgraded(){
			boolean result = importance > prevImportance;
			prevImportance = importance;
			return result;
		}
		
		public WayPoint getCapital(double level){
			WayPoint w = this;
			while(w.parent != null && w.importance < level)
				w = w.parent;
			return w;
		}
		
		public double getBorderLevel(WayPoint that){
			WayPoint w0 = this;
			WayPoint w1 = that;
			double level = 0;
			while(w0 != w1){
				level = Math.min(w0.importance, w1.importance);
				if(w0.importance < w1.importance)
					w0 = w0.parent;
				else
					w1 = w1.parent;
				if(w0 == null || w1 == null)
					return Double.POSITIVE_INFINITY;
			}
			return level;
		}
		
		public int getColour(){
			return colour;
		}
	}
}
