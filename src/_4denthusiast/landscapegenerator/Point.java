package _4denthusiast.landscapegenerator;

import java.util.Iterator;

public class Point extends IPoint{
	public static int size;
	public final int x;
	public final int y;
	
	public Point(int x, int y){
		this.x = (x%size+size)%size;
		this.y = (y%size+size)%size;
	}
	
	public Point E(){
		return new Point(x+1, y/*, size*/);
	}
	
	public Point W(){
		return new Point(x-1, y/*, size*/);
	}
	
	public Point N(){
		return new Point(x, y+1/*, size*/);
	}
	
	public Point S(){
		return new Point(x, y-1/*, size*/);
	}
	
	public double distanceTo(Point p){//Given that everything else works that way I'm tempted to make this taxicab.
		int dx = x-p.x;
		int dy = y-p.y;
		if(dx > size/2)
			dx -= size;
		if(dy > size/2)
			dy -= size;
		return Math.hypot(dx, dy);
	}
	
	public double getElement(double[][] array){
		return array[x][y];
	}
	
	public Point[] getAdjacent(){
		return new Point[]{N(),E(),S(),W()};
	}
	
	public double distanceTo(IPoint other){
		//required: other is adjacent.
		return 1;
	}
	
	@Override
	public Iterator<IPoint> iterator(){
		return new Iterator<IPoint>(){
			private int x, y;
			
			public boolean hasNext(){
				return y < size;
			}
			
			public Point next(){
				Point p = new Point(x++,y);
				if(x>size){
					x=0;
					y++;
				}
				return p;
			}
		};
	}
	
	@Override
	public Object get(Object field){
		return ((Object[][])field)[x][y];
	}
	
	@Override
	public double getDouble(Object field){
		return ((double[][])field)[x][y];
	}
	
	@Override
	public void set(Object field, Object value){
		((Object[][])field)[x][y] = value;
	}
	
	@Override
	public void setDouble(Object field, double value){
		((double[][])field)[x][y] = value;
	}
	
	@Override
	public Object[][] makeField(){
		return new Object[size][size];
	}
	
	@Override
	public double[][] makeDoubleField(){
		return new double[size][size];
	}
	
	@Override
	public boolean equals(Object other){
		if(!(other instanceof Point))return false;
		return (((Point)other).x==x) && (((Point)other).y==y) && (((Point)other).size==size);
	}
	
	@Override
	public int hashCode(){
		return (int)(size*1.618033989)*x+y;
	}
	
	@Override
	public int compareTo(IPoint o){
		return (x-((Point)o).x)*size+y-((Point)o).y;
	}
	
	public String toString(){
		return "("+x+","+y+")";
	}
}
