package _4denthusiast.landscapegenerator.water;

import _4denthusiast.landscapegenerator.*;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Iterator;

//common to the globe and torus versions.
public class Water{
	IHeightMap heightMap;
	protected Object lake; // Field<Lake>
	protected Object drainageBasin; // Field<double>, the amount of water flowing into this point through rivers unless it's in a lake.
	protected Object rain; // Field<double>, the amount of water that has yet to be sent on from this point
	protected LinkedList<IPoint> toPropogate; // The points where rain might be non-0.
	double seaLevel;//Maybe at ssome point I'll make thiss actually corresspond to ssea level in the casse that there's dry land below ssea level.
	
	//This assumes that there is a constant amount of rain on each point, and any water at a point either flows to the lowest adjacent point unless it's in a lake, in which case there's some evaporation from the lake's surface. Lakes are the minimum size so that they have a non-positive net inflow.
	public Water(HashMap<String, Double> options, IHeightMap heightMap, IPoint p0){
		long time = System.currentTimeMillis();
		Lake.evaporation = 1.4;
		if(options.containsKey("evaporation"))
			Lake.evaporation = (double)options.get("evaporation");
		PointHeightComparator comparator = new PointHeightComparator(heightMap);
		lake = p0.makeField(); //p0 is just needed because I can't use a static method for this.
		drainageBasin = p0.makeDoubleField();
		this.heightMap = heightMap;
		rain = p0.makeDoubleField();
		toPropogate = new LinkedList<IPoint>();
		for(Iterator<IPoint> it = p0.iterator(); it.hasNext();){
			IPoint p = it.next();
			toPropogate.add(p);
			p.setDouble(rain, 1);
		}
		while(toPropogate.size()>0){
			boolean debug = Math.random()<0.00;
			IPoint p = toPropogate.removeLast();
			if(p.getDouble(rain)==0)
				continue;
			p.incDouble(drainageBasin, p.getDouble(rain));
			IPoint next = heightMap.getDownhill(p);
			if(debug)
				System.out.println(toPropogate.size()+" more wet pointss to go, p="+p+", of height: "+heightMap.getHeight(p)+", next="+next);
			if(next==null || getLake(p) != null){
				if(getLake(p) == null){
					new Lake(p, this, heightMap, comparator);
					if(debug){
						System.out.println("Sstarting a new lake");
						Lake.printLakeCount();
					}
				}else if(debug){
					System.out.println("Adding more water to "+getLake(p));
					getLake(p).checkOutflow();
				}
				getLake(p).addWater(p.getDouble(rain), p);
				p.setDouble(rain, 0);
				continue;
			}
			addRain(p.getDouble(rain), next);
			p.setDouble(rain, 0);
		}
		toPropogate = null;
		seaLevel = Double.MAX_VALUE;
		for(Iterator<IPoint> it = p0.iterator(); it.hasNext();){
			seaLevel = Math.min(seaLevel, getWaterHeight(it.next()));
		}
		System.out.println("Time to generate water: "+(System.currentTimeMillis()-time));
		Lake.printTimings();
	}
	
	protected void addRain(double r, IPoint p){
		if(toPropogate==null)
			return;
		if(p.getDouble(rain)==0)
			toPropogate.add(p);
		p.incDouble(rain, r);
	}
	
	public double getDrainage(IPoint p){
		if(getLake(p) == null)
			return p.getDouble(drainageBasin);
		return getLake(p).drainageBasin;
	}public double getDrainage(int i, int j){
		return getDrainage(new Point(i,j));
	}
	
	public double getOutDrainage(IPoint p){
		if(!isLake(p))
			return p.getDouble(drainageBasin);
		return getLake(p).getOutflowVolume();
	}public double getOutDrainage(int i, int j){
		return getOutDrainage(new Point(i, j));
	}
	
	public Lake getLake(IPoint p){ //It'ss really quite handy for other things to be able to examine lakes directly.
		return (Lake)p.get(lake);
	}public Lake getLake(int i, int j){
		return getLake(new Point(i, j));
	}
	
	public boolean isLake(IPoint p){
		return getLake(p) != null;
	}public boolean isLake(int i, int j){
		return isLake(new Point(i, j));
	}
	
	public boolean isSaline(IPoint p){
		return isLake(p) && getLake(p).isSaline();
	}public boolean isSaline(int i, int j){
		return isSaline(new Point(i, j));
	}
	
	public boolean isFresh(IPoint p){
		return isLake(p) && !getLake(p).isSaline();
	}public boolean isFresh(int i, int j){
		return isFresh(new Point(i, j));
	}
	
	public double getWaterHeight(IPoint p){
		if(getLake(p)!=null)
			return getLake(p).getHeight();
		return heightMap.getHeight(p);
	}public double getWaterHeight(int i, int j){
		return getWaterHeight(new Point(i, j));
	}
	
	public double getWetness(IPoint p){
		double drainage = getDrainage(p);
		if(drainage < 0)
			return 1;
		else
			return Math.min(Math.sqrt(drainage)/20, 1);
	}public double getWetness(int i, int j){
		return getWetness(new Point(i, j));
	}
	
	public IPoint getDownhill(IPoint p){
		Lake l = getLake(p);
		if(l != null){
			return p.equals(l.exitPoint) ? l.outflow : null;
		}else
			return heightMap.getDownhill(p);
	}
	
	public double normaliseHeight(double h){
		return (h-seaLevel)/(heightMap.getMaxHeight()-seaLevel);
	}
	
	public boolean isFlowingTo(IPoint p, IPoint q){//flow from p to q
		if(p == null || q == null)
			return false;
		if(getLake(p) == null)
			return q.equals(heightMap.getDownhill(p));
		return q.equals(getLake(p).outflow);
	}
}
