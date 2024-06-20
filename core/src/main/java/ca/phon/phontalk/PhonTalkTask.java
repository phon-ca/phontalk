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
package ca.phon.phontalk;

import java.io.File;

import ca.phon.worker.PhonTask;

public abstract class PhonTalkTask extends PhonTask {
	
	private File inputFile;
	
	private File outputFile;
	
	/**
	 * Message listener
	 */
	private PhonTalkListener listener;

	private final StringBuffer outputBuffer = new StringBuffer();

	private int bufferOffset = -1;
	
	public PhonTalkTask(File inputFile, File outputFile, PhonTalkListener listener) {
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.listener = listener;
	}

	public File getInputFile() {
		return inputFile;
	}

	public void setInputFile(File inputFile) {
		this.inputFile = inputFile;
	}

	public File getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	public PhonTalkListener getListener() {
		return listener;
	}

	public void setListener(PhonTalkListener listener) {
		this.listener = listener;
	}

	public abstract String getProcessName();

	public StringBuffer getOutputBuffer() {
		return outputBuffer;
	}

	public void setBufferOffset(int offset) {
		this.bufferOffset = offset;
	}

	public int getBufferOffset() {
		return bufferOffset;
	}
	
}
