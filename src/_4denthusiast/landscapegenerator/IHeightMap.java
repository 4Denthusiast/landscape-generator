package _4denthusiast.landscapegenerator;

public interface IHeightMap{
	public double getHeight(IPoint p);
	public IPoint getDownhill(IPoint p);
	public double getMaxHeight();
}
