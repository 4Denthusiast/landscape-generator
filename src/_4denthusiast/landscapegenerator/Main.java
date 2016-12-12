package _4denthusiast.landscapegenerator;

import _4denthusiast.landscapegenerator.globe.GlobeGenerator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class Main{
	public static void main(String[] args){
		HashMap<String, Double> options = new HashMap<>();
		BufferedReader cfg = null;
		String line = null;
		try{
			cfg = new BufferedReader(new FileReader("config.txt"));
			while((line = cfg.readLine()) != null){
				if(line.isEmpty() || line.startsWith("//"))
					continue;
				String[] parts = line.split(" = ");
				if(parts.length!=2)
					throw new Exception("Line should contain \" = \"");
				options.put(parts[0], Double.parseDouble(parts[1]));
			}
		}catch(IOException e){
			System.err.println("Error in reading config file, using default values: "+e.toString());
			options = new HashMap<>();
		}catch(Exception e){
			System.err.println(e.toString()+" when parsing line "+line);
			options = new HashMap<>();
		}finally{
			if(cfg != null)
				try{cfg.close();}catch(IOException e){System.err.println(e);}
		}
		if(args.length == 0 || "torus".equals(args[0])){
			new LandscapeGenerator(options);
		}else if("rose".equals(args[0]))
			new GlobeGenerator(options).run();
		else
			System.out.println("unrecognised argument");
	}
}
