package ca.phon.phontalk.plugin.wizard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.xml.XMLConstants;
import javax.xml.bind.ValidationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXTree;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.xpath.XPathResult;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTreeCellRenderer;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.DefaultCheckboxTreeCellRenderer;
import ca.phon.application.PhonWorker;
import ca.phon.application.project.IPhonProject;
import ca.phon.gui.DialogHeader;
import ca.phon.gui.SessionSelector;
import ca.phon.gui.components.CheckedTreeNode;
import ca.phon.gui.components.FileSelectionField;
import ca.phon.gui.components.PhonLoggerConsole;
import ca.phon.gui.components.FileSelectionField.SelectionMode;
import ca.phon.gui.wizard.WizardFrame;
import ca.phon.gui.wizard.WizardStep;
import ca.phon.phontalk.DefaultPhonTalkListener;
import ca.phon.phontalk.TalkbankValidator;
import ca.phon.phontalk.Xml2PhonTask;
import ca.phon.phontalk.plugin.PTMessageRenderer;
import ca.phon.phontalk.plugin.PluginMessageListener;
import ca.phon.system.logger.PhonLogger;
import ca.phon.util.NativeDialogEvent;
import ca.phon.util.NativeDialogs;

public class Talkbank2PhonWizard extends WizardFrame {
	
	private static final long serialVersionUID = -8143097674166717906L;
	
	/**
	 * Input folder selection
	 */
	private FileSelectionField inputFolderField;
	
	/**
	 * Import tree 
	 */
	private CheckboxTree importTree;
	
	/**
	 * Table
	 */
	private final PluginMessageListener listener = new PluginMessageListener();
	private final JXTree errTable = new JXTree(listener);
	
	/**
	 * Busy labels
	 */
	private JPanel scanBusyPanel;
	private JXBusyLabel scanBusyLabel;
	private JPanel exportBusyPanel;
	private JXBusyLabel exportBusyLabel;
	
	/**
	 * Steps
	 */
	private WizardStep step1;
	private WizardStep step2;
	
	/**
	 * Current worker
	 */
	private PhonWorker worker;
	private final Lock workerLock = new ReentrantLock();
	
	/**
	 * constructor
	 */
	public Talkbank2PhonWizard(IPhonProject project) {
		super("PhonTalk : Import from Talkbank XML");
		super.setProject(project);
		
		init();
	}
	
	private void init() {
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
	 * Select input folder and files for import.
	 */
	private WizardStep createSelectionStep() {
		inputFolderField = new FileSelectionField();
		inputFolderField.setPrompt("Input folder");
		inputFolderField.setMode(SelectionMode.FOLDERS);
		inputFolderField.addPropertyChangeListener("_selected_file_", inputFolderListener);
		inputFolderField.setEditable(false);
		inputFolderField.setFocusable(false);
		
		final JPanel wizardPanel = new JPanel(new BorderLayout());
		
		final JPanel outputFolderPanel = new JPanel();
		outputFolderPanel.setBorder(BorderFactory.createTitledBorder("Select input folder:"));
		outputFolderPanel.setLayout(new BorderLayout());
		outputFolderPanel.add(inputFolderField, BorderLayout.CENTER);
		
		final JPanel selectorPanel = new JPanel(new BorderLayout());
		final ImportTreeModel importTreeModel = new ImportTreeModel();
		importTree = new CheckboxTree(importTreeModel);
		importTree.setRootVisible(false);
		importTree.addCheckingPath(new TreePath(importTreeModel.getRoot()));
		
		final JScrollPane sessionScroller = new JScrollPane(importTree);
		selectorPanel.setBorder(BorderFactory.createTitledBorder("Select files for import:"));
		selectorPanel.add(sessionScroller, BorderLayout.CENTER);
		
		scanBusyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		scanBusyLabel = new JXBusyLabel(new Dimension(16, 16));
		scanBusyPanel.add(scanBusyLabel);
		scanBusyPanel.add(new JLabel("Scanning folder..."));
		scanBusyPanel.setVisible(false);
		selectorPanel.add(scanBusyPanel, BorderLayout.NORTH);
		
		wizardPanel.add(selectorPanel, BorderLayout.CENTER);
		wizardPanel.add(outputFolderPanel, BorderLayout.NORTH);
		
		final DialogHeader header = new DialogHeader("PhonTalk : Import from Talkbank", "Select input folder and files to import.");
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
//		loggerConsole = new PhonLoggerConsole();
		
		exportBusyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		exportBusyLabel = new JXBusyLabel(new Dimension(16, 16));
		exportBusyPanel.add(exportBusyLabel);
		exportBusyPanel.add(new JLabel("Importing files, this may take some time..."));
		exportBusyPanel.setVisible(false);
		
		final JPanel wizardPanel = new JPanel(new BorderLayout());
		wizardPanel.setBorder(BorderFactory.createTitledBorder("Converting files:"));
		wizardPanel.add(exportBusyPanel, BorderLayout.NORTH);
//		wizardPanel.add(loggerConsole, BorderLayout.CENTER);
		errTable.setRootVisible(false);
		errTable.setCellRenderer(new PTMessageRenderer());
		final JScrollPane errScroller = new JScrollPane(errTable);
		wizardPanel.add(errScroller, BorderLayout.CENTER);
		
		final DialogHeader header = new DialogHeader("PhonTalk : Import from Talkbank", "Importing files.");
		
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
					NativeDialogs.showOkCancelDialogBlocking(this, null, "Cancel import", "Cancel current import and close window?");
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
					NativeDialogs.showOkCancelDialogBlocking(this, null, "Cancel import", "Cancel current import?");
			if(userChoice == NativeDialogEvent.OK_OPTION) {
				cancelAndReset(null);
				super.prev();
			}
		} else {
			super.prev();
		}
	}

	@Override
	public void next() {
		if(super.getCurrentStep() == step1) {
			// check for a selection
			final TreePath[] selectedPaths = 
					importTree.getCheckingPaths();
			if(selectedPaths.length == 0) {
				NativeDialogs.showMessageDialogBlocking(this, null, 
						"Nothing to import", "Please select at least one file for import.");
				return;
			}
			
			if(scanBusyLabel.isBusy()) {
				NativeDialogs.showMessageDialogBlocking(this, null, "Scanning Folder", "Please wait for the scan to complete.");
				return;
			}
			
			// setup import thread
			workerLock.lock(); {
				worker = PhonWorker.createWorker();
				
				final Runnable startProgress = new Runnable() {
					
					@Override
					public void run() {
						exportBusyPanel.setVisible(true);
						exportBusyLabel.setBusy(true);
					}
				};
				final Runnable endProgress = new Runnable() {
					
					@Override
					public void run() {
						exportBusyLabel.setBusy(false);
						exportBusyPanel.setVisible(false);
						
						worker = null;
					}
				};
				
				worker.invokeLater(startProgress);
				for(TreePath selection:selectedPaths) {
					if(selection.getPathCount() == 4) {
						final DefaultMutableTreeNode fileNode = (DefaultMutableTreeNode)selection.getLastPathComponent();
						final File tbFile = (File) fileNode.getUserObject();
						
						final String corpus = ((CheckedTreeNode)selection.getPathComponent(1)).getUserObject().toString();
						final String session = ((CheckedTreeNode)selection.getPathComponent(2)).getUserObject().toString();
						
						final File projectFolder = new File(getProject().getProjectLocation());
						final File corpusFolder = new File(projectFolder, corpus);
						final File sessionFile = new File(corpusFolder, session + ".xml");
						
						if(!corpusFolder.exists()) {
							corpusFolder.mkdirs();
						}
						
						// import file
						final Xml2PhonTask importTask = 
								new Xml2PhonTask(tbFile.getAbsolutePath(), sessionFile.getAbsolutePath(), listener);
	
						worker.invokeLater(importTask);
					}
				}
				worker.invokeLater(endProgress);
				
				worker.setFinishWhenQueueEmpty(true);
			}
			workerLock.unlock();
			worker.start();
		}
		super.next();
	}
	
	private void scanFolder(File folder) {
		if(folder == null || !folder.isDirectory()) {
			return;
		} else {
			for(File f:folder.listFiles()) {
				if(f.isFile() && f.getName().endsWith(".xml")) {
					// check for a talkbank file
					final TalkbankValidator tbValidator = new TalkbankValidator();
					try {
						if(tbValidator.validate(f)) {
							try {
								final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
								final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
								final Document doc = docBuilder.parse(f);
								
								// use xpath to grab the corpus and session names
								final XPathFactory xpathFactory = XPathFactory.newInstance();
								final XPath xpath = xpathFactory.newXPath();
							
								final XPathExpression corpusExpr = xpath.compile("/CHAT/@Corpus");
								final XPathExpression sessionExpr = xpath.compile("/CHAT/@Id");
								
								final String corpusName = 
										(String)corpusExpr.evaluate(doc, XPathConstants.STRING);
								final String sessionName = 
										(String)sessionExpr.evaluate(doc, XPathConstants.STRING);
								
								// add node to import tree using the provided session and corpus names
								final ImportTreeModel treeModel = 
										(ImportTreeModel)importTree.getModel();
								treeModel.addImport(corpusName, sessionName, f);
							} catch (XPathExpressionException e) {
								e.printStackTrace();
								PhonLogger.severe(e.getMessage());
							} catch (FileNotFoundException e) {
								e.printStackTrace();
								PhonLogger.severe(e.getMessage());
							} catch (ParserConfigurationException e) {
								e.printStackTrace();
								PhonLogger.severe(e.getMessage());
							} catch (SAXException e) {
								e.printStackTrace();
								PhonLogger.severe(e.getMessage());
							} catch (IOException e) {
								e.printStackTrace();
								PhonLogger.severe(e.getMessage());
							}
						}
					} catch (ValidationException e) {
						e.printStackTrace();
						PhonLogger.severe(e.getMessage());
					}
				} else if(f.isDirectory()) {
					scanFolder(f);
				}
			}
		}
	}
	
	private final PropertyChangeListener inputFolderListener = new PropertyChangeListener() {
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			final Runnable scanFolder = new Runnable() {
				
				@Override
				public void run() {
					scanBusyPanel.setVisible(true);
					scanBusyLabel.setBusy(true);
					final File folder = inputFolderField.getSelectedFile();
					final ImportTreeModel treeModel = new ImportTreeModel();
					importTree.setModel(treeModel);
					importTree.setRootVisible(true);
					importTree.addCheckingPath(new TreePath(treeModel.getRoot()));
					scanFolder(folder);
					scanBusyLabel.setBusy(false);
					scanBusyPanel.setVisible(false);
				}
			};
			PhonWorker.getInstance().invokeLater(scanFolder);
		}
		
	};
	
	/**
	 * Import tree table model
	 */
	private class ImportTreeModel extends DefaultTreeModel {
		
		public ImportTreeModel() {
			super(new CheckedTreeNode(getProject().getProjectName()));
		}
		
		/**
		 * Add a new import file to the model
		 * 
		 * @param corpus
		 * @param session
		 * @param file
		 */
		public void addImport(String corpus, String session, File file) {
			// find corpus node
			final CheckedTreeNode rootNode = (CheckedTreeNode) super.getRoot();
			
			CheckedTreeNode corpusNode = null;
			for(int i = 0; i < rootNode.getChildCount(); i++) {
				CheckedTreeNode cNode = (CheckedTreeNode) rootNode.getChildAt(i);
				if(cNode.getUserObject().toString().equals(corpus)) {
					corpusNode = cNode;
					break;
				}
			}
			if(corpusNode == null) {
				corpusNode = new CheckedTreeNode(corpus);
				rootNode.add(corpusNode);
				super.fireTreeNodesInserted(rootNode, new Object[]{rootNode},
						new int[]{rootNode.getIndex(corpusNode)}, new Object[]{corpusNode});
			}
			
			// create a new session node
			final CheckedTreeNode sessionNode = new CheckedTreeNode(session);
			corpusNode.add(sessionNode);
			super.fireTreeNodesInserted(corpusNode, new Object[]{rootNode, corpusNode},
					new int[]{corpusNode.getIndex(sessionNode)}, new Object[]{sessionNode});
			
			final MutableTreeNode fileNode = new DefaultMutableTreeNode(file);
			sessionNode.add(fileNode);
			super.fireTreeNodesInserted(sessionNode, new Object[]{rootNode, corpusNode, sessionNode},
					new int[]{sessionNode.getIndex(fileNode)}, new Object[]{fileNode});
			
			final MutableTreeNode fCorpusNode = corpusNode;
			final Runnable onEDT = new Runnable() {
				
				@Override
				public void run() {
					final TreePath corpusPath = new TreePath(new Object[]{rootNode, fCorpusNode});
					if(!importTree.isExpanded(corpusPath)) {
						importTree.expandPath(corpusPath);
					}
				}
			};
			SwingUtilities.invokeLater(onEDT);
		}
		
	}
}
