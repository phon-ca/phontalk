/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2015, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
 * Dept of Linguistics, Memorial University <https://phon.ca>
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
package ca.phon.phontalk.plugin.ui;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
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

import ca.phon.app.log.LogUtil;
import ca.phon.app.project.DesktopProjectFactory;
import ca.phon.phontalk.Phon2XmlTask;
import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.PhonTalkMessage;
import ca.phon.phontalk.PhonTalkTask;
import ca.phon.phontalk.TalkbankValidator;
import ca.phon.phontalk.Xml2PhonTask;
import ca.phon.project.Project;
import ca.phon.project.ProjectFactory;
import ca.phon.project.exceptions.ProjectConfigurationException;
import ca.phon.query.report.csv.CSVTableDataWriter;
import ca.phon.syllabifier.Syllabifier;
import ca.phon.ui.HidablePanel;
import ca.phon.ui.action.PhonActionEvent;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.decorations.DialogHeader;
import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.ui.nativedialogs.NativeDialogs;
import ca.phon.util.*;
import ca.phon.worker.*;
import ca.phon.worker.PhonTask.TaskStatus;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class PhonTalkFrame extends JFrame {

	private static final long serialVersionUID = -4442294523695339523L;
	
	private PhonTalkDropPanel dropPanel;
	
	private PhonTalkSettingPanel settingsPanel;
	
	private JXTable taskTable;
	private PhonTalkTaskTableModel taskTableModel;
	
	private JXTable reportTable;
	private PhonTalkMessageTableModel reportTableModel;
	
	private JXBusyLabel busyLabel;
	
	private JLabel statusLabel;
	
	private PhonWorkerGroup workerGroup;
		
	private JButton saveAsCSVButton;
	
	public PhonTalkFrame() {
		super("PhonTalk");
		
		init();
		
		PhonWorker.getInstance().start();
		
		workerGroup = new PhonWorkerGroup(1);
		workerGroup.begin();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		
		final DialogHeader header = new DialogHeader("PhonTalk", "");
		add(header, BorderLayout.NORTH);
		
		settingsPanel = new PhonTalkSettingPanel();
		
		final PhonUIAction saveAsCSVAct = new PhonUIAction(this, "saveAsCSV");
		saveAsCSVAct.putValue(PhonUIAction.NAME, "Save log as CSV...");
		final KeyStroke saveAsKs = KeyStroke.getKeyStroke(KeyEvent.VK_S,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
		saveAsCSVAct.putValue(PhonUIAction.ACCELERATOR_KEY, saveAsKs);
		
		final PhonUIAction clearAct = new PhonUIAction(this, "onClear");
		clearAct.putValue(PhonUIAction.NAME, "Clear log");
		final KeyStroke clearKs = KeyStroke.getKeyStroke(KeyEvent.VK_C,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK);
		clearAct.putValue(PhonUIAction.ACCELERATOR_KEY, clearKs);
		
		final PhonUIAction redoAct = new PhonUIAction(this, "onRedo");
		redoAct.putValue(PhonUIAction.NAME, "Redo");
		final KeyStroke redoKs = KeyStroke.getKeyStroke(KeyEvent.VK_R, 
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK);
		redoAct.putValue(PhonUIAction.ACCELERATOR_KEY, redoKs);
		
		saveAsCSVButton = new JButton(saveAsCSVAct);
		
		taskTableModel = new PhonTalkTaskTableModel();
		taskTable = new JXTable(taskTableModel);
		taskTable.setColumnControlVisible(true);
		taskTable.setOpaque(false);
		taskTable.addMouseListener(taskTableContextListener);
		
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
		msgPanel.setTopLabelText("<html>To convert files between Phon and TalkBank formats, <br/>"
				+ "drop files onto the table above or use the Open button <br/> "
				+ "to select a file/folder containing either a Phon project <br/> "
				+ "or Talkbank .xml files.</html>" );
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 3;
		settingsPanel.add(msgPanel, gbc);
		dropPanel.add(settingsPanel, BorderLayout.SOUTH);
		
		
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
			workerGroup.queueTask(task);
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
	
	public void openFile(File file) {
		if(Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().open(file);
			} catch (IOException e) {
				LogUtil.severe(e);
				Toolkit.getDefaultToolkit().beep();
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
				} catch (IOException | ProjectConfigurationException e) {
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
	
	public void scanTalkBankFolder(File file) throws IOException {
		
		final String projectName = JOptionPane.showInputDialog(this, 
				"Please enter Phon project name:", file.getName() + "-phon");
		if(projectName == null) return;
		
		final File projectFolder = new File(file.getParentFile(), projectName);
		final ProjectFactory factory = new DesktopProjectFactory();
		final Project project = factory.createProject(projectFolder);
		project.setName(projectName);
		
		scanTalkBankFolderR(project, file);
	}
	
	private void scanTalkBankFolderR(Project project, File file) {
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
								
								final String chatCorpusName = 
										(String)corpusExpr.evaluate(doc, XPathConstants.STRING);
								final String corpusName = f.getParentFile().getName();
								final String sessionName = 
										f.getName().substring(0, f.getName().length()-4);
								
								final String sessionPath = 
										corpusName + File.separator + sessionName + ".xml";
								final String sessionAbsPath = 
										project.getLocation() + File.separator + sessionPath;
								
								final File sessionFile = new File(sessionAbsPath);
								final File corpusFile = sessionFile.getParentFile();
								if(!corpusFile.exists()) {
									corpusFile.mkdirs();
								}
								
								final Xml2PhonTask task = new Xml2PhonTask(f.getAbsolutePath(), sessionAbsPath, phonTalkListener);
								task.addTaskListener(taskListener);
								task.setName(f.getName());
								workerGroup.queueTask(task);
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
		workerGroup.queueTask(task);
		taskTableModel.addTask(task);
	}
	
	private void convertPhonSession(File file) {
		final String name = file.getName();
		final String newName = name.substring(0, name.length()-4) + "-xml.xml";
		
		final File newFile = new File(file.getParentFile(), newName);
		final Phon2XmlTask task = new Phon2XmlTask(file.getAbsolutePath(), newFile.getAbsolutePath(), phonTalkListener);
		task.addTaskListener(taskListener);
		task.setName(file.getName());
		workerGroup.queueTask(task);
		taskTableModel.addTask(task);
	}
	
	private void convertPhonProject(File file) throws IOException, ProjectConfigurationException {
		final ProjectFactory factory = new DesktopProjectFactory();
		final Project project = factory.openProject(file);
		final File projectFolder = new File(project.getLocation());
		final File outputFolder = new File(projectFolder.getParentFile(), projectFolder.getName() + "-xml");
		outputFolder.mkdirs();
		
		for(String corpus:project.getCorpora()) {
			final File corpusFile = new File(projectFolder, corpus);
			final File outputCorpusFolder = new File(outputFolder, corpus);
			outputCorpusFolder.mkdir();
			
			for(String sessionName:project.getCorpusSessions(corpus)) {
				final File sessionFile = new File(corpusFile, sessionName + ".xml");
				final File outputFile = new File(outputCorpusFolder, sessionName + ".xml");
				final Phon2XmlTask task = new Phon2XmlTask(sessionFile.getAbsolutePath(), 
						outputFile.getAbsolutePath(), phonTalkListener);
				task.addTaskListener(taskListener);
				task.setName(sessionFile.getName());
				workerGroup.queueTask(task);
				taskTableModel.addTask(task);
			}
		}
	}
	
	private final MouseInputAdapter taskTableContextListener = new MouseInputAdapter() {

		@Override
		public void mousePressed(MouseEvent e) {
			if(e.isPopupTrigger()) {
				showContextMenu(e);
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if(e.isPopupTrigger()) {
				showContextMenu(e);
			}
		}
		
		private void showContextMenu(MouseEvent e) {
			int row = taskTable.rowAtPoint(e.getPoint());
			if(row >= 0 && row < taskTableModel.getRowCount()) {
				taskTable.getSelectionModel().setSelectionInterval(row, row);
				final PhonTalkTask task = taskTableModel.taskForRow(row);
				if(task == null) return;
				
				final JPopupMenu menu = new JPopupMenu();
				
				final JMenuItem processItem = new JMenuItem(
						task.getInputFile().getName() + " (" + task.getProcessName() + ")");
				processItem.setEnabled(false);
				menu.add(processItem);
				
				final PhonUIAction openInputAct = new PhonUIAction(PhonTalkFrame.this, "openFile", task.getInputFile());
				openInputAct.putValue(PhonUIAction.NAME, "Open input file");
				openInputAct.putValue(PhonUIAction.SHORT_DESCRIPTION, task.getInputFile().getAbsolutePath());
				menu.add(openInputAct);
				
				final PhonUIAction openOuputAct = new PhonUIAction(PhonTalkFrame.this, "openFile", task.getOutputFile());
				openOuputAct.putValue(PhonUIAction.NAME, "Open output file");
				openOuputAct.putValue(PhonUIAction.SHORT_DESCRIPTION, task.getOutputFile().getAbsolutePath());
				menu.add(openOuputAct);
				
				final PhonUIAction redoAct = new PhonUIAction(workerGroup.getTaskList(), "add", task);
				redoAct.putValue(PhonUIAction.NAME, "Redo");
				menu.add(redoAct);
				
				menu.show(taskTable, e.getX(), e.getY());
			}
		}
		
	};
	
	private final PhonTalkDropListener dropListener = new PhonTalkDropListener() {
		
		@Override
		public void dropTalkBankFolder(final File file) {
			taskTableModel.setParentFolder(file.getParentFile());
			reportTableModel.setParentFolder(file.getParentFile());
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
			PhonWorker.getInstance().invokeLater(onBgThread);
		}
		
		@Override
		public void dropTalkBankFile(File file) {
			taskTableModel.setParentFolder(file.getParentFile());
			reportTableModel.setParentFolder(file.getParentFile());
			convertTalkBankFile(file);
		}
		
		@Override
		public void dropPhonSession(File file) {
			taskTableModel.setParentFolder(file.getParentFile());
			reportTableModel.setParentFolder(file.getParentFile());
			convertPhonSession(file);
		}
		
		@Override
		public void dropPhonProject(File file) {
			taskTableModel.setParentFolder(file.getParentFile());
			reportTableModel.setParentFolder(file.getParentFile());
			try {
				convertPhonProject(file);
			} catch (IOException | ProjectConfigurationException e) {
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
						// scroll to current task if no selection
						if(taskTable.getSelectedRow() < 0)
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
