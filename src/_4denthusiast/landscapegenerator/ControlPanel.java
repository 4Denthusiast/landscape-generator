package _4denthusiast.landscapegenerator;

import javax.swing.*;
import java.awt.Font;
import java.awt.event.*;
import javax.swing.event.*;
import java.io.File;

public class ControlPanel extends JFrame implements ActionListener, ChangeListener{
	LandscapeGenerator landscapeGenerator;
	Display display;
	
	private GroupLayout layout;
	private GroupLayout.Group vLayout;
	private GroupLayout.Group hLayout;
	private JButton reDrawButton;
	//Eventially I may make this all into an array if I add more.
	private JCheckBox displayDiagonalCheckBox;
	private JCheckBox displayHeightCheckBox;
	private JCheckBox displayWaterCheckBox;
	private JCheckBox displaySettlementsCheckBox;
	private JCheckBox displayRoadsCheckBox;
	private JCheckBox displayBordersCheckBox;
	private JSpinner kingdomCutoffSpinner;
	
	private JButton genHeightButton;
	private JButton genWaterButton;
	private JButton genFlowButton;
	private JButton genSettlementsButton;
	private JButton genRoadsButton;
	
	private JProgressBar progressBar;
	
	private JButton saveButton;
	private JFileChooser fileChooser;
	private File defaultSaveFile;
	
	ControlPanel(LandscapeGenerator parent, Display display, String compositionNo){
		super("CP for étude #"+compositionNo);
		landscapeGenerator = parent;
		this.display = display;
		display.setControlPanel(this);
		
		layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		
		JLabel displayOptionsLabel = new JLabel("Display options");
		displayOptionsLabel.setFont(displayOptionsLabel.getFont().deriveFont(Font.BOLD));
		JLabel displayDiagonalLabel = new JLabel("Display diagonal");
		JLabel displayHeightLabel = new JLabel("Display height");
		JLabel displayWaterLabel = new JLabel("Display water");
		JLabel displaySettlementsLabel = new JLabel("Display settlements");
		JLabel displayRoadsLabel = new JLabel("Display roads");
		JLabel displayBordersLabel = new JLabel("Display borders");
		JLabel kingdomCutoffLabel = new JLabel("Kingdom zise threshold");
		
		reDrawButton = new JButton("Re-paint");
		reDrawButton.addActionListener(this);
		displayDiagonalCheckBox = new JCheckBox();
		displayHeightCheckBox = new JCheckBox();
		displayWaterCheckBox = new JCheckBox();
		displaySettlementsCheckBox = new JCheckBox();
		displayRoadsCheckBox = new JCheckBox();
		displayBordersCheckBox = new JCheckBox();
		displayHeightCheckBox.setEnabled(false);
		displayWaterCheckBox.setEnabled(false);
		displaySettlementsCheckBox.setEnabled(false);
		displayRoadsCheckBox.setEnabled(false);
		displayBordersCheckBox.setEnabled(false);
		displayDiagonalCheckBox.addActionListener(this);
		displayHeightCheckBox.addActionListener(this);
		displayWaterCheckBox.addActionListener(this);
		displaySettlementsCheckBox.addActionListener(this);
		displayRoadsCheckBox.addActionListener(this);
		displayBordersCheckBox.addActionListener(this);
		
		kingdomCutoffSpinner = new JSpinner(new SpinnerNumberModel(128, 0, Double.POSITIVE_INFINITY, 16));
		kingdomCutoffSpinner.addChangeListener(this);
		
		JSeparator sep1 = new JSeparator();
		
		JLabel generationLabel = new JLabel("Generation steps");
		genHeightButton = new JButton("Height map");
		genWaterButton = new JButton("Water");
		genFlowButton = new JButton("Lake flow");
		genSettlementsButton = new JButton("Settlements");
		genRoadsButton = new JButton("Roads");
		genHeightButton.addActionListener(this);
		genWaterButton.addActionListener(this);
		genFlowButton.addActionListener(this);
		genSettlementsButton.addActionListener(this);
		genRoadsButton.addActionListener(this);
		
		progressBar = new JProgressBar();
		
		JSeparator sep2 = new JSeparator();
		saveButton = new JButton("Save");
		saveButton.setEnabled(false);
		saveButton.addActionListener(this);
		fileChooser = new JFileChooser();
		defaultSaveFile = new File(java.nio.file.Paths.get(".").toFile().getAbsolutePath(), "imaginary landscape map number "+compositionNo.replace('.',','));
		
		vLayout = layout.createSequentialGroup()
			.addGroup(layout.createSequentialGroup()
				.addComponent(displayOptionsLabel)
				.addComponent(reDrawButton)
				.addGroup(layout.createParallelGroup()
					.addComponent(displayDiagonalLabel)
					.addComponent(displayDiagonalCheckBox)
				)
				.addGroup(layout.createParallelGroup()
					.addComponent(displayHeightLabel)
					.addComponent(displayHeightCheckBox)
				)
				.addGroup(layout.createParallelGroup()
					.addComponent(displayWaterLabel)
					.addComponent(displayWaterCheckBox)
				)
				.addGroup(layout.createParallelGroup()
					.addComponent(displaySettlementsLabel)
					.addComponent(displaySettlementsCheckBox)
				)
				.addGroup(layout.createParallelGroup()
					.addComponent(displayRoadsLabel)
					.addComponent(displayRoadsCheckBox)
				)
				.addGroup(layout.createParallelGroup()
					.addComponent(displayBordersLabel)
					.addComponent(displayBordersCheckBox)
				)
				.addGroup(layout.createParallelGroup()
					.addComponent(kingdomCutoffLabel)
					.addComponent(kingdomCutoffSpinner)
				)
			)
			.addComponent(sep1)
			.addGroup(layout.createSequentialGroup()
				.addComponent(generationLabel)
				.addGroup(layout.createParallelGroup()
					.addComponent(genHeightButton)
					.addComponent(genWaterButton)
				)
				.addGroup(layout.createParallelGroup()
					.addComponent(genFlowButton)
					.addComponent(genSettlementsButton)
				)
				.addGroup(layout.createParallelGroup()
					.addComponent(genRoadsButton)
				)
				.addComponent(progressBar)
			)
			.addComponent(sep2)
			.addComponent(saveButton);
		
		hLayout = layout.createParallelGroup()
			.addGroup(layout.createParallelGroup()
				.addComponent(displayOptionsLabel, GroupLayout.Alignment.CENTER)
				.addComponent(reDrawButton)
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup()
						.addComponent(displayDiagonalLabel)
						.addComponent(displayHeightLabel)
						.addComponent(displayWaterLabel)
						.addComponent(displaySettlementsLabel)
						.addComponent(displayRoadsLabel)
						.addComponent(displayBordersLabel)
						.addComponent(kingdomCutoffLabel)
					)
					.addGroup(layout.createParallelGroup()
						.addComponent(displayDiagonalCheckBox)
						.addComponent(displayHeightCheckBox)
						.addComponent(displayWaterCheckBox)
						.addComponent(displaySettlementsCheckBox)
						.addComponent(displayRoadsCheckBox)
						.addComponent(displayBordersCheckBox)
						.addComponent(kingdomCutoffSpinner)
					)
				)
			)
			.addComponent(sep1)
			.addGroup(layout.createParallelGroup()
				.addComponent(generationLabel, GroupLayout.Alignment.CENTER)
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup()
						.addComponent(genHeightButton)
						.addComponent(genFlowButton)
						.addComponent(genRoadsButton)
					)
					.addGroup(layout.createParallelGroup()
						.addComponent(genWaterButton)
						.addComponent(genSettlementsButton)
					)
				)
				.addComponent(progressBar)
			)
			.addComponent(sep2)
			.addComponent(saveButton);
		layout.linkSize(SwingConstants.HORIZONTAL, genHeightButton, genFlowButton, genRoadsButton);
		layout.linkSize(SwingConstants.HORIZONTAL, genWaterButton, genSettlementsButton);
		
		layout.setVerticalGroup(vLayout);
		layout.setHorizontalGroup(hLayout);
		pack();
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		display.reDraw();
		display.setVisible(true);
		setVisible(true);
	}
	
	public boolean shouldDisplayDiagonal(){
		return displayDiagonalCheckBox.isSelected();
	}
	
	public boolean shouldDisplayHeight(){
		return displayHeightCheckBox.isSelected();
	}
	
	public boolean shouldDisplayWater(){
		return displayWaterCheckBox.isSelected();
	}
	
	public boolean shouldDisplaySettlements(){
		return displaySettlementsCheckBox.isSelected();
	}
	
	public boolean shouldDisplayRoads(){
		return displayRoadsCheckBox.isSelected();
	}
	
	public boolean shouldDisplayBorders(){
		return displayBordersCheckBox.isSelected();
	}
	
	public double getKingdomCutoff(){
		return (double)kingdomCutoffSpinner.getValue();
	}
	
	public void hideHeightButton(){
		genHeightButton.setEnabled(false);
	}
	
	public void enableHeight(){
		genHeightButton.setEnabled(false);
		if(!displayHeightCheckBox.isEnabled()){
			displayHeightCheckBox.setEnabled(true);
			displayHeightCheckBox.doClick();
		}
	}
	
	public void enableWater(){
		genWaterButton.setEnabled(false);
		if(!displayWaterCheckBox.isEnabled()){
			displayWaterCheckBox.setEnabled(true);
			displayWaterCheckBox.doClick();
		}
	}
	
	public void enableSettlements(){
		genSettlementsButton.setEnabled(false);
		if(!displaySettlementsCheckBox.isEnabled()){
			displaySettlementsCheckBox.setEnabled(true);
			displaySettlementsCheckBox.doClick();
		}
	}
	
	public void enableRoads(){
		genRoadsButton.setEnabled(false);
		if(!displayRoadsCheckBox.isEnabled()){
			displayRoadsCheckBox.setEnabled(true);
			displayRoadsCheckBox.doClick();
		}
	}
	
	public void enableBorders(){
		if(!displayBordersCheckBox.isEnabled()){
			displayBordersCheckBox.setEnabled(true);
			displayBordersCheckBox.doClick();
			saveButton.setEnabled(true);
		}
	}
	
	public void setProgressDescription(String s){
		progressBar.setString(s);
	}
	
	public void setProgress(double p){
		progressBar.setValue((int)(p*100));
	}
	
	public void clearProgressBar(){
		progressBar.setValue(0);
		progressBar.setString("");
	}
	
	public void actionPerformed(ActionEvent e){
		if(e.getSource() == displayDiagonalCheckBox ||
		   e.getSource() == reDrawButton)
			display.repaint();
		else if(e.getSource() == displayHeightCheckBox ||
		   e.getSource() == displayWaterCheckBox ||
		   e.getSource() == displaySettlementsCheckBox ||
		   e.getSource() == displayRoadsCheckBox ||
		   e.getSource() == displayBordersCheckBox)
			display.reDraw();
		else if(e.getSource() == genHeightButton)
			landscapeGenerator.generateHeightMap();
		else if(e.getSource() == genWaterButton)
			landscapeGenerator.generateWater();
		else if(e.getSource() == genFlowButton)
			landscapeGenerator.generateFlow();
		else if(e.getSource() == genSettlementsButton)
			landscapeGenerator.generateSettlements();
		else if(e.getSource() == genRoadsButton)
			landscapeGenerator.generateRoads();
		else if(e.getSource() == saveButton){
			fileChooser.setSelectedFile(defaultSaveFile);
			if(fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
				display.save(fileChooser.getSelectedFile());
		}
	}
	
	public void stateChanged(ChangeEvent e){
		if(e.getSource() == kingdomCutoffSpinner){
			if(shouldDisplayBorders())
				display.reDraw();
		}else
			assert false;
	}
}
