package ca.phon.phontalk.parser;

import ca.phon.formatter.Formatter;
import ca.phon.formatter.FormatterFactory;
import ca.phon.orthography.Orthography;
import ca.phon.session.MediaSegment;
import ca.phon.session.MediaUnit;
import ca.phon.session.Tier;
import ca.phon.session.TierString;
import ca.phon.util.MsFormatter;
import org.antlr.runtime.tree.CommonTree;
import org.apache.commons.lang3.StringEscapeUtils;

import javax.print.attribute.standard.Media;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds ANTLR trees for various CHAT elements as well
 * as handle codes which have been enclosed inside parenthesis
 * in Orthography.
 */
public class TalkBankCodeTreeBuilder {

    private final static Logger LOGGER = Logger.getLogger(Phon2XmlTreeBuilder.class.getName());

    private static final AntlrTokens talkbankTokens = new AntlrTokens("TalkBank2AST.tokens");

    private final static String MS_DISPLAY_REGEX = "([0-9]{2,3}):([0-9]{2})\\.([0-9]{2,3})";
    private final static String INTERNAL_MEDIA_REGEX = "\\((" + MS_DISPLAY_REGEX + ")-(" + MS_DISPLAY_REGEX + ")\\)";
    private final static int INTERNAL_MEDIA_START_GROUP = 1;
    private final static int INTERNAL_MEDIA_END_GROUP = 5;

    private final static String OVERLAP_POINT_REGEX = "([⌈⌉⌊⌋])([0-9]+)?";
    private final static int OVERLAP_POINT_SYMBOL_GROUP = 1;
    private final static int OVERLAP_POINT_INDEX_GROUP = 2;

    /**
     * Handle data in parentheses.
     */
    public CommonTree handleParentheticData(CommonTree tree, String d) {
        if (!d.startsWith("(") || !d.endsWith(")"))
            throw new IllegalArgumentException(d);

        String data = d.substring(1, d.length() - 1);

        // attempt to find the utterance parent
        CommonTree uttNode = null;
        int uttTokenType = talkbankTokens.getTokenType("U_START");
        CommonTree cNode = tree;
        while (cNode != null) {
            if (cNode.getToken().getType() == uttTokenType) {
                uttNode = cNode;
                break;
            }
            cNode = (CommonTree) cNode.getParent();
        }

        // try to find last w node
        CommonTree parentNode = tree;
        // find the last 'w' node
        int wTokenType = talkbankTokens.getTokenType("W_START");
        CommonTree wNode = null;
        for (int cIndex = parentNode.getChildCount() - 1; cIndex >= 0; cIndex--) {
            CommonTree tNode = (CommonTree) parentNode.getChild(cIndex);
            if (tNode.getToken().getType() == wTokenType) {
                wNode = tNode;
                break;
            }
        }

        // handle special cases
        // overlaps
        if (data.matches("<[0-9]*") || data.matches(">[0-9]*")) {
            return addOverlap(tree, data);
        }

        // duration
        if(data.matches("# [0-9.]+(m?s)?")) {
            return addDuration(tree, data);
        }

        // freecode
        if(data.matches("\\^\\s.+")) {
            return addFreecode(tree, data);
        }

        // tagMarkers
        else if (data.equals(",")
                || data.equals("\u201e")
                || data.equals("\u2021")) {
            return addTagMarker(tree, data);
        }

        // s
        else if (data.equals("^c")) {
            return addSeparator(tree, data);
        }

        // overlap-point
        else if (data.matches(OVERLAP_POINT_REGEX)) {
            return addOverlapPoint(tree, data);
        }

        // linkers
        else if (data.equals("+\"")
                || data.equals("+^")
                || data.equals("+<")
                || data.equals("+,")
                || data.equals("++")
                || data.equals("+\u224b")
                || data.equals("+\u2248")) {
            return addLinker(uttNode, data);
        }

        // pauses
        else if (data.equals(".")
                || data.equals("..")
                || data.equals("...")) {
            return addPause(tree, data);
        }

        // makers
        else if (data.equals("!")
                || data.equals("!!")
                || data.equals("?")
                || data.equals("/")
                || data.equals("//")
                || data.equals("///")
                || data.equals("/?")
                || data.equals("/-")) {
            return addMarker(tree, data);
        }

        // ga
        else if (data.startsWith("=?")) {
            return addGa(tree, "alternative", data.substring(2).trim());
        } else if (data.startsWith("%")) {
            return addGa(tree, "comments", data.substring(1).trim());
        } else if (data.startsWith("=!")) {
            return addGa(tree, "paralinguistics", data.substring(2).trim());
        } else if (data.startsWith("=")) {
           return addGa(tree, "explanation", data.substring(1).trim());
        }

        // repeats
        else if (data.matches("x\\p{Space}?[0-9]+")) {
            return addRepetition(tree, data.substring(1).trim());
        }

        // terminator
        if (data.equals(".") || data.equals("?") || data.equals("!")
                || data.equals("+.") || data.equals("+...") || data.equals("+..?")
                || data.equals("+!?") || data.equals("+/.") || data.equals("+/?")
                || data.equals("+//.") || data.equals("+//?") || data.equals("+\"/.")
                || data.equals("+\".") || data.equals("\u224b") || data.equals("\u2248")) {
            return addTerminator(uttNode, data);
        }

        // everything else
        else {
            Pattern pattern = Pattern.compile("([-\\w_]+):(.*)");
            Matcher matcher = pattern.matcher(data);

            if (matcher.matches()) {
                String eleName = matcher.group(1);
                String eleData = matcher.group(2);

                if (eleName.equals("happening")) {
                    return addHappening(tree, eleData);
                } else if (eleName.equals("action")) {
                    return addAction(tree, eleData);
                } else if (eleName.equals("error")) {
                    return addError(tree, eleData);
                } else if (eleName.equals("ca-element")) {
                    return addCaElement(wNode, eleData);
                } else if (eleName.equals("ca-delimiter")) {
                    return addCaDelimiter(wNode, eleData);
                } else if (eleName.equals("underline")) {
                    return addUnderline(tree, eleData);
                } else if (eleName.equals("italic")) {
                    return addItalic(tree, eleData);
                } else if (eleName.equals("pause")) {
                    return addPause(tree, eleData);
                } else if (eleName.equals("internal-media")) {
                    return addInternalMedia(tree, eleData);
                } else if (eleName.equals("overlap-point")) {
                    return addOverlapPoint(tree, eleData);
                } else if (eleName.equals("long-feature-begin")) {
                    return addLongFeature(tree, "begin", eleData);
                } else if (eleName.equals("long-feature-end")) {
                    return addLongFeature(tree, "end", eleData);
                } else if (eleName.equals("nonvocal-begin")) {
                    return addNonvocal(tree, "begin", eleData);
                } else if(eleName.equals("nonvocal-end")) {
                    return addNonvocal(tree, "end", eleData);
                } else if(eleName.equals("nonvocal")) {
                    return addNonvocal(tree, "simple", eleData);
                } else if(eleName.equals("mediapic")) {
                    return addMediaPic(tree, eleData);
                } else {
                    return addGenericElement(tree, eleName, eleData);
                }
            }
        }
        return null;
    }

    public CommonTree addNonvocal(CommonTree parent, String type, String data) {
        CommonTree nvNode =
                AntlrUtils.createToken(talkbankTokens, "NONVOCAL_START");
        nvNode.setParent(parent);
        parent.addChild(nvNode);

        CommonTree nvTypeNode =
                AntlrUtils.createToken(talkbankTokens, "NONVOCAL_ATTR_TYPE");
        nvTypeNode.getToken().setText(type);
        nvTypeNode.setParent(nvNode);
        nvNode.addChild(nvTypeNode);

        addTextNode(nvNode, data);

        return nvNode;
    }

    /**
     * Add an overlap element
     */
    public CommonTree addOverlap(CommonTree parent, String ovdata) {
        CommonTree ovNode =
                AntlrUtils.createToken(talkbankTokens, "OVERLAP_START");
        ovNode.setParent(parent);
        parent.addChild(ovNode);

        final Pattern overlapPattern = Pattern.compile("([<>])([0-9]*)");
        final Matcher matcher = overlapPattern.matcher(ovdata);

        if (matcher.matches()) {
            String ovType = matcher.group(1);
            String actualType = "";
            if (ovType.equals(">")) {
                actualType = "overlap follows";
            } else if (ovType.equals("<")) {
                actualType = "overlap precedes";
            }
            CommonTree typeNode =
                    AntlrUtils.createToken(talkbankTokens, "OVERLAP_ATTR_TYPE");
            typeNode.getToken().setText(actualType);
            typeNode.setParent(ovNode);
            ovNode.addChild(typeNode);

            if (matcher.group(2) != null && matcher.group(2).length() > 0) {
                String ovIndex = matcher.group(2);

                CommonTree indexNode =
                        AntlrUtils.createToken(talkbankTokens, "OVERLAP_ATTR_INDEX");
                indexNode.getToken().setText(ovIndex);
                indexNode.setParent(ovNode);
                ovNode.addChild(indexNode);
            }
        }

        return ovNode;
    }

    public CommonTree addTagMarker(CommonTree parent, String data) {
        CommonTree tagMarkerTree = AntlrUtils.createToken(talkbankTokens, "TAGMARKER_START");
        tagMarkerTree.setParent(parent);
        parent.addChild(tagMarkerTree);

        CommonTree tagMarkerTypeTree = AntlrUtils.createToken(talkbankTokens, "TAGMARKER_ATTR_TYPE");
        tagMarkerTypeTree.setParent(tagMarkerTree);
        String type = switch (data) {
            case "," -> "comma";
            case "\u201e" -> "tag";
            case "\u2021" -> "vocative";
            default -> data;
        };
        tagMarkerTypeTree.getToken().setText(type);
        tagMarkerTree.addChild(tagMarkerTypeTree);

        return tagMarkerTree;
    }

    public CommonTree addSeparator(CommonTree parent, String type) {
        CommonTree sNode =
                AntlrUtils.createToken(talkbankTokens, "S_START");
        sNode.setParent(parent);
        parent.addChild(sNode);

        type = switch(type) {
            case ";" -> "semicolon";
            case ":" -> "colon";
            case "^c" -> "clause delimiter";
            case "\u21d7" -> "rising to high";
            case "\u2197" -> "rising to mid";
            case "\u2192" -> "level";
            case "\u2198" -> "falling to mid";
            case "\u21d8" -> "falling to low";
            case "\u221e" -> "unmarked ending";
            case "\u2261" -> "uptake";
            default -> type;
        };

        CommonTree sTypeNode =
                AntlrUtils.createToken(talkbankTokens, "S_ATTR_TYPE");
        sTypeNode.getToken().setText(type);
        sTypeNode.setParent(sNode);
        sNode.addChild(sTypeNode);

        return sNode;
    }

    public CommonTree addOverlapPoint(CommonTree parent, String data) {
        CommonTree opNode =
                AntlrUtils.createToken(talkbankTokens, "OVERLAP_POINT_START");
        opNode.setParent(parent);
        parent.addChild(opNode);

        final Pattern overlapPtPattern = Pattern.compile(OVERLAP_POINT_REGEX);
        final Matcher matcher = overlapPtPattern.matcher(data);

        String startEnd = "start";
        String topBottom = "top";
        int index = -1;
        if(matcher.matches()) {
            final String symbol = matcher.group(OVERLAP_POINT_SYMBOL_GROUP);
            startEnd = switch(symbol) {
                case "⌈", "⌊"  -> "start";
                case "⌉", "⌋" -> "end";
                default -> "";
            };
            topBottom = switch(symbol) {
                case "⌈", "⌉" -> "top";
                case "⌊", "⌋" -> "bottom";
                default -> "";
            };
            final String indexGrp = matcher.group(OVERLAP_POINT_INDEX_GROUP);
            index = indexGrp != null ? Integer.parseInt(indexGrp) : -1;
        } else {
            String[] attrs = data.split(",");
            if (attrs.length == 2) {
                startEnd = attrs[0];
                topBottom = attrs[1];
            }
        }

        CommonTree startEndNode =
                AntlrUtils.createToken(talkbankTokens, "OVERLAP_POINT_ATTR_START_END");
        startEndNode.getToken().setText(startEnd);
        startEndNode.setParent(opNode);
        opNode.addChild(startEndNode);

        CommonTree topBtmNode =
                AntlrUtils.createToken(talkbankTokens, "OVERLAP_POINT_ATTR_TOP_BOTTOM");
        topBtmNode.getToken().setText(topBottom);
        topBtmNode.setParent(opNode);
        opNode.addChild(topBtmNode);

        if (index >= 0) {
            CommonTree indexNode =
                    AntlrUtils.createToken(talkbankTokens, "OVERLAP_POINT_ATTR_INDEX");
            indexNode.getToken().setText(String.valueOf(index));
            indexNode.setParent(opNode);
            opNode.addChild(indexNode);
        }

        return opNode;
    }

    /**
     * Add a linker element
     */
    public CommonTree addLinker(CommonTree parent, String lkType) {
        CommonTree lkNode =
                AntlrUtils.createToken(talkbankTokens, "LINKER_START");
        lkNode.setParent(parent);

        if (parent.getChildCount() > 1) {

            int sIndex = 0;
            int cIndex = 0;
            CommonTree cNode = (CommonTree) parent.getChild(cIndex++);
            while (cNode != null && (cNode.getToken().getType() == talkbankTokens.getTokenType("U_ATTR_WHO")
                    || cNode.getToken().getType() == talkbankTokens.getTokenType("LINKER_START"))) {
                sIndex++;
                cNode = (CommonTree) parent.getChild(cIndex++);
            }

            List<CommonTree> nodes = new ArrayList<CommonTree>();
            for (int i = sIndex; i < parent.getChildCount(); i++) {
                nodes.add((CommonTree) parent.getChild(i));
            }
            parent.replaceChildren(sIndex, parent.getChildCount() - 1, lkNode);
            for (CommonTree c : nodes) parent.addChild(c);
        } else {
            parent.addChild(lkNode);
        }

        final String actualType = switch(lkType) {
            case "+\"" -> "quoted utterance next";
            case "+^" -> "quick uptake";
            case "+<" -> "lazy overlap mark";
            case "+," -> "self completion";
            case "++" -> "other completion";
            case "+\u224b" -> "technical break TCU completion";
            case "+\u2248" -> "no break TCU completion";
            default -> "";
        };
        CommonTree typeNode =
                AntlrUtils.createToken(talkbankTokens, "LINKER_ATTR_TYPE");
        typeNode.getToken().setText(actualType);
        typeNode.setParent(lkNode);
        lkNode.addChild(typeNode);

        return lkNode;
    }

    /**
     * Add a pause element
     */
    public CommonTree addPause(CommonTree parent, String data) {
        CommonTree pNode =
                AntlrUtils.createToken(talkbankTokens, "PAUSE_START");
        pNode.setParent(parent);
        parent.addChild(pNode);

        if (data.equals(".")) {
            CommonTree slNode =
                    AntlrUtils.createToken(talkbankTokens, "PAUSE_ATTR_SYMBOLIC_LENGTH");
            slNode.getToken().setText("simple");
            slNode.setParent(pNode);
            pNode.addChild(slNode);
        } else if (data.equals("..")) {
            CommonTree slNode =
                    AntlrUtils.createToken(talkbankTokens, "PAUSE_ATTR_SYMBOLIC_LENGTH");
            slNode.getToken().setText("long");
            slNode.setParent(pNode);
            pNode.addChild(slNode);

        } else if (data.equals("...")) {
            CommonTree slNode =
                    AntlrUtils.createToken(talkbankTokens, "PAUSE_ATTR_SYMBOLIC_LENGTH");
            slNode.getToken().setText("very long");
            slNode.setParent(pNode);
            pNode.addChild(slNode);
        } else {
            CommonTree slNode =
                    AntlrUtils.createToken(talkbankTokens, "PAUSE_ATTR_LENGTH");
            Float fVal = Float.parseFloat(data);
            slNode.getToken().setText(String.format("%.1f", fVal));
            slNode.setParent(pNode);
            pNode.addChild(slNode);
        }

        return pNode;
    }

    public CommonTree addDuration(CommonTree parent, String data) {
        Pattern pattern = Pattern.compile("# ([0-9.]+)(m?s)?");
        Matcher m = pattern.matcher(data);

        if(m.matches()) {
            String lengthText = m.group(1);
            String unit = m.group(2);

            Float length = Float.parseFloat(lengthText);
            MediaUnit mediaUnit = unit != null ? (unit.equals("ms") ? MediaUnit.Millisecond : MediaUnit.Second) : MediaUnit.Second;

            if(mediaUnit == MediaUnit.Millisecond) {
                length /= 1000.0f;
            }
            NumberFormat numberFormat = NumberFormat.getInstance();
            numberFormat.setMaximumFractionDigits(3);

            CommonTree durationNode = AntlrUtils.createToken(talkbankTokens, "DURATION_START");
            durationNode.setParent(parent);
            parent.addChild(durationNode);

            addTextNode(durationNode, numberFormat.format(length));

            return durationNode;
        } else {
            return null;
        }
    }

    /**
     * Add a marker <k> element
     */
    public CommonTree addMarker(CommonTree parent, String data) {
        String type = null;

        if (data.equals("!")) {
            type = "stressing";
        } else if (data.equals("!!")) {
            type = "contrastive stressing";
        } else if (data.equals("?")) {
            type = "best guess";
        } else if (data.equals("/")) {
            type = "retracing";
        } else if (data.equals("//")) {
            type = "retracing with correction";
        } else if (data.equals("///")) {
            type = "retracing reformulation";
        } else if (data.equals("/?")) {
            type = "retracing unclear";
        } else if (data.equals("/-")) {
            type = "false start";
        } else {
            LOGGER.warning("Invalid marker type '" + data + "'");
        }

        if (type != null) {
            CommonTree markerNode =
                    AntlrUtils.createToken(talkbankTokens, "K_START");
            markerNode.setParent(parent);
            parent.addChild(markerNode);

            CommonTree typeNode =
                    AntlrUtils.createToken(talkbankTokens, "K_ATTR_TYPE");
            typeNode.getToken().setText(type);
            typeNode.setParent(markerNode);
            markerNode.addChild(typeNode);

            return markerNode;
        }
        return null;
    }

    /**
     * Add a 'ga' element
     */
    public CommonTree addGa(CommonTree parent, String type, String data) {
        CommonTree gaNode =
                AntlrUtils.createToken(talkbankTokens, "GA_START");
        gaNode.setParent(parent);
        parent.addChild(gaNode);

        CommonTree gaTypeNode =
                AntlrUtils.createToken(talkbankTokens, "GA_ATTR_TYPE");
        gaTypeNode.getToken().setText(type);
        gaTypeNode.setParent(gaNode);
        gaNode.addChild(gaTypeNode);

        addTextNode(gaNode, data);

        return gaNode;
    }

    /**
     * Add a repetition element
     */
    public CommonTree addRepetition(CommonTree parent, String times) {
        CommonTree rNode =
                AntlrUtils.createToken(talkbankTokens, "R_START");
        rNode.setParent(parent);
        parent.addChild(rNode);

        CommonTree timesNode =
                AntlrUtils.createToken(talkbankTokens, "R_ATTR_TIMES");
        timesNode.getToken().setText(times);
        timesNode.setParent(rNode);
        rNode.addChild(timesNode);

        return rNode;
    }

    /**
     * Add a happening element
     */
    public CommonTree addHappening(CommonTree parent, String data) {
        CommonTree hNode =
                AntlrUtils.createToken(talkbankTokens, "HAPPENING_START");
        hNode.setParent(parent);
        parent.addChild(hNode);

        addTextNode(hNode, data);

        return hNode;
    }

    /**
     * Add an action element.
     */
    public CommonTree addAction(CommonTree parent, String data) {
        CommonTree aNode =
                AntlrUtils.createToken(talkbankTokens, "ACTION_START");
        aNode.setParent(parent);
        parent.addChild(aNode);

        if (data == null) data = "";
        addTextNode(aNode, data);

        return aNode;
    }

    /**
     * Add an error element
     */
    public CommonTree addError(CommonTree parent, String data) {
        CommonTree eNode =
                AntlrUtils.createToken(talkbankTokens, "ERROR_START");
        eNode.setParent(parent);
        parent.addChild(eNode);

        addTextNode(eNode, data);

        return eNode;
    }

    public CommonTree addCaElement(CommonTree parent, String type) {
        CommonTree caElementNode =
                AntlrUtils.createToken(talkbankTokens, "CA_ELEMENT_START");
        caElementNode.setParent(parent);
        parent.addChild(caElementNode);

        type = switch (type) {
            case "\u2260" -> "blocked segments";
            case "\u223e" -> "constriction";
            case "\u2219" -> "inhalation";
            case "\u1f29" -> "laugh in word";
            case "\u2193" -> "pitch down";
            case "\u21bb" -> "pitch reset";
            case "\u2191" -> "pitch up";
            case "\u02c8" -> "primary stress";
            case "\u02cc" -> "secondary stress";
            default -> type;
        };

        CommonTree caElementTypeNode =
                AntlrUtils.createToken(talkbankTokens, "CA_ELEMENT_ATTR_TYPE");
        caElementTypeNode.getToken().setText(type);
        caElementTypeNode.setParent(caElementNode);
        caElementNode.addChild(caElementTypeNode);

        return caElementNode;
    }

    public CommonTree addCaDelimiter(CommonTree parent, String data) {
        String[] info = data.split(",");
        if (info.length != 2)
            throw new IllegalArgumentException("ca-delimiter requires type and label");
        return addCaDelimiter(parent, info[0], info[1]);
    }

    public CommonTree addUnderline(CommonTree parent, String data) {
        CommonTree underlineNode =
                AntlrUtils.createToken(talkbankTokens, "UNDERLINE_START");
        underlineNode.setParent(parent);
        parent.addChild(underlineNode);

        CommonTree beginEndNode =
                AntlrUtils.createToken(talkbankTokens, "UNDERLINE_ATTR_TYPE");
        beginEndNode.setParent(underlineNode);
        underlineNode.addChild(beginEndNode);
        beginEndNode.getToken().setText(data);

        return underlineNode;
    }

    public CommonTree addItalic(CommonTree parent, String data) {
        CommonTree italicNode =
                AntlrUtils.createToken(talkbankTokens, "ITALIC_START");
        italicNode.setParent(parent);
        parent.addChild(italicNode);

        CommonTree beginEndNode =
                AntlrUtils.createToken(talkbankTokens, "ITALIC_ATTR_TYPE");
        beginEndNode.setParent(italicNode);
        italicNode.addChild(beginEndNode);
        beginEndNode.getToken().setText(data);

        return italicNode;
    }

    public CommonTree addInternalMedia(CommonTree parent, String data) {
        CommonTree imNode =
                AntlrUtils.createToken(talkbankTokens, "INTERNAL_MEDIA_START");
        imNode.setParent(parent);
        parent.addChild(imNode);

        String[] range = data.split("-");
        if(range.length != 2) throw new IllegalArgumentException(data);

        String startVal = range[0];
        if(startVal.matches(MS_DISPLAY_REGEX)) {
            try {
                startVal = Float.toString(MsFormatter.displayStringToMs(startVal)/1000.0f);
            } catch (ParseException e) {
                throw new IllegalArgumentException(startVal);
            }
        }
        String endVal = range[1];
        if(endVal.matches(MS_DISPLAY_REGEX)) {
            try {
                endVal = Float.toString(MsFormatter.displayStringToMs(endVal)/1000.0f);
            } catch (ParseException e) {
                throw new IllegalArgumentException(endVal);
            }
        }

        CommonTree startAttrNode =
                AntlrUtils.createToken(talkbankTokens, "INTERNAL_MEDIA_ATTR_START");
        startAttrNode.getToken().setText(startVal);
        imNode.addChild(startAttrNode);
        startAttrNode.setParent(imNode);

        CommonTree endAttrNode =
                AntlrUtils.createToken(talkbankTokens, "INTERNAL_MEDIA_ATTR_END");
        endAttrNode.getToken().setText(endVal);
        imNode.addChild(endAttrNode);
        endAttrNode.setParent(imNode);

        CommonTree unitNode =
                AntlrUtils.createToken(talkbankTokens, "INTERNAL_MEDIA_ATTR_UNIT");
        unitNode.getToken().setText("s");
        unitNode.setParent(imNode);
        imNode.addChild(unitNode);

        return imNode;
    }

    /**
     * Phon: (eleName,attr=val: data)
     *
     * @param parent
     * @param eleName
     * @param eleData may be <code>null</code>
     */
    public CommonTree addGenericElement(CommonTree parent, String eleName, String eleData) {
        String[] eleparts = eleName.split(",");
        eleName = eleparts[0].trim();

        String tokenName = eleName
                .replaceAll("-", "_")
                .replaceAll("\\p{Space}", "_")
                .toUpperCase();
        String startTokenName = tokenName + "_START";
        CommonTree eleTree = AntlrUtils.createToken(talkbankTokens, startTokenName);
        eleTree.setParent(parent);
        parent.addChild(eleTree);

        for (int i = 1; i < eleparts.length; i++) {
            // setup attributes
            String[] keyVal = eleparts[i].split("=");
            // assign 'type' attribute by default
            String attrName = (keyVal.length == 1 ? keyVal[0] : "type").trim();
            String attrVal = (keyVal.length == 2 ? keyVal[1] : keyVal[0]).trim();

            String attrTokenName = tokenName + "_ATTR_" + attrName.toUpperCase();
            CommonTree attrToken = AntlrUtils.createToken(talkbankTokens, attrTokenName);
            attrToken.getToken().setText(attrVal);
            eleTree.addChild(attrToken);
            attrToken.setParent(eleTree);
        }

        if (eleData != null && eleData.trim().length() > 0) {
            addTextNode(eleTree,
                    StringEscapeUtils.escapeXml(eleData.trim().replaceAll("\\\\\\(", "(").replaceAll("\\\\\\)", ")")));
        }

        return eleTree;
    }

    /**
     * add a text node
     */
    public CommonTree addTextNode(CommonTree parent, String data) {
        CommonTree txtNode =
                AntlrUtils.createToken(talkbankTokens, "TEXT");
        txtNode.getToken().setText(StringEscapeUtils.escapeXml(data));
        txtNode.setParent(parent);
        parent.addChild(txtNode);
        return txtNode;
    }

    public CommonTree addCaDelimiter(CommonTree parent, String type, String label) {
        CommonTree caDelimNode =
                AntlrUtils.createToken(talkbankTokens, "CA_DELIMITER_START");
        caDelimNode.setParent(parent);
        parent.addChild(caDelimNode);

        CommonTree caDelimBeginEndNode =
                AntlrUtils.createToken(talkbankTokens, "CA_DELIMITER_ATTR_TYPE");
        caDelimBeginEndNode.getToken().setText(type);
        caDelimBeginEndNode.setParent(caDelimNode);
        caDelimNode.addChild(caDelimBeginEndNode);

        label = switch (label) {
            case "\u264b" -> "breathy voice";
            case "\u204e" -> "creaky";
            case "\u2206" -> "faster";
            case "\u2594" -> "high-pitch";
            case "\u25c9" -> "louder";
            case "\u2581" -> "low-pitch";
            case "\u00a7" -> "precise";
            case "\u21ab" -> "repeated-segment";
            case "\u222e" -> "singing";
            case "\u2207" -> "slower";
            case "\u263a" -> "smile voice";
            case "\u00b0" -> "softer";
            case "\u2047" -> "unsure";
            case "\u222c" -> "whisper";
            case "\u03ab" -> "yawn";
            default -> label;
        };

        CommonTree caDelimTypeNode =
                AntlrUtils.createToken(talkbankTokens, "CA_DELIMITER_ATTR_LABEL");
        caDelimTypeNode.getToken().setText(label);
        caDelimTypeNode.setParent(caDelimNode);
        caDelimNode.addChild(caDelimTypeNode);

        return caDelimNode;
    }

    /**
     * Add a shortening element
     */
    public CommonTree addShortening(CommonTree parent, String data) {
        CommonTree shNode =
                AntlrUtils.createToken(talkbankTokens, "SHORTENING_START");
        shNode.setParent(parent);
        addTextNode(shNode, data);

        parent.addChild(shNode);

        return shNode;
    }

    public CommonTree addLongFeature(CommonTree parent, String type, String data) {
        CommonTree lfNode =
                AntlrUtils.createToken(talkbankTokens, "LONG_FEATURE_START");
        lfNode.setParent(parent);
        parent.addChild(lfNode);

        CommonTree lfTypeNode =
                AntlrUtils.createToken(talkbankTokens, "LONG_FEATURE_ATTR_TYPE");
        lfTypeNode.getToken().setText(type);
        lfTypeNode.setParent(lfNode);
        lfNode.addChild(lfTypeNode);

        addTextNode(lfNode, data);

        return lfNode;
    }

    public CommonTree addUnderlineBefore(CommonTree parent, boolean isStart) {
        CommonTree uNode =
                AntlrUtils.createToken(talkbankTokens, "UNDERLINE_START");
        CommonTree realParent = (CommonTree) parent.getParent();
        uNode.setParent(parent);
        int idx = realParent.getChildren().indexOf(parent);
        realParent.setChild(idx, uNode);
        realParent.addChild(parent);

        CommonTree uTypeNode =
                AntlrUtils.createToken(talkbankTokens, "UNDERLINE_ATTR_TYPE");
        uTypeNode.setParent(uNode);
        uTypeNode.getToken().setText(isStart ? "begin" : "end");
        uNode.addChild(uTypeNode);

        return uNode;
    }

    public CommonTree addUnderline(CommonTree parent, boolean isStart) {
        CommonTree uNode =
                AntlrUtils.createToken(talkbankTokens, "UNDERLINE_START");
        uNode.setParent(parent);

        CommonTree uTypeNode =
                AntlrUtils.createToken(talkbankTokens, "UNDERLINE_ATTR_TYPE");
        uTypeNode.setParent(uNode);
        uTypeNode.getToken().setText(isStart ? "begin" : "end");
        uNode.addChild(uTypeNode);

        parent.addChild(uNode);

        return uNode;
    }

    /**
     * Add a postcode element
     */
    public void addPostcode(CommonTree parent, Tier<TierString> data) {
        if (data.numberOfGroups() == 0 || data.getGroup(0).trim().length() == 0) return;

        final String postcodeRegex = "\\([^)]+\\)( \\([^)]+\\))*";
        final String postcodeData = data.getGroup(0).toString().trim();
        if(postcodeData.matches(postcodeRegex)) {
            final Pattern postcodePattern = Pattern.compile("\\(([^)]+)\\)");
            final Matcher matcher = postcodePattern.matcher(postcodeData);
            while(matcher.find()) {
                CommonTree pcNode =
                        AntlrUtils.createToken(talkbankTokens, "POSTCODE_START");
                pcNode.setParent(parent);
                parent.addChild(pcNode);
                addTextNode(pcNode, matcher.group(1));
            }
        } else {
            CommonTree pcNode =
                    AntlrUtils.createToken(talkbankTokens, "POSTCODE_START");
            pcNode.setParent(parent);
            parent.addChild(pcNode);
            addTextNode(pcNode, postcodeData);
        }
    }

    public CommonTree addProsody(CommonTree parent, String type) {
        CommonTree prosodyNode =
                AntlrUtils.createToken(talkbankTokens, "P_START");
        prosodyNode.setParent(parent);
        parent.addChild(prosodyNode);

        type = switch (type) {
            case ":" -> "drawl";
            case "^" -> "pause";
            default -> type;
        };

        CommonTree pTypeNode =
                AntlrUtils.createToken(talkbankTokens, "P_ATTR_TYPE");
        pTypeNode.getToken().setText(type);
        pTypeNode.setParent(prosodyNode);
        prosodyNode.addChild(pTypeNode);

        return prosodyNode;
    }

    public void addDependentTierContent(CommonTree parent, String data) {
        final Pattern mediaPattern = Pattern.compile(INTERNAL_MEDIA_REGEX);
        final Pattern mediaPicPattern = Pattern.compile(("\\(mediapic:(.+)\\)"));

        final StringBuilder builder = new StringBuilder();
        final String[] parts = data.split("\\s");
        final Formatter<MediaSegment> segmentFormatter = FormatterFactory.createFormatter(MediaSegment.class);

        for (String part : parts) {
            final Matcher matcher = mediaPattern.matcher(part);
            final Matcher mediapicMatcher = mediaPicPattern.matcher(part);
            if (matcher.matches()) {
                if (!builder.isEmpty()) {
                    addTextNode(parent, builder.toString());
                    builder.setLength(0);
                }
                try {
                    String segmentStr = String.format("%s-%s",
                            matcher.group(INTERNAL_MEDIA_START_GROUP), matcher.group(INTERNAL_MEDIA_END_GROUP));
                    final MediaSegment segment = segmentFormatter.parse(segmentStr);
                    addMedia(parent, segment);
                } catch (ParseException e) {
                    addTextNode(parent, StringEscapeUtils.escapeXml(part));
                }
            } else if(mediapicMatcher.matches()) {
                if (!builder.isEmpty()) {
                    addTextNode(parent, builder.toString() + " ");
                    builder.setLength(0);
                }
                addMediaPic(parent, mediapicMatcher.group(1));
            } else {
                if (builder.length() > 0)
                    builder.append(' ');
                builder.append(part);
            }
        }
        if (!builder.isEmpty()) {
            addTextNode(parent, builder.toString());
        }
    }

    /**
     * Add a media element
     */
    public CommonTree addMedia(CommonTree parent, MediaSegment media) {
        // don't add media if len = 0
        float len = media.getEndValue() - media.getStartValue();
        if (len == 0.0f) {
            return null;
        }

        CommonTree mediaNode =
                AntlrUtils.createToken(talkbankTokens, "MEDIA_START");
        mediaNode.setParent(parent);
        parent.addChild(mediaNode);

        // we need to convert our units to s from ms
        float startS = media.getStartValue() / 1000.0f;
        float endS = media.getEndValue() / 1000.0f;

        CommonTree unitNode =
                AntlrUtils.createToken(talkbankTokens, "MEDIA_ATTR_UNIT");
        unitNode.getToken().setText("s");
        unitNode.setParent(mediaNode);
        mediaNode.addChild(unitNode);

        CommonTree startNode =
                AntlrUtils.createToken(talkbankTokens, "MEDIA_ATTR_START");
        startNode.getToken().setText(String.valueOf(startS));
        startNode.setParent(mediaNode);
        mediaNode.addChild(startNode);

        CommonTree endNode =
                AntlrUtils.createToken(talkbankTokens, "MEDIA_ATTR_END");
        endNode.getToken().setText(String.valueOf(endS));
        endNode.setParent(mediaNode);
        mediaNode.addChild(endNode);

        return mediaNode;
    }

    public CommonTree addMediaPic(CommonTree parent, String data) {
        CommonTree mediapicNode =
                AntlrUtils.createToken(talkbankTokens, "MEDIAPIC_START");
        mediapicNode.setParent(parent);
        parent.addChild(mediapicNode);

        CommonTree mediapicHrefNode =
                AntlrUtils.createToken(talkbankTokens, "MEDIAPIC_ATTR_HREF");
        mediapicHrefNode.getToken().setText(data);
        mediapicHrefNode.setParent(mediapicNode);
        mediapicNode.addChild(mediapicHrefNode);

        return mediapicNode;
    }


    /**
     * Add a terminator
     */
    public CommonTree addTerminator(CommonTree parent, String type) {
        CommonTree tNode =
                AntlrUtils.createToken(talkbankTokens, "T_START");
        tNode.setParent(parent);
        parent.addChild(tNode);

        type = switch (type) {
            case "." -> "p";
            case "?" -> "q";
            case "!" -> "e";
            case "+." -> "broken for coding";
            case "+..." -> "trail off";
            case "+..?" -> "trail off question";
            case "+!?" -> "question exclamation";
            case "+/." -> "interruption";
            case "+/?" -> "interruption question";
            case "+//." -> "self interruption";
            case "+//?" -> "self interruption question";
            case "+\"/." -> "quotation next line";
            case "+\"." -> "quotation precedes";
            case "" -> "missing CA terminator";
            case "\u224b" -> "technical break TCU continuation";
            case "\u2248" -> "no break TCU continuation";
            default -> type;
        };

        CommonTree ttNode =
                AntlrUtils.createToken(talkbankTokens, "T_ATTR_TYPE");
        ttNode.getToken().setText(type);
        ttNode.setParent(tNode);
        tNode.addChild(ttNode);

        return tNode;
    }

    /**
     * Add a replacement element
     */
    public CommonTree addReplacement(CommonTree parent, String data) {
        CommonTree rNode =
                AntlrUtils.createToken(talkbankTokens, "REPLACEMENT_START");
        rNode.setParent(parent);
        parent.addChild(rNode);

        try {
            OrthographyTreeBuilder innerBuilder = new OrthographyTreeBuilder();
            Orthography replacementOrtho = Orthography.parseOrthography(data);
            CommonTree fakeU = AntlrUtils.createToken(talkbankTokens, "U_START");
            Stack<CommonTree> fakeUStack = new Stack<>();
            fakeUStack.push(fakeU);
            innerBuilder.buildTree(fakeUStack, rNode, replacementOrtho);
        } catch (ParseException e) {
            LOGGER.warning(e.getLocalizedMessage());
        }

        return rNode;
    }

    public CommonTree addFreecode(CommonTree parent, String data) {
        CommonTree freecodeNode =
                AntlrUtils.createToken(talkbankTokens, "FREECODE_START");
        freecodeNode.setParent(parent);
        parent.addChild(freecodeNode);

        Pattern pattern = Pattern.compile("\\^\\s(.+)");
        Matcher m = pattern.matcher(data);
        if(m.matches()) {
            data = m.group(1);
        }
        addTextNode(freecodeNode, data);

        return freecodeNode;
    }

    /**
     * Add other spoken event
     */
    public CommonTree addOtherSpokenEvent(CommonTree parent, String speaker, String data) {
        CommonTree oseNode =
                AntlrUtils.createToken(talkbankTokens, "OTHERSPOKENEVENT_START");
        oseNode.setParent(parent);
        parent.addChild(oseNode);

        CommonTree whoNode =
                AntlrUtils.createToken(talkbankTokens, "OTHERSPOKENEVENT_ATTR_WHO");
        whoNode.getToken().setText(speaker);
        whoNode.setParent(oseNode);
        oseNode.addChild(whoNode);

        CommonTree saidNode =
                AntlrUtils.createToken(talkbankTokens, "OTHERSPOKENEVENT_ATTR_SAID");
        saidNode.getToken().setText(data);
        saidNode.setParent(oseNode);
        oseNode.addChild(saidNode);

        return oseNode;
    }

    /**
     * Add a langs element
     */
    public CommonTree addLangs(CommonTree parent, String data) {
        /*
         * Langs format is:
         *  (single|multiple|ambiguous),<lang data>
         */
        int cIndex = data.indexOf(',');
        if (cIndex < 0) return null;

        String langsType = data.substring(0, cIndex);
        String langsData = data.substring(cIndex + 1);

        CommonTree langsNode =
                AntlrUtils.createToken(talkbankTokens, "LANGS_START");
        langsNode.setParent(parent);
        parent.addChild(langsNode);

        CommonTree dataNode = null;
        if (langsType.equals("single")) {
            dataNode =
                    AntlrUtils.createToken(talkbankTokens, "SINGLE_START");
        } else if (langsType.equals("multiple")) {
            dataNode =
                    AntlrUtils.createToken(talkbankTokens, "MULTIPLE_START");
        } else if (langsType.equals("ambiguous")) {
            dataNode =
                    AntlrUtils.createToken(talkbankTokens, "AMBIGUOUS_START");
        } else {
            // just make it ambiguous
            dataNode =
                    AntlrUtils.createToken(talkbankTokens, "AMBIGUOUS_START");
        }
        addTextNode(dataNode, langsData);
        dataNode.setParent(langsNode);
        langsNode.addChild(dataNode);

        return langsNode;
    }

    /**
     * wk
     */
    public CommonTree addWordnet(CommonTree parent, String type) {
        CommonTree wkNode =
                AntlrUtils.createToken(talkbankTokens, "WK_START");
        wkNode.setParent(parent);

        CommonTree typeNode =
                AntlrUtils.createToken(talkbankTokens, "WK_ATTR_TYPE");
        typeNode.getToken().setText(type);
        typeNode.setParent(wkNode);
        wkNode.addChild(typeNode);

        parent.addChild(wkNode);

        return wkNode;
    }

    public CommonTree addPos(CommonTree wParent, String pos) {
        CommonTree posNode =
                AntlrUtils.createToken(talkbankTokens, "POS_START");
        posNode.setParent(wParent);

        CommonTree cNode =
                AntlrUtils.createToken(talkbankTokens, "C_START");
        addTextNode(cNode, pos);
        cNode.setParent(posNode);
        posNode.addChild(cNode);

        wParent.addChild(posNode);

        return posNode;
    }

}
