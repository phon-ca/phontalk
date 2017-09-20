package ca.phon.phontalk;

import ca.phon.util.PrefHelper;

public class PhonTalkSettings {
	
	/* Syllabify and align imported IPA transcripts */
	public final static String SYLLABIFY_AND_ALIGN = "ca.phon.phontalk.syllabifyAndAlign";
	public final static boolean DEFAULT_SYLLABIFY_AND_ALIGN = false;
	
	private boolean syllabifyAndAlign = PrefHelper.getBoolean(SYLLABIFY_AND_ALIGN, DEFAULT_SYLLABIFY_AND_ALIGN);

	public boolean isSyllabifyAndAlign() {
		return this.syllabifyAndAlign;
	}
	
	public void setSyllabifyAndAlign(boolean syllabifyAndAlign) {
		this.syllabifyAndAlign = syllabifyAndAlign;
		PrefHelper.getUserPreferences().putBoolean(SYLLABIFY_AND_ALIGN, syllabifyAndAlign);
	}
	
	/* Setting for syllabifer selction */
	public final static String SYLLABIFIER = "ca.phon.phontalk.syllabifier";
	public final static String DEFAULT_SYLLABIFIER = "eng-simple";
	
	private String syllabifer = PrefHelper.get(SYLLABIFIER, DEFAULT_SYLLABIFIER);
	
	public String getSyllabifer() {
		return this.syllabifer;
	}
	
	public void setSyllabifier(String syllabifier) {
		this.syllabifer = syllabifier;
		PrefHelper.getUserPreferences().put(SYLLABIFIER, syllabifier);
	}
	
	/* Export alignment tier */
	public final static String EXPORT_ALIGNMENT = "ca.phon.phontalk.exportAlignment";
	public final static boolean DEFAULT_EXPORT_ALIGNMENT = false;
	
	private boolean exportAlignment = PrefHelper.getBoolean(EXPORT_ALIGNMENT, DEFAULT_EXPORT_ALIGNMENT);
	
	public boolean isExportAlignment() {
		return this.exportAlignment;
	}
	
	public void setExportAlignment(boolean exportAlignment) {
		this.exportAlignment = exportAlignment;
		PrefHelper.getUserPreferences().putBoolean(EXPORT_ALIGNMENT, exportAlignment);
	}
	
}
