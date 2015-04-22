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
package ca.phon.phontalk.app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import ca.phon.phontalk.PhonTalkMessage;

public class PhonTalkMessageTableModel extends AbstractTableModel {
	
	private static final long serialVersionUID = -7371534071899851171L;

	private static enum Columns {
		FILE,
		LINE,
		COL,
		MESSAGE
	};
	
	private List<PhonTalkMessage> messages = new ArrayList<>();

	public void clear() {
		messages.clear();
		fireTableDataChanged();
	}
	
	public void addMessage(PhonTalkMessage message) {
		messages.add(message);
		super.fireTableRowsInserted(messages.size()-1, messages.size()-1);
	}
	
	@Override
	public String getColumnName(int col) {
		String retVal = super.getColumnName(col);
		
		switch(col) {
		case 0:
			retVal = "File";
			break;
			
		case 1:
			retVal = "Line";
			break;
		
		case 2:
			retVal = "Column";
			break;
			
		case 3:
			retVal = "Message";
			break;
			
		default:
			break;
		}
		
		return retVal;
	}
	
	@Override
	public Class<?> getColumnClass(int col) {
		Class<?> retVal = Object.class;
		switch(col) {
		case 0:
			retVal = File.class;
			break;
			
		case 1:
		case 2:
			retVal = Integer.class;
			break;
			
		case 3:
			retVal = String.class;
			break;
			
		default:
			break;
		}
		return retVal;
	}
	
	@Override
	public int getRowCount() {
		return messages.size();
	}

	@Override
	public int getColumnCount() {
		return Columns.values().length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Object retVal = null;
		
		if(rowIndex < 0 || rowIndex >= messages.size()) return retVal;
		
		final PhonTalkMessage msg = messages.get(rowIndex);
		switch(columnIndex) {
		case 0:
			retVal = msg.getFile().getName();
			break;
			
		case 1:
			retVal = msg.getLineNumber();
			break;
			
		case 2:
			retVal = msg.getColNumber();
			break;
			
		case 3:
			retVal = msg.getMessage();
			break;
			
		default:
			retVal = "<html>" + msg.toString() + "</html>";
			break;
		}
		
		return retVal;
	}

}
