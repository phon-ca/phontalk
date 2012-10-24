package ca.phon.phontalk.plugin.wizard;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXBusyLabel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import ca.phon.application.PhonTask;
import ca.phon.application.PhonWorker;
import ca.phon.application.project.IPhonProject;
import ca.phon.application.transcript.SessionLocation;
import ca.phon.gui.CommonModuleFrame;
import ca.phon.gui.DialogHeader;
import ca.phon.gui.SessionSelector;
import ca.phon.gui.action.PhonUIAction;
import ca.phon.gui.components.PhonLoggerConsole;
import ca.phon.gui.wizard.WizardFrame;
import ca.phon.gui.wizard.WizardStep;
import ca.phon.phontalk.Phon2XmlStreamTask;
import ca.phon.util.NativeDialogs;
import ca.phon.util.iconManager.IconManager;
import ca.phon.util.iconManager.IconSize;

/**
 * Wizard for exporting session to Talkbank.
 * 
 */
public class Phon2TalkbankWizard extends WizardFrame {

	private static final long serialVersionUID = -4135395782785639623L;
	
	/**
	 * Session selector
	 */
	private SessionSelector sessionSelector;
	
	/**
	 * Folder label
	 */
	private JLabel folderLabel;
	
	/**
	 * Folder selection button
	 */
	private JButton selectFolderBtn;
	private File outputFolder;
	
	/**
	 * Phon Logger console
	 */
	private PhonLoggerConsole loggerConsole;
	
	/**
	 * Busy label
	 */
	private JXBusyLabel busyLabel;
	
	/**
	 * Generated wizard steps
	 */
	private WizardStep step1;
	private WizardStep step2;
	
	/**
	 * Constructor
	 */
	public Phon2TalkbankWizard(IPhonProject project) {
		super("PhonTalk : Convert Sessions to Talkbank XML");
		super.setProject(project);
		
		init();
	}

	private void init() {
		// create wizard steps
		step1 = createSelectionStep();
		step1.setNextStep(1);
		
		step2 = createReportStep();
		step2.setPrevStep(0);
		
		addWizardStep(step1);
		addWizardStep(step2);
		
		super.btnFinish.setVisible(false);
	}
	
	/**
	 * Step 1
	 * 
	 * Selection session and output folder
	 */
	private WizardStep createSelectionStep() {
		// default output folder is Documents
		final String outputFolderRoot = System.getProperty("user.home");
		String defaultOutputFolder = outputFolderRoot + File.separator + "Documents";
		outputFolder = new File(defaultOutputFolder);
		
		folderLabel = new JLabel(defaultOutputFolder);
		
		final ImageIcon browseIcon = IconManager.getInstance().getIcon("actions/document-open", IconSize.SMALL);
		final PhonUIAction browseAct = new PhonUIAction(this, "browseForFolder");
		browseAct.putValue(PhonUIAction.NAME, "");
		browseAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Browse for output folder...");
		browseAct.putValue(PhonUIAction.SMALL_ICON, browseIcon);
		selectFolderBtn = new JButton(browseAct);
		
		
		final JPanel wizardPanel = new JPanel(new BorderLayout());
		
		final JPanel outputFolderPanel = new JPanel();
		outputFolderPanel.setBorder(BorderFactory.createTitledBorder("Select output folder:"));
		final FormLayout outputFolderLayout = new FormLayout(
				"fill:pref:grow, pref", 
				"pref");
		final CellConstraints cc = new CellConstraints();
		outputFolderPanel.setLayout(outputFolderLayout);
		outputFolderPanel.add(folderLabel, cc.xy(1,1));
		outputFolderPanel.add(selectFolderBtn, cc.xy(2,1));

		sessionSelector = new SessionSelector(getProject());
		final JScrollPane sessionScroller = new JScrollPane(sessionSelector);
		sessionSelector.setBorder(BorderFactory.createTitledBorder("Selection sessions for export:"));
	
		wizardPanel.add(sessionScroller, BorderLayout.CENTER);
		wizardPanel.add(outputFolderPanel, BorderLayout.SOUTH);
		
		final DialogHeader header = new DialogHeader("PhonTalk : Export to Talkbank", "Select sessions and output folder.");
		final WizardStep retVal = new WizardStep();
		retVal.setLayout(new BorderLayout());
		retVal.add(header, BorderLayout.NORTH);
		retVal.add(wizardPanel, BorderLayout.CENTER);
		return retVal;
	}
	
	/**
	 * Step 2
	 * 
	 * Console reporting
	 */
	private WizardStep createReportStep() {
		loggerConsole = new PhonLoggerConsole();
		
		busyLabel = new JXBusyLabel(new Dimension(16, 16));
		busyLabel.setBusy(true);
		
		final JPanel topPanel = new JPanel(new FlowLayout());
		topPanel.add(busyLabel);
		topPanel.add(new JLabel("Exporting files, this may take some time."));
		
		final JPanel wizardPanel = new JPanel(new BorderLayout());
		wizardPanel.setBorder(BorderFactory.createTitledBorder("Converting files:"));
		wizardPanel.add(topPanel, BorderLayout.NORTH);
		wizardPanel.add(loggerConsole, BorderLayout.CENTER);
		
		final DialogHeader header = new DialogHeader("PhonTalk : Export to Talkbank", "Exporting files.");
		
		final WizardStep retVal = new WizardStep();
		retVal.setLayout(new BorderLayout());
		
		retVal.add(header, BorderLayout.NORTH);
		retVal.add(wizardPanel, BorderLayout.CENTER);
		
		return retVal;
	}
	
	/**
	 * Show browse dialog for output folder.
	 */
	public void browseForFolder() {
		final String selectedFolder = 
				NativeDialogs.browseForDirectoryBlocking(CommonModuleFrame.getCurrentFrame(),
						(outputFolder == null ? null : outputFolder.getAbsolutePath()), "Select output folder");
		if(selectedFolder != null) {
			outputFolder = new File(selectedFolder);
			folderLabel.setText(selectedFolder);
		}
	}

	@Override
	public void next() {
		if(super.getCurrentStep() == step1) {
			// make sure a valid output folder is selected
			// and we have some selected sessions
			final List<SessionLocation> selectedSessions = sessionSelector.getSelectedSessions();
			final File outputLocation = outputFolder;
			
			if(outputLocation == null) {
				NativeDialogs.showMessageDialogBlocking(this, null, "null output folder", "Please select an output folder.");
				return;
			}
			
			if(!outputFolder.exists()) {
				if(!outputFolder.mkdirs()) {
					NativeDialogs.showMessageDialogBlocking(this, null, "Invalid output folder", "Could not create output folder.");
					return;
				}
			}
			
			if(!outputLocation.canWrite()) {
				NativeDialogs.showMessageDialogBlocking(this, null, "Invalid output folder", "Cannot write to selected folder.");
				return;
			}
			
			if(selectedSessions.size() == 0) {
				NativeDialogs.showMessageDialogBlocking(this, null, "No sessions selected", "Please select at least one session for export.");
				return;
			}
			
			// setup conversion tasks
			final PhonWorker worker = PhonWorker.createWorker();
			worker.setFinishWhenQueueEmpty(true);
			
			for(SessionLocation sessionLocation:selectedSessions) {
				final String sessionPath = sessionLocation.getCorpus() + File.separator + sessionLocation.getSession() + ".xml";
				final String projectRoot = getProject().getProjectLocation();
				final File sessionFile = new File(projectRoot, sessionPath);
				
				final String outputPath = getProject().getProjectName() + File.separator + sessionLocation.getCorpus();
				final File outputRoot = new File(outputLocation, outputPath);
				if(!outputRoot.exists()) {
					outputRoot.mkdirs();
				}
				final File outputFile = new File(outputRoot, sessionLocation.getSession() + ".xml");
				
				final PhonTask task = new Phon2XmlStreamTask(sessionFile.getAbsolutePath(), outputFile.getAbsolutePath());
				worker.invokeLater(task);
			}
			
			// reset console
			loggerConsole.clearText();
			loggerConsole.addReportThread(worker);
			loggerConsole.startLogging();
			
			worker.start();
		}
		super.next();
	}
	
}
