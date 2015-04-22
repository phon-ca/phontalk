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
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.jdesktop.swingx.VerticalLayout;

import ca.phon.application.project.IPhonProject;
import ca.phon.application.project.PhonProject;
import ca.phon.gui.DialogHeader;
import ca.phon.gui.components.FileSelectionField;
import ca.phon.gui.components.FileSelectionField.SelectionMode;
import ca.phon.gui.wizard.WizardStep;
import ca.phon.util.PhonUtilities;

public class ProjectSelectionStep extends WizardStep {

	private static final long serialVersionUID = -1908527260534577209L;
	
	private FileSelectionField projectFolderField;

	public ProjectSelectionStep() {
		super();
		
		init();
	}
	
	private void init() {
		projectFolderField = new FileSelectionField();
		projectFolderField.setMode(SelectionMode.FOLDERS);
		
		setLayout(new BorderLayout());
		
		final DialogHeader header = new DialogHeader(
				"PhonTalk : Export to Talkbank", "Select Phon project");
		add(header, BorderLayout.NORTH);
		
		final JPanel centerPanel = new JPanel(new VerticalLayout());
		centerPanel.add(projectFolderField);
		centerPanel.setBorder(BorderFactory.createTitledBorder("Select project:"));
		add(centerPanel, BorderLayout.CENTER);
	}
	
	public IPhonProject getProject() throws IOException {
		return PhonProject.fromFile(projectFolderField.getSelectedFile().getAbsolutePath());
	}
	
	@Override
	public boolean validateStep() {
		final File projectFolder = projectFolderField.getSelectedFile();
		if(projectFolder != null) {
			final File xmlFile = new File(projectFolder, "project.xml");
			if(!xmlFile.exists()) {
				PhonUtilities.showComponentMessage(projectFolderField, "Not a Phon project.");
				return false;
			}
		} else {
			PhonUtilities.showComponentMessage(projectFolderField, "Please select a project.");
			return false;
		}
		return true;
	}
	
}
