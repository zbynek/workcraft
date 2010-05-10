/**
 *
 */
package org.workcraft.plugins.desij;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.w3c.dom.Element;
import org.workcraft.util.FileUtils;
import org.workcraft.util.XmlUtil;

/**
 * @author Dominic Wist
 *
 */
public class DesiJSettings {
	// related to logicSynthesiser member
	public static final int SYN_PETRIFY = 0;
	public static final int SYN_MPSAT = 1;

	// DesiJ Option Types

	public enum DecompositionStrategy {
		BASIC,
		TREE,
		LAZYSINGLE,
		LAZYMULTI
	}

	public enum PartitionMode {
		FINEST,
		BEST,
		CUSTOM
	}

	public enum PlaceHandling {
		LOOP_DUPLICATE,
		SHORTCUT,
		IMPLICIT
	}

	public enum ContractionMode {
		SAFENESS_PRESERVING,
		OUT_DET,
		RISKY
	}

	public enum SynthesisOptions {
		CSC_AWARE,
		INT_COM
	}

	// Possible DesiJ Options

	private DesiJOperation operation;

	private DecompositionStrategy decoStrategy;
	private int aggregationFactor;

	private PartitionMode partitionMode;
	private String partition;

	private PlaceHandling placeDeletion;

	private ContractionMode contractionMode;

	private boolean postSynthesis;
	private int logicSynthesiser;
	private SynthesisOptions synthesisOptions;

	// ******** Constructors to create a DesiJSetting *******************

	public DesiJSettings(DesiJOperation operation,
			DecompositionStrategy decoStrategy, int aggregationFactor,
			PartitionMode partitionMode, String partition,
			PlaceHandling placeDeletion, ContractionMode contractionMode,
			boolean postSynthesis, int synthesiser, SynthesisOptions synOptions) {
		super();
		this.operation = operation;
		this.decoStrategy = decoStrategy;
		this.aggregationFactor = aggregationFactor;
		this.partitionMode = partitionMode;
		this.partition = partition;
		this.placeDeletion = placeDeletion;
		this.contractionMode = contractionMode;
		this.postSynthesis = postSynthesis;
		this.logicSynthesiser = synthesiser;
		this.synthesisOptions = synOptions;
	}

	public DesiJSettings(Element element) {
		operation = DesiJOperation.getOperation(element.getAttribute("operation"));

		decoStrategy = DecompositionStrategy.valueOf(XmlUtil.readStringAttr(element, "decoStrategy"));
		aggregationFactor = XmlUtil.readIntAttr(element, "aggregationFactor", 1);

		partitionMode = PartitionMode.valueOf(XmlUtil.readStringAttr(element, "partitionMode"));
		Element part = XmlUtil.getChildElement("partition", element);
		partition = part.getTextContent();

		placeDeletion = PlaceHandling.valueOf(XmlUtil.readStringAttr(element, "placeDeletion"));

		contractionMode = ContractionMode.valueOf(XmlUtil.readStringAttr(element, "contractionMode"));

		postSynthesis = XmlUtil.readBoolAttr(element, "postSynthesis");
		logicSynthesiser = XmlUtil.readIntAttr(element, "logicSynthesiser", 0);
		synthesisOptions = SynthesisOptions.valueOf(XmlUtil.readStringAttr(element, "synthesisOptions"));
	}

	/**
	 * to serialise the Options into an XML file
	 * @param parent
	 */
	public void toXML(Element parent) {
		Element e = parent.getOwnerDocument().createElement("settings");
		e.setAttribute("operation", operation.getArgument());

		e.setAttribute("decoStrategy", decoStrategy.name());
		e.setAttribute("aggregationFactor", Integer.toString(aggregationFactor));

		e.setAttribute("partitionMode", partitionMode.name());
		Element part = parent.getOwnerDocument().createElement("partition");
		part.setTextContent(partition);
		e.appendChild(part);

		e.setAttribute("placeDeletion", placeDeletion.name());

		e.setAttribute("contractionMode", contractionMode.name());

		e.setAttribute("postSynthesis", Boolean.toString(postSynthesis));
		e.setAttribute("logicSynthesiser", Integer.toString(logicSynthesiser));
		e.setAttribute("synthesisOptions", synthesisOptions.name());

		parent.appendChild(e);
	}

	//  ***********  Getters for all Options ***************

	public DesiJOperation getOperation() {
		return operation;
	}

	public DecompositionStrategy getDecoStrategy() {
		return decoStrategy;
	}

	public int getAggregationFactor() {
		return aggregationFactor;
	}

	public PartitionMode getPartitionMode() {
		return partitionMode;
	}

	public String getPartition() {
		return partition;
	}

	public PlaceHandling getPlaceHandlingStrategy () {
		return placeDeletion;
	}

	public ContractionMode getContractionMode() {
		return contractionMode;
	}

	public boolean getPostSynthesisOption() {
		return postSynthesis;
	}

	public int getSynthesiser() {
		return logicSynthesiser;
	}

	public SynthesisOptions getSynthesisOptions() {
		return synthesisOptions;
	}

	// for building the command line string as parameter for DesiJ

	/**
	 * for building the command line string as parameter for DesiJ
	 * @return - only the arguments exluding "desij"
	 */
	public String[] getDesiJArguments() {
		ArrayList<String> args = new ArrayList<String>();
		args.add(getOperation().getArgument());

		if (getOperation().usesContraction()) // irgendwas custom parition muss hierhin
			try {
				File part = File.createTempFile("partitionFile", null);
				part.deleteOnExit();
				FileUtils.dumpString(part, getPartition());
				args.add("-d");
				args.add("@"+part.getCanonicalPath());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		args.add(String.format("-$%d", getSynthesiser()));

		switch (getDecoStrategy()) {
		case BASIC:
			break;
		case TREE:
			args.add("-f");
			break;
		case LAZYSINGLE:
			int aggregationFactor = getAggregationFactor();
			if (aggregationFactor>0)
				args.add("-a" + Integer.toString(aggregationFactor));
			else
				args.add("-a");
		case LAZYMULTI:
			args.add("-f");
			break;
		}

		return args.toArray(new String[args.size()]);
	}

}
