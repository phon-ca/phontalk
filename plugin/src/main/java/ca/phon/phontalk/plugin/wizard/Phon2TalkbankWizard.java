/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2008 The Phon Project, Memorial University <http://phon.ling.mun.ca>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.phon.phontalk.plugin.wizard;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTree;
import org.jdesktop.swingx.painter.BusyPainter;

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
import ca.phon.gui.components.FileSelectionField;
import ca.phon.gui.components.FileSelectionField.SelectionMode;
import ca.phon.gui.components.PhonLoggerConsole;
import ca.phon.gui.wizard.WizardFrame;
import ca.phon.gui.wizard.WizardStep;
import ca.phon.phontalk.DefaultPhonTalkListener;
import ca.phon.phontalk.Phon2XmlTask;
import ca.phon.phontalk.plugin.PTMessageRenderer;
import ca.phon.phontalk.plugin.PluginMessageListener;
import ca.phon.system.logger.PhonLogger;
import ca.phon.util.NativeDialogEvent;
import ca.phon.util.NativeDialogs;
import ca.phon.util.iconManager.IconManager;
import ca.phon.util.iconManager.IconSize;

/**
 * Wizard for exporting session to Talkbank.
 * 
 */
public class Phon2TalkbankWizard extends WizardFrame {

	private static final long serialVersionUID = -4135395782785639623L;

	static {
		System.setProperty("phontalk.verbose", Boolean.TRUE.toString());
	}
	
	/**
	 * Session selector
	 */
	private SessionSelector sessionSelector;
	
	/**
	 * Folder label
	 */
	private FileSelectionField outputFolderField;
	
	/**
	 * Busy label
	 */
	private JPanel busyLabelPanel;
	private JXBusyLabel busyLabel;
	
	/**
	 * Table
	 */
	private final PluginMessageListener listener = new PluginMessageListener();
	private final JXTree errTable = new JXTree(listener);
	
	/**
	 * Generated wizard steps
	 */
	private WizardStep step1;
	private WizardStep step2;
	
	/**
	 * Current worker
	 */
	private PhonWorker worker;
	private final Lock workerLock = new ReentrantLock();
	
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
		super.btnCancel.setText("Close");
	}
	
	/**
	 * Step 1
	 * 
	 * Selection session and output folder
	 */
	private WizardStep createSelectionStep() {
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
		busyLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		busyLabel = new JXBusyLabel(new Dimension(16, 16));
		busyLabel.setBusy(true);
		busyLabelPanel.add(busyLabel);
		busyLabelPanel.add(new JLabel("Exporting files, this may take some time."));
		busyLabelPanel.setVisible(false);
		
		final JPanel wizardPanel = new JPanel(new BorderLayout());
		wizardPanel.setBorder(BorderFactory.createTitledBorder("Converting files:"));
		wizardPanel.add(busyLabelPanel, BorderLayout.NORTH);
		errTable.setRootVisible(false);
		listener.addTreeModelListener(new TreeModelListener() {
			
			@Override
			public void treeStructureChanged(TreeModelEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void treeNodesRemoved(TreeModelEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void treeNodesInserted(TreeModelEvent arg0) {
				final TreePath tp = arg0.getTreePath().getParentPath();
				final Runnable onEDT = new Runnable() {
					
					@Override
					public void run() {
						if(!errTable.isExpanded(tp)) {
							errTable.expandPath(tp);
						}
					}
				};
				SwingUtilities.invokeLater(onEDT);
			}
			
			@Override
			public void treeNodesChanged(TreeModelEvent arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		errTable.setCellRenderer(new PTMessageRenderer());
		final JScrollPane errScroller = new JScrollPane(errTable);
		wizardPanel.add(errScroller, BorderLayout.CENTER);
		
		final DialogHeader header = new DialogHeader("PhonTalk : Export to Talkbank", "Exporting files.");
		
		final WizardStep retVal = new WizardStep();
		retVal.setLayout(new BorderLayout());
		
		retVal.add(header, BorderLayout.NORTH);
		retVal.add(wizardPanel, BorderLayout.CENTER);
		
		return retVal;
	}
	
	/**
	 * Stop the conversion process and reset error table. 
	 * 
	 * @param a final task for the worker to complete.
	 */
	private void cancelAndReset(Runnable toDo) {
		workerLock.lock(); {
			if(worker != null) {
				if(toDo != null)
					worker.setFinalTask(toDo);
				worker.shutdown();
				
				worker = null;
			}
		}
		workerLock.unlock();
		
		listener.reset();
	}
	
	@Override
	protected void cancel() {
		if(worker != null && worker.isAlive()) {
			final int userChoice = 
					NativeDialogs.showOkCancelDialogBlocking(this, null, "Cancel export", "Cancel current export and close window?");
			if(userChoice == NativeDialogEvent.OK_OPTION) {
				cancelAndReset(null);
				super.cancel();
			}
		} else {
			super.cancel();
		}
	}
	
	

	@Override
	protected void prev() {
		if(worker != null && worker.isAlive()) {
			final int userChoice = 
					NativeDialogs.showOkCancelDialogBlocking(this, null, "Cancel export", "Cancel current export?");
			if(userChoice == NativeDialogEvent.OK_OPTION) {
				cancelAndReset(null);
				super.prev();
			}
		} else {
			super.prev();
		}
	}
	
//	/**
//	 * Show browse dialog for output folder.
//	 */
//	public void browseForFolder() {
//		final String selectedFolder = 
//				NativeDialogs.browseForDirectoryBlocking(CommonModuleFrame.getCurrentFrame(),
//						(outputFolder == null ? null : outputFolder.getAbsolutePath()), "Select output folder");
//		if(selectedFolder != null) {
//			outputFolder = new File(selectedFolder);
//			folderLabel.setText(selectedFolder);
//		}
//	}

	@Override
	public void next() {
		if(super.getCurrentStep() == step1) {
			// make sure a valid output folder is selected
			// and we have some selected sessions
			final List<SessionLocation> selectedSessions = sessionSelector.getSelectedSessions();
			final File outputLocation = outputFolderField.getSelectedFile();
			
			if(outputLocation == null) {
				NativeDialogs.showMessageDialogBlocking(this, null, "null output folder", "Please select an output folder.");
				return;
			}
			
			if(!outputLocation.exists()) {
				if(!outputLocation.mkdirs()) {
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
			workerLock.lock(); {
				worker = PhonWorker.createWorker();
				worker.setFinishWhenQueueEmpty(true);
				Collections.sort(selectedSessions);
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
					
					final Runnable startProgress = new Runnable() {
						
						@Override
						public void run() {
							busyLabelPanel.setVisible(true);
							busyLabel.setBusy(true);
						}
					};
					final Runnable endProgress = new Runnable() {
						
						@Override
						public void run() {
							busyLabel.setBusy(false);
							busyLabelPanel.setVisible(false);
						}
					};
					
					final PhonTask task = new Phon2XmlTask(sessionFile.getAbsolutePath(), outputFile.getAbsolutePath(), listener);
					
					worker.invokeLater(startProgress);
					worker.invokeLater(task);
					worker.invokeLater(endProgress);
				}
			}
			workerLock.unlock();
			worker.start();
		}
		super.next();
	}
	
}
