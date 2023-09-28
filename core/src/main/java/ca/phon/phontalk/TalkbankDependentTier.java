package ca.phon.phontalk;

/**
 * Possible values for the 'a' element of TalkBank utterances
 */
public enum TalkbankDependentTier {
    Addressee("addressee", "%add"),
    Actions("actions", "%act"),
    Alternative("alternative", "%alt"),
    Coding("coding", "%cod"),
    Cohesion("cohesion", "%coh"),
    Comments("comments", "%com"),
    EnglishTranslation("english translation", "%eng"),
    Errcoding("errcoding", "%err"),
    Explanation("explanation", "%exp"),
    Flow("flow", "%flo"),
    TargetGloss("target gloss", "%gls"),
    Gesture("gesture", "%gpx"),
    Intonation("intonation", "%int"),
    Orthography("orthography", "%ort"),
    Paralinguistics("paralinguistics", "%par"),
    SALT("SALT", "%def"),
    Situation("situation", "%sit"),
    SpeechAct("speech act", "%spa"),
    TimeStamp("time stamp", "%tim"),
    // used with flavor
    Extension("extension", "%x");

    private String aType;

    private String tierName;

    TalkbankDependentTier(String aType, String tierName) {
        this.aType = aType;
        this.tierName = tierName;
    }

    public String getaType() {
        return aType;
    }

    public String getTierName() {
        return tierName;
    }

    public static TalkbankDependentTier fromType(String type) {
        for(TalkbankDependentTier tier:values()) {
            if(tier.getaType().equals(type))
                return tier;
        }
        return null;
    }

}