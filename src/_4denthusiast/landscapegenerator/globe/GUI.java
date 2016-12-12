package _4denthusiast.landscapegenerator.globe;

import _4denthusiast.landscapegenerator.water.Water;
import _4denthusiast.landscapegenerator.settlements.Settlements;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.LWJGLException;

public class GUI{
	private DisplayManager display;
	private boolean shouldRepaint;
	
	public GUI(){
		try{
			Display.setDisplayMode(new DisplayMode(500,500));
			Display.create(
				new PixelFormat(),
				new ContextAttribs(4,0,ContextAttribs.CONTEXT_FORWARD_COMPATIBLE_BIT_ARB)
			);
			System.out.println("OpenGL version: " + GL11.glGetString(GL11.GL_VERSION));
			Display.setResizable(true);
			Mouse.create();
			Keyboard.create();
			Display.setTitle("⚘GLob⚘");
		}catch(LWJGLException e){
			throw new RuntimeException(e);
			//There's really nothing I can do to recover from thiss.
		}
	}
	
	public void initDisplayManager(Geometry geo, HeightMap heightMap, Water water, Settlements settlements){
		if(display != null)
			throw new IllegalStateException("The display manager already exists.");
		display = new DisplayManager(this, geo);
		display.useLandscape(heightMap, water, settlements);
	}
	
	public void run(){
		int framerateTimeout = 0;
		shouldRepaint = true;
		long prevTime = System.nanoTime();
		while(!Display.isCloseRequested()){
			long newTime = System.nanoTime();
			long dt = newTime-prevTime;
			prevTime = newTime;
			while(Mouse.next())
				handleMouseEvent(dt);
			while(Keyboard.next())
				handleKeyboardEvent();
			spin(dt);
			if(shouldRepaint){
				framerateTimeout = 60;
				if(display != null)
					display.repaint();
			}
			try{
				synchronized(this){
					wait(framerateTimeout-->0?20:100);
				}
			}catch(InterruptedException e){}//If the framerate limit is failing, that's really not that much of a problem.
			Display.update();
			shouldRepaint = false;
		}
		Display.destroy();
	}
	
	double dx, dy, mdt;
	private void handleMouseEvent(long dt){
		if(Mouse.isButtonDown(0)){
			double scale = getScale();
			//I really don't know why 2* is necsesssary.
			dx += 0.1*(2*Mouse.getEventDX()/scale - dx);
			dy += 0.1*(2*Mouse.getEventDY()/scale - dy);
			mdt+= 0.1*(dt - mdt);
			if(Mouse.getEventDX()!=0 || Mouse.getEventDY()!=0){
				shouldRepaint = true;
				if(display != null)
					display.rotate(Mouse.getEventDX()/scale, Mouse.getEventDY()/scale);
			}
		}
	}
	
	private boolean spin = false;
	private void handleKeyboardEvent(){
		if(Keyboard.getEventKey() == Keyboard.KEY_SPACE){
			if(Keyboard.getEventKeyState() && !Keyboard.isRepeatEvent()){
				spin = true;
			}
		}
	}
	
	private void spin(long dt){
		if(spin){
			if(Math.hypot(dx,dy)/0.02 < 0.05)
				spin = false;
			if(Mouse.isButtonDown(0))
				return;
			shouldRepaint = true;
			double angle = dt/mdt;
			if(display != null)
				display.rotate(dx*angle,dy*angle);
		}
	}
	
	public double getScale(){
		return 0.4* Math.min(Display.getWidth(), Display.getHeight());
	}
}
