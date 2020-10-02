package ca.phon.phontalk;

import ca.phon.util.PrefHelper;

public class Phon2XMLSettings {
	
	public final static String EXPORT_SYLLABIFY_AND_ALIGN = "ca.phon.phontalk.exportSyllabAndAlign";
	public final static boolean DEFAULT_EXPORT_SYLLABIFY_AND_ALIGN = true;
	
	private boolean exportSyllabAndAlign = PrefHelper.getBoolean(EXPORT_SYLLABIFY_AND_ALIGN, DEFAULT_EXPORT_SYLLABIFY_AND_ALIGN);

	public boolean isExportSyllabAndAlign() {
		return this.exportSyllabAndAlign;
	}
	
	public void setExportSyllabifyAndAlign(boolean exportSyllabAndAlign) {
		this.exportSyllabAndAlign = exportSyllabAndAlign;
		PrefHelper.getUserPreferences().putBoolean(EXPORT_SYLLABIFY_AND_ALIGN, exportSyllabAndAlign);
	}
}
