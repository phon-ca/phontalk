package ca.phon.phontalk.plugin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.TreePath;

import ca.phon.app.project.*;
import ca.phon.formatter.MediaTimeFormatter;
import ca.phon.project.LocalProject;
import ca.phon.ui.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.WordUtils;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.JXStatusBar.Constraint.ResizeBehavior;
import org.jdesktop.swingx.JXTable;

import ca.phon.app.log.BufferPanel;
import ca.phon.app.log.LogUtil;
import ca.phon.app.modules.EntryPointArgs;
import ca.phon.app.workspace.Workspace;
import ca.phon.app.workspace.WorkspaceHistory;
import ca.phon.phontalk.CHAT2PhonTask;
import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.PhonTalkMessage;
import ca.phon.phontalk.PhonTalkTask;
import ca.phon.phontalk.Xml2PhonTask;
import ca.phon.plugin.PluginEntryPointRunner;
import ca.phon.plugin.PluginException;
import ca.phon.project.Project;
import ca.phon.project.ProjectFactory;
import ca.phon.project.exceptions.ProjectConfigurationException;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.decorations.DialogHeader;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.jbreadcrumb.BreadcrumbButton;
import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.ui.nativedialogs.MessageDialogProperties;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxState;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxTree;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxTreeCellEditor;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxTreeCellRenderer;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxTreeModel;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxTreeNode;
import ca.phon.ui.wizard.BreadcrumbWizardFrame;
import ca.phon.ui.wizard.WizardStep;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;
import ca.phon.worker.PhonTask;
import ca.phon.worker.PhonTaskListener;
import ca.phon.worker.PhonWorker;
import ca.phon.worker.PhonTask.TaskStatus;
import org.jdesktop.swingx.VerticalLayout;

public class ImportWizard extends BreadcrumbWizardFrame {
	
	public final static String DIALOG_TITLE = "Import from CHAT/TalkBank";
	public final static String DIALOG_MESAGE = "Import to Phon from a folder of CHAT (.cha) or TalkBank (.xml) files";
	
	/* Step 1 */
	private WizardStep folderStep;
	
	private final static int MAX_FOLDERS = 5;

	private final WorkspaceHistory workspaceHistory = new WorkspaceHistory();
	
	private final static String IMPORTFOLDER_HISTORY_PROP = ImportWizard.class.getName() + ".importFolderHistory";
	private FileHistorySelectionButton importFolderButton;

	private JRadioButton useWorkspaceButton = new JRadioButton("Create new project in workspace folder");
	private JRadioButton storeInProjectButton = new JRadioButton("Add files to existing Phon project");

	private ProjectSelectionButton projectButton;

	private final static String OUTPUTFOLDER_HISTORY_PROP = ImportWizard.class.getName() + ".outputFolderHistory";
	private FileHistorySelectionButton outputFolderButton;

	private TristateCheckBoxTree fileSelectionTree;
	
	/* Step 2 */
	private WizardStep optionsStep;
	private Xml2PhonSettingsPanel settingsPanel;
	
	/* Step 3 */
	private WizardStep importStep;
	private PhonWorker currentWorker;
	private JSplitPane splitPane;
	private JXTable taskTable;
	private JXTable messageTable;
	private BufferPanel bufferPanel;
	
	protected BreadcrumbButton btnStop;
	protected BreadcrumbButton btnRunAgain;
	protected BreadcrumbButton btnOpenProject;
	
	private boolean running = false;
	private boolean canceled = false;
	private long importStartedMs = 0L;
	private long importFinishedMs = 0L;
	private JXStatusBar statusBar;
	private JXBusyLabel busyLabel;
	private JLabel statusLabel;
	
	private int numFilesToCopy = 0;
	private int numCHATFiles = 0;
	private int numTBFiles = 0;
	
	private int numCHATFilesProcessed = 0;
	private int numTBFilesProcessed = 0;
	private int numFilesCopied = 0;
	private int numFilesFailed = 0;


	public ImportWizard(@Nullable Project p) {
		super(DIALOG_TITLE);
		setWindowName(DIALOG_TITLE);

		putExtension(Project.class, p);
		
		init();
	}

	public Project getProject() {
		return getExtension(Project.class);
	}
	
	private void init() {
		setupFolderStep();
		setupOptionsStep();
		createImportStep();
		
		folderStep.setNextStep(1);
		optionsStep.setPrevStep(0);
		optionsStep.setNextStep(2);
		importStep.setPrevStep(1);
		
		addWizardStep(folderStep);
		addWizardStep(optionsStep);
		addWizardStep(importStep);
		
		btnStop = new BreadcrumbButton();
		btnStop.setFont(FontPreferences.getTitleFont().deriveFont(Font.BOLD));
		btnStop.setText("Stop");
		btnStop.setBackground(Color.red);
		btnStop.setForeground(Color.white);
		btnStop.addActionListener( (e) -> close() );

		btnOpenProject = new BreadcrumbButton();
		btnOpenProject.setFont(FontPreferences.getTitleFont().deriveFont(Font.BOLD));
		btnOpenProject.setText("Open project");
		btnOpenProject.addActionListener( (e) -> {
			File projectFolder = outputFolderButton.getSelection();
			EntryPointArgs epArgs = new EntryPointArgs();
			epArgs.put(EntryPointArgs.PROJECT_LOCATION, projectFolder.getAbsolutePath());
			
			try {
				PluginEntryPointRunner.executePlugin(OpenProjectEP.EP_NAME, epArgs);
			} catch (PluginException e1) {
				LogUtil.severe(e1);
			}
		});
		
		btnRunAgain = new BreadcrumbButton();
		btnRunAgain.setFont(FontPreferences.getTitleFont().deriveFont(Font.BOLD));
		btnRunAgain.setText("Run again");
		btnRunAgain.addActionListener( (e) -> {
			if(!running) {
				beginImport();
			}
		});
		
		statusBar = new JXStatusBar();
		busyLabel = new JXBusyLabel(new Dimension(16, 16));
		busyLabel.setBusy(false);
		statusBar.add(busyLabel, new JXStatusBar.Constraint(16, new Insets(1, 5, 1, 5)));
		statusLabel = new JLabel();
		statusBar.add(statusLabel, new JXStatusBar.Constraint(ResizeBehavior.FILL));
		
		add(statusBar, BorderLayout.SOUTH);
		
		SwingUtilities.invokeLater( () -> updateBreadcrumbButtons() );
	}
	
	public void updateBreadcrumbButtons() {
		JButton endBtn = nextButton;
		
		// remove all buttons from breadcrumb
		if(nextButton != null)
			breadCrumbViewer.remove(nextButton);
		if(btnStop != null)
			breadCrumbViewer.remove(btnStop);
		if(btnOpenProject != null)
			breadCrumbViewer.remove(btnOpenProject);
		if(btnRunAgain != null)
			breadCrumbViewer.remove(btnRunAgain);
	
		if(breadCrumbViewer.getBreadcrumb().getCurrentState() == importStep && btnStop != null) {
			if(running) {
				btnStop.setText("Stop");
				btnStop.setBackground(Color.red);
				btnStop.setForeground(Color.white);
				
				breadCrumbViewer.add(btnStop);
				setBounds(btnStop);
				endBtn = btnStop;
			} else {
				btnStop.setText("Close window");
				btnStop.setBackground(btnNext.getBackground());
				btnStop.setForeground(Color.black);
				
				breadCrumbViewer.add(btnOpenProject);
				setBounds(btnOpenProject);
				
				breadCrumbViewer.add(btnRunAgain);
				setBounds(btnRunAgain);
				btnRunAgain.setBounds(btnRunAgain.getBounds().x + btnOpenProject.getPreferredSize().width - btnOpenProject.getInsets().left/2-1, 
						btnRunAgain.getBounds().y, btnRunAgain.getBounds().width, btnRunAgain.getBounds().height);
				
				breadCrumbViewer.add(btnStop);
				setBounds(btnStop);
				btnStop.setBounds(btnStop.getBounds().x + (btnRunAgain.getPreferredSize().width - btnRunAgain.getInsets().left/2-1)
							+ (btnOpenProject.getPreferredSize().width - btnOpenProject.getInsets().left/2-1), 
						btnStop.getBounds().y, btnStop.getBounds().width, btnStop.getBounds().height);
				endBtn = btnStop;
			}
		} else {
			breadCrumbViewer.add(nextButton);
			setBounds(nextButton);
			endBtn = nextButton;
		}

		if(getCurrentStep() != importStep)
			getRootPane().setDefaultButton(endBtn);
		else
			getRootPane().setDefaultButton(null);

		breadCrumbViewer.revalidate();
		breadCrumbViewer.scrollRectToVisible(endBtn.getBounds());
	}
	
	private void setupFolderStep() {
		folderStep = new WizardStep();
		folderStep.setLayout(new BorderLayout());
		
		DialogHeader header = new DialogHeader(DIALOG_TITLE, DIALOG_MESAGE);
		folderStep.add(header, BorderLayout.NORTH);

		importFolderButton = new FileHistorySelectionButton(IMPORTFOLDER_HISTORY_PROP);
		importFolderButton.setSelectFolder(true);
		importFolderButton.setSelectFile(false);
		importFolderButton.setTopLabelText("Import folder (click to select)");
		importFolderButton.setBorder(BorderFactory.createTitledBorder("Import Folder"));
		importFolderButton.addPropertyChangeListener("selection", (e) -> {
			if(importFolderButton.getSelection() != null) {
				busyLabel.setBusy(true);
				statusLabel.setText("Scanning folder...");
				PhonWorker.getInstance().invokeLater( () -> {
					final TristateCheckBoxTreeNode treeNode = scanFolder(importFolderButton.getSelection());
					treeNode.setCheckingState(TristateCheckBoxState.CHECKED);

					SwingUtilities.invokeLater( () -> {
						TristateCheckBoxTreeModel treeModel = new TristateCheckBoxTreeModel(treeNode);
						fileSelectionTree.setRootVisible(true);
						fileSelectionTree.setModel(treeModel);

						outputFolderButton.setSelection(determineOutputFolder());

						busyLabel.setBusy(false);
						statusLabel.setText("");
					});
				});
			} else {
				fileSelectionTree.setModel(new TristateCheckBoxTreeModel(new TristateCheckBoxTreeNode()));
				fileSelectionTree.setRootVisible(false);
			}
		} );

		projectButton = new ProjectSelectionButton();
		projectButton.setBorder(BorderFactory.createTitledBorder("Project"));
		projectButton.setVisible(getProject() != null);
		projectButton.addPropertyChangeListener("selection", (e) -> outputFolderButton.setSelection(projectButton.getSelection()) );

		outputFolderButton = new FileHistorySelectionButton(OUTPUTFOLDER_HISTORY_PROP);
		outputFolderButton.setSelectFolder(true);
		outputFolderButton.setSelectFile(false);
		outputFolderButton.setTopLabelText("Output folder (root of export)");
		outputFolderButton.setBorder(BorderFactory.createTitledBorder("Output Folder"));
		outputFolderButton.setVisible(false);

		// setup project location if we have an open project
		if(getProject() != null) {
			projectButton.setSelection(new File(getProject().getLocation()));
		}

		GridBagConstraints gbc = new GridBagConstraints();

		ActionListener listener = (e) -> {
			outputFolderButton.setSelection(determineOutputFolder());
			projectButton.setVisible(storeInProjectButton.isSelected());
		};
		useWorkspaceButton.addActionListener(listener);
		storeInProjectButton.addActionListener(listener);

		// radio buttons
		ButtonGroup bg = new ButtonGroup();
		bg.add(useWorkspaceButton);
		bg.add(storeInProjectButton);
		useWorkspaceButton.setSelected(getProject() == null);
		storeInProjectButton.setSelected(getProject() != null);

		JPanel radioBoxPanel = new JPanel(new VerticalLayout());
		radioBoxPanel.add(useWorkspaceButton);
		radioBoxPanel.add(storeInProjectButton);
		radioBoxPanel.setBorder(BorderFactory.createTitledBorder("Import Action"));

		JPanel optionalsPanel = new JPanel(new VerticalLayout());
		optionalsPanel.add(projectButton);
		optionalsPanel.add(outputFolderButton);

		JPanel topPanel = new JPanel(new VerticalLayout());
		topPanel.add(importFolderButton);
		topPanel.add(radioBoxPanel);
		topPanel.add(optionalsPanel);

		fileSelectionTree = new TristateCheckBoxTree();
		fileSelectionTree.setRootVisible(false);
		fileSelectionTree.setCellRenderer(new CellRenderer());
		fileSelectionTree.setCellEditor(new TristateCheckBoxTreeCellEditor(fileSelectionTree, new CellRenderer()));
		
		JScrollPane scroller = new JScrollPane(fileSelectionTree);
		scroller.setBorder(BorderFactory.createTitledBorder("File selection"));
		
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(topPanel, BorderLayout.NORTH);
		centerPanel.add(scroller, BorderLayout.CENTER);
		
		folderStep.setTitle("Select files");
		folderStep.add(centerPanel, BorderLayout.CENTER);
	}

	private File determineOutputFolder() {
		File outputFolder = null;
		if(useWorkspaceButton.isSelected() && importFolderButton.getSelection() != null) {
			outputFolder = new File(Workspace.userWorkspaceFolder(), importFolderButton.getSelection().getName());
			int idx = 1;
			while(outputFolder.exists()) {
				outputFolder = new File(Workspace.userWorkspaceFolder(), importFolderButton.getSelection().getName() + " (" + (idx++) + ")");
			}
//			outputFolderButton.setSelection(outputFolder);
		} else if(storeInProjectButton.isSelected()) {
			outputFolder = projectButton.getSelection();
		} else {
			outputFolder = outputFolderButton.getSelection();
		}
		return outputFolder;
	}
	
	private void setupOptionsStep() {
		optionsStep = new WizardStep();
		optionsStep.setTitle("Options");
		optionsStep.setLayout(new BorderLayout());
		
		settingsPanel = new Xml2PhonSettingsPanel();
		
		DialogHeader header = new DialogHeader(DIALOG_TITLE, DIALOG_MESAGE);
		optionsStep.add(header, BorderLayout.NORTH);
		optionsStep.add(settingsPanel, BorderLayout.CENTER);
	}
	
	private void createImportStep() {
		importStep = new WizardStep();
		importStep.setTitle("Import");
		
		importStep.setLayout(new BorderLayout());
		
		DialogHeader header = new DialogHeader(DIALOG_TITLE, DIALOG_MESAGE);
		importStep.add(header, BorderLayout.NORTH);
		
		PhonTalkTaskTableModel taskTableModel = new PhonTalkTaskTableModel();
		taskTable = new JXTable(taskTableModel);
		taskTable.setDefaultRenderer(TaskStatus.class, statusCellRenderer);
		taskTable.addMouseListener(new MouseInputAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				if(e.isPopupTrigger()) {
					showTaskTableMenu(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if(e.isPopupTrigger()) {
					showTaskTableMenu(e);
				}
			}

		});
		
		JScrollPane taskScroller = new JScrollPane(taskTable);
		Dimension prefSize = taskScroller.getPreferredSize();
		prefSize.width = 300;
		taskScroller.setPreferredSize(prefSize);
		taskScroller.setMinimumSize(new Dimension(250, prefSize.height));
		
		PhonTalkMessageTableModel messageTableModel = new PhonTalkMessageTableModel();
		messageTable = new JXTable(messageTableModel);
		JScrollPane messageScroller = new JScrollPane(messageTable);
		
		bufferPanel = new BufferPanel("PhonTalk");
		bufferPanel.getLogBuffer().setEditable(false);
		
		JTabbedPane tabPane = new JTabbedPane();
		tabPane.add("Log", bufferPanel);
		tabPane.add("Table", messageScroller);
		
		splitPane = new JSplitPane();
		splitPane.setLeftComponent(taskScroller);
		splitPane.setRightComponent(tabPane);
		importStep.add(splitPane, BorderLayout.CENTER);
	}
	
	private void showTaskTableMenu(MouseEvent me) {
		int row = taskTable.rowAtPoint(me.getPoint());
		if(row >= 0 && row < taskTable.getRowCount()) {
			taskTable.getSelectionModel().setSelectionInterval(row, row);
			PhonTalkTask task = ((PhonTalkTaskTableModel)taskTable.getModel()).taskForRow(row);
			if(task != null) {
				JPopupMenu menu = new JPopupMenu();

				final Consumer<File> openFile = (f) -> {
					try {
						Desktop.getDesktop().open(f);
					} catch (IOException e) {
						Toolkit.getDefaultToolkit().beep();
						LogUtil.warning(e);
					}
				};
				PhonUIAction<File> showInputFileAct = PhonUIAction.consumer(openFile, task.getInputFile());
				showInputFileAct.putValue(PhonUIAction.NAME, "Open input file");
				showInputFileAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Open input file: " + task.getInputFile().getAbsolutePath());
				menu.add(showInputFileAct);
				
				PhonUIAction<File> showOutputFileAct = PhonUIAction.consumer(openFile, task.getOutputFile());
				showOutputFileAct.putValue(PhonUIAction.NAME, "Open output file");
				showOutputFileAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Open output file: " + task.getOutputFile().getAbsolutePath());
				menu.add(showOutputFileAct).setEnabled(task.getStatus() == TaskStatus.FINISHED);
			
				menu.show(me.getComponent(), me.getX(), me.getY());
			}
		}
	}
	
	private TristateCheckBoxTreeNode scanFolder(File folder) {
		TristateCheckBoxTreeNode retVal = new TristateCheckBoxTreeNode(folder);
		retVal.setEnablePartialCheck(false);
		
		FileFilter chatFilter = new FileFilter("CHAT files", "cha");
		
		if(!folder.exists()) return retVal;
		
		List<File> fileList = new ArrayList<>(List.of(folder.listFiles()));
		fileList.sort( (f1, f2) -> { return f1.getName().compareTo(f2.getName()); } );
		for(File file:fileList) {
			if(file.isHidden()) continue;
			if(file.isDirectory()) {
				TristateCheckBoxTreeNode subtree = scanFolder(file);
				retVal.add(subtree);
			} else if(FileFilter.xmlFilter.accept(file)) {
				TalkBankSessionReader tbReader = new TalkBankSessionReader();
				try {
					if(tbReader.canRead(file)) {
						PhonTalkTreeNode fileNode = new PhonTalkTreeNode(file, PhonTalkTaskType.XML2Phon);
						fileNode.setEnablePartialCheck(false);
						retVal.add(fileNode);
					} else {
						PhonTalkTreeNode copyFile = new PhonTalkTreeNode(file, PhonTalkTaskType.Copy);
						copyFile.setEnablePartialCheck(false);
						retVal.add(copyFile);
					}
				} catch (IOException e) {
					LogUtil.severe(e);
					
					PhonTalkTreeNode copyFile = new PhonTalkTreeNode(file, PhonTalkTaskType.Copy);
					copyFile.setEnablePartialCheck(false);
					retVal.add(copyFile);
				}
			} else if(chatFilter.accept(file)) {
				CHATSessionReader reader = new CHATSessionReader();
				try {
					if(reader.canRead(file)) {
						PhonTalkTreeNode fileNode = new PhonTalkTreeNode(file, PhonTalkTaskType.CHAT2Phon);
						fileNode.setEnablePartialCheck(false);
						retVal.add(fileNode);
					} else {
						PhonTalkTreeNode copyFile = new PhonTalkTreeNode(file, PhonTalkTaskType.Copy);
						copyFile.setEnablePartialCheck(false);
						retVal.add(copyFile);
					}
				} catch (IOException e) {
					LogUtil.severe(e);
					
					PhonTalkTreeNode copyFile = new PhonTalkTreeNode(file, PhonTalkTaskType.Copy);
					copyFile.setEnablePartialCheck(false);
					retVal.add(copyFile);
				}
			} else {
				PhonTalkTreeNode copyFile = new PhonTalkTreeNode(file, PhonTalkTaskType.Copy);
				copyFile.setEnablePartialCheck(false);
				retVal.add(copyFile);
			}
		}
		
		return retVal;
	}
	
	private void setupTasks(PhonWorker worker, Project project) {
		List<TreePath> checkedPaths = fileSelectionTree.getCheckedPaths();
		for(TreePath path:checkedPaths) {
			TristateCheckBoxTreeNode node = (TristateCheckBoxTreeNode)path.getLastPathComponent();
			TristateCheckBoxTreeNode parentNode = (TristateCheckBoxTreeNode)node.getParent();
			
			if(node.isLeaf() && node instanceof PhonTalkTreeNode) {
				PhonTalkTreeNode ptNode = (PhonTalkTreeNode)node;
				File parentFile = (parentNode != null ? (File)parentNode.getUserObject() : null);
				
				File inputFile = (File)ptNode.getUserObject();

				Path inputPath = inputFile.toPath();
				Path parentPath = importFolderButton.getSelection().toPath();

				Path relativePath = parentPath.relativize(inputPath);

				boolean keepLongPath = true;
				// output folder for *this* file
				File outputFolder = outputFolderButton.getSelection();
				for(int i = 0; i < relativePath.getNameCount()-1; i++) {
					if(i == 0 || keepLongPath) {
						outputFolder = new File(outputFolder, relativePath.getName(i).toString());
					} else {
						outputFolder = new File(outputFolderButton.getSelection(), outputFolder.getName() + "_" + relativePath.getName(i));
					}
				}

//				if((ptNode.getType() == PhonTalkTaskType.CHAT2Phon ||
//						ptNode.getType() == PhonTalkTaskType.XML2Phon)
//					&& (useWorkspaceButton.isSelected() || storeInProjectButton.isSelected())) {
//					// ensure files go into a proper corpus folder
//					if(relativePath.getNameCount() == 1) {
//						// move into corpus folder with name 'Transcripts'
//						outputFolder = new File(outputFolder, "Transcripts");
//					}
//				}

				if(!outputFolder.exists())
					outputFolder.mkdirs();

				switch(ptNode.type) {
				case CHAT2Phon:
					CHAT2PhonTask chat2phonTask = new CHAT2PhonTask(inputFile, 
							new File(outputFolder, FilenameUtils.removeExtension(inputFile.getName()) + ".xml"), phonTalkListener);
					chat2phonTask.addTaskListener(taskListener);
					chat2phonTask.setName(inputFile.getName());
					numCHATFiles++;
					worker.invokeLater(chat2phonTask);
					((PhonTalkTaskTableModel)taskTable.getModel()).addTask(chat2phonTask);
					break;
					
				case XML2Phon:
					Xml2PhonTask xml2phonTask = new Xml2PhonTask(inputFile, new File(outputFolder, inputFile.getName()), phonTalkListener);
					xml2phonTask.addTaskListener(taskListener);
					xml2phonTask.setName(inputFile.getName());
					worker.invokeLater(xml2phonTask);
					numTBFiles++;
					((PhonTalkTaskTableModel)taskTable.getModel()).addTask(xml2phonTask);
					break;
					
				case Copy:
					CopyFilePhonTalkTask copyTask = new CopyFilePhonTalkTask(inputFile, new File(outputFolder, inputFile.getName()), phonTalkListener);
					copyTask.addTaskListener(taskListener);
					copyTask.setName(inputFile.getName());
					worker.invokeLater(copyTask);
					numFilesToCopy++;
					((PhonTalkTaskTableModel)taskTable.getModel()).addTask(copyTask);
					break;
					
				default:
					break;					
				}
			}
		}
		
		SwingUtilities.invokeLater( taskTable::packAll );
		
		worker.setFinalTask( () -> {
			running = false;
			SwingUtilities.invokeLater( () -> { 
				busyLabel.setBusy(false);
				statusLabel.setText(String.format("%d/%d files processed",
						numCHATFilesProcessed + numTBFilesProcessed + numFilesCopied, numCHATFiles + numTBFiles + numFilesToCopy));
				updateBreadcrumbButtons();
				
				importFinishedMs = System.currentTimeMillis();
				printEndOfImportReport();
			});
		});
		worker.setFinishWhenQueueEmpty(true);
	}
	
	private void beginImport() {
		((PhonTalkTaskTableModel)taskTable.getModel()).clear();
		((PhonTalkMessageTableModel)messageTable.getModel()).clear();
		bufferPanel.getLogBuffer().setText("");
		
		numFilesToCopy = 0;
		numCHATFiles = 0;
		numTBFiles = 0;
		numCHATFilesProcessed = 0;
		numTBFilesProcessed = 0;
		numFilesCopied = 0;
		numFilesFailed = 0;
		
		currentWorker = PhonWorker.createWorker();
		currentWorker.setName("PhonTalk");
		printBeginImportReport();
		
		// add import folder to history
		importFolderButton.saveSelectionToHistory();
		
		File outputFolder = outputFolderButton.getSelection();
		boolean inWorkspace = false;
		for(File workspaceFolder:workspaceHistory) {
			if(workspaceFolder.equals(outputFolder)) {
				inWorkspace = true;
				break;
			}
		}
		outputFolderButton.saveSelectionToHistory();
		
		((PhonTalkTaskTableModel)taskTable.getModel()).setParentFolder(importFolderButton.getSelection());
		
		try {
			// create project
			final AtomicReference<Project> projectRef = new AtomicReference<>();
			if(this.useWorkspaceButton.isSelected()) {
				projectRef.set(createProject());
			} else {
				bufferPanel.getLogBuffer().append("Opening project at " + outputFolder.getAbsolutePath() + "\n\n");
				projectRef.set((new DesktopProjectFactory()).openProject(outputFolder));
			}
			PhonWorker.getInstance().invokeLater( () -> setupTasks(currentWorker, projectRef.get()) );
		} catch (IOException | ProjectConfigurationException e) {
			showMessage(e.getLocalizedMessage());
		}

		running = true;
		canceled = false;
		busyLabel.setBusy(true);
		currentWorker.start();

		importStartedMs = System.currentTimeMillis();

		updateBreadcrumbButtons();
	}

	private Project createProject() throws IOException, ProjectConfigurationException {
		ProjectFactory projectFactory = new DesktopProjectFactory();

		File projectFolder = outputFolderButton.getSelection();
		int idx = 1;
		while(projectFolder.exists()) {
			projectFolder = new File(outputFolderButton.getSelection(), importFolderButton.getSelection().getName() + " (" + (idx++) + ")");
		}

		bufferPanel.getLogBuffer().append("Creating project at " + projectFolder.getAbsolutePath() + "\n\n");

		Project retVal = projectFactory.createProject(projectFolder);
		retVal.setName(importFolderButton.getSelection().getName());
		return retVal;
	}
	
	private void cancelImport() {
		if(running && currentWorker != null) {
			if(currentWorker.isAlive()) {
				canceled = true;
				currentWorker.shutdown();
			}
		}
	}
	
	/* Log methods */
	private void printBeginImportReport() {
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("Begin Project Import\n");
		buffer.append("----------------------------------------------\n");
		buffer.append("Input folder:\t");
		buffer.append(importFolderButton.getSelection().getAbsolutePath());
		buffer.append('\n');
		buffer.append("Output folder:\t");
		buffer.append(outputFolderButton.getSelection().getAbsolutePath());
		buffer.append('\n');
//		buffer.append("Project name:\t");
//		buffer.append(importFolderButton.getSelection().getName());
//		buffer.append('\n');
		
		buffer.append('\n');
		
		bufferPanel.getLogBuffer().append(buffer.toString());
	}
	
	private void printEndOfImportReport() {
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("----------------------------------------------\n");
		if(canceled)
			buffer.append("Import canceled:\t\t\t");
		else
			buffer.append("Import finished:\t\t\t");
		buffer.append(String.format("%d/%d files processed\n",
				numCHATFilesProcessed + numTBFilesProcessed + numFilesCopied, numCHATFiles + numTBFiles + numFilesToCopy));
		if(numFilesToCopy > 0)
			buffer.append(String.format("Files copied:\t\t\t\t%d/%d\n", numFilesCopied, numFilesToCopy));
		if(numCHATFiles > 0)
			buffer.append(String.format("CHAT files processed:\t\t%d/%d\n", numCHATFilesProcessed, numCHATFiles));
		if(numTBFiles > 0)
			buffer.append(String.format("TalkBank files processed:\t%d/%d\n", numTBFilesProcessed, numTBFiles));
		if(numFilesFailed > 0)
			buffer.append(String.format("Failed to process %d files\n", numFilesFailed));
		
		buffer.append(String.format("Elapsed time:\t\t\t\t%s\n", MediaTimeFormatter.timeToMinutesAndSeconds(importFinishedMs-importStartedMs)));
		
		bufferPanel.getLogBuffer().append(buffer.toString());
	}
	
	@Override
	protected void next() {
		if(getCurrentStep() == folderStep) {
			if(importFolderButton.getSelection() == null) {
				showMessage("Please select folder for import");
				return;
			}
			
			if(outputFolderButton.getSelection() == null) {
				showMessage("Please select output folder");
				return;
			} else {
				File outputFolder = outputFolderButton.getSelection();
				
//				if(!outputFolder.exists()) {
//					outputFolder.mkdirs();
//				}
				
				if(outputFolder.exists() && !outputFolder.isDirectory()) {
					showMessage("Selected output path is not a folder");
					return;
				} 
				
				if(outputFolder.exists() && !outputFolder.canWrite()) {
					showMessage("Cannot write to selected output folder");
					return;
				}
				
			}

			if(fileSelectionTree.getCheckedPaths().size() == 0) {
				showMessage("Please select files for import");
				return;
			}
		}
		
		super.next();
	}
	
	@Override
	public void gotoStep(int stepIndex) {
		if(running) {
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		// goto step
		super.gotoStep(stepIndex);
		
		if(importStep == getCurrentStep() && currentWorker == null) {
			beginImport();
		}
	}
	
	@Override
	public void close() {
		if(running) {
			int selected = showMessageDialog(DIALOG_TITLE, "Cancel import?", MessageDialogProperties.yesNoOptions);
			if(selected == 0) {
				cancelImport();
			} else {
				return;
			}
		} else {
			super.close();
		}
	}
	
	private void showMessage(String msg) {
		showMessageDialog(getCurrentStep().getTitle(), msg, MessageDialogProperties.okOptions);
	}
	
	private final PhonTalkListener phonTalkListener = new PhonTalkListener() {
		
		@Override
		public void message(final PhonTalkMessage msg) {
			final Runnable onEDT = new Runnable() {
				public void run() {
					final PhonTalkMessageTableModel reportTableModel =
							(PhonTalkMessageTableModel)messageTable.getModel();
					reportTableModel.addMessage(msg);
					
					String msgText = String.format("\t%s\n", msg.getMessage());
					bufferPanel.getLogBuffer().append(msgText);
				}
			};
			SwingUtilities.invokeLater(onEDT);
		}
		
	};
	
	private final PhonTaskListener taskListener = new PhonTaskListener() {
		
		@Override
		public void statusChanged(PhonTask task, TaskStatus oldStatus,
				TaskStatus newStatus) {
			final File inputFolder = importFolderButton.getSelection();
			final File inputFile = ((PhonTalkTask)task).getInputFile();
			
			final PhonTalkTask ptTask = (PhonTalkTask)task;
			final TaskStatus status = newStatus;
			final PhonTalkTaskTableModel taskTableModel = 
					(PhonTalkTaskTableModel)taskTable.getModel();
			final Runnable onEDT = new Runnable() {
				public void run() {
					String filename = inputFile.getAbsolutePath();
					if(inputFile.getAbsolutePath().startsWith(inputFolder.getAbsolutePath())) {
						filename = "\u2026" + File.separator + inputFolder.toPath().relativize(inputFile.toPath()).toString();
					}
					
					String outputFilename = ptTask.getOutputFile().getAbsolutePath();
//					if(outputFilename.startsWith(outputFolderField.getSelectedFile().getAbsolutePath())) {
//						outputFilename = "\u2026" + File.separator + outputFolderField.getSelectedFile().toPath().relativize(ptTask.getOutputFile().toPath()).toString();
//					}
					
					int taskRow = taskTableModel.rowForTask(ptTask);
					if(taskRow >= 0) {
						taskTableModel.fireTableCellUpdated(taskRow, PhonTalkTaskTableModel.Columns.STATUS.ordinal());
						// scroll to current task if no selection
						if(taskTable.getSelectedRow() < 0)
							taskTable.scrollRowToVisible(taskRow);
					}
					if(status == TaskStatus.RUNNING) {
						statusLabel.setText(filename + " (" + ptTask.getProcessName() + ")");
						
						String msgText = String.format("(%s) %s -> %s\n", 
								ptTask.getProcessName(), filename, outputFilename);
						bufferPanel.getLogBuffer().append(msgText);
					} else {
						statusLabel.setText("");
						if(status == TaskStatus.FINISHED) {
							if(task instanceof CopyFilePhonTalkTask) {
								numFilesCopied++;
							} else if (task instanceof CHAT2PhonTask) {
								numCHATFilesProcessed++;
							} else if (task instanceof Xml2PhonTask) {
								numTBFilesProcessed++;
							}
						} else {
							if(task.getException() != null) {
								bufferPanel.getLogBuffer().append("\t" + task.getException().getLocalizedMessage() + "\n");
							}
							numFilesFailed++;
						}
						bufferPanel.getLogBuffer().append("\n");
					}
				}
			};
			SwingUtilities.invokeLater(onEDT);
		}
		
		@Override
		public void propertyChanged(PhonTask task, String property,
				Object oldValue, Object newValue) {
			
		}
	};
	
	private enum PhonTalkTaskType {
		CHAT2Phon,
		XML2Phon,
		Copy;
	}
	
	private class PhonTalkTreeNode extends TristateCheckBoxTreeNode {
		
		private PhonTalkTaskType type;
		
		public PhonTalkTreeNode(File inputFile, PhonTalkTaskType type) {
			super(inputFile);
			this.type = type;
		}
		
		public PhonTalkTaskType getType() {
			return this.type;
		}
		
	}
	
	private class CellRenderer extends TristateCheckBoxTreeCellRenderer {
	
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {
			TristateCheckBoxTreeCellRenderer.TristateCheckBoxTreeNodePanel retVal = 
					(TristateCheckBoxTreeCellRenderer.TristateCheckBoxTreeNodePanel)super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			
			if(value instanceof TristateCheckBoxTreeNode) {
				TristateCheckBoxTreeNode node = (TristateCheckBoxTreeNode) value;
				TristateCheckBoxTreeNode parent = (TristateCheckBoxTreeNode)node.getParent();
				
				if(node.getUserObject() instanceof File) {
					File parentFolder = ( parent != null ? (File)parent.getUserObject() : null);
					File childFile = (File)node.getUserObject();
					
					Path folderPath = (parentFolder != null ? parentFolder.toPath() : null);
					Path childPath = childFile.toPath();
					
					if(folderPath != null) {
						Path relativePath = folderPath.relativize(childPath);
						retVal.getLabel().setText(relativePath.toString());
						
						if(node instanceof PhonTalkTreeNode) {
							retVal.getLabel().setText(retVal.getLabel().getText() + 
									" (" + ((PhonTalkTreeNode)node).getType() + ")");
						}
					}
					
					ImageIcon fileIcn = 
							IconManager.getInstance().getSystemIconForPath(childFile.getAbsolutePath(), IconSize.SMALL);
					if(fileIcn != null) {
						retVal.getLabel().setIcon(fileIcn);
					}
				}
			}
			
			return retVal;
		}
		
	}
	
	private final DefaultTableCellRenderer statusCellRenderer = new DefaultTableCellRenderer() {
		
		ImageIcon waitingIcon = 
			IconManager.getInstance().getIcon("actions/free_icon", IconSize.SMALL);
		
		ImageIcon runningIcon = 
			IconManager.getInstance().getIcon("actions/greenled", IconSize.SMALL);
		
		ImageIcon errorIcon =
			IconManager.getInstance().getIcon("status/dialog-error", IconSize.SMALL);
		
		ImageIcon finishedIcon =
			IconManager.getInstance().getIcon("actions/ok", IconSize.SMALL);
		
		ImageIcon terminatedIcon =
			IconManager.getInstance().getIcon("status/dialog-warning", IconSize.SMALL);

		/* (non-Javadoc)
		 * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			JLabel retVal = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
					row, column);
			
			TaskStatus status = (TaskStatus)value;
			
			if(status == TaskStatus.WAITING) {
				retVal.setIcon(waitingIcon);
			} else if(status == TaskStatus.RUNNING) {
				retVal.setIcon(runningIcon);
			} else if(status == TaskStatus.ERROR) {
				retVal.setIcon(errorIcon);
			} else if(status == TaskStatus.TERMINATED) {
				retVal.setIcon(terminatedIcon);
			} else if(status == TaskStatus.FINISHED) {
				retVal.setIcon(finishedIcon);
			}
			
			retVal.setText(WordUtils.capitalize(retVal.getText().toLowerCase()));
			
			return retVal;
		}
		
	};
	
}
