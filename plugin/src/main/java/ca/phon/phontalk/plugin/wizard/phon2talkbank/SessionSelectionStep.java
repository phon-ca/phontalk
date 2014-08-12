package ca.phon.phontalk.plugin.wizard.phon2talkbank;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import ca.phon.application.project.IPhonProject;
import ca.phon.gui.DialogHeader;
import ca.phon.gui.SessionSelector;
import ca.phon.gui.components.FileSelectionField;
import ca.phon.gui.components.FileSelectionField.SelectionMode;
import ca.phon.gui.wizard.WizardStep;

public class SessionSelectionStep extends WizardStep {
	
	private static final long serialVersionUID = 4260810910198482714L;
	
	private IPhonProject project;
	
	/**
	 * Session selector
	 */
	private SessionSelector sessionSelector;
	
	/**
	 * Folder label
	 */
	private FileSelectionField outputFolderField;

	public SessionSelectionStep(IPhonProject project) {
		super();
		
		this.project = project;
		init();
	}
	
	public IPhonProject getProject() {
		return this.project;
	}

	private void init() {
		outputFolderField = new FileSelectionField();
		outputFolderField.setPrompt("Output folder");
		outputFolderField.setMode(SelectionMode.FOLDERS);
		
		final JPanel wizardPanel = new JPanel(new BorderLayout());
		
		final JPanel outputFolderPanel = new JPanel();
		outputFolderPanel.setBorder(BorderFactory.createTitledBorder("Select output folder:"));
		outputFolderPanel.setLayout(new BorderLayout());
		outputFolderPanel.add(outputFolderField, BorderLayout.CENTER);
		
		final JPanel selectorPanel = new JPanel(new BorderLayout());
		sessionSelector = new SessionSelector(getProject());
		final JScrollPane sessionScroller = new JScrollPane(sessionSelector);
		selectorPanel.setBorder(BorderFactory.createTitledBorder("Select sessions for export:"));
		selectorPanel.add(sessionScroller, BorderLayout.CENTER);
		
		wizardPanel.add(selectorPanel, BorderLayout.CENTER);
		wizardPanel.add(outputFolderPanel, BorderLayout.NORTH);
		
		final DialogHeader header = new DialogHeader("PhonTalk : Export to Talkbank", "Select sessions and output folder.");
		setLayout(new BorderLayout());
		add(header, BorderLayout.NORTH);
		add(wizardPanel, BorderLayout.CENTER);
	}
	
	public FileSelectionField getOutputFolderField() {
		return this.outputFolderField;
	}
	
	public SessionSelector getSessionSelector() {
		return this.sessionSelector;
	}
	
}
