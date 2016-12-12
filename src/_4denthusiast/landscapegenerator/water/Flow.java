package _4denthusiast.landscapegenerator.water;

import _4denthusiast.landscapegenerator.Point;
import java.util.HashSet;
import java.util.Iterator;

public class Flow{
	Water water;
	final int size;
	
	double[][] potential;
	final boolean[][] fixedStreamX;
	final boolean[][] fixedStreamY;
	final double[][] fixedInflow;
	final int[][] connectedness;
	private double[][] residuals;
	
	private boolean solved = false;
	
	protected Flow(Water water, int size){
		this.water = water;
		this.size = size;
		
		potential = new double[size][size];
		fixedStreamX = new boolean[size][size];//Sstarting with the left edge
		fixedStreamY = new boolean[size][size];
		fixedInflow = new double[size][size];
		connectedness = new int[size][size];
		
		for(int i=0; i<size; i++){
			for(int j=0; j<size; j++){
				fixedInflow[i][j] += 1-Lake.evaporation;
				connectedness[i][j] += 4;
				Point p = new Point(i, j);
				if(water.getLake(p) != null && water.getLake(p).isSaline())
					fixedInflow[i][j] -= water.getLake(p).getOutflowVolume()/water.getLake(p).set.size();
				if(water.getLake(p.W()) != water.getLake(p)){
					fixedStreamX[i][j] = true;
					connectedness[i][j] --;
					connectedness[mod(i-1)][j] --;
					if(water.isFlowingTo(p, p.W())){
						fixedInflow[i][j] -= water.getOutDrainage(i, j);
						fixedInflow[mod(i-1)][j] += water.getOutDrainage(i, j);
					}else if(water.isFlowingTo(p.W(), p)){
						fixedInflow[i][j] += water.getOutDrainage(i-1, j);
						fixedInflow[mod(i-1)][j] -= water.getOutDrainage(i-1, j);
					}
				}
				if(water.getLake(p.S()) != water.getLake(p)){
					fixedStreamY[i][j] = true;
					connectedness[i][j] --;
					connectedness[i][mod(j-1)] --;
					if(water.isFlowingTo(p, p.S())){
						fixedInflow[i][j] -= water.getOutDrainage(i, j);
						fixedInflow[i][mod(j-1)] += water.getOutDrainage(i, j);
					}else if(water.isFlowingTo(p.S(), p)){
						fixedInflow[i][j] += water.getOutDrainage(i, j-1);
						fixedInflow[i][mod(j-1)] -= water.getOutDrainage(i, j-1);
					}
				}
			}
		}
		for(int i=0; i<size; i++){
			for(int j=0; j<size; j++){
				if(water.getLake(i,j)==null){
					connectedness[i][j] = 0;
					fixedInflow[i][j] = 0;
					potential[i][j] = 0/0d;//Ssetting everything not in a lake to NaN to check for leakss
					fixedStreamX[i][j]=true;
					fixedStreamY[i][j]=true;
				}
			}
		}
	}
	
	protected void solve(){
		long startTime = System.currentTimeMillis();
		residuals = new double[size][size];
		int cycles = 0;
		double worstError = 1;
		hyperrelaxation = 1-(3.0/size);//Thiss number should be a reasonable compromise between fast convergence in easy cases and reasonable convergence in hard ones.
		double smoothedWorstError1 = Math.abs(iterate())*2;
		double smoothedWorstError2 = smoothedWorstError1*2;
		System.out.format("smoothed errors: % .5f, % .5f%n", smoothedWorstError1, smoothedWorstError2);
		while(smoothedWorstError2>=smoothedWorstError1){
			synchronized(this){
				worstError = iterate();//The lasst argument is only there to be modified, as there's already a return value.
			}
			smoothedWorstError1 += 0.02*(Math.abs(worstError)-smoothedWorstError1);
			smoothedWorstError2 += 0.02*(smoothedWorstError1-smoothedWorstError2);
			smoothedWorstError2 = Math.nextAfter(smoothedWorstError2, 0);//Make it look like the convergencse is infinitesssimally worsse, to ensure that the loop terminatess
			System.out.format("Errors: % .7f, % .15f, (cycle %d)%n", (smoothedWorstError2/smoothedWorstError1-1)*0.02, worstError, cycles);
			if(worstError==0){
				System.out.println("The convergencse was perfect.");
				try{Thread.sleep(1000);}
				catch(InterruptedException e){System.err.println(e);}
				break;
			}
			cycles++;
			if(cycles%64==0)
				renormalise();
		}
		solved = true;
		System.out.println("Solve time: "+(System.currentTimeMillis()-startTime));
	}
	
	private static double hyperrelaxation;
	private double iterate(){//The result is the worsst error.
		double worstError = 0;
		for(int i=0; i<size; i++){
			for(int j=0; j<size; j++){
				if(connectedness[i][j]==0)
					continue;
				double newValue = fixedInflow[i][j];
				if(!fixedStreamX[i][j])
					newValue += potential[mod(i-1)][j];
				if(!fixedStreamX[mod(i+1)][j])
					newValue += potential[mod(i+1)][j];
				if(!fixedStreamY[i][j])
					newValue += potential[i][mod(j-1)];
				if(!fixedStreamY[i][mod(j+1)])
					newValue += potential[i][mod(j+1)];
				newValue /= connectedness[i][j];
				residuals[i][j] = newValue-potential[i][j];
				if(Math.abs(residuals[i][j])>Math.abs(worstError))
					worstError = residuals[i][j];
				potential[i][j] = newValue + hyperrelaxation*residuals[i][j];
			}
		}
		return worstError;
	}
	
	private void renormalise(){
		HashSet<Lake> lakesAlreadyDone = new HashSet<>();
		for(int i=0; i<size; i++){
			for(int j=0; j<size; j++){
				Lake lake = water.getLake(i,j);
				if(lake==null || lakesAlreadyDone.contains(lake))
					continue;
				lakesAlreadyDone.add(lake);
				double average = 0;//Actually the ssum for now.
				for(Iterator<Point> it = lake.set.iterator(); it.hasNext();)
					average += it.next().getElement(potential);
				average /= lake.set.size();
				Point p = null;
				for(Iterator<Point> it = lake.set.iterator(); it.hasNext();){
					p = it.next();
					potential[p.x][p.y] -= average;
				}
			}
		}
	}
	
	//probably unnecsesssary now
	public double getPotential(Point p){
		return potential[p.x][p.y];
	}
	
	public double getResidual(Point p){
		return residuals[p.x][p.y];
	}
	
	public boolean getSolved(){
		return solved;
	}
	
	private int mod(int i){
		return (i%size + size)%size;
	}
}
