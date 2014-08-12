package ca.phon.phontalk.app;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.xml.bind.ValidationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ca.phon.application.PhonTask;
import ca.phon.application.PhonTaskListener;
import ca.phon.application.PhonWorker;
import ca.phon.application.PhonTask.TaskStatus;
import ca.phon.application.project.IPhonProject;
import ca.phon.application.project.PhonProject;
import ca.phon.gui.CommonModuleFrame;
import ca.phon.gui.DialogHeader;
import ca.phon.phontalk.Phon2XmlTask;
import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.PhonTalkMessage;
import ca.phon.phontalk.TalkbankValidator;
import ca.phon.phontalk.Xml2PhonTask;

public class PhonTalkFrame extends CommonModuleFrame {

	private static final long serialVersionUID = -4442294523695339523L;
	
	private PhonTalkDropPanel dropPanel;
	
	private JTextArea textArea;
	
	private JProgressBar progressBar;
	
	private PhonWorker worker;
	
	private final ConcurrentLinkedQueue<Runnable> taskQueue = 
			new ConcurrentLinkedQueue<Runnable>();

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
		
		dropPanel = new PhonTalkDropPanel();
		dropPanel.setPhonTalkDropListener(dropListener);
		dropPanel.setFont(dropPanel.getFont().deriveFont(Font.BOLD));
		add(dropPanel, BorderLayout.WEST);
		
		final JPanel btmPanel = new JPanel(new BorderLayout());
		progressBar = new JProgressBar();
		btmPanel.add(progressBar, BorderLayout.NORTH);
		textArea = new JTextArea();
		final JScrollPane textScroller = new JScrollPane(textArea);
		textArea.setEditable(false);
		textArea.setRows(10);
		btmPanel.add(textScroller, BorderLayout.CENTER);
		
		add(btmPanel, BorderLayout.CENTER);
	}
	
	public static void main(String[] args) {
		final PhonTalkFrame frame = new PhonTalkFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1024, 768);
		frame.centerWindow();
		frame.setVisible(true);
	}
	
	public void scanTalkBankFolder(File file) throws IOException {
		final String projectName = file.getName();
		final File projectFolder = new File(file.getParentFile(), projectName + "-phon");
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
								taskQueue.add(task);
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
		taskQueue.add(task);
	}
	
	private void convertPhonSession(File file) {
		final String name = file.getName();
		final String newName = name.substring(0, name.length()-4) + "-xml.xml";
		
		final File newFile = new File(file.getParentFile(), newName);
		final Phon2XmlTask task = new Phon2XmlTask(file.getAbsolutePath(), newFile.getAbsolutePath(), phonTalkListener);
		task.addTaskListener(taskListener);
		taskQueue.add(task);
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
				taskQueue.add(task);
			}
		}
	}
	
	private final PhonTalkDropListener dropListener = new PhonTalkDropListener() {
		
		@Override
		public void dropTalkBankFolder(File file) {
			try {
				scanTalkBankFolder(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
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
		public void message(PhonTalkMessage msg) {
			textArea.append(msg.getMessage());
		}
		
	};

	private final PhonTaskListener taskListener = new PhonTaskListener() {
		
		@Override
		public void statusChanged(PhonTask task, TaskStatus oldStatus,
				TaskStatus newStatus) {
			if(newStatus == TaskStatus.RUNNING) {
				progressBar.setIndeterminate(true);
			} else {
				progressBar.setIndeterminate(false);
			}
		}
		
		@Override
		public void propertyChanged(PhonTask task, String property,
				Object oldValue, Object newValue) {
			
		}
	};
	
}
