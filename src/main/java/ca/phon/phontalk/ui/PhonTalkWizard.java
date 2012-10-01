package ca.phon.phontalk.ui;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXBusyLabel;

import ca.phon.application.PhonTask;
import ca.phon.application.PhonWorker;
import ca.phon.application.project.IPhonProject;
import ca.phon.application.project.PhonProject;
import ca.phon.engines.syllabifier.Syllabifier;
import ca.phon.gui.wizard.*;
import ca.phon.phontalk.DirectoryTalkbankSource;
import ca.phon.phontalk.Phon2XmlTask;
import ca.phon.phontalk.TalkbankSource;
import ca.phon.phontalk.Xml2PhonTask;
import ca.phon.phontalk.ui.PhonTalkWizardModel.ConversionMode;
import ca.phon.system.logger.PhonLogger;
import ca.phon.util.StringUtils;

/**
 * Wizard for importing talkbank directories.
 */
public class PhonTalkWizard extends WizardFrame {

	/** Model */
	private PhonTalkWizardModel model = new PhonTalkWizardModel();
	
	/** Steps */
	private ModeStep modeStep;
	private ProjectStep projStep;
	private DirectoryStep dirStep;
	private ConversionStep conversionStep;

	private JXBusyLabel busyLabel;
	
	public PhonTalkWizard(String title) {
		super(title);
		super.setJMenuBar(null);
		init();
	}
	
	private void init() {
		_setupSteps();
		_setupButtons();
	}
	
	private void _setupSteps() {
		modeStep = new ModeStep(model);
		modeStep.setNextStep(1);
		
		projStep = new ProjectStep(model);
		projStep.setPrevStep(0);
		projStep.setNextStep(3);
		
		dirStep = new DirectoryStep(model);
		dirStep.setPrevStep(0);
		dirStep.setNextStep(3);
		
		conversionStep = new ConversionStep(model);
		conversionStep.setPrevStep(1);
		
		super.addWizardStep(modeStep);
		super.addWizardStep(projStep);
		super.addWizardStep(dirStep);
		super.addWizardStep(conversionStep);
	}
	
	private void _setupButtons() {
		super.btnFinish.setVisible(false);
		super.btnCancel.setVisible(false);
	}
	
	@Override
	public void next() {
		if(super.getCurrentStep() == modeStep) {
			// setup step numbering
			int nextStep = 
				(model.get(PhonTalkWizardModel.CONVERSION_MODE) == PhonTalkWizardModel.ConversionMode.Xml2Phon ? 2 : 1);
			modeStep.setNextStep(nextStep);
			conversionStep.setPrevStep(nextStep);
		}
		if(super.getCurrentStep() == dirStep) {
			// set project name
			model.put(PhonTalkWizardModel.PROJECT_NAME, dirStep.getProjectName());
			model.put(PhonTalkWizardModel.SYLLABIFIER_OBJ, dirStep.getSyllabifier());
		}
		super.next();
		if(super.getCurrentStep() == conversionStep) {
			startupWorker();
		}
	}
	
	private void startupWorker() {
		PhonWorker worker = PhonWorker.createWorker();
		worker.setName("Xml2Phon worker");
		worker.setFinishWhenQueueEmpty(true);
		
		worker.invokeLater(new StartTask(worker));
		
		conversionStep.getConsole().addReportThread(worker);
		conversionStep.getConsole().startLogging();
		
		worker.start();
	}
	
	private void setupBusyLabel() {
		if(busyLabel == null)
			busyLabel = new JXBusyLabel();
		
		JComponent glassPane = (JComponent)super.getGlassPane();
		
		Rectangle consoleBounds = conversionStep.getConsole().getBounds();
		Rectangle rect = SwingUtilities.convertRectangle(conversionStep.getConsole().getParent(), consoleBounds, glassPane);
		Rectangle busyBounds = 
			new Rectangle(rect.x + rect.width - busyLabel.getPreferredSize().width-20,
					rect.y+10,
					busyLabel.getPreferredSize().width,
					busyLabel.getPreferredSize().height);
		busyLabel.setBounds(busyBounds);
		
		glassPane.setLayout(null);
		glassPane.add(busyLabel);
		glassPane.setVisible(true);
	}
	
	private class StartTask extends PhonTask {
		
		private PhonWorker worker;
		
		public StartTask(PhonWorker w) {
			this.worker = w;
		}
		
		@Override
		public void performTask() {
			super.setStatus(TaskStatus.RUNNING);
			
			btnBack.setEnabled(false);
			setupBusyLabel();
			busyLabel.setBusy(true);
			
			if(model.get(PhonTalkWizardModel.CONVERSION_MODE) == ConversionMode.Xml2Phon) {
				// create project file and setup converter task
				String libDir = model.get(PhonTalkWizardModel.LIBRARY_PATH).toString();
				String outDir = null;
				if(model.get(PhonTalkWizardModel.OUTPUT_PATH) != null) {
					outDir = model.get(PhonTalkWizardModel.OUTPUT_PATH).toString();
				} else {
					outDir = libDir;
				}
				
				File outDirFile = new File(outDir);
				if(!outDirFile.exists()) {
					PhonLogger.info("Creating output directory '" + outDir + "'");
					if(!outDirFile.mkdir()) {
						PhonLogger.severe("Could not create output directory '" + outDir + "'");
						super.setStatus(TaskStatus.ERROR);
						return;
					}
				}
				
				String projectName = 
					model.get(PhonTalkWizardModel.PROJECT_NAME).toString();
//				File projFile = new File(outDirFile, projectName + "-xml2phon" + ".phon");
				File projFile = outDirFile;
//				if(projFile.exists()) {
//					PhonLogger.warning("Overwriting file '" + projFile.getAbsolutePath() + "'");
//					projFile.delete();
//				}
				
				IPhonProject newProject = null;
				try {
					String projPath = projFile.getAbsolutePath();
					newProject = PhonProject.newProject(projPath);
					newProject.setProjectName(projectName);
				} catch (IOException e) {
					PhonLogger.severe(e.toString());
					super.setStatus(TaskStatus.ERROR);
					super.err = e;
					return;
				}
				
				TalkbankSource tbSource = new DirectoryTalkbankSource(libDir);
				Xml2PhonTask converterTask = new Xml2PhonTask(tbSource, newProject);
				if(model.get(PhonTalkWizardModel.SYLLABIFIER_OBJ) != null) {
					converterTask.setSyllabifier((Syllabifier)model.get(PhonTalkWizardModel.SYLLABIFIER_OBJ));
				}
				worker.invokeLater(converterTask);
			} else if (model.get(PhonTalkWizardModel.CONVERSION_MODE) == ConversionMode.Phon2Xml) {
				// get the project
				File projFile = new File(model.get(PhonTalkWizardModel.PROJECT_PATH).toString());
				if(!projFile.exists()) {
					PhonLogger.severe("Could not find project at location " + projFile.getAbsolutePath());
					super.setStatus(TaskStatus.ERROR);
					return;
				}
				
				IPhonProject project = null;
				try {
					project = PhonProject.fromFile(projFile.getAbsolutePath());
				} catch (IOException e) {
					PhonLogger.severe(e.toString());
					super.setStatus(TaskStatus.ERROR);
					super.err = e;
					return;
				}
				
				File outDir = null;
				if(model.get(PhonTalkWizardModel.OUTPUT_PATH) != null) {
					outDir = new File(model.get(PhonTalkWizardModel.OUTPUT_PATH).toString());
				} else {
					outDir = projFile.getParentFile();
				}
				
				if(outDir.exists() && !outDir.isDirectory()) {
					PhonLogger.severe(outDir.getAbsolutePath() + " exists but is not a directory.");
					super.setStatus(TaskStatus.ERROR);
					return;
				}
				if(!outDir.exists()) {
					if(!outDir.mkdir()) {
						PhonLogger.severe("Could not create directory " + outDir.getAbsolutePath());
						super.setStatus(TaskStatus.ERROR);
						return;
					}
				}
				
				Phon2XmlTask converterTask = new Phon2XmlTask(project, outDir);
				worker.invokeLater(converterTask);
			}
			worker.setFinalTask(new FinalTask());
			
			super.setStatus(TaskStatus.FINISHED);
		}	
	}
	
	private class FinalTask extends PhonTask {
		
		private long startMs;
		
		public FinalTask() {
			this.startMs = System.currentTimeMillis();
		}

		@Override
		public void performTask() {
			long endMs = System.currentTimeMillis();
			long durMs = endMs - startMs;
			
			PhonLogger.info("Task complete " + StringUtils.msToWrittenString(durMs));
			
			btnBack.setEnabled(true);
			
			busyLabel.setBusy(false);
			getGlassPane().setVisible(false);
		}
		
	}
}
