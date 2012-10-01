package ca.phon.phontalk.ui;

import java.util.HashMap;

/**
 * Entity class implementing Xml2Phon wizard model
 */
public class PhonTalkWizardModel extends HashMap<String, Object> {

	/* Static props */
	
	/** Project name */
	public final static String PROJECT_NAME = "_project_name_";
	
	/** Syllabifier */
	public final static String SYLLABIFIER_OBJ = "_syllabifier_obj_";
	
	/** Corpus naming */
	public final static String CORPUS_NAME_TYPE = "_corpus_name_type_";
	public final static Integer CORPUS_NAME_FROM_XML = 0x01;
	public final static Integer CORPUS_NAME_FROM_PARENT_DIR = 0x02;
	
	/** Paths */
	public final static String LIBRARY_PATH = "_lib_path_";
	public final static String OUTPUT_PATH = "_out_path_";

	public static final String PROJECT_PATH = "_project_path_";
	
	public static final String CONVERSION_MODE = "_conversion_mode_";
	
	public static enum ConversionMode {
		Xml2Phon,
		Phon2Xml
	};
	
	public PhonTalkWizardModel() {
		super();
		
		initDefaults();
	}
	
	private void initDefaults() {
		super.put(PROJECT_NAME, "New Project");
		super.put(SYLLABIFIER_OBJ, null);
		super.put(CORPUS_NAME_TYPE, CORPUS_NAME_FROM_XML);
		super.put(LIBRARY_PATH, ".");
		super.put(CONVERSION_MODE, ConversionMode.Xml2Phon);
	}
}
