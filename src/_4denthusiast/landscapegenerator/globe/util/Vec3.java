package _4denthusiast.landscapegenerator.globe.util;

public class Vec3{
	public static double[] transform(double[] vec, double[] mat){
		return new double[]{
			mat[0]*vec[0] + mat[1]*vec[1] + mat[2]*vec[2],
			mat[3]*vec[0] + mat[4]*vec[1] + mat[5]*vec[2],
			mat[6]*vec[0] + mat[7]*vec[1] + mat[8]*vec[2]
		};
	}
	
	public static double[] cross(double[] a, double[] b){
		return new double[]{
			a[1]*b[2] - b[1]*a[2],
			a[2]*b[0] - b[2]*a[0],
			a[0]*b[1] - b[0]*a[1]
		};
	}
	
	public static double[] subtract(double[] a, double[] b){
		return new double[]{
			a[0] - b[0],
			a[1] - b[1],
			a[2] - b[2]
		};
	}
	
	public static double dot(double[] a, double[] b){
		return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
	}
}
