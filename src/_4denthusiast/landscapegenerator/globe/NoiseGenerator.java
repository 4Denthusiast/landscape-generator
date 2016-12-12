package _4denthusiast.landscapegenerator.globe;

import _4denthusiast.landscapegenerator.globe.util.GLUtils;

import java.util.Random;
import java.util.HashMap;

import org.lwjgl.opengl.*;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;

//Based on the Legendre spherical harmonic algorithm from http://arxiv.org/pdf/1410.1748v1.pdf
public class NoiseGenerator{
	private final Random rand = new Random();
	private final long seed;
	private int seedOffset;
	private final Geometry geo;
	private final int size, L;
	private double[][] a, b;
	private static final int groupSize = 256;
	private static final String shaderSource = 
		"#version 440\n"+
		"layout(std430) buffer;\n"+
		"layout(local_size_x = "+groupSize+", local_size_y = 1, local_size_z = 1) in;\n"+
		"precision highp float;\n"+
		"\n"+
		"layout(binding = 0) buffer Points{ dvec3[] points; };\n"+
		"layout(binding = 1) buffer Weights{ dvec2[] weights; };\n"+
		"layout(binding = 2) buffer AB{ dvec2[] ab; };\n"+
		"layout(location = 3) uniform uint L;\n"+
		"layout(location = 4) uniform uint size;\n"+
		"layout(binding = 5) buffer Heights{ double[] heights; };\n"+//The uniform and buffer block indices could overlap.
		"\n"+
		"void main(){\n"+
		"	if(gl_GlobalInvocationID.x > points.length())\n"+
		"		return;\n"+
		"	double result = 0;\n"+
		"	dvec3 p = points[gl_GlobalInvocationID.x];\n"+
		"	double xy = sqrt(1-p.z*p.z);\n"+
		"	dmat2 cs1 = dmat2(p.x,p.y,-p.y,p.x)/xy;\n"+
		"	double Pmm = sqrt(2.0);\n"+
		"	dvec2 cs = dvec2(sqrt(0.5),0);\n"+
		"	for(int m = 0; m < L; m+=1){\n"+
		"		double pprevP = 0;\n"+
		"		double prevP = Pmm;\n"+
		"		result += dot(weights[(m+1)*(m+2)/2-1], cs) * Pmm;\n"+
		"		for(int l=m+1; l<L; l++){\n"+
		"			dvec2 curAb = ab[l*(l-1)/2+m];\n"+
		"			double P = curAb.x*(p.z*prevP + curAb.y*pprevP);\n"+
		"			pprevP = prevP;\n"+
		"			prevP = P;\n"+
		"			result += P*dot(weights[(l*(l+1))/2+m], cs);\n"+
		"		}\n"+
		"		if(m==0)\n"+
		"			cs = cs1[0];\n"+
		"		else\n"+
		"			cs = cs1 * cs;\n"+
		"		Pmm *= -xy*sqrt(1+0.5/(m+1));\n"+
		"	}\n"+
		"	heights[gl_GlobalInvocationID.x] = result;\n"+
		"}";
	private static final int bPoints, bWeights, bAb, uL, uSize, bHeights;
	static{bPoints = 0; bWeights = 1; bAb = 2; uL = 3; uSize = 4; bHeights = 5;}
	private int prog, abBuf;
	
	public NoiseGenerator(HashMap<String, Double> options, Geometry geo){
		if(options.containsKey("seed"))
			seed = (long)(double)options.get("seed");
		else
			seed = rand.nextLong();
		this.geo = geo;
		size = geo.getSize();
		L = (int)Math.ceil(Math.sqrt(size));
		
		this.prog = GL20.glCreateProgram();
		int csh = GL20.glCreateShader(GL43.GL_COMPUTE_SHADER);
		GL20.glShaderSource(csh, shaderSource);
		GL20.glCompileShader(csh);
		if(GL20.glGetShaderi(csh, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
			throw new RuntimeException("Shader compilation failed: "+GL20.glGetShaderInfoLog(csh, 1024));
		GL20.glAttachShader(prog, csh);
		GL20.glLinkProgram(prog);
		GL20.glUseProgram(prog);
		GL30.glUniform1ui(uL, L);
		GL30.glUniform1ui(uSize, size);
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, bPoints, geo.getPointsBuffer());
		
		ByteBuffer bb = BufferUtils.createByteBuffer(size*4 *8);
		a = new double[L][];
		b = new double[L][];
		for(int l=1; l<L; l++){
			double ls = l*l;
			double lm1s = (l-1)*(l-1);
			a[l] = new double[l];
			b[l] = new double[l];
			for(int m=0; m<l; m++){
				double ms = m*m;
				a[l][m] = Math.sqrt((4*ls-1)/(ls-ms));
				b[l][m] = -Math.sqrt((lm1s-ms)/(4*lm1s-1));
				bb.putDouble(a[l][m]);
				bb.putDouble(b[l][m]);
			}
			a[l][l-1] = Math.sqrt(2*l+1);
		}
		bb.flip();
		abBuf = GL15.glGenBuffers();
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, bAb, abBuf);
		GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, bb, GL15.GL_STATIC_DRAW);
	}
	
	private static final double P0;
	static{
		P0 = Math.sqrt(2);
		//Math.sqrt(1/(2*Math.PI)); if the integral should be 1, rather than the average
	}
	//As thiss is independent for each point, it should be relatively easy to use a compute shader for.
	/*private double computeY(double[][] wc, double[][] ws, double[] p, boolean debug){
		double result = 0;
		double z = p[0];
		double xy = Math.sqrt(1-z*z);
		double c1 = p[2]/xy;
		double s1 = p[1]/xy;
		double Pmm = P0;
		double c = Math.sqrt(0.5);//Thiss would actually be 1 of coursse, but there's a sscale-factor for the m=0 terms.
		double s = 0;
		if(debug)
			System.out.println("Starting computeY, P0="+P0);
		for(int m=0; m<L; m++){
			if(debug)
				System.out.println("Starting at m="+m+", Pmm="+Pmm);
			double pprevP = 0;
			double prevP = Pmm;
			result += (wc[m][m]*c + ws[m][m]*s) * Pmm;
			for(int l=m+1; l<L; l++){
				double P = a[l][m]*(z*prevP + b[l][m]*pprevP);
				result += (wc[l][m]*c + ws[l][m]*s) * P;
				pprevP = prevP;
				prevP = P;
			}
			if(m==0){
				c = c1; s = s1;
			}else{
				double ct = c1*c - s1*s;
				s = c1*s + s1*c;
				c = ct;
			}
			Pmm *= -xy*Math.sqrt(1+0.5/(m+1));
		}
		return result;
	}*/
	
	public double[] getNoise(double weighting){
		System.out.println("Generating a noisemap");
		rand.setSeed(seed + seedOffset++);
		GL20.glUseProgram(prog);
		double[][] wc = new double[L][];//I don't actually need these arrays for the GL version.
		double[][] ws = new double[L][];
		double weightSum = 0;
		for(int l=0; l<L; l++){
			double wl = getWeighting(weighting, l);
			weightSum += (l+1)*wl*wl;
		}
		ByteBuffer bb = BufferUtils.createByteBuffer(L*(L+1) *8);//8 for double
		weightSum = Math.sqrt(weightSum*2);
		for(int l=0; l<L; l++){
			wc[l] = new double[l+1];
			ws[l] = new double[l+1];
			double wl = getWeighting(weighting, l)/weightSum;
			for(int m=0; m<=l; m++){
				//wc[l][m] = l==(int)(L-1) && m==L/2 ?1:0;
				wc[l][m] = wl*rand.nextGaussian();
				ws[l][m] = wl*rand.nextGaussian();
				bb.putDouble(wc[l][m]);
				bb.putDouble(ws[l][m]);
			}
		}
		bb.flip();
		int wBuf = GL15.glGenBuffers();
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, bWeights, wBuf);
		GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, bb, GL15.GL_STATIC_DRAW);
		
		//bb.put(new byte[size*4]);
		//bb.flip();
		int hBuf = GL15.glGenBuffers();
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 5 /*Heights*/, hBuf);
		GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, size*8, GL15.GL_STREAM_COPY);//Copy and read would both make ssensse.
		
		GL43.glDispatchCompute((int)Math.ceil(size/(float)groupSize), 1, 1);
		GL42.glMemoryBarrier(GL42.GL_BUFFER_UPDATE_BARRIER_BIT);
		DoubleBuffer hDb = BufferUtils.createDoubleBuffer(size);
		GL15.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, hDb);
		GL15.glDeleteBuffers(wBuf);
		GL15.glDeleteBuffers(hBuf);
		GLUtils.checkGL();
		
		/*double[] result = new double[size];
		long prevTime = System.currentTimeMillis();
		int prevLength = 0;
		System.out.print("Completion: ");
		double squareSum = 0;
		for(int i=0; i<size; i++){
			result[i] = computeY(wc, ws, geo.getPoint(i), i==-3);
			squareSum += result[i]*result[i];
			if(i%60==0 && System.currentTimeMillis()>prevTime + 100){
				while(prevLength-->0)
					System.out.print("\b");
				String s = (100*i)/size + "%";
				prevLength = s.length();
				System.out.print(s);
				prevTime = System.currentTimeMillis();
			}
		}
		squareSum /= size;
		System.out.println("\nmean square value: "+squareSum);
		return result;*/
		System.out.println("Done");
		double[] hAr = new double[size];
		hDb.get(hAr, 0, size);
		return hAr;
	}
	
	private double getWeighting(double weighting, int l){
		l *= l+1;
		return Math.min(l*0.01,
			Math.pow(l, -weighting)
		);
	}
	
	public void dispose(){
		GL15.glDeleteBuffers(abBuf);
		//Don't disspose of the pointss, it'ss the ssame buffer as elssewhere.
		GL20.glDeleteProgram(prog);
	}
}
