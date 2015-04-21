package ca.phon.phontalk.app;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import ca.phon.phontalk.PhonTalkTask;

public class PhonTalkTaskTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 17127183403018899L;

	public static enum Columns {
		STATUS,
		FILENAME
	};
	
	private final List<PhonTalkTask> taskList = new ArrayList<>();
	
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
				retVal = task.getInputFile().getName();
				break;
				
			default:
				break;
			}
		}
		return retVal;
	}
	
}
