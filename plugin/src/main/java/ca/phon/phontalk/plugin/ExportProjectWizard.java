package ca.phon.phontalk.plugin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
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
import org.jdesktop.swingx.VerticalLayout;

import ca.phon.app.log.BufferPanel;
import ca.phon.app.log.LogUtil;
import ca.phon.app.project.DesktopProjectFactory;
import ca.phon.app.workspace.Workspace;
import ca.phon.phontalk.Phon2CHATTask;
import ca.phon.phontalk.Phon2XmlTask;
import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.PhonTalkMessage;
import ca.phon.phontalk.PhonTalkTask;
import ca.phon.project.Project;
import ca.phon.project.exceptions.ProjectConfigurationException;
import ca.phon.session.io.SessionIO;
import ca.phon.session.io.SessionInputFactory;
import ca.phon.session.io.SessionReader;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.DropDownButton;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.decorations.DialogHeader;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.jbreadcrumb.BreadcrumbButton;
import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.ui.nativedialogs.MessageDialogProperties;
import ca.phon.ui.text.FileSelectionField;
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
import ca.phon.worker.PhonTask.TaskStatus;
import ca.phon.worker.PhonTaskListener;
import ca.phon.worker.PhonWorker;

public class ExportProjectWizard extends BreadcrumbWizardFrame {
	
	final public static String DIALOG_TITLE = "Export Project";
	final public static String DIALOG_MESAGE = "Export a Phon project to a folder of CHAT (.cha) or TalkBank (.xml) files";
	
	/* Step 1 */
	private WizardStep folderStep;
	
	private final static int MAX_FOLDERS = 5;
	
	private final Workspace workspace = Workspace.userWorkspace();
	
	private final static String PROJECTFOLDER_HISTORY_PROP = ExportProjectWizard.class.getName() + ".projectFolderHistory";
	private FolderHistory projectFolderHistory;
	private FileSelectionField projectFolderField;
	private DropDownButton projectBrowseButton;
	
	private final static String EXPORTFOLDER_HISTORY_PROP = ExportProjectWizard.class.getName() + ".exportFolderHistory";
	private FolderHistory exportFolderHistory;
	private FileSelectionField exportFolderField;
	private DropDownButton exportFolderButton;

	private TristateCheckBoxTree fileSelectionTree;
	
	/* Step 2 */
	private WizardStep optionsStep;
	private ButtonGroup exportTypeGroup;
	private JRadioButton exportCHATBtn;
	private JRadioButton exportTBBtn;
	
	/* Step 3 */
	private WizardStep importStep;
	private PhonWorker currentWorker;
	private JSplitPane splitPane;
	private JXTable taskTable;
	private JXTable messageTable;
	private BufferPanel bufferPanel;
	
	/*
	 * Excluded paths (relative to project folder)
	 */
	private final static List<String> EXCLUDED_PATHS = List.of( new String[] {
		".query_results", "__res", "project.xml", "backups.zip"
	});
	
	/*
	 * Excluded file/folder names (path does not matter)
	 */
	private final static List<String> EXCLUDED_FILENAMES = List.of(new String[] {
		".DS_Store", ".git", ".svn"
	});
	
	protected BreadcrumbButton btnStop;
	protected BreadcrumbButton btnRunAgain;
	protected BreadcrumbButton btnOpenFolder;
	
	private boolean running = false;
	private boolean canceled = false;
	private long exportStartedMs = 0L;
	private long exportFinishedMs = 0L;
	private JXStatusBar statusBar;
	private JXBusyLabel busyLabel;
	private JLabel statusLabel;
	
	private int numFilesToCopy = 0;
	private int numSessions = 0;
	
	private int numFilesCopied = 0;
	private int numSessionsProcessed = 0;
	private int numFilesFailed = 0;
	
	public ExportProjectWizard() {
		super(DIALOG_TITLE);
		setWindowName(DIALOG_TITLE);
		
		init();
	}
	
	private void init() {
		setupFolderStep();
		setupOptionsStep();
		createExportStep();
		
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

		btnOpenFolder = new BreadcrumbButton();
		btnOpenFolder.setFont(FontPreferences.getTitleFont().deriveFont(Font.BOLD));
		btnOpenFolder.setText("Open folder");
		btnOpenFolder.addActionListener( (e) -> {
			File outputFolder = exportFolderField.getSelectedFile();
			if(outputFolder != null && outputFolder.exists() && Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().open(outputFolder);
				} catch (IOException e1) {
					LogUtil.severe(e1);
					Toolkit.getDefaultToolkit().beep();
				}
			} else {
				Toolkit.getDefaultToolkit().beep();
			}
		});
		
		btnRunAgain = new BreadcrumbButton();
		btnRunAgain.setFont(FontPreferences.getTitleFont().deriveFont(Font.BOLD));
		btnRunAgain.setText("Run again");
		btnRunAgain.addActionListener( (e) -> {
			if(!running) {
				beginExport();
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
	
	public void setProjectLocation(String projectLocation) {
		projectFolderField.setFile(new File(projectLocation));
	}
	
	public void updateBreadcrumbButtons() {
		JButton endBtn = nextButton;
		
		// remove all buttons from breadcrumb
		if(nextButton != null)
			breadCrumbViewer.remove(nextButton);
		if(btnStop != null)
			breadCrumbViewer.remove(btnStop);
		if(btnOpenFolder != null)
			breadCrumbViewer.remove(btnOpenFolder);
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
				
				breadCrumbViewer.add(btnOpenFolder);
				setBounds(btnOpenFolder);
				
				breadCrumbViewer.add(btnRunAgain);
				setBounds(btnRunAgain);
				btnRunAgain.setBounds(btnRunAgain.getBounds().x + btnOpenFolder.getPreferredSize().width - btnOpenFolder.getInsets().left/2-1, 
						btnRunAgain.getBounds().y, btnRunAgain.getBounds().width, btnRunAgain.getBounds().height);
				
				breadCrumbViewer.add(btnStop);
				setBounds(btnStop);
				btnStop.setBounds(btnStop.getBounds().x + (btnRunAgain.getPreferredSize().width - btnRunAgain.getInsets().left/2-1)
							+ (btnOpenFolder.getPreferredSize().width - btnOpenFolder.getInsets().left/2-1), 
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
		
		projectFolderHistory = new FolderHistory(PROJECTFOLDER_HISTORY_PROP, MAX_FOLDERS);
		projectFolderField = new FileSelectionField();
		projectFolderField.setMode(SelectionMode.FOLDERS);
		projectFolderField.addPropertyChangeListener(FileSelectionField.FILE_PROP, e -> {
			if(projectFolderField.getSelectedFile() != null) {
				busyLabel.setBusy(true);
				statusLabel.setText("Scanning folder...");
				PhonWorker.getInstance().invokeLater( () -> {
					final TristateCheckBoxTreeNode treeNode = scanFolder(projectFolderField.getSelectedFile());
					treeNode.setCheckingState(TristateCheckBoxState.CHECKED);
					final TristateCheckBoxTreeModel treeModel = new TristateCheckBoxTreeModel(treeNode);
					
					SwingUtilities.invokeLater( () -> {
						fileSelectionTree.setRootVisible(true);
						fileSelectionTree.setModel(treeModel);
						
						List<TreePath> checkedPaths = fileSelectionTree.getCheckedPaths();
						for(TreePath path:checkedPaths) {
							TristateCheckBoxTreeNode lastNode = (TristateCheckBoxTreeNode)path.getLastPathComponent();
							String relativePath = 
									projectFolderField.getSelectedFile().toPath().relativize(((File)lastNode.getUserObject()).toPath()).toString();
							if(EXCLUDED_PATHS.contains(relativePath) || EXCLUDED_FILENAMES.contains(((File)lastNode.getUserObject()).getName())) {
								lastNode.setCheckingState(TristateCheckBoxState.UNCHECKED);
							}
						}
						
//						setupProjectName();
						busyLabel.setBusy(false);
						statusLabel.setText("");
					});
				});
			} else {
				fileSelectionTree.setModel(new TristateCheckBoxTreeModel(new TristateCheckBoxTreeNode()));
				fileSelectionTree.setRootVisible(false);
			}
		});
		
		JPopupMenu projectMenu = new JPopupMenu();
		projectMenu.addPopupMenuListener(new PopupMenuListener() {
			
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				projectMenu.removeAll();
				
				List<Project> workspaceProjects = workspace.getProjects();
				
				int idx = 0;
				for(File folder:projectFolderHistory) {
					PhonUIAction folderAct = new PhonUIAction(projectFolderField, "setFile", folder);
					folderAct.putValue(PhonUIAction.NAME, folder.getAbsolutePath());
					projectMenu.add(folderAct);
					++idx;
				}
				if(idx > 0) {
					projectMenu.addSeparator();
					PhonUIAction clearAct = new PhonUIAction(projectFolderHistory, "clearHistory");
					clearAct.putValue(PhonUIAction.NAME, "Clear history");
					clearAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Clear output folder history");
					projectMenu.add(clearAct);
				}
				
				Map<Project, List<CommonModuleFrame>> projectWindows = CommonModuleFrame.getProjectWindows();
				if(projectWindows.size() > 0) {
					Set<Project> openProjects = projectWindows.keySet();
					
					if(idx > 0) projectMenu.addSeparator();
					
					JMenu openProjectsMenu = new JMenu("Open projects");
					projectMenu.add(openProjectsMenu);
					
					for(Project project:openProjects) {
						PhonUIAction projectAct = new PhonUIAction(projectFolderField, "setFile", new File(project.getLocation()));
						projectAct.putValue(PhonUIAction.NAME, project.getLocation());
						openProjectsMenu.add(projectAct);
						++idx;
					}
				}
				
				if(workspaceProjects.size() > 0) {
					if(idx > 0) {
						projectMenu.addSeparator();
					}
					
					JMenu workspaceMenu = new JMenu("Workspace projects");
					projectMenu.add(workspaceMenu);
					for(Project project:workspaceProjects) {
						PhonUIAction workspaceProjectAct = new PhonUIAction(projectFolderField, "setFile", new File(project.getLocation()));
						workspaceProjectAct.putValue(PhonUIAction.NAME, project.getLocation());
						workspaceMenu.add(workspaceProjectAct);
					}
				}
				
				projectMenu.addSeparator();
				PhonUIAction browseAct = new PhonUIAction(projectFolderField, "onBrowse");
				browseAct.putValue(PhonUIAction.NAME, "Select folder...");
				browseAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Select output folder");
				projectMenu.add(browseAct);
			}
			
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
			
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
			}
		});
	
		
		PhonUIAction dropDownAct = new PhonUIAction(projectFolderField, "onBrowse");
		dropDownAct.putValue(DropDownButton.BUTTON_POPUP, projectMenu);
		dropDownAct.putValue(DropDownButton.ARROW_ICON_GAP, 0);
		dropDownAct.putValue(DropDownButton.ARROW_ICON_POSITION, SwingConstants.BOTTOM);
		dropDownAct.putValue(PhonUIAction.SMALL_ICON, projectFolderField.getBrowseButton().getIcon());
		
		projectBrowseButton = new DropDownButton(dropDownAct);
		projectBrowseButton.setOnlyPopup(true);
		var constraints = ((GridBagLayout)projectFolderField.getLayout()).getConstraints(projectFolderField.getBrowseButton());
		projectFolderField.remove(projectFolderField.getBrowseButton());
		projectFolderField.add(projectBrowseButton, constraints);
		
		exportFolderHistory = new FolderHistory(EXPORTFOLDER_HISTORY_PROP, MAX_FOLDERS);
		exportFolderField = new FileSelectionField();
		exportFolderField.setMode(SelectionMode.FOLDERS);
		Iterator<File> itr = exportFolderHistory.iterator();
		if(itr.hasNext()) {
			exportFolderField.setFile(itr.next());
		}
		
		JPopupMenu folderMenu = new JPopupMenu();
		folderMenu.addPopupMenuListener(new PopupMenuListener() {
			
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				folderMenu.removeAll();
				
				int idx = 0;
				for(File folder:exportFolderHistory) {
					PhonUIAction folderAct = new PhonUIAction(exportFolderField, "setFile", folder);
					folderAct.putValue(PhonUIAction.NAME, folder.getAbsolutePath());
					folderMenu.add(folderAct);
					++idx;
				}
				
				if(idx == 0 ) {
					folderMenu.setVisible(false);
					exportFolderField.onBrowse();
					return;
				}
				folderMenu.addSeparator();
				PhonUIAction clearAct = new PhonUIAction(exportFolderHistory, "clearHistory");
				clearAct.putValue(PhonUIAction.NAME, "Clear history");
				clearAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Clear import folder history");
				folderMenu.add(clearAct);
				
				folderMenu.addSeparator();
				PhonUIAction browseAct = new PhonUIAction(exportFolderField, "onBrowse");
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
		
		PhonUIAction folderAct = new PhonUIAction(exportFolderField, "onBrowse");
		folderAct.putValue(PhonUIAction.SMALL_ICON, exportFolderField.getBrowseButton().getIcon());
		folderAct.putValue(DropDownButton.ARROW_ICON_GAP, 0);
		folderAct.putValue(DropDownButton.ARROW_ICON_POSITION, SwingConstants.BOTTOM);
		folderAct.putValue(DropDownButton.BUTTON_POPUP, folderMenu);
		
		exportFolderButton = new DropDownButton(folderAct);
		exportFolderButton.setOnlyPopup(true);
		var importFolderConstraints = ((GridBagLayout)exportFolderField.getLayout()).getConstraints(exportFolderField.getBrowseButton());
		exportFolderField.remove(exportFolderField.getBrowseButton());
		exportFolderField.add(exportFolderButton, importFolderConstraints);
		
		JPanel folderSelectionPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		folderSelectionPanel.setBorder(BorderFactory.createTitledBorder("Folder setup"));
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		
		folderSelectionPanel.add(new JLabel("Project folder:"), gbc);
		++gbc.gridy;
		folderSelectionPanel.add(new JLabel("Output folder:"), gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		
		folderSelectionPanel.add(projectFolderField, gbc);
		++gbc.gridy;
		folderSelectionPanel.add(exportFolderField, gbc);
		
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
		
		exportTypeGroup = new ButtonGroup();
		exportCHATBtn = new JRadioButton("Export to CHAT (.cha) format");
		exportCHATBtn.setToolTipText("Export sessions to CHAT (.cha) format");
		
		exportTBBtn = new JRadioButton("Export to TalkBank (.xml) format");
		exportTBBtn.setToolTipText("Export sessions to TalkBank (.xml) format");
		
		exportTypeGroup.add(exportCHATBtn);
		exportTypeGroup.add(exportTBBtn);
		exportCHATBtn.setSelected(true);
		exportTBBtn.setSelected(false);
		
		JPanel typePanel = new JPanel(new GridLayout(2, 1));
		typePanel.setBorder(BorderFactory.createTitledBorder("Export type"));
		typePanel.add(exportCHATBtn);
		typePanel.add(exportTBBtn);
		
		JPanel centerPanel = new JPanel(new VerticalLayout());
		centerPanel.add(typePanel);
		
		DialogHeader header = new DialogHeader(DIALOG_TITLE, DIALOG_MESAGE);
		optionsStep.add(header, BorderLayout.NORTH);
		optionsStep.add(centerPanel, BorderLayout.CENTER);
	}
	
	private void createExportStep() {
		importStep = new WizardStep();
		importStep.setTitle("Export");
		
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
				
				PhonUIAction showInputFileAct = new PhonUIAction(Desktop.getDesktop(), "open", task.getInputFile());
				showInputFileAct.putValue(PhonUIAction.NAME, "Open input file");
				showInputFileAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Open input file: " + task.getInputFile().getAbsolutePath());
				menu.add(showInputFileAct);
				
				PhonUIAction showOutputFileAct = new PhonUIAction(Desktop.getDesktop(), "open", task.getOutputFile());
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
		
		SessionInputFactory inputFactory = new SessionInputFactory();
		
		List<File> fileList = new ArrayList<>(List.of(folder.listFiles()));
		fileList.sort( (f1, f2) -> { return f1.getName().compareTo(f2.getName()); } );
		for(File file:fileList) {
			if(file.isHidden()) continue;
			if(file.getName().startsWith("~")) continue;
			
			if(file.isDirectory()) {
				TristateCheckBoxTreeNode subtree = scanFolder(file);
				retVal.add(subtree);
			} else if(FileFilter.xmlFilter.accept(file)) {
				SessionIO defaultSessionIO = inputFactory.availableReaders().get(0);
				SessionReader sessionReader = inputFactory.createReader(defaultSessionIO);
				try {
					if(sessionReader.canRead(file)) {
						PhonTalkTreeNode fileNode = new PhonTalkTreeNode(file, PhonTalkTaskType.ExportSession);
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
				
				// FIX ME
				File outputFolder = (parentFile == null || parentNode.getParent() == null ? new File(exportFolderField.getSelectedFile(), project.getName())
						: new File(new File(exportFolderField.getSelectedFile(), project.getName()), parentFile.getName()));
				if(!outputFolder.exists()) {
					outputFolder.mkdirs();
				}
				
				switch(ptNode.type) {
				case ExportSession:
					PhonTalkTask exportTask = 
						(exportCHATBtn.isSelected() 
								? new Phon2CHATTask(inputFile, new File(outputFolder, FilenameUtils.removeExtension(inputFile.getName()) + ".cha"), phonTalkListener)
								: new Phon2XmlTask(inputFile, new File(outputFolder, FilenameUtils.removeExtension(inputFile.getName()) + ".xml"), phonTalkListener));
					exportTask.setName(inputFile.getName());
					exportTask.addTaskListener(taskListener);
					worker.invokeLater(exportTask);
					((PhonTalkTaskTableModel)taskTable.getModel()).addTask(exportTask);
					numSessions++;
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
						numSessionsProcessed + numFilesCopied, numSessions + numFilesToCopy));
				updateBreadcrumbButtons();
				
				exportFinishedMs = System.currentTimeMillis();
				printEndOfExportReport();
			});
		});
		worker.setFinishWhenQueueEmpty(true);
	}
	
	private void beginExport() {
		((PhonTalkTaskTableModel)taskTable.getModel()).clear();
		((PhonTalkMessageTableModel)messageTable.getModel()).clear();
		bufferPanel.getLogBuffer().setText("");
		
		numFilesToCopy = 0;
		numSessions = 0;
		numSessionsProcessed = 0;
		numFilesCopied = 0;
		numFilesFailed = 0;
		
		currentWorker = PhonWorker.createWorker();
		currentWorker.setName("PhonTalk");
		printBeginExportReport();
		
		// add import folder to history
		exportFolderHistory.addToHistory(exportFolderField.getSelectedFile());
		exportFolderHistory.saveHistory();
		
//		File projectFolder = projectFolderField.getSelectedFile();
//		boolean inWorkspace = false;
//		for(Project workspaceProject:workspace.getProjects()) {
//			if(workspaceProject.getLocation().equals(projectFolder.getAbsolutePath())) {
//				inWorkspace = true;
//				break;
//			}
//		}
//		if(!inWorkspace) {
			projectFolderHistory.addToHistory(projectFolderField.getSelectedFile());
			projectFolderHistory.saveHistory();
//		}
		
		((PhonTalkTaskTableModel)taskTable.getModel()).setParentFolder(projectFolderField.getSelectedFile());
		
		try {
			// create project
			final Project project = (new DesktopProjectFactory()).openProject(projectFolderField.getSelectedFile());
			PhonWorker.getInstance().invokeLater( () -> setupTasks(currentWorker, project) );
		} catch (IOException | ProjectConfigurationException e) {
			showMessage(e.getLocalizedMessage());
		}
		
		running = true;
		canceled = false;
		busyLabel.setBusy(true);
		currentWorker.start();
		
		exportStartedMs = System.currentTimeMillis();
		
		updateBreadcrumbButtons();
	}
	
//	private Project createProject() throws IOException, ProjectConfigurationException {
//		ProjectFactory projectFactory = new DesktopProjectFactory();
//		File projectFolder = new File(projectFolderField.getSelectedFile(), projectNameField.getText());
//		
//		if(!projectFolder.exists()) {
//			bufferPanel.getLogBuffer().append("Creating project at " + projectFolder.getAbsolutePath() + "\n\n");
//		} else {
//			bufferPanel.getLogBuffer().append("Opening project at " + projectFolder.getAbsolutePath() + "\n\n");
//		}
//	
//		Project retVal = projectFolder.exists() ? projectFactory.openProject(projectFolder) : projectFactory.createProject(projectFolder);
//		retVal.setName(projectNameField.getText());
//		return retVal;
//	}
	
	private void cancelExport() {
		if(running && currentWorker != null) {
			if(currentWorker.isAlive()) {
				canceled = true;
				currentWorker.shutdown();
			}
		}
	}
	
	/* Log methods */
	private void printBeginExportReport() {
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("Begin Project Export\n");
		buffer.append("----------------------------------------------\n");
		buffer.append("Project folder:\t");
		buffer.append(projectFolderField.getSelectedFile().getAbsolutePath());
		buffer.append('\n');
		buffer.append("Output folder:\t\t");
		buffer.append(exportFolderField.getSelectedFile().getAbsolutePath());
		buffer.append('\n');
		
		buffer.append('\n');
		
		bufferPanel.getLogBuffer().append(buffer.toString());
	}
	
	private void printEndOfExportReport() {
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("----------------------------------------------\n");
		if(canceled)
			buffer.append("Export canceled:\t\t");
		else
			buffer.append("Export finished:\t\t");
		buffer.append(String.format("%d/%d files processed\n",
				numSessionsProcessed + numFilesCopied, numSessions + numFilesToCopy));
		if(numFilesToCopy > 0)
			buffer.append(String.format("Files copied:\t\t\t%d/%d\n", numFilesCopied, numFilesToCopy));
		if(numSessions > 0)
			buffer.append(String.format("Sessions processed:\t\t%d/%d\n", numSessionsProcessed, numSessions));
		if(numFilesFailed > 0)
			buffer.append(String.format("Failed to process %d files\n", numFilesFailed));
		
		buffer.append(String.format("Elapsed time:\t\t\t%s\n", MsFormatter.msToDisplayString(exportFinishedMs-exportStartedMs)));
		
		bufferPanel.getLogBuffer().append(buffer.toString());
	}
	
	@Override
	protected void next() {
		if(getCurrentStep() == folderStep) {
			if(projectFolderField.getSelectedFile() == null) {
				showMessage("Please select project for export");
				return;
			}
			
			if(exportFolderField.getSelectedFile() == null) {
				showMessage("Please select output folder");
				return;
			} else {
				File outputFolder = exportFolderField.getSelectedFile();
				
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
			
			if(fileSelectionTree.getCheckedPaths().size() == 0) {
				showMessage("Please select files for export");
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
			beginExport();
		}
	}
	
	@Override
	public void close() {
		if(running) {
			int selected = showMessageDialog(DIALOG_TITLE, "Cancel export?", MessageDialogProperties.yesNoOptions);
			if(selected == 0) {
				cancelExport();
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
			final File inputFolder = projectFolderField.getSelectedFile();
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
					if(outputFilename.startsWith(exportFolderField.getSelectedFile().getAbsolutePath())) {
						outputFilename = "\u2026" + File.separator + exportFolderField.getSelectedFile().toPath().relativize(ptTask.getOutputFile().toPath()).toString();
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
							} else if (task instanceof Phon2CHATTask
									|| task instanceof Phon2XmlTask) {
								numSessionsProcessed++;
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
		ExportSession,
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
