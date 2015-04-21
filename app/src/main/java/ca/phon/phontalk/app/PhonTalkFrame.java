package ca.phon.phontalk.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
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
import org.jdesktop.swingx.JXTable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import ca.phon.application.PhonTask;
import ca.phon.application.PhonTaskListener;
import ca.phon.application.PhonWorker;
import ca.phon.application.PhonTask.TaskStatus;
import ca.phon.application.project.IPhonProject;
import ca.phon.application.project.PhonProject;
import ca.phon.engines.search.report.csv.CSVTableDataWriter;
import ca.phon.gui.CommonModuleFrame;
import ca.phon.gui.DialogHeader;
import ca.phon.gui.action.PhonActionEvent;
import ca.phon.gui.action.PhonUIAction;
import ca.phon.gui.components.HidablePanel;
import ca.phon.phontalk.Phon2XmlTask;
import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.PhonTalkMessage;
import ca.phon.phontalk.PhonTalkTask;
import ca.phon.phontalk.TalkbankValidator;
import ca.phon.phontalk.Xml2PhonTask;
import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.ui.nativedialogs.NativeDialogs;

public class PhonTalkFrame extends JFrame {

	private static final long serialVersionUID = -4442294523695339523L;
	
	private PhonTalkDropPanel dropPanel;
	
	private JXTable taskTable;
	private PhonTalkTaskTableModel taskTableModel;
	
	private JXTable reportTable;
	private PhonTalkMessageTableModel reportTableModel;
	
	private JXBusyLabel busyLabel;
	
	private JLabel statusLabel;
	
	private PhonWorker worker;
	
	private final ConcurrentLinkedQueue<Runnable> taskQueue = 
			new ConcurrentLinkedQueue<Runnable>();
	
	private JButton saveAsCSVButton;
	
	public PhonTalkFrame() {
		super("PhonTalk");
		
		init();
		worker = PhonWorker.createWorker(taskQueue);
		worker.setFinishWhenQueueEmpty(false);
		worker.start();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		
		final DialogHeader header = new DialogHeader("PhonTalk", "");
		add(header, BorderLayout.NORTH);
		
		final PhonUIAction saveAsCSVAct = new PhonUIAction(this, "saveAsCSV");
		saveAsCSVAct.putValue(PhonUIAction.NAME, "Save as CSV...");
		final KeyStroke saveAsKs = KeyStroke.getKeyStroke(KeyEvent.VK_S,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
		saveAsCSVAct.putValue(PhonUIAction.ACCELERATOR_KEY, saveAsKs);
		
		final PhonUIAction clearAct = new PhonUIAction(this, "onClear");
		clearAct.putValue(PhonUIAction.NAME, "Clear tables");
		final KeyStroke clearKs = KeyStroke.getKeyStroke(KeyEvent.VK_C,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
		clearAct.putValue(PhonUIAction.ACCELERATOR_KEY, clearKs);
		
		final PhonUIAction redoAct = new PhonUIAction(this, "onRedo");
		redoAct.putValue(PhonUIAction.NAME, "Redo");
		final KeyStroke redoKs = KeyStroke.getKeyStroke(KeyEvent.VK_R, 
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
		redoAct.putValue(PhonUIAction.ACCELERATOR_KEY, redoKs);
		
		saveAsCSVButton = new JButton(saveAsCSVAct);
		
		taskTableModel = new PhonTalkTaskTableModel();
		taskTable = new JXTable(taskTableModel);
		taskTable.setColumnControlVisible(true);
		taskTable.setOpaque(false);
		
		taskTable.getColumnModel().getColumn(0).setPreferredWidth(70);
		taskTable.getColumnModel().getColumn(1).setPreferredWidth(200);
		taskTable.setAutoResizeMode(JXTable.AUTO_RESIZE_OFF);
		
		JScrollPane taskScroller = new JScrollPane(taskTable);
		taskScroller.setOpaque(false);
		
		dropPanel = new PhonTalkDropPanel(dropListener);
		dropPanel.setLayout(new BorderLayout());
		dropPanel.add(taskScroller, BorderLayout.CENTER);
		dropPanel.setFont(dropPanel.getFont().deriveFont(Font.BOLD));
		
		final HidablePanel msgPanel = new HidablePanel(PhonTalkFrame.class.getName() + ".infoMsg");
		msgPanel.setTopLabelText("<html>To convert files between Phon and CHAT formats, "
				+ "drop files onto the panel above or use the Open button "
				+ "to select a file/folder containing either a Phon project "
				+ "or Talkbank .xml files.</html>" );
		dropPanel.add(msgPanel, BorderLayout.SOUTH);
		
		
		statusLabel = new JLabel();
		busyLabel = new JXBusyLabel(new Dimension(16, 16));
		
		JPanel topPanel = new JPanel(
				new FormLayout("pref, fill:pref:grow, right:pref", "pref"));
		final CellConstraints cc = new CellConstraints();
		topPanel.setBackground(Color.white);
		topPanel.add(busyLabel, cc.xy(1,1));
		topPanel.add(statusLabel, cc.xy(2,1));
		topPanel.add(saveAsCSVButton, cc.xy(3,1));
		
		reportTableModel = new PhonTalkMessageTableModel();
		reportTable = new JXTable(reportTableModel);
		reportTable.setColumnControlVisible(true);
		
		reportTable.getColumnModel().getColumn(0).setPreferredWidth(200);
		reportTable.getColumnModel().getColumn(1).setPreferredWidth(50);
		reportTable.getColumnModel().getColumn(2).setPreferredWidth(50);
		reportTable.getColumnModel().getColumn(3).setPreferredWidth(300);
		reportTable.setAutoResizeMode(JXTable.AUTO_RESIZE_OFF);
		
		JScrollPane scroller = new JScrollPane(reportTable);
		
		final JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.add(topPanel, BorderLayout.NORTH);
		rightPanel.add(scroller, BorderLayout.CENTER);
		final PhonUIAction act = new PhonUIAction(this, "onBrowse");
		act.putValue(PhonUIAction.NAME, "Open...");
		act.putValue(PhonUIAction.SHORT_DESCRIPTION, "Select folder for conversion");
		act.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, 
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		
		final JButton btn = new JButton(act);
		dropPanel.add(btn, BorderLayout.NORTH);
		
		final JMenuBar menuBar = new JMenuBar();
		final JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		
		fileMenu.add(new JMenuItem(act));
		fileMenu.addSeparator();
		fileMenu.add(saveAsCSVAct);
		fileMenu.addSeparator();
		final PhonUIAction exitAct = new PhonUIAction(this, "onExit");
		exitAct.putValue(PhonUIAction.NAME, "Exit");
		final KeyStroke exitKs = KeyStroke.getKeyStroke(KeyEvent.VK_Q,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
		exitAct.putValue(PhonUIAction.ACCELERATOR_KEY, exitKs);
		fileMenu.add(exitAct);
		
		final JMenu editMenu = new JMenu("Edit");
		menuBar.add(editMenu);
		editMenu.add(redoAct);
		editMenu.addSeparator();
		editMenu.add(clearAct);
		
		setJMenuBar(menuBar);
		
		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, dropPanel, rightPanel);
		add(splitPane, BorderLayout.CENTER);
	}
	
	public void onRedo() {
		List<PhonTalkTask> tasks = new ArrayList<>(taskTableModel.getTasks());
		onClear();
		for(PhonTalkTask task:tasks) {
			taskTableModel.addTask(task);
			taskQueue.add(task);
		}
	}
	
	public void onClear() {
		taskTableModel.clear();
		reportTableModel.clear();
	}
	
	public void onExit() {
		System.exit(1);
	}
	
	public void saveAsCSV(PhonActionEvent pae) {
		final String saveTo = NativeDialogs.showSaveFileDialogBlocking(this, null, ".csv", 
				new FileFilter[] { FileFilter.csvFilter }, "Save as CSV");
		if(saveTo != null) {
			final CSVTableDataWriter writer = new CSVTableDataWriter();
			try {
				writer.writeTableToFile(reportTable, new File(saveTo));
			} catch (IOException e) {
				NativeDialogs.showMessageDialogBlocking(this, null, "Save failed", e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
	}
	
	public void onBrowse() {
		final String selectedFolder = 
				NativeDialogs.browseForDirectoryBlocking(this, null, "Select folder");
		if(selectedFolder != null) {
			final File f = new File(selectedFolder);
			final File projectFile = new File(f, "project.xml");
			if(projectFile.exists()) {
				try {
					convertPhonProject(f);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				try {
					scanTalkBankFolder(f);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void main(String[] args) {
		final PhonTalkFrame frame = new PhonTalkFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1024, 768);
		frame.setVisible(true);
	}
	
	public void scanTalkBankFolder(File file) throws IOException {
		
		final String projectName = JOptionPane.showInputDialog(this, 
				"Please enter Phon project name:", file.getName() + "-phon");
		if(projectName == null) return;
		
		final File projectFolder = new File(file.getParentFile(), projectName);
		final IPhonProject project = PhonProject.newProject(projectFolder.getAbsolutePath());
		project.setProjectName(projectName);
		project.save();
		
		scanTalkBankFolderR(project, file);
	}
	
	private void scanTalkBankFolderR(IPhonProject project, File file) {
		if(file.isDirectory()) {
			// check each file in the folder
			for(File f:file.listFiles()) {
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
								
								final String sessionPath = 
										corpusName + File.separator + sessionName + ".xml";
								final String sessionAbsPath = 
										project.getProjectLocation() + File.separator + sessionPath;
								
								final File sessionFile = new File(sessionAbsPath);
								final File corpusFile = sessionFile.getParentFile();
								if(!corpusFile.exists()) {
									corpusFile.mkdirs();
								}
								
								final Xml2PhonTask task = new Xml2PhonTask(f.getAbsolutePath(), sessionAbsPath, phonTalkListener);
								task.addTaskListener(taskListener);
								task.setName(f.getName());
								taskQueue.add(task);
								taskTableModel.addTask(task);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					} catch (ValidationException e) {
						e.printStackTrace();
					} catch (XPathExpressionException e) {
						e.printStackTrace();
					} catch (ParserConfigurationException e) {
						e.printStackTrace();
					} catch (SAXException e) {
						e.printStackTrace();
					}
				} else if(f.isDirectory()) {
					scanTalkBankFolderR(project, f);
				}
			}
		}
	}
	
	private void convertTalkBankFile(File file) {
		final String name = file.getName();
		final String newName = name.substring(0, name.length()-4) + "-phon.xml";
		
		final File newFile = new File(file.getParentFile(), newName);
		final Xml2PhonTask task = new Xml2PhonTask(file.getAbsolutePath(), newFile.getAbsolutePath(), phonTalkListener);
		task.addTaskListener(taskListener);
		task.setName(file.getName());
		taskQueue.add(task);
		taskTableModel.addTask(task);
	}
	
	private void convertPhonSession(File file) {
		final String name = file.getName();
		final String newName = name.substring(0, name.length()-4) + "-xml.xml";
		
		final File newFile = new File(file.getParentFile(), newName);
		final Phon2XmlTask task = new Phon2XmlTask(file.getAbsolutePath(), newFile.getAbsolutePath(), phonTalkListener);
		task.addTaskListener(taskListener);
		task.setName(file.getName());
		taskQueue.add(task);
		taskTableModel.addTask(task);
	}
	
	private void convertPhonProject(File file) throws IOException {
		final IPhonProject project = PhonProject.fromFile(file.getAbsolutePath());
		final File projectFolder = new File(project.getProjectLocation());
		final File outputFolder = new File(projectFolder.getParentFile(), projectFolder.getName() + "-xml");
		outputFolder.mkdirs();
		
		for(String corpus:project.getCorpora()) {
			final File corpusFile = new File(projectFolder, corpus);
			final File outputCorpusFolder = new File(outputFolder, corpus);
			outputCorpusFolder.mkdir();
			
			for(String sessionName:project.getCorpusTranscripts(corpus)) {
				final File sessionFile = new File(corpusFile, sessionName + ".xml");
				final File outputFile = new File(outputCorpusFolder, sessionName + ".xml");
				final Phon2XmlTask task = new Phon2XmlTask(sessionFile.getAbsolutePath(), 
						outputFile.getAbsolutePath(), phonTalkListener);
				task.addTaskListener(taskListener);
				task.setName(sessionFile.getName());
				taskQueue.add(task);
				taskTableModel.addTask(task);
			}
		}
	}
	
	private final PhonTalkDropListener dropListener = new PhonTalkDropListener() {
		
		@Override
		public void dropTalkBankFolder(final File file) {
			final Runnable onEDT = new Runnable() {
				public void run() {
					busyLabel.setBusy(true);
					statusLabel.setText("Scanning folder...");
				}
			};
			SwingUtilities.invokeLater(onEDT);
			final Runnable onBgThread = new Runnable() {
				public void run() {
					try {
						scanTalkBankFolder(file);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			worker.invokeLater(onBgThread);
		}
		
		@Override
		public void dropTalkBankFile(File file) {
			convertTalkBankFile(file);
		}
		
		@Override
		public void dropPhonSession(File file) {
			convertPhonSession(file);
		}
		
		@Override
		public void dropPhonProject(File file) {
			try {
				convertPhonProject(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	};
	
	private final PhonTalkListener phonTalkListener = new PhonTalkListener() {
		
		@Override
		public void message(final PhonTalkMessage msg) {
			final Runnable onEDT = new Runnable() {
				public void run() {
					reportTableModel.addMessage(msg);
				}
			};
			SwingUtilities.invokeLater(onEDT);
		}
		
	};

	private final PhonTaskListener taskListener = new PhonTaskListener() {
		
		@Override
		public void statusChanged(PhonTask task, TaskStatus oldStatus,
				TaskStatus newStatus) {
			final String filename = task.getName();
			final TaskStatus status = newStatus;
			final Runnable onEDT = new Runnable() {
				public void run() {
					int taskRow = taskTableModel.rowForTask((PhonTalkTask)task);
					if(taskRow >= 0) {
						taskTableModel.fireTableCellUpdated(taskRow, PhonTalkTaskTableModel.Columns.STATUS.ordinal());
						taskTable.scrollRowToVisible(taskRow);
					}
					if(status == TaskStatus.RUNNING) {
						busyLabel.setBusy(true);
						statusLabel.setText(filename + " (" + ((PhonTalkTask)task).getProcessName() + ")");
					} else {
						busyLabel.setBusy(false);
						statusLabel.setText("");
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
	
}
