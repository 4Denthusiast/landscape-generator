package _4denthusiast.landscapegenerator.globe.util;

public class Mat3{
	public static double[] identity(){
		return new double[]{1,0,0,0,1,0,0,0,1};
	}
	
	//Mutates
	public static double[] orthonormalize(double[] mat){
		for(int i=0; i<9; i+=3){
			for(int j=0; j<i; j+=3){
				double c = mat[0+i]*mat[0+j] + mat[1+i]*mat[1+j] + mat[2+i]*mat[2+j];
				mat[0+i] -= c*mat[0+j]; mat[1+i] -= c*mat[1+j]; mat[2+i] -= c*mat[2+j];
			}
			double scale = 1/Math.sqrt(mat[0+i]*mat[0+i] + mat[1+i]*mat[1+i] + mat[2+i]*mat[2+i]);
			mat[0+i] *= scale; mat[1+i] *= scale; mat[2+i] *= scale;
		}
		return mat;
	}
	
	//Mutates
	public static double[] approxRotate(double[] mat, double x, double y){
		for(int i=0; i<3; i++){
			mat[0+i] += mat[6+i]*x;
			mat[3+i] += mat[6+i]*y;
		}
		return orthonormalize(mat);
	}
}
