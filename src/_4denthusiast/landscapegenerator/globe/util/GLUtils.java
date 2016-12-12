package _4denthusiast.landscapegenerator.globe.util;

import java.nio.ByteBuffer;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.opengl.GL11;

public class GLUtils{
	public static void bufferDoubles(ByteBuffer bb, double[] data){
		for(int i=0; i<data.length; i++)
			bb.putFloat((float)data[i]);
	}
	
	public static void checkGL(){
		int code = GL11.glGetError();
		if(code != GL11.GL_NO_ERROR)
			throw new RuntimeException(GLU.gluErrorString(code));
	}
}
