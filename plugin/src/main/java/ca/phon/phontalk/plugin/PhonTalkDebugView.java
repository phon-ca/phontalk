package ca.phon.phontalk.plugin;

import java.awt.BorderLayout;

import javax.swing.ImageIcon;
import javax.swing.JMenu;

import ca.phon.app.session.editor.EditorEventType;
import ca.phon.app.session.editor.EditorView;
import ca.phon.app.session.editor.SessionEditor;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;

public class PhonTalkDebugView extends EditorView {

	private static final long serialVersionUID = -5514763970376504590L;
	
	private TalkBankInfoPanel infoPanel;
	
	public final static String VIEW_NAME = "PhonTalk Debug";
	
	public final static String VIEW_ICON = "apps/phontalk";

	public PhonTalkDebugView(SessionEditor editor) {
		super(editor);
		
		init();
		
		editor.getEventManager().registerActionForEvent(
				EditorEventType.RECORD_CHANGED_EVT, (e) -> infoPanel.setRecord(getEditor().currentRecord()) );
	}
	
	private void init() {
		setLayout(new BorderLayout());
		
		infoPanel = new TalkBankInfoPanel(getEditor().getProject(), getEditor().getSession());
		infoPanel.setRecord(getEditor().currentRecord());
		add(infoPanel, BorderLayout.CENTER);
	}

	@Override
	public String getName() {
		return VIEW_NAME;
	}

	@Override
	public ImageIcon getIcon() {
		return IconManager.getInstance().getIcon(VIEW_ICON, IconSize.SMALL);
	}

	@Override
	public JMenu getMenu() {
		return null;
	}

}
