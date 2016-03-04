package ca.phon.phontalk.plugin;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;




import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;




import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.swingx.JXTitledSeparator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.DOMDifferenceEngine;
import org.xmlunit.diff.DefaultComparisonFormatter;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.DifferenceEngine;




import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;




import ca.phon.phontalk.parser.AST2TalkBank;
import ca.phon.phontalk.parser.AntlrTokens;
import ca.phon.phontalk.parser.AntlrUtils;
import ca.phon.phontalk.parser.Phon2XmlTreeBuilder;
import ca.phon.phontalk.parser.TreeBuilderException;
import ca.phon.project.Project;
import ca.phon.session.Participant;
import ca.phon.session.Record;
import ca.phon.session.Session;
import ca.phon.session.SessionFactory;
import ca.phon.ui.decorations.DialogHeader;

/**
 * Display information about the ANTLR tree and
 * TalkBank XML which will be generated from a
 * single record.  Useful for debugging issues.
 *
 */
public class TalkBankInfoPanel extends JPanel {
	
	private final static Logger LOGGER = Logger.getLogger(TalkBankInfoPanel.class.getName());

	private static final long serialVersionUID = -7401258917978425246L;

	// UI
	private JTabbedPane tabs;
	private RSyntaxTextArea antlrArea;
	private RSyntaxTextArea origXMLArea;
	private RSyntaxTextArea xmlArea;
	private RSyntaxTextArea diffArea;
	
	// model
	private final Project project;
	private final Session session;
	private final Record record;

	public TalkBankInfoPanel(Project project, Session session, int recordIdx) {
		this(project, session, session.getRecord(recordIdx));
	}
	
	public TalkBankInfoPanel(Project project, Session session, Record record) {
		super();
		
		this.project = project;
		this.session = session;
		this.record = record;
		
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		
		tabs = new JTabbedPane();
		add(tabs, BorderLayout.CENTER);
		
		// ANTLR tree
		antlrArea = new RSyntaxTextArea();
		antlrArea.setEditable(false);
		CommonTree tree = null;
		try {
			tree = toAntlrTree();
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			AntlrUtils.printTree(tree, bout);
			final String treeTxt = bout.toString("UTF-8");
			antlrArea.setText(treeTxt);
		} catch (TreeBuilderException | IOException e) {
			LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
			antlrArea.setText(e.getLocalizedMessage());
		}
		final RTextScrollPane antlrScroller = new RTextScrollPane(antlrArea, true);
		tabs.addTab("ANTLR Tree", antlrScroller);
		
		// original xml
		origXMLArea = new RSyntaxTextArea();
		origXMLArea.setSyntaxEditingStyle(RSyntaxTextArea.SYNTAX_STYLE_XML);
		try {
			origXMLArea.setText(getOrigXml());
		} catch (XPathExpressionException | ParserConfigurationException
				| SAXException | IOException
				| TransformerFactoryConfigurationError | TransformerException e1) {
			LOGGER.log(Level.SEVERE, e1.getLocalizedMessage(), e1);
			origXMLArea.setText(e1.getLocalizedMessage());
		}
		origXMLArea.setEditable(false);
		final RTextScrollPane origXmlScroller = new RTextScrollPane(origXMLArea, true);
		
		// new xml
		xmlArea = new RSyntaxTextArea();
		xmlArea.setSyntaxEditingStyle(RSyntaxTextArea.SYNTAX_STYLE_XML);
		try {
			xmlArea.setText(toXml(tree));
		} catch (RecognitionException | TransformerFactoryConfigurationError | TransformerException e) {
			LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
			xmlArea.setText(e.getLocalizedMessage());
		}
		xmlArea.setEditable(false);
		final RTextScrollPane xmlScroller = new RTextScrollPane(xmlArea, true);
		
		diffArea = new RSyntaxTextArea();
		diffArea.setEditable(false);
		diffArea.setRows(8);
		final RTextScrollPane diffScroller = new RTextScrollPane(diffArea, true);
		
		final GridBagLayout gbl = new GridBagLayout();
		final JPanel xmlPanel = new JPanel(gbl);

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		xmlPanel.add(new JXTitledSeparator("Original XML"), gbc);
		
		gbc.gridy = 1;
		gbc.weightx = 0.5;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		xmlPanel.add(origXmlScroller, gbc);
		
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridy = 0;
		gbc.gridx = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		xmlPanel.add(new JXTitledSeparator("Round-Trip XML"), gbc);
		
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridy = 1;
		gbc.weightx = 0.5;
		gbc.weighty = 1.0;
		xmlPanel.add(xmlScroller, gbc);
		
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridy = 2;
		gbc.gridx = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		xmlPanel.add(new JXTitledSeparator("Differences"), gbc);
		
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridy = 3;
		gbc.weightx = 1.0;
		gbc.weighty = 0.2;
		xmlPanel.add(diffScroller, gbc);
		calcDiff();
		
		tabs.addTab("TalkBank XML", xmlPanel);
		
	}
	
	private void calcDiff() {
		final String origXml = origXMLArea.getText();
		final String xml = xmlArea.getText();
		
		final Source control = Input.fromString(origXml).build();
		final Source test = Input.fromString(xml).build();
		
		final DifferenceEngine diff = new DOMDifferenceEngine();
		diff.addDifferenceListener( (comparision, outcome) -> {
			ComparisonType ct = comparision.getType();
			if(ct != ComparisonType.TEXT_VALUE) {
				// add to diff list
				final StringBuffer buf = new StringBuffer();
				buf.append(diffArea.getText());
				if(buf.length() > 0) buf.append('\n');
				buf.append(comparision.toString(new DefaultComparisonFormatter()));
				diffArea.setText(buf.toString());
			}
		});
		diff.compare(control, test);
	}
	
	private String getOrigXml() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException, TransformerFactoryConfigurationError, TransformerException {
		final String filename = project.getSessionPath(session);
		final File sessionFile = new File(filename);
		
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		final DocumentBuilder builder = dbf.newDocumentBuilder();
		final Document doc = builder.parse(sessionFile);
		
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		final String txtExpr = "//u[" + (session.getRecordPosition(record)+1) + "]";
		XPathExpression expr = xpath.compile(txtExpr);
		
		Node xmlNode = (Node)expr.evaluate(doc, XPathConstants.NODE);
		
		return prettyPrint(new DOMSource(xmlNode));
	}
	
	private CommonTree toAntlrTree() throws TreeBuilderException {
		final SessionFactory factory = SessionFactory.newFactory();
		final Session tempSession = factory.createSession();
		factory.copySessionInformation(session, tempSession);
		for(Participant part:session.getParticipants()) {
			final Participant clonedPart = factory.cloneParticipant(part);
			tempSession.addParticipant(clonedPart);
		}
		tempSession.addRecord(record);
		
		final Phon2XmlTreeBuilder treeBuilder = new Phon2XmlTreeBuilder();
		
		final CommonTree sessionTree = treeBuilder.buildTree(tempSession);
		final List<CommonTree> recordTrees = 
				AntlrUtils.findAllChildrenWithType(sessionTree, new AntlrTokens("TalkBank2AST.tokens"), "U_START");
		return (recordTrees.isEmpty() ? new CommonTree() : recordTrees.get(0));
	}
	
	private String toXml(CommonTree tree) throws RecognitionException, TransformerFactoryConfigurationError, TransformerException {
		final CommonTreeNodeStream stream = new CommonTreeNodeStream(tree);
		final AST2TalkBank ast2Tb = new AST2TalkBank(stream);
		final AST2TalkBank.u_return ret = ast2Tb.u();
		
		final String xml = ret.st.toString();
		return prettyPrint(new StreamSource(new StringReader(xml)));
	}
	
	private String prettyPrint(Source source) throws TransformerFactoryConfigurationError, TransformerException {
		// pretty-print XML
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		StreamResult result = new StreamResult(new StringWriter());
		transformer.transform(source, result);
		return result.getWriter().toString();
	}
	
}
