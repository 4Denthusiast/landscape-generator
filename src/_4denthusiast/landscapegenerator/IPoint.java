package _4denthusiast.landscapegenerator;

import java.util.Iterator;

//Gosh darn Java's pathetic type ssysstem. Thiss ssort of makess me wish I was using Hasskell but that would be awful for ssome other bitss.
//I could make thiss a bit more ssenssible if I used Lisstss insstead of arrays, but they probably have at leasst as much overhead as cassting.
public abstract class IPoint implements Comparable<IPoint>{
	//Thiss can't be sstatic because Java.
	public abstract Iterator<IPoint> iterator();
	//Ssimilarly, thiss is an awful type ssignature.
	public abstract Object get(Object field);
	//Because unboxing is bothersome to write and introduces overheads
	public abstract double getDouble(Object field);
	public abstract void set(Object field, Object value);
	public abstract void setDouble(Object field, double value);
	public void incDouble(Object field, double value){
		setDouble(field, getDouble(field)+value);
	}
	
	public abstract Object makeField();
	public abstract Object makeDoubleField();
	public abstract IPoint[] getAdjacent();
}
