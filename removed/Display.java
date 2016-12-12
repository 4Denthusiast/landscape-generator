package _4denthusiast.globegenerator;

import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;

import _4denthusiast.globegenerator.util.*;

public class Display extends JFrame implements MouseMotionListener{
	private Geometry geo;
	private double[] view;
	private double prevMouseX, prevMouseY;
	
	private long window;
	
	public Display(Geometry geo){
		super("⚘glob⚘");
		this.geo = geo;
		this.view = Mat3.identity();
		addMouseMotionListener(this);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setSize(new Dimension(500,500));
		setVisible(true);
	}
	
	private double getScale(){
		return 0.4* Math.min(getWidth(), getHeight());
	}
	
	@Override
	public void paint(Graphics g){
		BufferedImage buf = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics bufG = buf.createGraphics();
		double scale = getScale();
		bufG.translate(getWidth()/2, getHeight()/2);
		bufG.setColor(new Color(0x002040));
		bufG.fillOval(-(int)scale, -(int)scale, 2*(int)scale, 2*(int)scale);
		geo.drawPoints(bufG, view, scale);
		g.drawImage(buf, 0, 0, null);
	}
	
	@Override
	public void mouseDragged(MouseEvent e){
		double scale = getScale();
		Mat3.approxRotate(view, (e.getX()-prevMouseX)/scale, (e.getY()-prevMouseY)/scale);
		mouseMoved(e);
		repaint();
	}
	
	@Override
	public void mouseMoved(MouseEvent e){
		prevMouseX = e.getX();
		prevMouseY = e.getY();
	}
}
