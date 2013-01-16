package ca.phon.phontalk.plugin.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ca.phon.application.project.IPhonProject;
import ca.phon.gui.CommonModuleFrame;
import ca.phon.phontalk.plugin.wizard.Phon2TalkbankWizard;

/**
 * Action for starting the Phon2Talkbank wizard.
 *
 */
public class Phon2TalkbankAction extends AbstractAction {
	
	private final static String ACTION_TEXT = "Export sessions to Talkbank...";
	
	private final static String TOOLTIP_TEXT = "Export sessions to Talkbank XML for import into CLAN";
	
	
	public Phon2TalkbankAction() {
		super();
		
		putValue(NAME, ACTION_TEXT);
		putValue(SHORT_DESCRIPTION, TOOLTIP_TEXT);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		final CommonModuleFrame cmf = CommonModuleFrame.getCurrentFrame();
		final IPhonProject project = cmf.getProject();
		if(project != null) {
			// init and show the Phon2TalkbankWizard
			final Phon2TalkbankWizard wizard = new Phon2TalkbankWizard(project);
			wizard.showWizard();
		}
		
	}

}
