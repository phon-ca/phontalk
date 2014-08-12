package ca.phon.phontalk.app;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree;

import javax.imageio.ImageIO;
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
	
	private final static String DROP_IMG = "drop-img.png";
	
	private BufferedImage dropImg;

	private static final long serialVersionUID = -626029566860375266L;
	
	private String message = "";

	private CheckboxTree tree;
	
	private PhonTalkDropListener ptDropListener;
	
	public PhonTalkDropPanel() {
		super();
		
		init();
		
		new DropTarget(this, dropListener);
	}
	
	public void setPhonTalkDropListener(PhonTalkDropListener listener) {
		this.ptDropListener = listener;
	}
	
	public PhonTalkDropListener getPhonTalkDropListener() {
		return this.ptDropListener;
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
	}
	
	private final AtomicReference<File> dropFile = new AtomicReference<File>();
	private volatile boolean isPhon = false;
	private final DropTargetListener dropListener = new DropTargetListener() {
		
		@Override
		public void dropActionChanged(DropTargetDragEvent dtde) {
		}
		
		@Override
		public void drop(DropTargetDropEvent dtde) {
			if(dropFile.get() != null) {
				final File f = dropFile.get();
				if(f.isDirectory()) {
					if(!isPhon) {
						getPhonTalkDropListener().dropTalkBankFolder(f);
					} else {
						getPhonTalkDropListener().dropPhonProject(f);
					}
				} else {
					if(isPhon) {
						getPhonTalkDropListener().dropPhonSession(f);
					} else {
						getPhonTalkDropListener().dropTalkBankFile(f);
					}
				}
				
			}
			message = "";
			repaint();
		}
		
		@Override
		public void dragOver(DropTargetDragEvent dtde) {
		}
		
		@Override
		public void dragExit(DropTargetEvent dte) {
			message = "";
			dropFile.set(null);
			repaint();
		}
		
		@Override
		public void dragEnter(DropTargetDragEvent dtde) {
			for(DataFlavor f : dtde.getTransferable().getTransferDataFlavors()) {
	             System.out.println("flavor f:" + f + " type:" + f.getMimeType() + " javaClas:" + f.getDefaultRepresentationClass());  
	      }
			// check type 
			if(dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				try {
					final List<File> draggedFiles = 
							(List<File>)dtde.getTransferable().getTransferData(dtde.getCurrentDataFlavors()[0]);
					if(draggedFiles.size() > 1) {
						dtde.rejectDrag();
					} else {
						final File draggedFile = draggedFiles.get(0);
						
						// check type of conversion
						if(draggedFile.isDirectory()) {
							final File projectFile = new File(draggedFile, "project.xml");
							if(projectFile.exists()) {
								message = "Convert Phon project to xml";
								isPhon = true;
							} else {
								message = "Convert TalkBank files to a new Phon project";
								isPhon = false;
							}
							dropFile.set(draggedFile);
							dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
						} else {
							// make sure it's and xml file
							if(!draggedFile.getName().endsWith(".xml")) {
								dtde.rejectDrag();
							} else {
								// get the type of the input file
								final String rootEleName = getRootElementName(draggedFile);
								if(rootEleName.equalsIgnoreCase("CHAT")) {
									message = "Convert TalkBank file to Phon session";
									dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
									dropFile.set(draggedFile);
									isPhon = false;
								} else if(rootEleName.equalsIgnoreCase("session")) {
									message = "Convert Phon session to TalkBank";
									dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
									dropFile.set(draggedFile);
									isPhon = true;
								} else {
									message = "Unknown file type";
									dtde.rejectDrag();
								}
							}
						}
					}
				} catch (UnsupportedFlavorException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			repaint();
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
