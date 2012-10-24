package ca.phon.phontalk.plugin.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ca.phon.application.project.IPhonProject;
import ca.phon.gui.CommonModuleFrame;
import ca.phon.phontalk.plugin.wizard.Talkbank2PhonWizard;

/**
 * Action for starting the Phon2Talkbank wizard.
 *
 */
public class Talkbank2PhonAction extends AbstractAction {
	
	private final static String ACTION_TEXT = "Import sessions from Talkbank...";
	
	private final static String TOOLTIP_TEXT = "Import session from Talkbank XML into the current project.";
	
	
	public Talkbank2PhonAction() {
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
			final Talkbank2PhonWizard wizard = new Talkbank2PhonWizard(project);
			wizard.showWizard();
		}
		
	}

}
