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
package ca.phon.phontalk.plugin;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import ca.phon.phontalk.PhonTalkTask;
import ca.phon.worker.PhonTask.TaskStatus;

public class PhonTalkTaskTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 17127183403018899L;

	public static enum Columns {
		STATUS,
		FILENAME
	};
	
	private final List<PhonTalkTask> taskList = new ArrayList<>();
	
	private File parentFolder = new File(".");

	public File getParentFolder() {
		return this.parentFolder;
	}
	
	public void setParentFolder(File parentFolder) {
		this.parentFolder = parentFolder;
	}
	
	public void clear() {
		taskList.clear();
		fireTableDataChanged();
	}
	
	public List<PhonTalkTask> getTasks() {
		return taskList;
	}
	
	@Override
	public int getRowCount() {
		return taskList.size();
	}

	@Override
	public int getColumnCount() {
		return Columns.values().length;
	}
	
	@Override
	public String getColumnName(int col) {
		String retVal = super.getColumnName(col);
		switch(col) {
		case 0:
			retVal = "Status";
			break;
		case 1:
			retVal = "File";
			break;
		default:
			break;
		}
		return retVal;
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if(columnIndex == 0)
			return TaskStatus.class;
		else
			return String.class;
	}

	public void addTask(PhonTalkTask task) {
		taskList.add(task);
		fireTableRowsInserted(taskList.size()-1, taskList.size()-1);
	}
	
	public PhonTalkTask taskForRow(int row) {
		return (row >= 0 && row < taskList.size() 
				? taskList.get(row) : null);
	}

	public int rowForTask(PhonTalkTask task) {
		return taskList.indexOf(task);
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		PhonTalkTask task = taskForRow(rowIndex);
		Object retVal = null;
		if(task != null) {
			switch(columnIndex) {
			case 0:
				retVal = task.getStatus();
				break;
				
			case 1:
				File inputFile = task.getInputFile();
				retVal = inputFile.getAbsolutePath();
				if(parentFolder != null) {
					retVal = ".." + File.separator + parentFolder.toPath().relativize(inputFile.toPath()).toString();
				}
				break;
				
			default:
				break;
			}
		}
		return retVal;
	}
	
}
