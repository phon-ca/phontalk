package ca.phon.phontalk.plugin;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.actions.SessionEditorAction;
import ca.phon.session.Record;
import ca.phon.session.Session;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.decorations.DialogHeader;

public class PhonTalkInfoAction extends SessionEditorAction {

	private static final long serialVersionUID = -564227849329387762L;
	
	private final static String TXT = "PhonTalk Debug Panel";
	private final static String DESC = "Show PhonTalk debug panel for current record";

	public PhonTalkInfoAction(SessionEditor editor) {
		super(editor);
		
		putValue(NAME, TXT);
		putValue(SHORT_DESCRIPTION, DESC);
	}

	@Override
	public void hookableActionPerformed(ActionEvent ae) {
		final Session session = getEditor().getSession();
		final Record record = getEditor().currentRecord();
		
		final CommonModuleFrame cmf = new CommonModuleFrame("PhonTalk Debug");
		final DialogHeader header = new DialogHeader("PhonTalk Debug", "Display ANTLR and TalkBank XML information");
		
		final TalkBankInfoPanel infoPanel = 
				new TalkBankInfoPanel(getEditor().getProject(), session);
		infoPanel.setRecord(record);
		cmf.getContentPane().setLayout(new BorderLayout());
		cmf.getContentPane().add(header, BorderLayout.NORTH);
		cmf.getContentPane().add(infoPanel, BorderLayout.CENTER);
		
		cmf.pack();
		cmf.setSize(1024, 768);
		cmf.setLocationRelativeTo(getEditor());
		cmf.setVisible(true);
	}

}
