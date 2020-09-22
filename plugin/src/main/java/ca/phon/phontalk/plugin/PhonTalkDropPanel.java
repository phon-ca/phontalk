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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.datatransfer.Transferable;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * A panel which accepts a folder/.xml file which is drag-ed and drop-ed
 * onto the component.
 * 
 */
public class PhonTalkDropPanel extends JPanel {
	
	private final static Logger LOGGER = Logger.getLogger(PhonTalkDropPanel.class.getName());
	
	private final static String DROP_IMG = "drop-img.png";
	
	private BufferedImage dropImg;

	private static final long serialVersionUID = -626029566860375266L;
	
	private String message = "";
	
	private final PhonTalkDropListener listener;
	
	public PhonTalkDropPanel(PhonTalkDropListener listener) {
		super();
		
		this.listener = listener;
		init();
	}
	
	@Override
	public Dimension getPreferredSize() {
		Dimension retVal = super.getPreferredSize();
		
		if(dropImg != null) {
			retVal = new Dimension(dropImg.getWidth(), dropImg.getHeight());
		}
		
		return retVal;
	}
	
	@Override
	public void paintComponent(Graphics g) {
		int width = getWidth();
		int height = getHeight();
		g.setColor(Color.white);
		g.fillRect(0, 0, width, height);
		if(dropImg != null) {
			int x = 0;
			if(width != dropImg.getWidth()) {
				x = (width - dropImg.getWidth()) / 2;
			}
			int y = 0;
			if(height != dropImg.getHeight()) {
				y = (height - dropImg.getHeight()) / 2;
			}
			g.drawImage(dropImg, x, y, this);
		}
		
		if(message.length() > 0) {
			final FontMetrics fm = getFontMetrics(getFont());
			final Rectangle2D messageRect = fm.getStringBounds(message, g);
			
			int x = (width / 2) - (int)(messageRect.getWidth() / 2);
			int y =  height - (int)messageRect.getHeight() - 5;
			
			g.setColor(Color.black);
			g.drawString(message, x, y);
		}
	}
	
	private void init() {
		try {
			dropImg = ImageIO.read(getClass().getClassLoader().getResource(DROP_IMG));
		} catch (IOException e) {
			e.printStackTrace();
		}
		setTransferHandler(dropListener);
	}
	
	public PhonTalkDropListener getPhonTalkDropListener() {
		return listener;
	}
	
	private final FileTransferHandler dropListener = new FileTransferHandler() {
		
		@Override
		public boolean importData(JComponent comp, Transferable t) {
			try {
				final List<File> files = getFiles(t);
				
				for(File draggedFile:files) {
					// check type of conversion
					if(draggedFile.isDirectory()) {
						final File projectFile = new File(draggedFile, "project.xml");
						if(projectFile.exists()) {
							getPhonTalkDropListener().dropPhonProject(draggedFile);
						} else {
							getPhonTalkDropListener().dropTalkBankFolder(draggedFile);
						}
					} else {
						// make sure it's an xml file
						if(draggedFile.getName().endsWith(".xml")) {
							// get the type of the input file
							final String rootEleName = getRootElementName(draggedFile);
							if(rootEleName.equalsIgnoreCase("CHAT")) {
								getPhonTalkDropListener().dropTalkBankFile(draggedFile);
							} else if(rootEleName.equalsIgnoreCase("session")) {
								getPhonTalkDropListener().dropPhonSession(draggedFile);
							}
						}
					}
				}
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
			
			return true;
		}
		
	};
	
	private static String getRootElementName(File f) 
			throws IOException {
			String retVal = null;
			
			final FileInputStream fin = new FileInputStream(f);
			final XMLInputFactory factory = XMLInputFactory.newFactory();
			try {
				final XMLStreamReader xmlStreamReader =
						factory.createXMLStreamReader(fin);
				
				while(xmlStreamReader.hasNext()) {
					final int nextType = xmlStreamReader.next();
					if(nextType == XMLStreamReader.START_ELEMENT) {
						// get the element name and break
						retVal = xmlStreamReader.getName().getLocalPart();
						break;
					}
				}
				xmlStreamReader.close();
			} catch (XMLStreamException e) {
				throw new IOException(e);
			}
			
			return retVal;
		}
	
}
