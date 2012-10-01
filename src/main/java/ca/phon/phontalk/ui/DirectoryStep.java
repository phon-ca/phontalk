package ca.phon.phontalk.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import ca.phon.engines.syllabifier.Syllabifier;
import ca.phon.gui.DialogHeader;
import ca.phon.gui.wizard.*;
import ca.phon.system.logger.PhonLogger;
import ca.phon.util.NativeDialogEvent;
import ca.phon.util.NativeDialogs;
import ca.phon.util.PhonConstants;
import ca.phon.util.StringUtils;
import ca.phon.util.iconManager.IconManager;
import ca.phon.util.iconManager.IconSize;

public class DirectoryStep extends WizardStep {
	
	/* UI */
	private JLabel libDirLabel = new JLabel();
	private JButton libBrowseBtn = new JButton();
	private JLabel outDirLabel = new JLabel();
	private JButton outBrowseBtn = new JButton();
	private JTextField projectNameField = new JTextField();
	
	private JCheckBox doSyllabificationBox = new JCheckBox();
	private JComboBox syllabifierBox = new JComboBox();
	
	/*
	 * TODO: For now we will list all available syllabifiers here.  In the future we should
	 * create this list dynamically.
	 */
	private final String syllabifierDirectory = "data/syllabifier/";
	private final String syllabifiers[] = {
			"cat-simple.xml",
			"cat.xml",
			"eng-ambi.xml",
			"eng-simple.xml",
			"eng.xml",
			"spa.xml",
			"fra-simple.xml",
			"fra.xml",
			"gue.xml",
			"nld-ambi.xml",
			"nld-CLPF-ambi.xml",
			"nld-CLPF.xml",
			"nld.xml",
			"por.xml"
	};
	
	private PhonTalkWizardModel model;
	
	public DirectoryStep(PhonTalkWizardModel model) {
		this.model = model;
		init();
	}
	
	private void init() {
//		JPanel centerPanel = new JPanel();
//		centerPanel.setBorder(BorderFactory.createLineBorder(Color.black));
//		
//		FormLayout centerLayout = new FormLayout(
//				"fill:pref:grow", "pref, pref");
//		centerPanel.setLayout(centerLayout);
		
		JPanel centerPanel = new JPanel();
//		dirPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		
		// setup layout
		FormLayout layout = new FormLayout(
				"right:pref, 3dlu, fill:pref:grow, pref",
				"pref, pref, pref, 3dlu, pref, pref");
		CellConstraints cc = new CellConstraints();
		centerPanel.setLayout(layout);

		String libDir = model.get(PhonTalkWizardModel.LIBRARY_PATH).toString();
		String outDir = null;
		if(model.get(PhonTalkWizardModel.OUTPUT_PATH) != null)
			outDir = model.get(PhonTalkWizardModel.OUTPUT_PATH).toString();
		else
			outDir = libDir;
		
		libDirLabel.setText(StringUtils.shortenStringUsingToken((new File(libDir)).getAbsolutePath(), PhonConstants.ellipsis + "", 50));
		outDirLabel.setText(StringUtils.shortenStringUsingToken((new File(outDir)).getAbsolutePath(), PhonConstants.ellipsis + "", 50));
		
		ImageIcon browseIcon = 
			IconManager.getInstance().getIcon("actions/document-open", IconSize.SMALL);
		libBrowseBtn.setIcon(browseIcon);
		libBrowseBtn.setToolTipText("Select input directory");
		libBrowseBtn.addActionListener(new LibBrowseAction());
		
		outBrowseBtn.setIcon(browseIcon);
		outBrowseBtn.setToolTipText("Select output directory");
		outBrowseBtn.addActionListener(new OutBrowseAction());
		
		centerPanel.add(new JLabel("Input Folder"), cc.xy(1,1));
		centerPanel.add(libDirLabel, cc.xy(3,1));
		centerPanel.add(libBrowseBtn, cc.xy(4, 1));
		
		centerPanel.add(new JLabel("Output Folder"), cc.xy(1,2));
		centerPanel.add(outDirLabel, cc.xy(3,2));
		centerPanel.add(outBrowseBtn, cc.xy(4, 2));
		
		centerPanel.add(new JLabel("Project Name"), cc.xy(1,3));
		centerPanel.add(projectNameField, cc.xy(3, 3));
		
		// setup syllabifier box
//		JPanel syllPanel = new JPanel();
//		syllPanel.setBorder(BorderFactory.createLineBorder(Color.black));
//		
//		FormLayout syllLayout = new FormLayout(
//				"right:pref, 3dlu, fill:pref:grow, pref",
//				"pref, pref");
//		syllPanel.setLayout(syllLayout);
		
		doSyllabificationBox = new JCheckBox("Perform syllabification");
		doSyllabificationBox.setSelected(false);
		doSyllabificationBox.addActionListener(new DoSyllabificationAction());
		
		List<Syllabifier> loadedSyllabifiers = 
			new ArrayList<Syllabifier>();
		for(String sFile:syllabifiers) {
			String sURL = syllabifierDirectory + sFile;
			InputStream sStream = 
				ClassLoader.getSystemResourceAsStream(sURL);
			if(sStream != null) {
				Syllabifier s = Syllabifier.getInstanceOf(sStream);
				if(s != null) {
					loadedSyllabifiers.add(s);
				}
			} else {
				PhonLogger.warning("Could not load syllabifier '" + sURL + "'");
			}
		}
		Comparator<Syllabifier> syllComparator = new Comparator<Syllabifier>() {

			@Override
			public int compare(Syllabifier o1, Syllabifier o2) {
				return o1.getName().compareTo(o2.getName());
			}
			
		};
		Collections.sort(loadedSyllabifiers, syllComparator);
		syllabifierBox = new JComboBox(loadedSyllabifiers.toArray(new Syllabifier[0]));
		syllabifierBox.setEnabled(false);
		syllabifierBox.setRenderer(new SyllabifierRenderer());
		
		centerPanel.add(doSyllabificationBox, cc.xy(3, 5));
		centerPanel.add(new JLabel("Syllabifier"), cc.xy(1, 6));
		centerPanel.add(syllabifierBox, cc.xy(3, 6));
		
//		syllPanel.add(doSyllabificationBox, cc.xy(3, 1));
//		syllPanel.add(new JLabel("Syllabifier"), cc.xy(1, 2));
//		syllPanel.add(syllabifierBox, cc.xy(3, 2));
		
//		centerPanel.add(dirPanel, cc.xy(1, 1));
//		centerPanel.add(syllPanel, cc.xy(1, 2));
		
		setLayout(new BorderLayout());
		add(new DialogHeader("Xml2Phon", "Select folders, project name, and syllabifier."), BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
	}
	
	private class LibBrowseAction implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			JFrame parentFrame = 
				(JFrame)SwingUtilities.getAncestorOfClass(JFrame.class, DirectoryStep.this);
			String selectedDir = 
				NativeDialogs.browseForDirectoryBlocking(parentFrame, 
						model.get(PhonTalkWizardModel.LIBRARY_PATH).toString(), "Select input directory");
			if(selectedDir != null) {
				model.put(PhonTalkWizardModel.LIBRARY_PATH, selectedDir);
				
				String outDir = null;
				if(model.get(PhonTalkWizardModel.OUTPUT_PATH) != null)
					outDir = model.get(PhonTalkWizardModel.OUTPUT_PATH).toString();
				else {
					File selDir = new File(selectedDir);
					File oDir = selDir.getParentFile();
					outDir = new File(oDir, selDir.getName()+"-phon").getAbsolutePath();
					model.put(PhonTalkWizardModel.OUTPUT_PATH, outDir);
				}
				
				libDirLabel.setText(StringUtils.shortenStringUsingToken((new File(selectedDir)).getAbsolutePath(), PhonConstants.ellipsis+"", 50));
				outDirLabel.setText(StringUtils.shortenStringUsingToken((new File(outDir)).getAbsolutePath(), PhonConstants.ellipsis+"", 50));
				
				if(model.get(PhonTalkWizardModel.PROJECT_NAME) != null) {
					model.put(PhonTalkWizardModel.PROJECT_NAME, (new File(selectedDir).getName()));
					projectNameField.setText((new File(selectedDir).getName()));
				}
			}
				
		}
		
	}
	
	private class SyllabifierRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			
			JLabel retVal = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);
			
			Syllabifier syllabifier = (Syllabifier)value;
			retVal.setText(syllabifier.getName());
			
			return retVal;
		}
		
		
		
	}
	
	private class DoSyllabificationAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			boolean enabled = doSyllabificationBox.isSelected();
			syllabifierBox.setEnabled(enabled);
		}
		
	}
	
	private class OutBrowseAction implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			JFrame parentFrame = 
				(JFrame)SwingUtilities.getAncestorOfClass(JFrame.class, DirectoryStep.this);
			String selectedDir = 
				NativeDialogs.browseForDirectoryBlocking(parentFrame, 
						model.get(PhonTalkWizardModel.LIBRARY_PATH).toString(), "Select output directory");
			if(selectedDir != null) {
				model.put(PhonTalkWizardModel.OUTPUT_PATH, selectedDir);
				
				outDirLabel.setText(StringUtils.shortenStringUsingToken(
						new File(selectedDir).getAbsolutePath(), PhonConstants.ellipsis+"", 50));
			}
		}
		
	}
	
	public boolean validateStep() {
		boolean retVal = true;
		
		String libDir = model.get(PhonTalkWizardModel.LIBRARY_PATH).toString();
		/** Ensure lib directory exists */
		File lib = new File(libDir);
		if(!lib.exists()) {
			retVal = false;
		}
		
		/** Ensure a project name is specified */
		if(projectNameField.getText().length() == 0) {
			retVal = false;
		} else {
			// check if file exists
			String outDir = null;
			if(model.get(PhonTalkWizardModel.OUTPUT_PATH) != null)
				outDir = model.get(PhonTalkWizardModel.OUTPUT_PATH).toString();
			if(outDir == null)
				outDir = libDir;
			File newProjFile = new File(outDir, projectNameField.getText() + ".phon");
			if(newProjFile.exists()) {
				int rv = NativeDialogs.showYesNoDialogBlocking(null, null, "Overwrite file?", 
						"Project '" + newProjFile.getAbsolutePath() + "' already exists.  Click 'Yes' to overwrite.");
				if(rv == NativeDialogEvent.NO_OPTION)
					retVal = false;
			}
		}
		
		return retVal;
	}
	
	public String getProjectName() {
		return projectNameField.getText();
	}
	
	public Syllabifier getSyllabifier() {
		Syllabifier retVal = null;
		
		if(doSyllabificationBox.isSelected())
			retVal = (Syllabifier)syllabifierBox.getSelectedItem();
		
		return retVal;
	}
}
