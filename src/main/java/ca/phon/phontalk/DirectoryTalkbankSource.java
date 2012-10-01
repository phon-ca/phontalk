package ca.phon.phontalk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DirectoryTalkbankSource extends TalkbankSource {
	
	private List<File> files;

	/** The parent directory */
	private File dir = null;
	
	public DirectoryTalkbankSource(String path) {
		this(new File(path));
	}
	
	public DirectoryTalkbankSource(File parent) {
		super();
		
		this.dir = parent;
	}
	
	@Override
	public File[] listTalkbankFiles() {
		if(files == null) {
			List<File> fileList = new ArrayList<File>();
			findFiles(fileList, dir);
			
			files = new ArrayList<File>();
			files.addAll(fileList);
		}
		
		return files.toArray(new File[0]);
	}
	
	protected void findFiles(List<File> files, File d) {
		if(d.isDirectory()) {
			for(File f:d.listFiles()) {
				if(f.isFile() && f.getName().endsWith(".xml")) {
					files.add(f);
				} else if(f.isDirectory()) {
					findFiles(files, f);
				}
			}
		}
	}

	@Override
	public InputStream toInputStream(File file) 
		throws IOException {
		InputStream retVal = null;
		retVal = new FileInputStream(file);
		return retVal;
	}

	@Override
	public String getSourceName() {
		String retVal = "";
		
		if(dir != null)
			retVal = dir.getAbsolutePath();
		
		return retVal;
	}

}
