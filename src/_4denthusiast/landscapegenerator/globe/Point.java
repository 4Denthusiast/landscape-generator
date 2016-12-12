package _4denthusiast.landscapegenerator.globe;

import _4denthusiast.landscapegenerator.IPoint;

import java.util.Iterator;

public class Point extends IPoint{
	protected static int size;
	protected static Geometry geo;
	private final int i;
	
	public Point(int i){
		this.i = i;
	}
	
	public int index(){
		return i;
	}
	
	public Iterator<IPoint> iterator(){
		return new Iterator<IPoint>(){
			private int i;
			
			public boolean hasNext(){
				return i<Point.size;
			}
			
			public Point next(){
				return new Point(i++);
			}
		};
	}
	
	public Object get(Object field){
		return ((Object[])field)[i];
	}
	
	public double getDouble(Object field){
		return ((double[])field)[i];
	}
	
	public void set(Object field, Object value){
		((Object[])field)[i] = value;
	}
	
	public void setDouble(Object field, double value){
		((double[])field)[i] = value;
	}
	
	public Object[] makeField(){
		return new Object[size];
	}
	
	public double[] makeDoubleField(){
		return new double[size];
	}
	
	public Point[] getAdjacent(){
		int[] adj = geo.getAdj(i);
		Point[] result = new Point[adj.length];
		for(int j=0; j<adj.length; j++)
			result[j] = new Point(adj[j]);
		return result;
	}
	
	public int compareTo(IPoint o){
		return i - ((Point)o).i;
	}
	
	@Override
	public boolean equals(Object o){
		return (o instanceof Point) && ((Point)o).i == i;
	}
	
	@Override
	public int hashCode(){
		return i;
	}
}
