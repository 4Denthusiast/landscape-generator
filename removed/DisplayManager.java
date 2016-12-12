package _4denthusiast.landscapegenerator.globe;

import _4denthusiast.landscapegenerator.water.Water;
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
		"in dvec3 a_pos;\n"+
		"in float a_height;\n"+
		"in float a_waterHeight;\n"+
		"in float a_drainage;\n"+
		"uniform bool u_drawingRivers;\n"+
		"in vec3 a_normal;\n"+
		"uniform mat3 u_viewMat;\n"+
		"uniform vec2 u_scale;\n"+
		"out float v_height;\n"+
		"out float v_waterHeight;\n"+
		"out float v_lakeness;\n"+
		"out float v_drainage;\n"+//TODO change back to flat and ssee if it breakss anything.
		"out vec3 v_normal;\n"+
		"out vec3 v_pos;\n"+
		"\n"+
		"void main(){\n"+
		"	vec3 pos = vec3(a_pos.xyz) * u_viewMat;\n"+
		"	if(a_height < a_waterHeight)\n"+
		"		pos = normalize(pos)*(1+0.05*a_waterHeight);\n"+
		"	pos.xy *= u_scale;\n"+
		"	gl_Position = vec4(pos.xy, -0.5*pos.z, 1.0);\n"+
		"	v_height = a_height;\n"+
		"	if(a_waterHeight <= a_height){\n"+
		"		v_waterHeight = 0;\n"+
		"		v_lakeness = 0;\n"+
		"	}else{\n"+
		"		v_waterHeight = a_waterHeight;\n"+
		"		v_lakeness = 1;\n"+
		"	}\n"+
		"	v_pos = pos;\n"+
		"	v_drainage = a_drainage;\n"+
		"	v_normal = a_normal * u_viewMat;\n"+ //u_viewMat should be orthonormal.
		"}";
	private static final String fShaderSource =
		"#version 440\n"+
		"\n"+
		"const vec3 lightPos = normalize(vec3(1,0.7,0.5));\n"+
		"in float v_height;\n"+
		"in float v_waterHeight;\n"+
		"in float v_lakeness;\n"+
		"in float v_drainage;\n"+
		"uniform bool u_drawingRivers;\n"+
		"in vec3 v_normal;\n"+
		"in vec3 v_pos;\n"+
		"layout(location = 0) out vec4 colour;\n"+
		"\n"+
		"void main(){\n"+
		"	float height = v_height * 0.2;\n"+
		"	float waterHeight = v_lakeness == 0?-1/0 : v_waterHeight/v_lakeness;\n"+
		"	vec3 normal = normalize(v_normal);\n"+
		"	if(!u_drawingRivers){\n"+
		"		colour = vec4(height, height+0.3, height, 1);\n"+
		"		if(v_height <= waterHeight){\n"+
		"			normal = normalize(v_pos);\n"+
		"			float depth = waterHeight - v_height;\n"+
		"			colour = vec4(0,0.2,-0.2*depth+1,1);\n"+
		"		}\n"+
		"	}else{\n"+
		"		if(v_height <= waterHeight)\n"+
		"			discard;\n"+
		"		float r = sqrt(v_drainage)*25;\n"+
		"		colour = vec4(0.2, 0, 2-r, r);\n"+
		"	}\n"+
		"	colour.rgb *= 0.5+ max(0,dot(normal, lightPos));\n"+
		"}";
	private int a_pos, a_height, a_waterHeight, a_drainage, u_drawingRivers, a_normal, u_size, u_phase, u_viewMat, u_scale;
	private int linesBuf, facesBuf, riversBuf;
	private boolean drawingFaces = true;
	private boolean drawingLines = true;
	
	public DisplayManager(GUI gui, Geometry geo){
		this.gui = gui;
		this.geo = geo;
		this.size = geo.getSize();
		view = Mat3.identity();
		
		GL11.glEnable(GL11.GL_DEPTH_TEST);
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
		a_pos = GL20.glGetAttribLocation(prog, "a_pos");
		a_height = GL20.glGetAttribLocation(prog, "a_height");
		a_waterHeight = GL20.glGetAttribLocation(prog, "a_waterHeight");
		a_drainage = GL20.glGetAttribLocation(prog, "a_drainage");
		u_drawingRivers = GL20.glGetUniformLocation(prog, "u_drawingRivers");
		a_normal = GL20.glGetAttribLocation(prog, "a_normal");
		u_viewMat = GL20.glGetUniformLocation(prog, "u_viewMat");
		u_scale = GL20.glGetUniformLocation(prog, "u_scale");
		GLUtils.checkGL();
		
		vao = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vao);
		
		GL20.glEnableVertexAttribArray(a_pos);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, geo.getPointsBuffer());
		GL20.glEnableVertexAttribArray(a_pos);
		GL41.glVertexAttribLPointer(a_pos, 3, 32, 0);
		GLUtils.checkGL();
		
		int normalsBuf = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalsBuf);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, size*4*4, GL15.GL_STATIC_COPY);
		GL20.glEnableVertexAttribArray(a_normal);
		GL20.glVertexAttribPointer(a_normal, 3, GL11.GL_FLOAT, false, 16, 0);
		GLUtils.checkGL();
		
		linesBuf = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, linesBuf);
		geo.bufferLines();
		facesBuf = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, facesBuf);
		geo.bufferFaces();
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
	
	//TODO buffer and dissplay the water data.
	public void useLandscape(HeightMap hm, Water water){
		GLUtils.checkGL();
		int heightBuf = GL15.glGenBuffers();
		GL20.glUseProgram(cProg);
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1 /*ConnPointers*/, geo.bufferConnPointers());
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2 /*Connections*/, GL15.glGenBuffers());
		geo.bufferConnections();
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3 /*Heights*/, heightBuf);
		ByteBuffer bb = BufferUtils.createByteBuffer(size*2 *4); //The largesst thing thiss is used for is 2*size*int
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
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, heightBuf);
		GL20.glEnableVertexAttribArray(a_height);
		GL20.glVertexAttribPointer(a_height, 1, GL11.GL_FLOAT, false, 0, 0);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL15.glGenBuffers());
		for(int i=0; i<size; i++)
			bb.putFloat((float)water.getWaterHeight(new Point(i)));
		bb.flip();
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bb, GL15.GL_STATIC_DRAW);
		GL20.glEnableVertexAttribArray(a_waterHeight);
		GL20.glVertexAttribPointer(a_waterHeight, 1, GL11.GL_FLOAT, false, 0, 0);
		
		for(int i=0; i<size; i++)
			bb.putFloat(((float)water.getOutDrainage(new Point(i))-1)/size);
		bb.flip();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL15.glGenBuffers());
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bb, GL15.GL_STATIC_DRAW);
		GL20.glEnableVertexAttribArray(a_drainage);
		GL20.glVertexAttribPointer(a_drainage, 1, GL11.GL_FLOAT, false, 0, 0);
		
		bb.clear();
		for(int i=0; i<size; i++){
			bb.putInt(i);
			Point d = (Point)water.getDownhill(new Point(i));
			if(d == null)
				bb.putInt(i);
			else
				bb.putInt(d.index());
		}
		bb.flip();
		riversBuf = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, riversBuf);
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, bb, GL15.GL_STATIC_DRAW);
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
		
		GL20.glUniform1i(u_drawingRivers, 0);
		if(drawingFaces){
			GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
			GL11.glPolygonOffset(1, 0);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, facesBuf);
			GL11.glDrawElements(GL11.GL_TRIANGLES, geo.getNumFaces()*3, GL11.GL_UNSIGNED_INT, 0);
			GL20.glUniform1i(u_drawingRivers, 1);
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, riversBuf);
			GL11.glDrawElements(GL11.GL_LINES, geo.getSize()*2, GL11.GL_UNSIGNED_INT, 0);
		}else if(drawingLines){
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, linesBuf);
			GL11.glDrawElements(GL11.GL_LINES, geo.getNumLines()*2, GL11.GL_UNSIGNED_INT, 0);
		}else
			GL11.glDrawArrays(GL11.GL_POINTS, 0, geo.getSize());
	}
}
