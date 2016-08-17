package lasad.gwt.client.ui.common.argument.elements;

import java.util.Vector;

import lasad.gwt.client.communication.LASADActionSender;
import lasad.gwt.client.communication.helper.ActionFactory;
import lasad.gwt.client.model.ElementInfo;
import lasad.gwt.client.ui.common.ExtendedElementContainerInterface;
import lasad.gwt.client.ui.common.elements.AbstractExtendedRadioButtonElement;

public class ExtendedRadioButtonElementArgument extends
		AbstractExtendedRadioButtonElement {
	
	protected final LASADActionSender communicator = LASADActionSender.getInstance();
	protected final ActionFactory actionBuilder = ActionFactory.getInstance();

	public ExtendedRadioButtonElementArgument(boolean readonly,
			ExtendedElementContainerInterface container, ElementInfo config) {
		super(readonly, container, config);
	}

	public ExtendedRadioButtonElementArgument(
			ExtendedElementContainerInterface container, ElementInfo config,
			boolean _mode) {
		super(container, config, _mode);
	}

	@Override
	public void updateElementWithMultipleValues(String mapID, int id,
			Vector<Object[]> values) {
		communicator.sendActionPackage(actionBuilder.updateElementWithMultipleValues(mapID, id, values));
	}

}
