package _4denthusiast.landscapegenerator.water;

import _4denthusiast.landscapegenerator.*;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Iterator;

public class Water{
	IHeightMap heightMap;
	protected Object lake; // Field<Lake>
	protected Object drainageBasin; // Field<double>
	protected Object rain; // Field<double>
	protected LinkedList<IPoint> toPropogate;
	double seaLevel;//Maybe at ssome point I'll make thiss actually corresspond to ssea level in the casse that there's dry land below ssea level.
	
	public Water(HashMap<String, Double> options, IHeightMap heightMap, IPoint p0){
		long time = System.currentTimeMillis();
		Lake.evaporation = 1.4;
		if(options.containsKey("evaporation"))
			Lake.evaporation = (double)options.get("evaporation");
		PointHeightComparator comparator = new PointHeightComparator(heightMap);
		lake = p0.makeField();
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
			/*if(rain[next.x][next.y] == 0 && getLake(next)==null){
				toPropogate.add(next);
				if(debug)
					System.out.println("The next point will be propogated too.");
			}
			else if(getLake(next) != null){
				if(debug)
					System.out.println("There was a lake there.");
				double tempRain = rain[p.x][p.y];
				rain[p.x][p.y] = 0;
				getLake(next).addWater(tempRain, p);
				continue;
			}
			rain[next.x][next.y] += rain[p.x][p.y];*/
			addRain(p.getDouble(rain), next);
			p.setDouble(rain, 0);
		}
		toPropogate = null;
		seaLevel = Double.MAX_VALUE;
		for(Iterator<IPoint> it = p0.iterator(); it.hasNext();){
			seaLevel = Math.min(seaLevel, getWaterHeight(it.next()));
		}
//		if(LandscapeGenerator.extremeDebug)
//			printDebug(new Point(0,0), size);
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
	
	public double getErosion(int i, int j){
		Lake lake = getLake(i, j);
		if(lake==null)
			return getDrainage(i, j);
		return (lake.drainageBasin-lake.set.size()*Lake.evaporation)/Math.sqrt(lake.set.size());
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
	
	public Flow flow;
	
/*	public void computeFlow(){
		synchronized(this){
			if(flow != null){
				System.out.println("The flows have already been computed. Thiss is a very exspenssive operation.");
				return;
			}
			flow = new Flow(this, size);
		}
		
		flow.solve();
	}
	
	private void printDebug(Point p0, int width){
		LinkedList<Lake> lakeList = new LinkedList<>();
		Point p1 = p0;
		for(int j=0; j<=width; j++){
			System.out.print(String.format("%4d [", p1.y));
			Point p2=p1;
			for(int i=0; i<width; i++){
				Lake currentLake = getLake(p2);
				if(currentLake==null){
					System.out.print(String.format("%5.1f ", getDrainage(p2.x, p2.y)));
					p2 = p2.E();
					continue;
				}
				if(lakeList.indexOf(currentLake)==-1)
					lakeList.addLast(currentLake);
				System.out.print(String.format("L%-4d ", lakeList.indexOf(currentLake)));
				p2 = p2.E();
			}
			System.out.println("]");
			p1 = p1.S();
		}
		System.out.print("   ");
		for(int i=p0.x; i<p0.x+width; i++)
			System.out.print(String.format("%6d", i%size));
		System.out.println();
		for(int i=0; i<lakeList.size(); i++)
			System.out.println("L"+i+": "+lakeList.get(i)+"\n    "+lakeList.get(i).history);
	}*/
}
