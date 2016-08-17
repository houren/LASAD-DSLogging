package lasad.gwt.client.ui.box.helper;

import lasad.gwt.client.ui.box.AbstractBox;
import lasad.gwt.client.ui.workspace.graphmap.GraphMap;

import com.extjs.gxt.ui.client.event.Events;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

public abstract class AbstractDelElementFromBoxDialogListener implements EventListener {

//	private final LASADActionSender communicator = LASADActionSender.getInstance();
//	private final ActionFactory actionBuilder = ActionFactory.getInstance();

	protected GraphMap myMap;
	private AbstractDelElementFromBoxDialog myDialog;

	public AbstractDelElementFromBoxDialogListener(GraphMap map, AbstractBox box,
			AbstractDelElementFromBoxDialog DelElementfromBoxDialog) {
		this.myMap = map;
		this.myDialog = DelElementfromBoxDialog;
	}

	public void onBrowserEvent(Event be) {
		if (be.getTypeInt() == Events.OnMouseOver.getEventCode()) {
			handleMouseOver(be);
		} else if (be.getTypeInt() == Events.OnClick.getEventCode()) {
			handleOnClick(be);
		} else if (be.getTypeInt() == Events.OnMouseOut.getEventCode()) {
			handleMouseOut(be);

		}
		be.stopPropagation();
	}

	private void handleOnClick(Event be) {
		// Get childelement with ElementIDs
		Element clickedElement = (Element) be.getEventTarget().cast();

		Element childTypeElement = clickedElement.getFirstChildElement();
		Element childIDElement = null;
		if (childTypeElement != null) {
			childIDElement = childTypeElement.getNextSiblingElement();
		}
		if (childTypeElement != null && childIDElement != null) {
			// Extract elementID
			String elementID = childIDElement.getInnerText();

//			communicator.sendActionPackage(actionBuilder.removeElement(myMap
//					.getID(), Integer.parseInt(elementID)));
			onClickSendUpdate2Sever(myMap.getID(), Integer.parseInt(elementID));

		}
		// else Cancel Element

		myDialog.removeFromParent();
	}
	protected abstract void onClickSendUpdate2Sever(String mapId, int elementId);

	private void handleMouseOut(Event be) {
		// End hover effect
		if (((Element) be.getEventTarget().cast()).getClassName().equals(
		"dialog-text-highlighted")) {
			((Element) be.getEventTarget().cast()).setClassName("dialog-text");
		}
	}

	private void handleMouseOver(Event be) {
		// Start hover effect
		if (((Element) be.getEventTarget().cast()).getClassName().equals(
		"dialog-text")) {
			((Element) be.getEventTarget().cast())
			.setClassName("dialog-text-highlighted");
		}
	}
}