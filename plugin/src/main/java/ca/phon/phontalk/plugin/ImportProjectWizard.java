package ca.phon.phontalk.plugin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.TreePath;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.WordUtils;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.JXStatusBar.Constraint.ResizeBehavior;
import org.jdesktop.swingx.JXTable;

import com.sun.jna.platform.win32.WinUser.COPYDATASTRUCT;

import ca.phon.app.log.BufferPanel;
import ca.phon.app.log.LogUtil;
import ca.phon.app.modules.EntryPointArgs;
import ca.phon.app.project.DesktopProjectFactory;
import ca.phon.app.project.OpenProjectEP;
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
import ca.phon.ui.DropDownButton;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.decorations.DialogHeader;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.jbreadcrumb.BreadcrumbButton;
import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.ui.nativedialogs.MessageDialogProperties;
import ca.phon.ui.text.FileSelectionField;
import ca.phon.ui.text.PromptedTextField;
import ca.phon.ui.text.FileSelectionField.SelectionMode;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxState;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxTree;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxTreeCellEditor;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxTreeCellRenderer;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxTreeModel;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxTreeNode;
import ca.phon.ui.wizard.BreadcrumbWizardFrame;
import ca.phon.ui.wizard.WizardStep;
import ca.phon.util.MsFormatter;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;
import ca.phon.worker.PhonTask;
import ca.phon.worker.PhonTaskListener;
import ca.phon.worker.PhonWorker;
import ca.phon.worker.PhonWorkerGroup;
import ca.phon.worker.PhonTask.TaskStatus;

public class ImportProjectWizard extends BreadcrumbWizardFrame {
	
	public final static String DIALOG_TITLE = "Import Project";
	public final static String DIALOG_MESAGE = "Create a new Phon project from a folder of CHAT (.cha) or TalkBank (.xml) files";
	
	/* Step 1 */
	private WizardStep folderStep;
	
	private final static int MAX_FOLDERS = 5;
	
	private final WorkspaceHistory workspaceHistory = new WorkspaceHistory();
	
	private final static String IMPORTFOLDER_HISTORY_PROP = ImportProjectWizard.class.getName() + ".importFolderHistory";
	private FolderHistory importFolderHistory;
	private FileSelectionField importFolderField;
	private DropDownButton importFolderButton;

	private final static String OUTPUTFOLDER_HISTORY_PROP = ImportProjectWizard.class.getName() + ".outputFolderHistory";
	private FolderHistory outputFolderHistory;
	private FileSelectionField outputFolderField;
	private DropDownButton browseButton;
	
	private PromptedTextField projectNameField;
	
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
	
	public ImportProjectWizard() {
		super(DIALOG_TITLE);
		setWindowName(DIALOG_TITLE);
		
		init();
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
			File projectFolder = new File(outputFolderField.getSelectedFile(), projectNameField.getText());
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
		
		importFolderHistory = new FolderHistory(IMPORTFOLDER_HISTORY_PROP, MAX_FOLDERS);
		importFolderField = new FileSelectionField();
		importFolderField.setMode(SelectionMode.FOLDERS);
		importFolderField.addPropertyChangeListener(FileSelectionField.FILE_PROP, e -> {
			if(importFolderField.getSelectedFile() != null) {
				busyLabel.setBusy(true);
				statusLabel.setText("Scanning folder...");
				PhonWorker.getInstance().invokeLater( () -> {
					final TristateCheckBoxTreeNode treeNode = scanFolder(importFolderField.getSelectedFile());
					treeNode.setCheckingState(TristateCheckBoxState.CHECKED);
					
					SwingUtilities.invokeLater( () -> {
						TristateCheckBoxTreeModel treeModel = new TristateCheckBoxTreeModel(treeNode);
						fileSelectionTree.setRootVisible(true);
						fileSelectionTree.setModel(treeModel);
						
						setupProjectName();
						busyLabel.setBusy(false);
						statusLabel.setText("");
					});
				});
			} else {
				fileSelectionTree.setModel(new TristateCheckBoxTreeModel(new TristateCheckBoxTreeNode()));
				fileSelectionTree.setRootVisible(false);
			}
		});
		
		JPopupMenu folderMenu = new JPopupMenu();
		folderMenu.addPopupMenuListener(new PopupMenuListener() {
			
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				folderMenu.removeAll();
				
				int idx = 0;
				for(File folder:importFolderHistory) {
					PhonUIAction folderAct = new PhonUIAction(importFolderField, "setFile", folder);
					folderAct.putValue(PhonUIAction.NAME, folder.getAbsolutePath());
					folderMenu.add(folderAct);
					++idx;
				}
				
				if(idx == 0 ) {
					folderMenu.setVisible(false);
					importFolderField.onBrowse();
					return;
				}
				folderMenu.addSeparator();
				PhonUIAction clearAct = new PhonUIAction(importFolderHistory, "clearHistory");
				clearAct.putValue(PhonUIAction.NAME, "Clear history");
				clearAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Clear import folder history");
				folderMenu.add(clearAct);
				
				folderMenu.addSeparator();
				PhonUIAction browseAct = new PhonUIAction(importFolderField, "onBrowse");
				browseAct.putValue(PhonUIAction.NAME, "Select folder...");
				browseAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Select import folder");
				folderMenu.add(browseAct);
				
			}
			
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
			
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
			}
			
		});
		
		PhonUIAction folderAct = new PhonUIAction(importFolderField, "onBrowse");
		folderAct.putValue(PhonUIAction.SMALL_ICON, importFolderField.getBrowseButton().getIcon());
		folderAct.putValue(DropDownButton.ARROW_ICON_GAP, 0);
		folderAct.putValue(DropDownButton.ARROW_ICON_POSITION, SwingConstants.BOTTOM);
		folderAct.putValue(DropDownButton.BUTTON_POPUP, folderMenu);
		
		importFolderButton = new DropDownButton(folderAct);
		importFolderButton.setOnlyPopup(true);
		var importFolderConstraints = ((GridBagLayout)importFolderField.getLayout()).getConstraints(importFolderField.getBrowseButton());
		importFolderField.remove(importFolderField.getBrowseButton());
		importFolderField.add(importFolderButton, importFolderConstraints);
		
		outputFolderHistory = new FolderHistory(OUTPUTFOLDER_HISTORY_PROP, MAX_FOLDERS);
		outputFolderField = new FileSelectionField();
		outputFolderField.setMode(SelectionMode.FOLDERS);
		if(outputFolderHistory.iterator().hasNext())
			outputFolderField.setFile(outputFolderHistory.iterator().next());
		else
			outputFolderField.setFile(Workspace.userWorkspaceFolder());
		
		JPopupMenu outputFolderMenu = new JPopupMenu();
		outputFolderMenu.addPopupMenuListener(new PopupMenuListener() {
			
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				outputFolderMenu.removeAll();
				
				int idx = 0;
				for(File folder:outputFolderHistory) {
					PhonUIAction folderAct = new PhonUIAction(outputFolderField, "setFile", folder);
					folderAct.putValue(PhonUIAction.NAME, folder.getAbsolutePath());
					outputFolderMenu.add(folderAct);
					++idx;
				}
				if(idx > 0) {
					outputFolderMenu.addSeparator();
					PhonUIAction clearAct = new PhonUIAction(outputFolderHistory, "clearHistory");
					clearAct.putValue(PhonUIAction.NAME, "Clear history");
					clearAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Clear output folder history");
					outputFolderMenu.add(clearAct);
				}
				
				if(workspaceHistory.iterator().hasNext()) {
					if(idx > 0) {
						outputFolderMenu.addSeparator();
					}
					
					JMenu workspaceMenu = new JMenu("Workspace history");
					outputFolderMenu.add(workspaceMenu);
					
					for(File workspaceFolder:workspaceHistory) {
						PhonUIAction workspaceAct = new PhonUIAction(outputFolderField, "setFile", workspaceFolder);
						workspaceAct.putValue(PhonUIAction.NAME, workspaceFolder.getAbsolutePath());
						workspaceMenu.add(workspaceAct);
						++idx;
					}
				}
				
				outputFolderMenu.addSeparator();
				PhonUIAction browseAct = new PhonUIAction(outputFolderField, "onBrowse");
				browseAct.putValue(PhonUIAction.NAME, "Select folder...");
				browseAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Select output folder");
				outputFolderMenu.add(browseAct);
			}
			
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
			
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
			}
		});
	
		
		PhonUIAction dropDownAct = new PhonUIAction(outputFolderField, "onBrowse");
		dropDownAct.putValue(DropDownButton.BUTTON_POPUP, outputFolderMenu);
		dropDownAct.putValue(DropDownButton.ARROW_ICON_GAP, 0);
		dropDownAct.putValue(DropDownButton.ARROW_ICON_POSITION, SwingConstants.BOTTOM);
		dropDownAct.putValue(PhonUIAction.SMALL_ICON, outputFolderField.getBrowseButton().getIcon());
		
		browseButton = new DropDownButton(dropDownAct);
		browseButton.setOnlyPopup(true);
		var constraints = ((GridBagLayout)outputFolderField.getLayout()).getConstraints(outputFolderField.getBrowseButton());
		outputFolderField.remove(outputFolderField.getBrowseButton());
		outputFolderField.add(browseButton, constraints);
		
		projectNameField = new PromptedTextField("Project name");
		
		JPanel folderSelectionPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		
		folderSelectionPanel.add(new JLabel("Import folder:"), gbc);
		++gbc.gridy;
		folderSelectionPanel.add(new JLabel("Output folder:"), gbc);
		++gbc.gridy;
		folderSelectionPanel.add(new JLabel("Project name:"), gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		
		folderSelectionPanel.add(importFolderField, gbc);
		++gbc.gridy;
		folderSelectionPanel.add(outputFolderField, gbc);
		++gbc.gridy;
		folderSelectionPanel.add(projectNameField, gbc);
		folderSelectionPanel.setBorder(BorderFactory.createTitledBorder("Folder setup"));
		
		fileSelectionTree = new TristateCheckBoxTree();
		fileSelectionTree.setRootVisible(false);
		fileSelectionTree.setCellRenderer(new CellRenderer());
		fileSelectionTree.setCellEditor(new TristateCheckBoxTreeCellEditor(fileSelectionTree, new CellRenderer()));
		
		JScrollPane scroller = new JScrollPane(fileSelectionTree);
		scroller.setBorder(BorderFactory.createTitledBorder("File selection"));
		
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(folderSelectionPanel, BorderLayout.NORTH);
		centerPanel.add(scroller, BorderLayout.CENTER);
		
		folderStep.setTitle("Select files");
		folderStep.add(centerPanel, BorderLayout.CENTER);
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
	
	public void clearImportFolderHistory() {
		FolderHistory history = new FolderHistory(IMPORTFOLDER_HISTORY_PROP);
		history.clearHistory();
		history.saveHistory();
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
				File outputFolder = (parentFile == null || parentNode.getParent() == null ? new File(project.getLocation())
						: new File(project.getLocation(), parentFile.getName()));
				if(!outputFolder.exists()) {
					outputFolder.mkdirs();
				}
				
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
	
	private void setupProjectName() {
		File outputFolder = outputFolderField.getSelectedFile();
		if(outputFolder == null || !outputFolder.isDirectory()) return;
		
		File importFolder = importFolderField.getSelectedFile();
		if(importFolder == null || !importFolder.isDirectory()) return;
		
		int idx = 0;
		String projectName = importFolder.getName().trim();
		File projectFile = new File(outputFolder, projectName);
		while(projectFile.exists()) {
			projectName = String.format("%s (%d)", importFolder.getName().trim(), ++idx);
			projectFile = new File(outputFolder, projectName);
		}
		
		projectNameField.setText(projectName);
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
		importFolderHistory.addToHistory(importFolderField.getSelectedFile());
		importFolderHistory.saveHistory();
		
		File outputFolder = outputFolderField.getSelectedFile();
		boolean inWorkspace = false;
		for(File workspaceFolder:workspaceHistory) {
			if(workspaceFolder.equals(outputFolder)) {
				inWorkspace = true;
				break;
			}
		}
		if(!inWorkspace) {
			outputFolderHistory.addToHistory(outputFolderField.getSelectedFile());
			outputFolderHistory.saveHistory();
		}
		
		((PhonTalkTaskTableModel)taskTable.getModel()).setParentFolder(importFolderField.getSelectedFile());
		
		try {
			// create project
			final Project project = createProject();
			PhonWorker.getInstance().invokeLater( () -> setupTasks(currentWorker, project) );
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
		File projectFolder = new File(outputFolderField.getSelectedFile(), projectNameField.getText());
		
		if(!projectFolder.exists()) {
			bufferPanel.getLogBuffer().append("Creating project at " + projectFolder.getAbsolutePath() + "\n\n");
		} else {
			bufferPanel.getLogBuffer().append("Opening project at " + projectFolder.getAbsolutePath() + "\n\n");
		}
	
		Project retVal = projectFolder.exists() ? projectFactory.openProject(projectFolder) : projectFactory.createProject(projectFolder);
		retVal.setName(projectNameField.getText());
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
		buffer.append(importFolderField.getSelectedFile().getAbsolutePath());
		buffer.append('\n');
		buffer.append("Output folder:\t");
		buffer.append(outputFolderField.getSelectedFile().getAbsolutePath());
		buffer.append('\n');
		buffer.append("Project name:\t");
		buffer.append(projectNameField.getText());
		buffer.append('\n');
		
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
		
		buffer.append(String.format("Elapsed time:\t\t\t\t%s\n", MsFormatter.msToDisplayString(importFinishedMs-importStartedMs)));
		
		bufferPanel.getLogBuffer().append(buffer.toString());
	}
	
	@Override
	protected void next() {
		if(getCurrentStep() == folderStep) {
			if(importFolderField.getSelectedFile() == null) {
				showMessage("Please select folder for import");
				return;
			}
			
			if(outputFolderField.getSelectedFile() == null) {
				showMessage("Please select output folder");
				return;
			} else {
				File outputFolder = outputFolderField.getSelectedFile();
				
				if(!outputFolder.exists()) {
					outputFolder.mkdirs();
				}
				
				if(outputFolder.exists() && !outputFolder.isDirectory()) {
					showMessage("Selected output path is not a folder");
					return;
				} 
				
				if(outputFolder.exists() && !outputFolder.canWrite()) {
					showMessage("Cannot write to selected output folder");
					return;
				}
				
			}
			
			if(projectNameField.getText().trim().length() == 0) {
				showMessage("Please enter project name");
				return;
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
			final File inputFolder = importFolderField.getSelectedFile();
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
					if(outputFilename.startsWith(outputFolderField.getSelectedFile().getAbsolutePath())) {
						outputFilename = "\u2026" + File.separator + outputFolderField.getSelectedFile().toPath().relativize(ptTask.getOutputFile().toPath()).toString();
					}
					
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
						bufferPanel.getLogBuffer().append("\n");
						if(status == TaskStatus.FINISHED) {
							if(task instanceof CopyFilePhonTalkTask) {
								numFilesCopied++;
							} else if (task instanceof CHAT2PhonTask) {
								numCHATFilesProcessed++;
							} else if (task instanceof Xml2PhonTask) {
								numTBFilesProcessed++;
							}
						} else
							numFilesFailed++;
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
