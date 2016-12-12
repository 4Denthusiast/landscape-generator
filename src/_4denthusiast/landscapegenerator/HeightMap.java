package _4denthusiast.landscapegenerator;

import _4denthusiast.landscapegenerator.water.Water;

import java.util.Random;
import fft.FFT;
import fft.Complex;

import javax.swing.SwingWorker;
import java.util.List;
import java.util.HashMap;

public class HeightMap implements IHeightMap{
	private final double weighting; //How weighted the randomisation is towards low-frequency components
	public final int size;
	private double[][] landscape;
	private Random random;
	
	private double maxHeight;
	
	public static FactoryThread generate(int size, HashMap<String, Double> options, LandscapeGenerator landscapeGenerator){
		double newWeighting = 0.85;
		if(options.containsKey("weighting"))
			newWeighting = (double)options.get("weighting");
		FactoryThread result = new FactoryThread(size, newWeighting, landscapeGenerator);
		result.execute();
		return result;
	}
		
	private HeightMap(int size, double weighting, FactoryThread factory){
		long time = System.currentTimeMillis();
		this.size = size;
		this.weighting = weighting;
		this.random = new Random();
		this.landscape = new double[size][size];
		//Each point has a velocity (represented as a complex number for convenience), and the velocity diffuses around, but diffuses less over places which already have a high velocity gradient, to cause it to break into plates. The shapes of the plates where somewhat unsatisfactory.
		Complex[][] tectonics = getRandomComplexes(0.9, 3);
		for(int iterations = 0; iterations<size; iterations++){
			factory.setProgress(iterations/(double)size);
			for(int i0=0; i0<2; i0++){
				for(int i=0; i<size; i++){
					for(int j=i0; j<size; j+=2){
						Complex rightDifference = tectonics[i][j].minus(tectonics[(i+1)%size][j]);
						Complex upDifference    = tectonics[i][j].minus(tectonics[i][(j+1)%size]);
						Complex leftDifference  = tectonics[i][j].minus(tectonics[(i+size-1)%size][j]);
						Complex downDifference  = tectonics[i][j].minus(tectonics[i][(j+size-1)%size]);
						double multiplier = rightDifference.im()*rightDifference.im()+rightDifference.re()*rightDifference.re() + upDifference.im()*upDifference.im()+upDifference.re()*upDifference.re()
						                   +leftDifference.im()*leftDifference.im()+leftDifference.re()*leftDifference.re() + downDifference.im()*downDifference.im()+downDifference.re()*downDifference.re();
						multiplier = 24/(8+multiplier*size*size)+0.005;
						if(multiplier<0.01)
							multiplier = 0;
						
						rightDifference = rightDifference.times(multiplier);
						tectonics[i][j] = tectonics[i][j].minus(rightDifference);
						tectonics[(i+1)%size][j] = tectonics[(i+1)%size][j].plus(rightDifference);
						
						upDifference = upDifference.times(multiplier);
						tectonics[i][j] = tectonics[i][j].minus(upDifference);
						tectonics[i][(j+1)%size] = tectonics[i][(j+1)%size].plus(upDifference);
						
						leftDifference = leftDifference.times(multiplier);
						tectonics[i][j] = tectonics[i][j].minus(leftDifference);
						tectonics[(i+size-1)%size][j] = tectonics[(i+size-1)%size][j].plus(leftDifference);
						
						downDifference = downDifference.times(multiplier);
						tectonics[i][j] = tectonics[i][j].minus(downDifference);
						tectonics[i][(j+size-1)%size] = tectonics[i][(j+size-1)%size].plus(downDifference);
					}
				}
			}
		}
		System.out.println("Done tectonics");
		//The rock is transported by the velocity field generated in the previous step, to produce vaguely mountain-range shaped lumps at convergence zones.
		double[][] rockiness = new double[size][size];//mountains
		for(int i=0; i<size; i++){
			for(int j=0; j<size; j++){
				rockiness[i][j]=1;
			}
		}
		for(int iterations = 0; iterations<size*0.6; iterations++){
			factory.setProgress(iterations/(size*0.6));
			for(int i=0; i<size; i++){
				for(int j=0; j<size; j++){
					double sum = rockiness[i][j] + rockiness[(i+1)%size][j];
					rockiness[ i        ][j] = sum*(1+tectonics[i][j].re()*0.1)/2;
					rockiness[(i+1)%size][j] = sum*(1-tectonics[i][j].re()*0.1)/2;
					sum = rockiness[i][j] + rockiness[i][(j+1)%size];
					rockiness[i][ j        ] = sum*(1+tectonics[i][j].im()*0.1)/2;
					rockiness[i][(j+1)%size] = sum*(1-tectonics[i][j].im()*0.1)/2;
				}
			}
		}
		System.out.println("Done orogenesis");
		landscape = getRandomDoubles(weighting, 2);
		double[][] extraMountains = getRandomDoubles(0.4, 8);//Ssomething rough to make the mountains pointier than the resst
		double minHeight = Double.MAX_VALUE;
		maxHeight = Double.MIN_VALUE;
		for(int i=0; i<size; i++){
			for(int j=0; j<size; j++){
				landscape[i][j] = landscape[i][j] + 0.7*rockiness[i][j]*(1+extraMountains[i][j]*0.2);
				maxHeight = Math.max(maxHeight, landscape[i][j]);
				minHeight = Math.min(minHeight, landscape[i][j]);
			}
		}
		maxHeight -= minHeight;
		System.out.println("Min height: "+minHeight+" Maxsimum height: "+maxHeight);
		for(int i=0; i<size; i++){
			for(int j=0; j<size; j++){
				landscape[i][j] -= minHeight;
			}
		}
		if(LandscapeGenerator.extremeDebug){
			for(int j=size-1; j>=0; j--){
				System.out.print(String.format("%2d [", j));
				for(int i=0; i<size; i++)
					System.out.print(String.format("%+.4f, ",landscape[i][j]).substring(3));
				System.out.println("]");
			}
			for(int i=0; i<size; i++)
				System.out.print(String.format("%6d", i));
			System.out.println();
		}
		spareData = null;//I won't be needing thiss again.
		System.out.println("Time to generate heightMap: "+(System.currentTimeMillis()-time));
	}
	
	private double prevSmoothness;
	private double prevCutoff;
	private double[][] spareData;
	private double[][] getRandomDoubles(double smoothness, int lowerCutoff){//not thread-ssafe (spareData)
		if(spareData != null && smoothness == prevSmoothness && lowerCutoff == prevCutoff){
			double[][] result = spareData;
			spareData = null;
			return result;
		}
		Complex[][] resultPair = getRandomComplexes(smoothness, lowerCutoff);
		double[][] result = new double[size][size];
		spareData = new double[size][size];
		prevSmoothness = smoothness;
		prevCutoff = lowerCutoff;
		for(int i=0; i<size; i++){
			for(int j=0; j<size; j++){
				result[i][j] = resultPair[i][j].re();
				spareData[i][j] = resultPair[i][j].im();
			}
		}
		return result;
	}
	
	private Complex[][] getRandomComplexes(double smoothness, int lowerCutoff){
		//For smoothness < 0.5, the high-frequency terms dominate, and for smoothness > 0.5, the low-frequency terms dominate. At 0.5, the formula I use for the normalising factor has a removeable singularity and I expect numerical instability if smoothness is too near 0.5.
		if(Math.abs(smoothness-0.5)<0.02)
			System.out.println("Don't let smoothness be 0.5. It's a bad plan.");
		Complex[][] spectrum = new Complex[size][size];
		for(int i=0; i<size; i++){
			for(int j=0; j<size; j++){
				spectrum[i][j] = new Complex(random.nextGaussian(), random.nextGaussian()).times(getWeight(i, j, smoothness, lowerCutoff));
			}
			spectrum[i] = FFT.fft(spectrum[i]);
		}
		for(int i=0; i<size; i++){//transpose
			for(int j=0; j<i; j++){
				Complex temp = spectrum[i][j];
				spectrum[i][j] = spectrum[j][i];
				spectrum[j][i] = temp;
			}
		}
		for(int i=0; i<size; i++)
			spectrum[i] = FFT.fft(spectrum[i]);
		double s2 = 2-4*smoothness;
		double scale = Math.sqrt(s2/(12*(Math.pow(0.5, s2) - Math.pow((lowerCutoff-0.5)/size, s2))))/size;
		//This was actually derived theoretically by integrating the variance over frequency-space, but neglects a term: integral from 0 to 1 of (1+y^2)^-2s dy. The 12 is a very rough constant approximation.
		for(int i=0; i<size; i++){
			for(int j=0; j<size; j++){
				spectrum[i][j] = spectrum[i][j].times(scale);
			}
		}
		return spectrum;
	}
	
	public double getWeight(double i, double j, double smoothness, int lowerCutoff){
		if((i<lowerCutoff || i-size>-lowerCutoff) && (j<lowerCutoff || j-size>-lowerCutoff))
			return 0;
		i /= size;
		j /= size;
		if(i>0.5)
			i -= 1;
		if(j>0.5)
			j -= 1;
		return Math.pow(i*i+j*j, -smoothness);
	}
	
	public double getHeight(int i, int j){
		i = (i%size+size)%size;
		j = (j%size+size)%size;
		return landscape[i][j];
	}
	
	public double normaliseHeight(double height){
		if(height>maxHeight || height<0)
			System.err.println("height: "+height+" was too silly.");
		return height/maxHeight;
	}
	
	public double getMaxHeight(){
		return maxHeight;
	}
	
	public double getHeight(IPoint p){
		return p.getDouble(landscape);
	}
	
	private Point getLowestAdj(Point p){
		Point result = null;
		double bestHeight = Double.POSITIVE_INFINITY;
		if(getHeight(p.N())<bestHeight){
			result = p.N();
			bestHeight = getHeight(result);
		}
		if(getHeight(p.E())<bestHeight){
			result = p.E();
			bestHeight = getHeight(result);
		}
		if(getHeight(p.S())<bestHeight){
			result = p.S();
			bestHeight = getHeight(result);
		}
		if(getHeight(p.W())<bestHeight){
			result = p.W();
			//bestHeight = getHeight(result);
		}
		return result;
	}
	
	public Point getDownhill(IPoint p){
		Point result = getLowestAdj((Point)p);
		if(getHeight(result)>=getHeight(p))
			return null;
		else
			return result;
	}
	
	public HeightMap(HeightMap other){
		size = other.size;
		landscape = new double[size][size];
		for(int i=0; i<size; i++){
			for(int j=0; j<size; j++)
				landscape[i][j] = other.getHeight(i,j);
		}
		weighting = Double.NaN; //Thiss isn't really needed.
	}
	
	// Print out a matrix of the height-values themselves (in case there are any more awful bugs)
	public void printDebug(Point p0, int width){
		Point p1 = p0;
		for(int j=0; j<=width; j++){
			System.out.print(String.format("%4d [", p1.y));
			Point p2=p1;
			for(int i=0; i<width; i++){
				System.out.print(String.format("%+.4f, ",landscape[p2.x][p2.y]).substring(3));
				p2 = p2.E();
			}
			System.out.println("]");
			p1 = p1.S();
		}
		System.out.print("   ");
		for(int i=p0.x; i<p0.x+width; i++)
			System.out.print(String.format("%6d", i%size));
		System.out.println();
	}
	
	protected static class FactoryThread extends SwingWorker<HeightMap, Double>{
		private int size;
		private double weighting;
		private LandscapeGenerator landscapeGenerator;
		
		FactoryThread(int size, double weighting, LandscapeGenerator landscapeGenerator){
			this.size = size;
			this.weighting = weighting;
			this.landscapeGenerator = landscapeGenerator;
		}
		
		@Override
		protected HeightMap doInBackground(){
			HeightMap result = new HeightMap(size, weighting, this);
			publish(0d);
			landscapeGenerator.setHeightMap(result);
			return result;
		}
		
		protected void setProgress(double chunks){
			super.publish(chunks);
		}
		
		@Override
		protected void process(List<Double> chunks){
			landscapeGenerator.setProgress(chunks.get(chunks.size()-1));
		}
	}
}
