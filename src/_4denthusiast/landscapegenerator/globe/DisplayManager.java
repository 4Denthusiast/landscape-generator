package _4denthusiast.landscapegenerator.globe;

import _4denthusiast.landscapegenerator.water.Water;
import _4denthusiast.landscapegenerator.settlements.Settlements;
import _4denthusiast.landscapegenerator.IPoint;
import _4denthusiast.landscapegenerator.globe.util.*;

import java.nio.FloatBuffer;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

public class DisplayManager{
	private final Geometry geo;
	private final GUI gui;
	private final int size;
	private double[] view;
	private ByteBuffer viewBuffer;
	
	private int prog, cProg, csh, vsh, gsh, fsh, vao = -1;
	private static final String normalCompShaderSource =
		"#version 440\n"+
		"\n"+
		"layout(std430) buffer;\n"+
		"layout(local_size_x=64,local_size_y=1,local_size_z=1) in;\n"+
		"layout(binding=0) buffer Points{ dvec3[] point; };\n"+
		"layout(binding=1) buffer ConnPointers{ uint[] connPointer; };\n"+ // pointers into the connections array
		"layout(binding=2) buffer Connections{ uint[] connections; };\n"+ // for each vertex, the number of connections then a list of indices in Points
		"layout(binding=3) buffer Heights{ float[] height; };\n"+
		"layout(binding=4) buffer Normals{ vec3[] normal; };\n"+
		"uniform uint size;\n"+
		"uniform uint phase;\n"+
		"\n"+
		"void main(){\n"+
		"	uint i = gl_GlobalInvocationID.x;\n"+
		"	if(i >= size)\n"+
		"		return;\n"+
		"	if(phase==0){\n"+
		"		point[i] *= double(1+0.05*height[i]);\n"+
		"	}else{\n"+
		"		uint cp = connPointer[i];\n"+
		"		uint nConns = connections[cp];\n"+
		"		if(nConns > 10)\n"+ //ssanity check
		"			nConns = 10;\n"+ //Maybe I should sset ssome flag to show that thiss has happened.
		"		vec3 n = cross(vec3(point[connections[cp+1]]), vec3(point[connections[cp+nConns]]));\n"+
		"		for(int j=0; j<nConns-1; j++)\n"+
		"			n += cross(vec3(point[connections[cp+j+2]]), vec3(point[connections[cp+j+1]]));\n"+
		"		normal[i] = normalize(n);\n"+
		"	}\n"+
		"}";
	private static final String vShaderSource =
		"#version 440\n"+
		"\n"+
		"in uint a_i;\n"+
		"layout(binding=5) buffer Pos{ dvec3[] a_pos; };\n"+//I use buffers sso as to decouple drainage from the other attributes, as it varies within each vertex according to direction.
		"layout(binding=6) buffer Height{ float[] a_height; };\n"+
		"layout(binding=7) buffer WaterHeight{ float[] a_waterHeight; };\n"+
		"in float a_drainage;\n"+
		"uniform uint u_drawingPhase;\n"+
		"layout(binding=8) buffer Normals{ vec3[] a_normal; };\n"+
		"layout(binding=9) buffer Populations{ float[] a_population; };\n"+
		"layout(binding=10) buffer ConnPointers{ uint[] a_connPointers; };\n"+
		"layout(binding=11) buffer Connections{ uint[] a_connections; };\n"+
		"layout(binding=12) buffer EdgeIndices{ uint[] a_edgeIndices; };\n"+//For each edge in each direction, the index of its parent node
		"layout(binding=13) buffer BorderLevels{ float[] a_borderLevel; };\n"+
		"uniform float u_borderThreshold;\n"+
		"uniform mat3 u_viewMat;\n"+
		"uniform vec2 u_scale;\n"+
		"out float v_height;\n"+
		"out float v_waterHeight;\n"+
		"out float v_lakeness;\n"+
		"out float v_drainage;\n"+//TODO change back to flat and ssee if it breakss anything.
		"out float v_population;\n"+
		"out float v_pointRadius;\n"+
		"out vec3 v_normal;\n"+
		"out vec3 v_pos;\n"+
		"\n"+
		"vec3 surfacePos(uint i){\n"+
		"	vec3 pos = vec3(a_pos[i].xyz);\n"+
		"	float graphicalWaterHeight = 1 + 0.05*a_waterHeight[i];\n"+
		"	if(length(pos) < graphicalWaterHeight)\n"+
		"		pos = normalize(pos)*graphicalWaterHeight;\n"+
		"	return pos;\n"+
		"}\n"+
		"\n"+
		"bool submerged(uint i){\n"+
		"	return a_waterHeight[i] > a_height[i];\n"+
		"}\n"+
		"\n"+
		"void main(){\n"+
		"	uint i;\n"+
		"	vec3 pos;\n"+
		"	if(u_drawingPhase == 3){\n"+
		"		uint edgeIndex = gl_VertexID/2;\n"+
		"		if(a_borderLevel[edgeIndex] < u_borderThreshold){\n"+
		"			gl_Position = vec4(0,0,0,1);\n"+
		"			return;\n"+
		"		}\n"+
		"		uint subIndex = gl_VertexID%2;\n"+
		"		uint basePoint = a_edgeIndices[edgeIndex];\n"+
		"		uint connBaseIndex = a_connPointers[basePoint];\n"+
		"		uint n = a_connections[connBaseIndex];\n"+
		"		uint connRelativeIndex = edgeIndex + basePoint - connBaseIndex;\n"+//The index of this edge in the adjacency list of the base point
		"		uint endPoint  = a_connections[connBaseIndex + 1 + connRelativeIndex];\n"+
		"		if(subIndex == 0){\n"+
		"			float weighting;\n"+
		"			if(submerged(basePoint) ^^ submerged(endPoint)){\n"+
		"				float waterLevel = min(a_waterHeight[basePoint], a_waterHeight[endPoint]);\n"+
		"				weighting = (waterLevel - a_height[basePoint])/(a_height[endPoint] - a_height[basePoint]);\n"+
		"			}else\n"+
		"				weighting = 0.5;\n"+
		"			pos = (1-weighting)*surfacePos(basePoint) + weighting*surfacePos(endPoint);\n"+
		"		}else{\n"+
		"			uint thirdPointRelativeIndex = (connRelativeIndex + 1) % n;\n"+
		"			uint thirdPoint = a_connections[connBaseIndex + 1 + thirdPointRelativeIndex];\n"+
		"			bool sub1 = submerged(basePoint);\n"+
		"			bool sub2 = submerged(endPoint);\n"+
		"			bool sub3 = submerged(thirdPoint);\n"+
		"			if((sub1 && sub2 && sub3) || !(sub1 || sub2 || sub3))\n"+
		"				pos = (surfacePos(basePoint) + surfacePos(endPoint) + surfacePos(thirdPoint))/3;\n"+
		"			else{\n"+
		"				float waterLevel = a_waterHeight[sub1? basePoint : sub2? endPoint : thirdPoint];\n"+
		"				bool mostlyWater = ! sub1 ^^ sub2 ^^ sub3;\n"+
		"				uint distinctPoint = (sub1 ^^ mostlyWater)? basePoint : (sub2 ^^ mostlyWater)? endPoint : thirdPoint;\n"+
		"				float weighting = 2*(waterLevel - a_height[distinctPoint])/(a_height[basePoint] + a_height[endPoint] + a_height[thirdPoint] - 3*a_height[distinctPoint]);\n"+
		"				vec3 dPointPos = surfacePos(distinctPoint);\n"+
		"				pos = dPointPos + weighting/2 * (surfacePos(basePoint) + surfacePos(endPoint) + surfacePos(thirdPoint) - 3*dPointPos);\n"+
		"			}\n"+
		"		}\n"+
		"	}else{\n"+
		"		if(u_drawingPhase == 1)\n"+
		"			i = a_i;\n"+
		"		else\n"+
		"			i = gl_VertexID;\n"+
		"		pos = surfacePos(i);\n"+
		"	}\n"+
		"	if(u_drawingPhase == 3)\n"+
		"		pos *= 1.00;\n"+
		"	pos *= u_viewMat;\n"+
		"	pos.xy *= u_scale;\n"+
		"	gl_Position = vec4(pos.xy, -0.5*pos.z, 1.0);\n"+
		"	v_pos = pos;\n"+
		"	if(u_drawingPhase == 3) return;\n"+
		"	v_height = a_height[i];\n"+
		"	if(a_waterHeight[i] <= a_height[i]){\n"+
		"		v_waterHeight = 0;\n"+
		"		v_lakeness = 0;\n"+
		"	}else{\n"+
		"		v_waterHeight = a_waterHeight[i];\n"+
		"		v_lakeness = 1;\n"+
		"	}\n"+
		"	v_drainage = a_drainage;\n"+
		"	v_normal = a_normal[i] * u_viewMat;\n"+ //u_viewMat should be orthonormal.
		"	if(u_drawingPhase == 2){\n"+
		"		v_population = a_population[i];\n"+
		"		v_pointRadius = sqrt(v_population/radians(180));\n"+
		"		gl_PointSize = ceil(v_pointRadius*2);\n"+
		"	}\n"+
		"}";
	private static final String fShaderSource =
		"#version 440\n"+
		"\n"+
		"const vec3 lightPos = normalize(vec3(1,0.7,0.5));\n"+
		"in float v_height;\n"+
		"in float v_waterHeight;\n"+
		"in float v_lakeness;\n"+
		"in float v_drainage;\n"+
		"in float v_population;\n"+
		"in float v_pointRadius;\n"+
		"uniform uint u_drawingPhase;\n"+
		"in vec3 v_normal;\n"+
		"in vec3 v_pos;\n"+
		"layout(location = 0) out vec4 colour;\n"+
		"\n"+
		"void main(){\n"+
		"	float height = v_height * 0.2;\n"+
		"	float waterHeight = v_lakeness == 0?-1/0 : v_waterHeight/v_lakeness;\n"+
		"	vec3 normal = normalize(v_normal);\n"+
		"	if(u_drawingPhase == 0){\n"+
		"		colour = vec4(height, height+0.3, height, 1);\n"+
		"		if(v_height <= waterHeight){\n"+
		"			normal = normalize(v_pos);\n"+
		"			float depth = waterHeight - v_height;\n"+
		"			colour = vec4(0,0.2,-0.2*depth+1,1);\n"+
		"		}\n"+
		"	}else if(u_drawingPhase == 1){\n"+
		"		if(v_height <= waterHeight)\n"+
		"			discard;\n"+
		"		float r = sqrt(v_drainage)*25;\n"+
		"		colour = vec4(0.2, 0, 2-r, r);\n"+
		"	}else if(u_drawingPhase == 2){\n"+
		"		float pointSize = ceil(v_pointRadius*2);\n"+
		"		float pointDist = length(gl_PointCoord-vec2(0.5,0.5))*pointSize*2;\n"+
		"		colour = vec4(1,1,0,v_pointRadius-pointDist);\n"+
		"		return;\n"+ //Towns have their own light.
		"	}else{\n"+
		"		colour = vec4(1,1,1,1);\n"+
		"		return;\n"+
		"	}\n"+
		"	colour.rgb *= 0.5+ max(0,dot(normal, lightPos));\n"+
//		"	colour.rgb = (v_normal+vec3(1,1,1))/2;\n"+
		"}";
	private int a_pos=5, a_height=6, a_waterHeight=7, a_drainage, u_drawingPhase, a_normal=8, a_population=9, a_connPointers=10, a_connections=11, a_edgeIndices=12, a_borderLevel=13, u_size, a_i, u_phase, u_viewMat, u_scale, u_borderThreshold;
	private int linesBuf, facesBuf;
	private int drawingMode = 0;
	
	public DisplayManager(GUI gui, Geometry geo){
		this.gui = gui;
		this.geo = geo;
		this.size = geo.getSize();
		view = Mat3.identity();
		
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL32.GL_PROGRAM_POINT_SIZE);
		prog = GL20.glCreateProgram();
		vsh = prepareShader(GL20.GL_VERTEX_SHADER, vShaderSource, prog);
		//gsh = prepareShader(GL32.GL_GEOMETRY_SHADER, gShaderSource, prog);
		fsh = prepareShader(GL20.GL_FRAGMENT_SHADER, fShaderSource, prog);
		GL20.glLinkProgram(prog);
		if(GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) != GL11.GL_TRUE)
			System.out.println("Shader linking failed: "+GL20.glGetProgramInfoLog(prog, 1024));
		GL20.glUseProgram(prog);
		GL20.glValidateProgram(prog);
		System.out.println("Shader program log: "+GL20.glGetProgramInfoLog(prog, 1024));
		a_drainage = GL20.glGetAttribLocation(prog, "a_drainage");
		a_i = GL20.glGetAttribLocation(prog, "a_i");
		u_drawingPhase = GL20.glGetUniformLocation(prog, "u_drawingPhase");
		u_viewMat = GL20.glGetUniformLocation(prog, "u_viewMat");
		u_scale = GL20.glGetUniformLocation(prog, "u_scale");
		u_borderThreshold = GL20.glGetUniformLocation(prog, "u_borderThreshold");
		GLUtils.checkGL();
		
		vao = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vao);
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, a_pos, geo.getPointsBuffer());
		GLUtils.checkGL();
		
		int normalsBuf = GL15.glGenBuffers();
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, a_normal, normalsBuf);
		GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, size*4*4, GL15.GL_STATIC_COPY);
		GLUtils.checkGL();
		
		linesBuf = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, linesBuf);
		geo.bufferLines();
		facesBuf = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, facesBuf);
		geo.bufferFaces();
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, a_edgeIndices, geo.bufferEdgeIndices());
		GLUtils.checkGL();
		
		cProg = GL20.glCreateProgram();
		prepareShader(GL43.GL_COMPUTE_SHADER, normalCompShaderSource, cProg);
		GL20.glLinkProgram(cProg);
		GL20.glUseProgram(cProg);
		u_size = GL20.glGetUniformLocation(cProg, "size");
		u_phase = GL20.glGetUniformLocation(cProg, "phase");
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0 /*Points*/, geo.getPointsBuffer());
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 4 /*Normals*/, normalsBuf);
		GL30.glUniform1ui(u_size, size);
		GLUtils.checkGL();
	
		viewBuffer = BufferUtils.createByteBuffer(9*4);
		updateTransforms();
	}
	
	private int prepareShader(int type, String source, int program){
		int sh = GL20.glCreateShader(type);
		GL20.glShaderSource(sh, source);
		GL20.glCompileShader(sh);
		if(GL20.glGetShaderi(sh, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
			throw new RuntimeException("Shader compilation failed: "+GL20.glGetShaderInfoLog(sh, 1024));
		GL20.glAttachShader(program, sh);
		return sh;
	}
	
	public void useLandscape(HeightMap hm, Water water, Settlements settlements){
		GLUtils.checkGL();
		int heightBuf = GL15.glGenBuffers();
		int connPointerBuf = geo.bufferConnPointers();
		int connectionsBuf = geo.bufferConnections();
		GL20.glUseProgram(cProg);
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1 /*ConnPointers*/, connPointerBuf);
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2 /*Connections*/, connectionsBuf);
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3 /*Heights*/, heightBuf);
		ByteBuffer bb = BufferUtils.createByteBuffer(geo.getNumLines()*2*4); //The largesst thing thiss is used for is 2*size*float
		GLUtils.bufferDoubles(bb, hm.getHeights());
		bb.flip();
		GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, bb, GL15.GL_STATIC_DRAW);
		//GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, size*4, GL15.GL_STATIC_DRAW);
		GL30.glUniform1ui(u_phase, 0);
		GL43.glDispatchCompute((int)Math.ceil(size/64f), 1, 1);
		GL30.glUniform1ui(u_phase, 1);
		GL43.glDispatchCompute((int)Math.ceil(size/64f), 1, 1);
		GL42.glMemoryBarrier(GL42.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);
		GL20.glDeleteProgram(cProg);
		
		GL20.glUseProgram(prog);
		GL30.glBindVertexArray(vao);
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, a_height, heightBuf);
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, a_connPointers, connPointerBuf);
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, a_connections, connectionsBuf);
		
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, a_waterHeight, GL15.glGenBuffers());
		for(int i=0; i<size; i++)
			bb.putFloat((float)water.getWaterHeight(new Point(i)));
		bb.flip();
		GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, bb, GL15.GL_STATIC_DRAW);
		GLUtils.checkGL();
		GLUtils.checkGL();
		
		bb.clear();
		ByteBuffer bb2 = BufferUtils.createByteBuffer(geo.getNumLines()*2*4);
		for(int i=0; i<size; i++){
			Point pi = new Point(i);
			IPoint[] adjs = pi.getAdjacent();
			for(int k=0; k<adjs.length; k++){
				int j = ((Point)adjs[k]).index();
				if(j>i){
					bb.putInt(i);
					bb.putInt(j);
					boolean jHigher = hm.getHeight(j) > hm.getHeight(i);
					Point pHigher = jHigher?(Point)adjs[k]:pi;
					Point pLower = jHigher?pi:(Point)adjs[k];
					if(pLower.equals(water.getDownhill(pHigher))){
						float dr = (float)water.getOutDrainage(pHigher);
						bb2.putFloat((jHigher?dr:dr-1)/size);
						bb2.putFloat((jHigher?dr-1:dr)/size);
					}else{
						bb2.putFloat(0);
						bb2.putFloat(0);
					}
				}
			}
		}
		bb.flip();
		bb2.flip();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL15.glGenBuffers());
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bb, GL15.GL_STATIC_DRAW);
		if(a_i >= 0){
			GL20.glEnableVertexAttribArray(a_i);
			GL30.glVertexAttribIPointer(a_i, 1, GL11.GL_INT, 0, 0);
		}else
			System.err.println("Warning: a_i not active");
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL15.glGenBuffers());
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bb2, GL15.GL_STATIC_DRAW);
		if(a_drainage >= 0){
			GL20.glEnableVertexAttribArray(a_drainage);
			GL20.glVertexAttribPointer(a_drainage, 1, GL11.GL_FLOAT, false, 0, 0);
		}
		
		bb.clear();
		for(int i=0; i<size; i++)
			bb.putFloat((float)settlements.getPopulation(new Point(i))*30000f/size);
		bb.flip();
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, a_population, GL15.glGenBuffers());
		GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, bb, GL15.GL_STATIC_DRAW);
		
		bb.clear();
		for(int i=0; i<size; i++){
			int[] adj = geo.getAdj(i);
			for(int j=0; j<adj.length; j++)
				bb.putFloat((float)settlements.getBorderLevel(new Point(i), new Point(adj[j])));
		}
		bb.flip();
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, a_borderLevel, GL15.glGenBuffers());
		GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, bb, GL15.GL_STATIC_DRAW);
		GL20.glUniform1f(u_borderThreshold, size/500f);
		GLUtils.checkGL();
	}
	
	public void rotate(double dx, double dy){
		Mat3.approxRotate(view, dx, dy);
		updateTransforms();
	}
	
	private void updateTransforms(){
		GL20.glUseProgram(prog);
		GL30.glBindVertexArray(vao);
		float scale = (float)gui.getScale()*2;
		GL20.glUniform2f(u_scale, scale/Display.getWidth(), scale/Display.getHeight());
		GLUtils.bufferDoubles(viewBuffer, view);
		viewBuffer.flip();
		GL20.glUniformMatrix3(u_viewMat, false, viewBuffer.asFloatBuffer());
	}
	
	public void repaint(){
		GL11.glViewport(0,0,Display.getWidth(), Display.getHeight());
		GL11.glClearColor(0f,0.1f,0.3f,1f);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL20.glUseProgram(prog);
		GL30.glBindVertexArray(vao);
		
		GL30.glUniform1ui(u_drawingPhase, 0);
		switch(drawingMode){
			case 0:
				//phase 0: faces
				GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, facesBuf);
				GL11.glDrawElements(GL11.GL_TRIANGLES, geo.getNumFaces()*3, GL11.GL_UNSIGNED_INT, 0);
				
				GL11.glDepthMask(false);
				GL30.glUniform1ui(u_drawingPhase, 1);//phase 1: rivers
				GL11.glDrawArrays(GL11.GL_LINES, 0, geo.getNumLines()*2);
				GL30.glUniform1ui(u_drawingPhase, 2);//phase 2: settlements
				GL11.glDrawArrays(GL11.GL_POINTS, 0, geo.getSize());
				GL30.glUniform1ui(u_drawingPhase, 3);//phase 3: borders
				GL11.glPolygonOffset(3, 1);
				GL11.glDrawArrays(GL11.GL_LINES, 0, geo.getNumLines()*4);
				GL11.glDepthMask(true);
			break;
			case 1:
				GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, linesBuf);
				GL11.glDrawElements(GL11.GL_LINES, geo.getNumLines()*2, GL11.GL_UNSIGNED_INT, 0);
			break;
			case 2:
				GL11.glDrawArrays(GL11.GL_POINTS, 0, geo.getSize());
		}
	}
}
