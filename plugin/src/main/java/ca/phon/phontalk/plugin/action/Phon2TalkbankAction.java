/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2008 The Phon Project, Memorial University <http://phon.ling.mun.ca>
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
