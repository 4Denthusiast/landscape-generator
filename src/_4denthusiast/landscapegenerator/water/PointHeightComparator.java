package _4denthusiast.landscapegenerator.water;

import _4denthusiast.landscapegenerator.*;

import java.util.Comparator;

class PointHeightComparator implements Comparator<IPoint>{
	private IHeightMap heightMap;
	
	PointHeightComparator(IHeightMap heightMap){
		this.heightMap = heightMap;
	}
	
	public int compare(IPoint p, IPoint q){
		double offset = heightMap.getHeight(p)-heightMap.getHeight(q);
		if(offset<0)return -1;
		if(offset>0)return 1;
		return 0;
	}
}
