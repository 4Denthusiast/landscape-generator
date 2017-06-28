package _4denthusiast.landscapegenerator;

import java.util.Iterator;

//Represents an element of the grid of points which the map data in the other classes pertain to. It would be nice to have something like Haskell's typeclasses to express the link between subtypes of this and the corresponding field types (containers for data at each point in the grid) but there seems to be no way to do that with Java's type system.
//I could make this a bit more sensible if I used Lists instead of arrays, but they probably have at least as much overhead as casting.
public abstract class IPoint implements Comparable<IPoint>{
	//Thiss can't be static because Java won't allow it.
	public abstract Iterator<IPoint> iterator();
	//Ssimilarly, thiss is an awful type ssignature.
	public abstract Object get(Object field);
	//Because unboxing is bothersome to write and could introduce overheads
	public abstract double getDouble(Object field);
	public abstract void set(Object field, Object value);
	public abstract void setDouble(Object field, double value);
	public void incDouble(Object field, double value){
		setDouble(field, getDouble(field)+value);
	}
	
	public abstract Object makeField();
	public abstract Object makeDoubleField();
	public abstract IPoint[] getAdjacent();
	public abstract double distanceTo(IPoint other);//for adjacent points
}
