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
package ca.phon.phontalk.plugin.wizard.phon2talkbank;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import ca.phon.application.project.IPhonProject;
import ca.phon.gui.DialogHeader;
import ca.phon.gui.SessionSelector;
import ca.phon.gui.components.FileSelectionField;
import ca.phon.gui.components.FileSelectionField.SelectionMode;
import ca.phon.gui.wizard.WizardStep;

public class SessionSelectionStep extends WizardStep {
	
	private static final long serialVersionUID = 4260810910198482714L;
	
	private IPhonProject project;
	
	/**
	 * Session selector
	 */
	private SessionSelector sessionSelector;
	
	/**
	 * Folder label
	 */
	private FileSelectionField outputFolderField;

	public SessionSelectionStep(IPhonProject project) {
		super();
		
		this.project = project;
		init();
	}
	
	public IPhonProject getProject() {
		return this.project;
	}

	private void init() {
		outputFolderField = new FileSelectionField();
		outputFolderField.setPrompt("Output folder");
		outputFolderField.setMode(SelectionMode.FOLDERS);
		
		final JPanel wizardPanel = new JPanel(new BorderLayout());
		
		final JPanel outputFolderPanel = new JPanel();
		outputFolderPanel.setBorder(BorderFactory.createTitledBorder("Select output folder:"));
		outputFolderPanel.setLayout(new BorderLayout());
		outputFolderPanel.add(outputFolderField, BorderLayout.CENTER);
		
		final JPanel selectorPanel = new JPanel(new BorderLayout());
		sessionSelector = new SessionSelector(getProject());
		final JScrollPane sessionScroller = new JScrollPane(sessionSelector);
		selectorPanel.setBorder(BorderFactory.createTitledBorder("Select sessions for export:"));
		selectorPanel.add(sessionScroller, BorderLayout.CENTER);
		
		wizardPanel.add(selectorPanel, BorderLayout.CENTER);
		wizardPanel.add(outputFolderPanel, BorderLayout.NORTH);
		
		final DialogHeader header = new DialogHeader("PhonTalk : Export to Talkbank", "Select sessions and output folder.");
		setLayout(new BorderLayout());
		add(header, BorderLayout.NORTH);
		add(wizardPanel, BorderLayout.CENTER);
	}
	
	public FileSelectionField getOutputFolderField() {
		return this.outputFolderField;
	}
	
	public SessionSelector getSessionSelector() {
		return this.sessionSelector;
	}
	
}
