package _4denthusiast.landscapegenerator.globe;

import java.awt.Graphics;
import java.awt.Color;

import java.util.HashMap;
import java.util.ArrayList;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL45;
import org.lwjgl.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import _4denthusiast.landscapegenerator.globe.util.*;

// Generates a fibonacci spherical grid (like http://onlinelibrary.wiley.com/doi/10.1256/qj.05.227/pdf ), pretty and well-distributed
public class Geometry{
	private final int size; //In this case it's the total number of points (which may be any sufficiently large integer) rather than the edge-length.
	private double[][] points;
	private int[][] adjs; //The indices of the points adjacent to each point.
	private int[] fibs;
	private static final double φ = Math.PI*(Math.sqrt(5)-3);
	private static final int[] adjPermutation = new int[]{-2,0,2,1,-1};
	
	private int pointsBuffer;
	
	public Geometry(HashMap<String, Double> options){
		if(options.containsKey("rose size"))
			size = (int)(double)options.get("rose size");
		else
			size = 101;
		if(size<16){
			//Several of the polar quads that I've implemented as exceptions are flipped when size is small.
			System.err.println("The lattice must include at least 16 points.");
			System.exit(1);
		}
		initFibs();
		points = new double[size][3];
		adjs = new int[size][];
		int n = 1; //The index of the esstimated relevant fibonacci number
		for(int i=0; i<size; i++){
			double t = i+0.5;
			double z = 1 - 2*t/size;
			double xy = Math.sqrt(1-z*z);
			double angle = φ*t;
			points[i][0] = xy*Math.cos(angle);
			points[i][1] = xy*Math.sin(angle);
			points[i][2] = z;
		}
		for(int i=0; i<size; i++){
			int target = (int)((1-points[i][2]*points[i][2])*size);
			while(n<fibs.length-1 && fibs[n  ]*fibs[n  ]< target) n++;
			while(n>0             && fibs[n-1]*fibs[n-1]>=target) n--;
			ArrayList<Integer> adj = new ArrayList<>();
			for(int j=0; j<10; j++){
				int n2 = adjPermutation[j%5];
				n2 += n;
				if(0<=n2 && n2 < fibs.length){
					int newAdjPoint = i+ (j<5?1:-1)* fibs[n2];
					if(0 <= newAdjPoint && newAdjPoint < size){
						if(adj.size()<2 || !farther(i, adj.get(adj.size()-2), adj.get(adj.size()-1), newAdjPoint))
							adj.add(newAdjPoint);
						else
							adj.set(adj.size()-1, newAdjPoint);
					}
				}
			}
			//Near the poles some points are connected to others where the difference in their indices is unusual (i.e. occurs finitely many times) so these are added as exceptions.
			if(i==0){
				adj.add(2,5);
				adj.add(2,8);
			}
			else if(i==size-1){
				adj.add(2,size-6);
				adj.add(2,size-9);
			}else if((i==2 || i==size-3) && !farther(2,10,15,7)){
				if(i==2)
					adj.add(3, 15);
				else
					adj.add(4, size-16);
			}
			while(farther(i, adj.get(adj.size()-1), adj.get(0), adj.get(1)))
				adj.remove(0);
			
			adjs[i] = new int[adj.size()];
			boolean flip = n%2==0;
			for(int j=0; j<adj.size(); j++){
				adjs[i][j] = (int)adj.get(flip?adj.size()-1-j:j);
			}
		}
		Point.geo = this;
		Point.size = size;
	}
	
	private void initFibs(){
		int f0 = 1;
		int f1 = 2;
		int n;
		for(n=1; f1<size; n++){
			int t = f0;
			f0 = f1;
			f1 = t + f0;
		}
		fibs = new int[n];
		fibs[0] = 1;
		fibs[1] = 2;
		for(int i=2; i<fibs.length; i++)
			fibs[i] = fibs[i-1] + fibs[i-2];
	}
	
	//Does the tetrahedron OLMR have positive volume (considering the points ordered by their actual positions on the sphere) (i.e. should OM or LR be connected)?
	private boolean farther(int o, int l, int m, int r){
		if(m<0 || m>=size)
			return true;
		if(l<0 || r<0 || l>=size || r>=size)
			return false;
		double[] po = points[o]; double[] pl = points[l];
		double[] pm = points[m]; double[] pr = points[r];
		double[] O = Vec3.cross(Vec3.subtract(pr, po), Vec3.subtract(pl, po));
		boolean reverse = Vec3.dot(Vec3.cross(Vec3.subtract(pm,po), Vec3.subtract(pl,pr)), po)<0;
		return reverse ^ Vec3.dot(O, po) > Vec3.dot(O, pm);
	}
	
	//I really ought to jusst exspose more of the data, rather than putting all thiss GL sstuff in here.
	public int getPointsBuffer(){
		if(pointsBuffer == 0){
			ByteBuffer bb = BufferUtils.createByteBuffer(getNumLines()*4*8);
			//(3 dimensions + 1 padding) * 8 bytes per double
			for(int i=0; i<size; i++){
				for(int j=0; j<3; j++)
					bb.putDouble(points[i][j]);
				bb.putDouble(0);//alignment
			}
			bb.flip();
			pointsBuffer = GL15.glGenBuffers();
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, pointsBuffer);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bb, GL15.GL_STATIC_DRAW);
		}
		return pointsBuffer;
	}
	
	//Ssort of deprecated.
	public void bufferPoints(){
		ByteBuffer bb = BufferUtils.createByteBuffer(getNumLines()*4*4);
		//(3 dimensions + 1 padding) * 4 bytes per float
		for(int i=0; i<size; i++){
			GLUtils.bufferDoubles(bb, points[i]);
			GLUtils.bufferDoubles(bb, points[i]);
		}
		bb.flip();
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bb, GL15.GL_STATIC_DRAW);
	}
	
	public void bufferLines(){
		ByteBuffer bb = BufferUtils.createByteBuffer(getNumLines()*2*4);
		//2 ends per line * 4 bytes per int
		for(int i=0; i<size; i++){
			for(int j=0; j<adjs[i].length; j++){
				if(adjs[i][j]>i){
					bb.putInt(i);
					bb.putInt(adjs[i][j]);
				}
			}
		}
		bb.flip();
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, bb, GL15.GL_STATIC_DRAW);
	}
	
	public void bufferFaces(){
		ByteBuffer bb = BufferUtils.createByteBuffer(getNumFaces()*3*4);
		//3 verts per face * 4 bytes per int
		for(int i=0; i<size; i++){
			for(int j=0; j<adjs[i].length; j++){
				int j1 = (j+1)%adjs[i].length;
				if(adjs[i][j]>i && adjs[i][j1]>i){
					bb.putInt(i);
					bb.putInt(adjs[i][j]);
					bb.putInt(adjs[i][j1]);
				}
			}
		}
		bb.flip();
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, bb, GL15.GL_STATIC_DRAW);
	}
	
	public int bufferConnPointers(){
		GLUtils.checkGL();
		ByteBuffer bb = BufferUtils.createByteBuffer(size*4*4);
		int n = 0;
		for(int i=0; i<size; i++){
			bb.putInt(n);
			n += adjs[i].length+1;
		}
		bb.flip();
		int buf = GL15.glGenBuffers();
		GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, buf);
		GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, bb, GL15.GL_STREAM_DRAW);
		GLUtils.checkGL();
		return buf;
	}
	
	public int bufferConnections(){
		ByteBuffer bb = BufferUtils.createByteBuffer((getNumLines()*2 + size)*4);
		for(int i=0; i<size; i++){
			bb.putInt(adjs[i].length);
			for(int j=0; j<adjs[i].length; j++)
				bb.putInt(adjs[i][j]);
		}
		bb.flip();
		int buf = GL15.glGenBuffers();
		GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, buf);
		GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, bb, GL15.GL_STREAM_DRAW);
		return buf;
	}
	
	public int bufferEdgeIndices(){
		ByteBuffer bb = BufferUtils.createByteBuffer(getNumLines()*2*4);
		for(int i=0; i<size; i++){
			for(int j=0; j<adjs[i].length; j++)
				bb.putInt(i);
		}
		bb.flip();
		int buf = GL15.glGenBuffers();
		GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, buf);
		GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, bb, GL15.GL_STREAM_DRAW);
		return buf;
	}
	
	//This follows from Euler's identity and the fact that there all faces are triangles.
	public int getNumLines(){
		return size*3-6;
	}
	
	public int getNumFaces(){
		return size*2-4;
	}
	
	public int getSize(){
		return size;
	}
	
	public double[] getPoint(int i){
		double[] p = new double[3];
		System.arraycopy(points[i], 0, p, 0, 3);
		return p;
	}
	
	public int[] getAdj(int i){
		return adjs[i];
	}
}
