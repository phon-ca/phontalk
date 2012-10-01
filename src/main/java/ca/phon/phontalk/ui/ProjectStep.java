package ca.phon.phontalk.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import ca.phon.gui.DialogHeader;
import ca.phon.gui.wizard.WizardStep;
import ca.phon.util.FileFilter;
import ca.phon.util.NativeDialogs;
import ca.phon.util.PhonConstants;
import ca.phon.util.StringUtils;
import ca.phon.util.iconManager.IconManager;
import ca.phon.util.iconManager.IconSize;

/** 
 * Select project and output directory when
 * converting from phon2xml.
 * 
 * 
 */
public class ProjectStep extends WizardStep {
	/* UI */
	private JLabel phonProjLabel = new JLabel();
	private JButton projBrowseBtn = new JButton();
	private JLabel outDirLabel = new JLabel();
	private JButton outBrowseBtn = new JButton();
	
	private PhonTalkWizardModel model;
	
	public ProjectStep(PhonTalkWizardModel model) {
		super();
		
		this.model = model;
		init();
	}
	
	private void init() {
		JPanel centerPanel = new JPanel();
		// setup layout
		FormLayout layout = new FormLayout(
				"right:pref, 3dlu, fill:pref:grow, pref",
				"pref, pref, pref");
		CellConstraints cc = new CellConstraints();
		centerPanel.setLayout(layout);

		String libDir = model.get(PhonTalkWizardModel.LIBRARY_PATH).toString();
		String outDir = null;
		if(model.get(PhonTalkWizardModel.OUTPUT_PATH) != null)
			outDir = model.get(PhonTalkWizardModel.OUTPUT_PATH).toString();
		else
			outDir = libDir;
		
		phonProjLabel.setText("");
		outDirLabel.setText(StringUtils.shortenStringUsingToken((new File(outDir)).getAbsolutePath(), PhonConstants.ellipsis+"", 50));
		
		ImageIcon browseIcon = 
			IconManager.getInstance().getIcon("actions/document-open", IconSize.SMALL);
		projBrowseBtn.setIcon(browseIcon);
		projBrowseBtn.setToolTipText("Select project");
		projBrowseBtn.addActionListener(new ProjBrowseAction());
		
		outBrowseBtn.setIcon(browseIcon);
		outBrowseBtn.setToolTipText("Select output directory");
		outBrowseBtn.addActionListener(new OutBrowseAction());
		
		centerPanel.add(new JLabel("Phon project"), cc.xy(1,1));
		centerPanel.add(phonProjLabel, cc.xy(3,1));
		centerPanel.add(projBrowseBtn, cc.xy(4, 1));
		
		centerPanel.add(new JLabel("Output Folder"), cc.xy(1,2));
		centerPanel.add(outDirLabel, cc.xy(3,2));
		centerPanel.add(outBrowseBtn, cc.xy(4, 2));
		
		setLayout(new BorderLayout());
		add(new DialogHeader("Phon2Xml", "Select project and output folder"), BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
	}
	
	private class ProjBrowseAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {

			JFrame parentFrame = 
				(JFrame)SwingUtilities.getAncestorOfClass(JFrame.class, ProjectStep.this);
			
			FileFilter[] phonFilter = new FileFilter[1];
			phonFilter[0] = FileFilter.phonFilter;
			
			String selectedFile =
					NativeDialogs.browseForDirectoryBlocking(parentFrame, "", "");
//				NativeDialogs.browseForFileBlocking(parentFrame, null, ".phon", phonFilter, "Select project");
			if(selectedFile != null) {
				model.put(PhonTalkWizardModel.PROJECT_PATH, selectedFile);
				
				phonProjLabel.setText(StringUtils.shortenStringUsingToken(
						(new File(selectedFile)).getAbsolutePath(), PhonConstants.ellipsis+"", 50));
				
				File parentDir = (new File(selectedFile)).getParentFile();
				File outDir = new File(parentDir, (new File(selectedFile)).getName()+"-xml");
				
				model.put(PhonTalkWizardModel.OUTPUT_PATH, outDir.getAbsolutePath());
				
				outDirLabel.setText(StringUtils.shortenStringUsingToken(
						outDir.getAbsolutePath(), PhonConstants.ellipsis+"", 50));
			}
		}
		
	}
	
	private class OutBrowseAction implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			JFrame parentFrame = 
				(JFrame)SwingUtilities.getAncestorOfClass(JFrame.class, ProjectStep.this);
			
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
}
