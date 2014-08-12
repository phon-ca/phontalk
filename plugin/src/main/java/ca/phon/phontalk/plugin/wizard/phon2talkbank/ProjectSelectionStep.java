package ca.phon.phontalk.plugin.wizard.phon2talkbank;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.jdesktop.swingx.VerticalLayout;

import ca.phon.application.project.IPhonProject;
import ca.phon.application.project.PhonProject;
import ca.phon.gui.DialogHeader;
import ca.phon.gui.components.FileSelectionField;
import ca.phon.gui.components.FileSelectionField.SelectionMode;
import ca.phon.gui.wizard.WizardStep;
import ca.phon.util.PhonUtilities;

public class ProjectSelectionStep extends WizardStep {

	private static final long serialVersionUID = -1908527260534577209L;
	
	private FileSelectionField projectFolderField;

	public ProjectSelectionStep() {
		super();
		
		init();
	}
	
	private void init() {
		projectFolderField = new FileSelectionField();
		projectFolderField.setMode(SelectionMode.FOLDERS);
		
		setLayout(new BorderLayout());
		
		final DialogHeader header = new DialogHeader(
				"PhonTalk : Export to Talkbank", "Select Phon project");
		add(header, BorderLayout.NORTH);
		
		final JPanel centerPanel = new JPanel(new VerticalLayout());
		centerPanel.add(projectFolderField);
		centerPanel.setBorder(BorderFactory.createTitledBorder("Select project:"));
		add(centerPanel, BorderLayout.CENTER);
	}
	
	public IPhonProject getProject() throws IOException {
		return PhonProject.fromFile(projectFolderField.getSelectedFile().getAbsolutePath());
	}
	
	@Override
	public boolean validateStep() {
		final File projectFolder = projectFolderField.getSelectedFile();
		if(projectFolder != null) {
			final File xmlFile = new File(projectFolder, "project.xml");
			if(!xmlFile.exists()) {
				PhonUtilities.showComponentMessage(projectFolderField, "Not a Phon project.");
				return false;
			}
		} else {
			PhonUtilities.showComponentMessage(projectFolderField, "Please select a project.");
			return false;
		}
		return true;
	}
	
}
