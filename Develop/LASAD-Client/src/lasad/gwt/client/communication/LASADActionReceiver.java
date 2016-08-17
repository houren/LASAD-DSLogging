package lasad.gwt.client.communication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import lasad.gwt.client.LASAD_Client;
import lasad.gwt.client.communication.helper.ActionFactory;
import lasad.gwt.client.communication.processors.FeedbackAuthoringActionProcessor;
import lasad.gwt.client.constants.lasad_clientConstants;
import lasad.gwt.client.importer.ARGUNAUT.ArgunautParser;
import lasad.gwt.client.importer.LARGO.LARGOParser;
import lasad.gwt.client.logger.Logger;
import lasad.gwt.client.model.AbstractUnspecifiedElementModel;
import lasad.gwt.client.model.ElementInfo;
import lasad.gwt.client.model.argument.MVCViewSession;
import lasad.gwt.client.model.argument.MVController;
import lasad.gwt.client.model.argument.MapInfo;
import lasad.gwt.client.model.argument.UnspecifiedElementModelArgument;
import lasad.gwt.client.model.events.LasadEvent;
import lasad.gwt.client.model.organization.ArgumentModel;
import lasad.gwt.client.model.organization.ArgumentThread;
// Added for autoOrganizer support by Kevin Loughlin
import lasad.gwt.client.model.organization.AutoOrganizer;
import lasad.gwt.client.model.organization.LinkedBox;
import lasad.gwt.client.model.organization.OrganizerLink;
import lasad.gwt.client.ui.LASADStatusBar;
import lasad.gwt.client.ui.box.AbstractBox;
import lasad.gwt.client.ui.replay.ReplayInitializer;
import lasad.gwt.client.ui.workspace.LASADInfo;
import lasad.gwt.client.ui.workspace.argumentmap.ArgumentMap;
import lasad.gwt.client.ui.workspace.argumentmap.ArgumentMapMVCViewSession;
import lasad.gwt.client.ui.workspace.argumentmap.ArgumentMapMenuBar;
import lasad.gwt.client.ui.workspace.feedback.argument.FeedbackPanelArgument;
import lasad.gwt.client.ui.workspace.graphmap.AbstractGraphMap;
import lasad.gwt.client.ui.workspace.loaddialogues.ImportingMapDialogue;
import lasad.gwt.client.ui.workspace.loaddialogues.LoadingLoginDialogue;
import lasad.gwt.client.ui.workspace.loaddialogues.LoadingMapDialogue;
import lasad.gwt.client.ui.workspace.loaddialogues.LoadingMapFromFileDialogue;
import lasad.gwt.client.ui.workspace.loaddialogues.LoadingReplayDialogue;
import lasad.gwt.client.ui.workspace.loaddialogues.ReloadingMapsDialogue;
import lasad.gwt.client.ui.workspace.tableview.ArgumentEditionStyleEnum;
import lasad.gwt.client.ui.workspace.tableview.ArgumentMapTableMVCViewSession;
import lasad.gwt.client.ui.workspace.tabs.LoginTab;
import lasad.gwt.client.ui.workspace.tabs.MapTab;
import lasad.gwt.client.ui.workspace.tabs.authoring.AuthoringTab;
import lasad.gwt.client.ui.workspace.tabs.authoring.steps.CreateAndDeleteSessions;
import lasad.gwt.client.ui.workspace.tabs.authoring.steps.CreateAndDeleteTemplate;
import lasad.gwt.client.ui.workspace.tabs.authoring.steps.CreateAndDeleteUsers;
import lasad.gwt.client.ui.workspace.tabs.authoring.steps.CreateModifyAndDeleteOntology;
import lasad.gwt.client.ui.workspace.tabs.map.JoinSessionContentPanel;
import lasad.gwt.client.ui.workspace.tabs.map.MapLoginTab;
import lasad.gwt.client.xml.OntologyReader;
import lasad.shared.communication.objects.Action;
import lasad.shared.communication.objects.ActionPackage;
import lasad.shared.communication.objects.Parameter;
import lasad.shared.communication.objects.categories.Categories;
import lasad.shared.communication.objects.commands.Commands;
import lasad.shared.communication.objects.parameters.ParameterTypes;

import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.xml.client.impl.DOMParseException;



/**
 * Manages all incoming actions from the servlet and, hence, from the EJB server Uses Singleton Pattern Refactoring Step 1 by
 * Sabine Niebuhr
 * Refactoring Step 1 by Sabine Niebuhr
 */
public class LASADActionReceiver {
	
	private final String BOX = "box";
	private final String RELATION = "relation";
	private final int DEFAULT_HEIGHT = 107;

	lasad_clientConstants myConstants = GWT.create(lasad_clientConstants.class);
	private static LASADActionReceiver myInstance = null;

	// David Drexler Edit-BEGIN
	private TreeMap<Integer, ActionPackage> treeForwReplay;
	private TreeMap<String, List<Integer>> treeUserReplay;
	private List<Integer> elementReplay;
	private ReplayInitializer init;

	private static int counterID = -1;

	// David Drexler Edit-END

	public static LASADActionReceiver getInstance() {
		if (myInstance == null) {
			myInstance = new LASADActionReceiver();
		}
		return myInstance;
	}

	private LASADActionReceiver()
	{
	}

	public void doActionPackage(ActionPackage p) {

		Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][doActionPackage] ActionPackage arrived with " + p.getActions().size() + " Actions", Logger.DEBUG);

		//Logger.log("[Details]: \n" + p.toString(), Logger.DEBUG);

		for (Action a : p.getActions()) {
			// Sort Action parameters so that USERNAME is at Position 0
			// Necessary for Highlighting
			a.sortParameterUsername();

			// Check if the given MapID relates to an controller or not

			//Logger.log(a.toString(), Logger.DEBUG_DETAILS);

			String mapIDString = a.getParameterValue(ParameterTypes.MapId);

			MVController controller = null;
			if (mapIDString != null) {
				controller = LASAD_Client.getMVCController(mapIDString);
			}
//			TODO anschauen, was mit Categories.Info ist...
			if (controller != null&&a.getCategory()!=Categories.Info) {
				// Valid controller found.
				Logger.log("--> workOnMVCControlledAction", Logger.DEBUG);
				workOnMVCControlledAction(controller, a);
			} else {
				// No valid controller found.

				Logger.log("--> workOnGlobalAction", Logger.DEBUG);
				workOnGlobalAction(a);
			}
		}
	}

	/**
	 * Work on ActionSets with valid/registered MAPIDs
	 * 
	 * @param controller
	 * @param actionSet
	 */
	private void workOnMVCControlledAction(MVController controller, Action a) {

		Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][workOnMVCControlledAction], Category: " + a.getCategory().toString(), Logger.DEBUG);
//		Logger.log("workOnMVCControlledAction entered, Category: " + a.getCategory(), Logger.DEBUG);

//		Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][workOnMVCControlledAction] Action arrived: \n" + a.toString(),
//				Logger.DEBUG);

		switch (a.getCategory()){
		case Heartbeat:
			processHeartbeatAction(controller, a);
			break;
		case Map:
			processMapAction(controller, a);
			break;
		case Management:
			processManagementAction(controller, a);
			break;
		case Communication:
			processChatAction(controller, a);
			break;
		case UserEvent:
			processUserEventAction(controller, a);
			break;
		case Questionnaire:
			processQuestionnaireAction(controller, a);
			break;
		default:
			Logger.log(
					"[lasad.gwt.client.communication.LASADActionReceiver][workOnMVCControlledActionPackage] Error: Unknown action category!",
					Logger.DEBUG);
		}
			
		// Set new Revision to the Model, if controller is still registered
		if (controller != null && a.getParameterValue(ParameterTypes.ToRev) != null) {
			if (controller.getActualRevision() < Integer.parseInt(a.getParameterValue(ParameterTypes.ToRev))) {
				Logger.log("setActualRevision to: " + a.getParameterValue(ParameterTypes.ToRev), Logger.DEBUG);
				controller.setActualRevision(Integer.parseInt(a.getParameterValue(ParameterTypes.ToRev)));

			}
		}
	}
	
    private void processFeedbackAuthoringAction(Action a) {
        Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][processFeedbackAuthoringAction] Processing Feedback Authoring action...", Logger.DEBUG);
        FeedbackAuthoringActionProcessor.translate(a);
    }

	private void processManagementAction(Action a) {
		{
			Logger.log(
					"[lasad.gwt.client.communication.LASADActionReceiver][processManagementAction] Processing global management action...",
					Logger.DEBUG);
			//Logger.log("[Details] " + a.toString(), Logger.DEBUG);
		}

		switch (a.getCmd()) {
		case Login:
			if (a.getParameterValue(ParameterTypes.Status).equals("OK")
					|| a.getParameterValue(ParameterTypes.Status).equals("ALREADYAUTHED")) {
					LASAD_Client.getInstance().setAuthed(true);
					LASAD_Client.getInstance().setUsername(a.getParameterValue(ParameterTypes.UserName));

					if (a.getParameterValue(ParameterTypes.Role) != null) {
						LASAD_Client.getInstance().setRole(a.getParameterValue(ParameterTypes.Role));
					}

					LoginTab myLoginTab = LoginTab.getInstance();
					myLoginTab.updateAuthStatus(true);

					LoadingLoginDialogue.getInstance().closeLoadingScreen();

					myLoginTab.getMapLoginPanel().updateOverviews();
					
					if (LASAD_Client.getInstance().urlParameterConfig.isAutoLogin() && LASAD_Client.getInstance().urlParameterConfig.getMapId() != null){
						LoadingMapDialogue.getInstance().showLoadingScreen();
						LASADActionSender.getInstance().sendActionPackage(ActionFactory.getInstance().joinMap(LASAD_Client.getInstance().urlParameterConfig.getMapId()));
					}
					
					// Receive current version of the server. Parameter is specified in server.cfg file
					// with name = "Server-Version"
					String srvVersionParam = a.getParameterValue(ParameterTypes.ServerVersion);
					if (srvVersionParam != null && !"".equals(srvVersionParam)) {
						LASADStatusBar.getInstance().setServerVersion(srvVersionParam);
					}
					
				} else if (a.getParameterValue(ParameterTypes.Status).equals("DENIED")) {
					LASAD_Client.getInstance().setAuthed(false);
					LASADStatusBar.getInstance().setLoginStatus("not logged in");

					LoginTab myLoginTab = LoginTab.getInstance();
					myLoginTab.updateAuthStatus(false);

					LoadingLoginDialogue.getInstance().closeLoadingScreen();

					LASADInfo.display("Login failed!", "Password and/or username incorrect.");

					Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][processManagementAction] Login denied!", Logger.DEBUG);
				}
				break;
			case LoginFailed:
				LoadingLoginDialogue.getInstance().closeLoadingScreen();
				LASADInfo.display("Error", a.getParameterValue(ParameterTypes.Message));
				break;
			case Logout:
				if (a.getParameterValue(ParameterTypes.Status).equals("OK")) {
				
					JoinSessionContentPanel.getInstance().clearMapDetails();
					LASAD_Client.getInstance().setAuthed(false);
					LASAD_Client.getInstance().setUsername("unknown");
					LASAD_Client.statusBar.setLoginStatus("not logged in");
					LASAD_Client.getInstance().refreshWorkspace();
					LASAD_Client.getInstance().setConfirmedTabClose(false);
				}
				break;
			case ForcedLogout:
				if (a.getParameterValue(ParameterTypes.Status).equals("OK")) {
					JoinSessionContentPanel.getInstance().clearMapDetails();
					LASAD_Client.getInstance().setConfirmedTabClose(true);
				}
				break;
			case ListMap:
				Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][processManagementAction] LISTMAP", Logger.DEBUG);
				MapLoginTab.getInstance().addListOfMaps(a, false);
				break;
			case MapDetails:
				Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][processManagementAction] Map details received.", Logger.DEBUG);
				MapLoginTab.getInstance().openMapDetails(a);
				break;
			case TemplateDetails:
				MapLoginTab.getInstance().openTemplateDetails(a);
				break;
			case AddTemplateToList:
				MapLoginTab.getInstance().addTemplateItem(a.getParameterValue(ParameterTypes.Ontology),
					a.getParameterValue(ParameterTypes.Template), a.getParameterValue(ParameterTypes.TemplateId),
					a.getParameterValue(ParameterTypes.TemplateMaxId));
				break;
			case Join:
				Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][processManagementAction] JOINMAP: Creating new MapArea",
					Logger.DEBUG);

				// Create MapInfo
				MapInfo mapInfo = new MapInfo(a.getParameterValue(ParameterTypes.MapId));

				mapInfo.setTitle(a.getParameterValue(ParameterTypes.MapName));

				mapInfo.setOntologyName(a.getParameterValue(ParameterTypes.OntologyName));
				mapInfo.setXmlOntology(a.getParameterValue(ParameterTypes.Ontology));

				mapInfo.setTemplateName(a.getParameterValue(ParameterTypes.TemplateName));
				mapInfo.setTemplateTitle(a.getParameterValue(ParameterTypes.TemplateTitle));
				mapInfo.setXmlTemplate(a.getParameterValue(ParameterTypes.Template));

				mapInfo = (MapInfo) OntologyReader.buildTemplateInfosFromXML(mapInfo, a.getParameterValue(ParameterTypes.Template));
				mapInfo = (MapInfo) OntologyReader.buildOntologyInfosFromXML(mapInfo, a.getParameterValue(ParameterTypes.Ontology));

				Logger.log("MAPINFO: " + mapInfo.toString(), Logger.DEBUG);
				// Create the MVC Controller for the New Map
				MVController newController = LASAD_Client.registerMVCController(new MVController(mapInfo));

				// Create the MapTab
				MapTab mapTab = LASAD_Client.getInstance().createMapTab(newController);

				// *************************************************************************************
				// add by Erkang:
				// Display Argument with another ViewSession, here setup a new
				// Session for Table
				// *************************************************************************************

				String mapId = a.getParameterValue(ParameterTypes.MapId);

				// TODO this two choice can be combined with MVCViewSession
				if (LASAD_Client.getMapEditionStyle(mapId) == ArgumentEditionStyleEnum.TABLE) {
					// Start table view
					MVCViewSession mapSession = new ArgumentMapTableMVCViewSession(newController, mapTab);
					newController.registerViewSession(mapSession);
				} else {
					// Start graph view (Default)
					ArgumentMapMVCViewSession mapSession = new ArgumentMapMVCViewSession(newController, mapTab);
					newController.registerViewSession(mapSession);
				}
				// TODO Zhenyu
				if (a.getParameterValue(ParameterTypes.BackgroundImageURL) != null) {
					mapTab.getMyMapSpace().getMyMap().add(new Image(a.getParameterValue(ParameterTypes.BackgroundImageURL)));
					mapTab.getMyMapSpace().getMyMap().layout();
				}
					
				// This is needed for auto-reconnect. Once the connection crashes, the user will have to rejoin all maps. After that,
				// the infinite loading screen has to disappear. This will be done here.
				ReloadingMapsDialogue.getInstance().decreaseMapCount();
				break;
			default:
				break;
		}
	}

	private void processSessionAction(Action a) {
		if (a.getCmd().equals(Commands.HeartbeatRequest)) {

			Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][processSessionAction] HeartbeatRequest received.",
					Logger.DEBUG);
			// Send an heartbeat back to the server
			LASADActionSender.getInstance().sendActionPackage(ActionFactory.getInstance().sendHeartbeat());

			Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][processSessionAction] HeartbeatRequest sent back to server.",
					Logger.DEBUG);
		}
	}

	/**
	 * Processes MVC controlled management actions
	 * 
	 * @param controller MVC controller
	 * @param a Action to be processed
	 */
	private void processManagementAction(MVController controller, Action a) {
		Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][processManagementAction] Processing map action...", Logger.DEBUG);
		Logger.log("[Details] " + a.toString(), Logger.DEBUG_DETAILS);

		if (a.getCmd().equals(Commands.Leave)) {
			ArgumentMap.mapIDtoCursorID.remove(controller.getMapID());

			// Unregister the MVController and removes it
			LASAD_Client.unregisterMVCController(controller);
			LASAD_Client.removeMapTab(controller.getMapID());

		}
	}

	private void processChatAction(MVController controller, Action a) {

		Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][processChatAction] Processing chat action...", Logger.DEBUG);

		Logger.log("[Details] " + a.toString(), Logger.DEBUG_DETAILS);

		if(! controller.getMapInfo().isSentenceOpener()){
			// TODO: Das laesst sich hier viel sch�ner machen...
			if ("TRUE".equalsIgnoreCase(a.getParameterValue(ParameterTypes.Replay))) {
				LASAD_Client
						.getMapTab(controller.getMapID())
						.getMyMapSpace()
						.getChatPanel()
						.addChatMessage(a.getParameterValue(ParameterTypes.UserName), a.getParameterValue(ParameterTypes.Time),
								a.getParameterValue(ParameterTypes.Message), true);
		} else {
				LASAD_Client
						.getMapTab(controller.getMapID())
						.getMyMapSpace()
						.getChatPanel()
						.addChatMessage(a.getParameterValue(ParameterTypes.UserName), a.getParameterValue(ParameterTypes.Time),
								a.getParameterValue(ParameterTypes.Message), false);
			}
		}
		else{
			if ("TRUE".equalsIgnoreCase(a.getParameterValue(ParameterTypes.Replay))) {
				LASAD_Client
						.getMapTab(controller.getMapID())
						.getMyMapSpace()
						.getExtendedChatPanel()
						.addChatMessage(a.getParameterValue(ParameterTypes.UserName), a.getParameterValue(ParameterTypes.Time),
								a.getParameterValue(ParameterTypes.Message), a.getParameterValue(ParameterTypes.Opener),
								a.getParameterValue(ParameterTypes.TextColor), true);
			} else {
				LASAD_Client
						.getMapTab(controller.getMapID())
						.getMyMapSpace()
						.getExtendedChatPanel()
						.addChatMessage(a.getParameterValue(ParameterTypes.UserName), a.getParameterValue(ParameterTypes.Time),
								a.getParameterValue(ParameterTypes.Message), a.getParameterValue(ParameterTypes.Opener),
								a.getParameterValue(ParameterTypes.TextColor), false);
			}
		}

	}

	private void processMapAction(MVController controller, Action a)
	{
		ArgumentModel argModel = LASAD_Client.getMapTab(controller.getMapID()).getMyMapSpace().getMyMap().getArgModel();

		Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][processMapAction] Processing map action...", Logger.DEBUG);

		AutoOrganizer autoOrganizer = LASAD_Client.getMapTab(controller.getMapID()).getMyMapSpace().getMyMap().getAutoOrganizer();

		Logger.log("[Details] " + a.toString(), Logger.DEBUG_DETAILS);

		String mapId = a.getParameterValue(ParameterTypes.MapId);
		
		//Every map action fires a LasadEvent which is recognized by the mini-map
		LasadEvent newEvent = new LasadEvent();
		newEvent.setType("MAP_" + a.getCmd());
		newEvent.setData(a.getParameters());
		controller.fireLasadEvent(newEvent);

		// TODO Zhenyu
		if (a.getCmd().equals(Commands.BackgroundImage)) {
			LASAD_Client.getMapTab(controller.getMapID()).getMyMapSpace().getMyMap().getBody()
					.applyStyles("background:url(" + a.getParameterValue(ParameterTypes.BackgroundImageURL) + ")");
			LASAD_Client.getMapTab(controller.getMapID()).getMyMapSpace().getMyMap().repaint();
		}

		if (a.getCmd().equals(Commands.ChangeFontSize)){
			Logger.log("Changing font size on ActionReceiver: "+a.getParameterValue(ParameterTypes.FontSize), Logger.DEBUG);
			if (a.getParameterValue(ParameterTypes.FontSize) != null){
				argModel.setFontSize(Integer.parseInt(a.getParameterValue(ParameterTypes.FontSize)), false);
				((ArgumentMapMenuBar)LASAD_Client.getMapTab(a.getParameterValue(ParameterTypes.MapId)).getMyMapSpace().getMenuBar()).setFontSizeSelection(Integer.parseInt(a.getParameterValue(ParameterTypes.FontSize)));
			}
		}
		
		// TODO vereinfachen
		if (LASAD_Client.getMapEditionStyle(mapId) == ArgumentEditionStyleEnum.GRAPH) {

			if (a.getCmd().equals(Commands.CreateElement)) {

				// Check if the feedback is for the current user, if not -->
				// ignore
				String elementType = a.getParameterValue(ParameterTypes.Type);

				// Feb 1 bug fix, lnks were being doubled up for clients in cases of groupable boxes, this fixes it
				if (elementType.equalsIgnoreCase(RELATION))
				{
					
					String startBoxStringID = a.getParameterValues(ParameterTypes.Parent).get(0);
					int startBoxID = Integer.parseInt(startBoxStringID);

					// For some reason, parent here gives box ID, not root ID, so plan accordingly
					String endBoxStringID = a.getParameterValues(ParameterTypes.Parent).get(1);
					int endBoxID = Integer.parseInt(endBoxStringID);

					LinkedBox startBox = argModel.getBoxByBoxID(startBoxID);
					LinkedBox endBox = argModel.getBoxByBoxID(endBoxID);

					if (startBox.hasLinkWith(endBox))
					{
						Logger.log("Will not create following link for client, already exists " + a.toString(), Logger.DEBUG);	
						return;
					}
				}

				if (elementType.equalsIgnoreCase("FEEDBACK-CLUSTER")) {
					if (!a.getParameterValue(ParameterTypes.ForUser).equalsIgnoreCase(LASAD_Client.getInstance().getUsername())
							&& !a.getParameterValue(ParameterTypes.ForUser).equals("")) {
						return;
					}
				}

				// Currently feedback engines count as "element", thus we have
				// to filter them here...
				else if (elementType.equalsIgnoreCase("FEEDBACK-AGENT")) {
					processRegisterFeedbackAgent(controller, a);
				}

				String elementIDString = a.getParameterValue(ParameterTypes.Id);

				int elementID = counterID;

				if (elementIDString != null) {
					elementID = Integer.parseInt(elementIDString);
				}
				else
				{
					counterID--;
				}

				String username = a.getParameterValue(ParameterTypes.UserName);

				Logger.log("[lasad.gwt.client.communication.LASADActionReceiver] Create Model: " + elementID + ", Type: " + elementType,
						Logger.DEBUG);
				AbstractUnspecifiedElementModel elementModel = new UnspecifiedElementModelArgument(elementID, elementType, username);

				if (a.getParameterValue(ParameterTypes.ReplayTime) != null) {
					elementModel.setIsReplay(true);
				}
				// Needed to enable the add and del buttons in box header
				if (a.getParameterValue(ParameterTypes.ElementId) != null) {
					elementModel.setElementId(a.getParameterValue(ParameterTypes.ElementId));
				}

				// Add more specific data to the model
				for (Parameter param : a.getParameters()) {
					if (param.getType() != null && !param.getType().equals(ParameterTypes.Parent)
							&& !param.getType().equals(ParameterTypes.HighlightElementId)) {
						elementModel.setValue(param.getType(), param.getValue());
					}
				}

				// Work on parent relations
				if (a.getParameterValues(ParameterTypes.Parent) != null) {
					for (String parentID : a.getParameterValues(ParameterTypes.Parent)) {
						controller.setParent(elementModel, controller.getElement(Integer.parseInt(parentID)));

						Logger.log("[lasad.gwt.client.communication.LASADActionReceiver] Added ParentElement: " + parentID, Logger.DEBUG);
					}
				}

				// Now Register new Element to the Model
				controller.addElementModel(elementModel);

				// Begin Kevin Loughlin code

				// Since we're creating a new element, we need to add it to the autoOrganize model and update the siblingLinks on the map
				String elementSubType = a.getParameterValue(ParameterTypes.ElementId);

				String rootIDString = a.getParameterValue(ParameterTypes.RootElementId);

				int rootID = -1;

				if (rootIDString != null)
				{
					rootID = Integer.parseInt(rootIDString);
				}

				// If it's a box, add it to the model
				if (elementType.equalsIgnoreCase(BOX))
				{
					String xLeftString = a.getParameterValue(ParameterTypes.PosX);
					double xLeft;
					if (xLeftString != null)
					{
						xLeft = Double.parseDouble(xLeftString);
					}
					else
					{
						Logger.log("Null xLeftString", Logger.DEBUG);
						return;
					}

					String yTopString = a.getParameterValue(ParameterTypes.PosY);
					double yTop;
					if (yTopString != null)
					{
						yTop = Double.parseDouble(yTopString);
					}
					else
					{
						Logger.log("Null yTopString", Logger.DEBUG);
						return;
					}

					ElementInfo newBoxInfo = controller.getMapInfo().getElementsByType(BOX).get(elementModel.getValue(ParameterTypes.ElementId));
					if (newBoxInfo == null)
					{
						Logger.log("newBoxDimenions are null", Logger.DEBUG);
					}

					String widthString = newBoxInfo.getUiOption(ParameterTypes.Width);

					// The default height is set incorrectly, don't know how, but it's always 107 yet comes out as 200.
					//String heightString = newBoxInfo.getUiOption(ParameterTypes.Height);
					String canBeGroupedString = newBoxInfo.getElementOption(ParameterTypes.CanBeGrouped);
					boolean canBeGrouped;
					if (canBeGroupedString == null)
					{
						canBeGrouped = false;
					}
					else
					{
						canBeGrouped = Boolean.parseBoolean(canBeGroupedString);
					}

					if (widthString == null)
					{
						Logger.log("width string is null", Logger.DEBUG);
					}

					int width = Integer.parseInt(widthString);

					argModel.addArgThread(new ArgumentThread(new LinkedBox(elementID, rootID, elementSubType, xLeft, yTop, width, DEFAULT_HEIGHT, canBeGrouped)));
					
					//Added by DSF, run setFontSize so new boxes get the right font size
					argModel.setFontSize(argModel.getFontSize(), false);
				}

				// If it's a relation, add it to the model
				else if (elementType.equalsIgnoreCase(RELATION))
				{	
					String startBoxStringID = a.getParameterValues(ParameterTypes.Parent).get(0);
					int startBoxID = Integer.parseInt(startBoxStringID);

					// For some reason, parent here gives box ID, not root ID, so plan accordingly
					String endBoxStringID = a.getParameterValues(ParameterTypes.Parent).get(1);
					int endBoxID = Integer.parseInt(endBoxStringID);

					LinkedBox startBox = argModel.getBoxByBoxID(startBoxID);
					LinkedBox endBox = argModel.getBoxByBoxID(endBoxID);

					if (startBox == null || endBox == null)
					{
						Logger.log("Error: sibling links could not be updated: null box", Logger.DEBUG);
						return;
					}

					ElementInfo newLinkInfo = controller.getMapInfo().getElementsByType(RELATION).get(elementModel.getValue(ParameterTypes.ElementId));
					String connectsGroupString = newLinkInfo.getElementOption(ParameterTypes.ConnectsGroup);
					boolean connectsGroup;
					if (connectsGroupString == null)
					{
						connectsGroup = false;
					}
					else
					{
						connectsGroup = Boolean.parseBoolean(connectsGroupString);
					}	

					OrganizerLink link = new OrganizerLink(elementID, startBox, endBox, elementSubType, connectsGroup);
					if (link.getConnectsGroup())
					{
						startBox.addSiblingLink(link);
						endBox.addSiblingLink(link);
					}
					else
					{
						startBox.addChildLink(link);
						endBox.addParentLink(link);
					}

					ArgumentThread startBoxThread = argModel.getBoxThread(startBox);

					ArgumentThread endBoxThread = argModel.getBoxThread(endBox);

					if (!startBoxThread.equals(endBoxThread))
					{
						startBoxThread.addBoxes(endBoxThread.getBoxes());
						argModel.removeArgThread(endBoxThread);
					}

					String siblingsAlreadyUpdated = a.getParameterValue(ParameterTypes.SiblingsAlreadyUpdated);

					if (siblingsAlreadyUpdated != null)
					{
						if (!Boolean.parseBoolean(siblingsAlreadyUpdated))
						{
							autoOrganizer.updateSiblingLinks(link);
						}
					}
				}

				// End Kevin Loughlin

				if (elementType.equalsIgnoreCase("FEEDBACK-CLUSTER")) {
					FeedbackPanelArgument feedbackPanel = LASAD_Client.getMapTab(controller.getMapID()).getMyMapSpace().getFeedbackPanel();
					if (feedbackPanel == null) {
						Logger.log("WARNING: There is no feedback panel in the map with id=" +controller.getMapID(), Logger.DEBUG);
						return;
					}
//					LASAD_Client.getMapTab(controller.getMapID()).getMyMapSpace().getFeedbackPanel()
//							.addFeedbackMessage(elementID, a.getParameterValue(ParameterTypes.Message));
					feedbackPanel.addFeedbackMessage(elementID, a.getParameterValue(ParameterTypes.Message));

					Vector<String> highlights = new Vector<String>();
					highlights = a.getParameterValues(ParameterTypes.HighlightElementId);

					if (highlights != null) {
						Vector<Parameter> highlightParam = new Vector<Parameter>();
						for (String s : a.getParameterValues(ParameterTypes.HighlightElementId)) {
							highlightParam.add(new Parameter(ParameterTypes.HighlightElementId, s));
						}

						if (highlightParam.size() > 0) {
							controller.updateElement(elementModel.getId(), highlightParam);
						}
					} else {

						Logger.log("WARNING: There is no highlight for this feedback", Logger.DEBUG);
					}
				}
			}
			else if (a.getCmd().equals(Commands.UpdateElement)) {

				Logger.log("[lasad.gwt.client.communication.LASADActionReceiver.processMapAction] UPDATE-ELEMENT", Logger.DEBUG);

				if (a.getParameterValue(ParameterTypes.UserName) != null
						&& a.getParameterValue(ParameterTypes.UserName).equalsIgnoreCase(LASAD_Client.getInstance().getUsername())
						&& a.getParameterValue(ParameterTypes.Status) != null && a.getParameterValue(ParameterTypes.Text) == null) {
					Action.removeParameter(a, ParameterTypes.Status);
				} else {

					String elementIDString = a.getParameterValue(ParameterTypes.Id);
					int elementID = Integer.parseInt(elementIDString);
					LinkedBox boxToUpdate = argModel.getBoxByBoxID(elementID);

					if (boxToUpdate != null)
					{
						String newWidthString = a.getParameterValue(ParameterTypes.Width);
						String newXLeftString = a.getParameterValue(ParameterTypes.PosX);

						if (newWidthString != null)
						{
							boxToUpdate.setWidth(Integer.parseInt(newWidthString));
							boxToUpdate.setHeight(Integer.parseInt(a.getParameterValue(ParameterTypes.Height)));
						}
						if (newXLeftString != null)
						{
							boxToUpdate.setXLeft(Double.parseDouble(newXLeftString));
							boxToUpdate.setYTop(Double.parseDouble(a.getParameterValue(ParameterTypes.PosY)));
						}
					}
					
					controller.updateElement(Integer.parseInt(a.getParameterValue(ParameterTypes.Id)), a.getParameters());
				}
			}
			else if (a.getCmd().equals(Commands.FinishAutoOrganize))
			{
				Logger.log("[lasad.gwt.client.communication.LASADActionReceiver.processMapAction] FINISHING AUTO-ORGANIZE", Logger.DEBUG);
				this.handleAutoOrganization(a);
			}
			else if (a.getCmd().equals(Commands.StartAutoOrganize))
			{
				Logger.log("[lasad.gwt.client.communication.LASADActionReceiver.processMapAction] STARTING AUTO-ORGANIZE", Logger.DEBUG);
			}
			else if (a.getCmd().equals(Commands.UpdateCursorPosition)) {

				Logger.log("[lasad.gwt.client.communication.LASADActionReceiver.processMapAction] UPDATE-CURSOR-POSITION", Logger.DEBUG);
				Logger.log(a.toString(), Logger.DEBUG_DETAILS);
				controller.updateElement(Integer.parseInt(a.getParameterValue(ParameterTypes.Id)), a.getParameters());
			}
			else if (a.getCmd().equals(Commands.DeleteElement))
			{
				Logger.log("[lasad.gwt.client.communication.LASADActionReceiver.processMapAction] DELETE-ELEMENT", Logger.DEBUG);
				int elementID = Integer.parseInt(a.getParameterValue(ParameterTypes.Id));
				
				if (controller.getElement(elementID) != null) {
					String elementType = controller.getElement(elementID).getType();
					if (controller.getElement(elementID).getType().equalsIgnoreCase("FEEDBACK-AGENT")) {
						processRemoveFeedbackAgent(controller, a);
					}
					try {
						controller.deleteElement(Integer.parseInt(a.getParameterValue(ParameterTypes.Id)),
								a.getParameterValue(ParameterTypes.UserName));
					}catch (Exception e) {
						e.printStackTrace();
						Logger.log("Can not delete element, because ID is not int!", Logger.DEBUG_ERRORS);
					}

					// Kevin Loughlin
					// Keep in mind that relations are removed before boxes
					if (elementType.equalsIgnoreCase(RELATION))
					{
						OrganizerLink removedLink = argModel.removeLinkByLinkID(elementID);
						if (removedLink != null)
						{
							argModel.createNewThreadIfNecessary(removedLink);

							if (!Boolean.parseBoolean(a.getParameterValue(ParameterTypes.LinksAlreadyRemoved)) 
								&& Integer.parseInt(a.getParameterValue(ParameterTypes.NumActions)) == 1)
							{
								autoOrganizer.determineLinksToRemove(removedLink);
							}
						}
						else
						{
							Logger.log("ERROR: Removed link is null", Logger.DEBUG);
						}
					}
					else if (elementType.equalsIgnoreCase(BOX))
					{
						LinkedBox removedBox = argModel.removeBoxByBoxID(elementID);
						if (removedBox != null)
						{
							argModel.removeLinksTo(removedBox);
						}
						else
						{
							Logger.log("ERROR: Removed box is null", Logger.DEBUG);
						}
					}
					// End Kevin Loughlin

				} else {
					// This occurs when another user deletes his/her feedback
					Logger.log("Cannot delete, because element ID is unknown.", Logger.DEBUG_ERRORS);
				}
			} else if (a.getCmd().equals(Commands.ListMap)) {
				Logger.log("[lasad.gwt.client.communication.LASADActionReceiver.processMapAction] LISTMAP", Logger.DEBUG);
				MapLoginTab.getInstance().addListOfMaps(a, true);
			} else if (a.getCategory().equals(Commands.Error)) {
				LASADInfo.display("Error", a.getParameterValue(ParameterTypes.Message));
			} else if (a.getCmd().equals(Commands.Import)) {
				//IMPORT ActionPackage holds a Parameter XMLTEXT 
				//This parameter's value is the map to be imported
				MVController newController = LASAD_Client.getMapTab(a.getParameterValue(ParameterTypes.MapId))
						.getMyMapSpace().getSession().getController();

				if (newController.getMapInfo().getOntologyName().equalsIgnoreCase("ARGUNAUT")){
					ArgunautParser parser = new ArgunautParser(a.getParameterValue(ParameterTypes.XMLText), LASAD_Client.getMapTab(
							a.getParameterValue(ParameterTypes.MapId)).getMyMapSpace(), newController);
					try {
						if (a.getParameterValue(ParameterTypes.XMLText).trim().equals("")) {
							MessageBox.info("No gml text found", "Please enter a proper gml-file text!", null);
						} else {
							parser.parseText();
						}
					} catch (DOMParseException e) {
						MessageBox.alert("Error", "The gml-file does not match the argunaut pattern!\n " + e.toString(), null);
						// e.printStackTrace();
					} catch (Exception e) {
						MessageBox.alert("Unknown Error",
								"An error occured in class ArgumentMapMenuBar.java. Contact the system developer for further information.\n "
										+ e.toString(), null);
						MessageBox.alert("Unknown Error", e.toString(), null);
						// e.printStackTrace();
					}
				} else {
					LARGOParser parser = new LARGOParser(a.getParameterValue(ParameterTypes.XMLText), newController.getMapInfo()
							.getTemplateName(), LASAD_Client.getMapTab(a.getParameterValue(ParameterTypes.MapId))
							.getMyMapSpace(), newController);
					try {
						if (a.getParameterValue(ParameterTypes.XMLText).trim().equals("")) {
							MessageBox.info("No gml text found", "Please enter a proper gml-file text!", null);
						} else {
							parser.parseText();
						}
					} catch (DOMParseException e) {
						MessageBox.alert("Error", "The xml-file does not match the largo pattern!", null);
						// e.printStackTrace();
					} catch (Exception e) {
						MessageBox.alert("Unknown Error",
								"An error occured in class ArgumentMapMenuBar.java. Contact the system developer for further information.",
								null);
						// e.printStackTrace();
					}
				}
				// LoadingLoginDialogue.getInstance().closeLoadingScreen();
			}
		} else if (LASAD_Client.getMapEditionStyle(mapId) == ArgumentEditionStyleEnum.TABLE) {
			//TODO vereinfachen!!!
			
			if (a.getCmd().equals(Commands.CreateElement)) {

				// Check if the feedback is for the current user, if not -->
				// ignore
				String elementType = a.getParameterValue(ParameterTypes.Type);

				if (elementType.equalsIgnoreCase(RELATION))
				{
					
					String startBoxStringID = a.getParameterValues(ParameterTypes.Parent).get(0);
					int startBoxID = Integer.parseInt(startBoxStringID);

					// For some reason, parent here gives box ID, not root ID, so plan accordingly
					String endBoxStringID = a.getParameterValues(ParameterTypes.Parent).get(1);
					int endBoxID = Integer.parseInt(endBoxStringID);

					LinkedBox startBox = argModel.getBoxByBoxID(startBoxID);
					LinkedBox endBox = argModel.getBoxByBoxID(endBoxID);

					if (startBox.hasLinkWith(endBox))
					{
						Logger.log("Will not create following link for client, already exists " + a.toString(), Logger.DEBUG);	
						return;
					}
				}
				if (elementType.equalsIgnoreCase("FEEDBACK-CLUSTER")) {
					if (!a.getParameterValue(ParameterTypes.UserName).equalsIgnoreCase(LASAD_Client.getInstance().getUsername())
							&& !a.getParameterValue(ParameterTypes.UserName).equals("")) {
						return;
					}
				}

				// Currently feedback engines count as "element", thus we have
				// to filter them here...
				else if (elementType.equalsIgnoreCase("FEEDBACK-AGENT")) {
					processRegisterFeedbackAgent(controller, a);
				}

				String elementIDString = a.getParameterValue(ParameterTypes.Id);

				int elementID = -1;

				if (elementIDString != null) {
					elementID = Integer.parseInt(elementIDString);
				}

				String username = a.getParameterValue(ParameterTypes.UserName);

				Logger.log("[lasad.gwt.client.communication.LASADActionReceiver] Create Model: " + elementID + ", Type: " + elementType,
						Logger.DEBUG);
				AbstractUnspecifiedElementModel elementModel = new UnspecifiedElementModelArgument(elementID, elementType, username);

				if (a.getParameterValue(ParameterTypes.ReplayTime) != null) {
					elementModel.setIsReplay(true);
				}
				// Needed to enable the add and del buttons in box header
				if (a.getParameterValue(ParameterTypes.ElementId) != null) {
					elementModel.setElementId(a.getParameterValue(ParameterTypes.ElementId));
				}

				// Add more specific data to the model
				for (Parameter param : a.getParameters()) {
					if (param.getType() != null && !param.getType().equals(ParameterTypes.Parent)
							&& !param.getType().equals(ParameterTypes.HighlightElementId)) {
						elementModel.setValue(param.getType(), param.getValue());
					}
				}

				// Work on parent relations
				if (a.getParameterValues(ParameterTypes.Parent) != null) {
					for (String parentID : a.getParameterValues(ParameterTypes.Parent)) {
						controller.setParent(elementModel, controller.getElement(Integer.parseInt(parentID)));

						Logger.log("[lasad.gwt.client.communication.LASADActionReceiver] Added ParentElement: " + parentID, Logger.DEBUG);
					}
				}

				// Now Register new Element to the Model
				controller.addElementModel(elementModel);
				
				// Begin Kevin Loughlin code

				// Since we're creating a new element, we need to add it to the autoOrganize model and update the siblingLinks on the map
				String elementSubType = a.getParameterValue(ParameterTypes.ElementId);

				String rootIDString = a.getParameterValue(ParameterTypes.RootElementId);

				int rootID = -1;

				if (rootIDString != null)
				{
					rootID = Integer.parseInt(rootIDString);
				}

				// If it's a box, add it to the model
				if (elementType.equalsIgnoreCase(BOX))
				{
					String xLeftString = a.getParameterValue(ParameterTypes.PosX);
					double xLeft;
					if (xLeftString != null)
					{
						xLeft = Double.parseDouble(xLeftString);
					}
					else
					{
						Logger.log("Null xLeftString", Logger.DEBUG);
						return;
					}

					String yTopString = a.getParameterValue(ParameterTypes.PosY);
					double yTop;
					if (yTopString != null)
					{
						yTop = Double.parseDouble(yTopString);
					}
					else
					{
						Logger.log("Null yTopString", Logger.DEBUG);
						return;
					}

					ElementInfo newBoxInfo = controller.getMapInfo().getElementsByType(BOX).get(elementModel.getValue(ParameterTypes.ElementId));
					if (newBoxInfo == null)
					{
						Logger.log("newBoxDimenions are null", Logger.DEBUG);
					}

					String widthString = newBoxInfo.getUiOption(ParameterTypes.Width);

					// The default height is set incorrectly, don't know how, but it's always 107 yet comes out as 200.
					//String heightString = newBoxInfo.getUiOption(ParameterTypes.Height);
					String canBeGroupedString = newBoxInfo.getElementOption(ParameterTypes.CanBeGrouped);
					boolean canBeGrouped;
					if (canBeGroupedString == null)
					{
						canBeGrouped = false;
					}
					else
					{
						canBeGrouped = Boolean.parseBoolean(canBeGroupedString);
					}

					if (widthString == null)
					{
						Logger.log("width string is null", Logger.DEBUG);
					}

					int width = Integer.parseInt(widthString);

					argModel.addArgThread(new ArgumentThread(new LinkedBox(elementID, rootID, elementSubType, xLeft, yTop, width, DEFAULT_HEIGHT, canBeGrouped)));
					
					//Added by DSF, run setFontSize so new boxes get the right font size
					argModel.setFontSize(argModel.getFontSize(), false);
				}

				// If it's a relation, add it to the model
				else if (elementType.equalsIgnoreCase(RELATION))
				{
					String startBoxStringID = a.getParameterValues(ParameterTypes.Parent).get(0);
					int startBoxID = Integer.parseInt(startBoxStringID);

					// For some reason, parent here gives box ID, not root ID, so plan accordingly
					String endBoxStringID = a.getParameterValues(ParameterTypes.Parent).get(1);
					int endBoxID = Integer.parseInt(endBoxStringID);

					LinkedBox startBox = argModel.getBoxByBoxID(startBoxID);
					LinkedBox endBox = argModel.getBoxByBoxID(endBoxID);

					if (startBox == null || endBox == null)
					{
						Logger.log("Error: sibling links could not be updated: null box", Logger.DEBUG);
						return;
					}

					ElementInfo newLinkInfo = controller.getMapInfo().getElementsByType(RELATION).get(elementModel.getValue(ParameterTypes.ElementId));
					String connectsGroupString = newLinkInfo.getElementOption(ParameterTypes.ConnectsGroup);
					boolean connectsGroup;
					if (connectsGroupString == null)
					{
						connectsGroup = false;
					}
					else
					{
						connectsGroup = Boolean.parseBoolean(connectsGroupString);
					}	

					OrganizerLink link = new OrganizerLink(elementID, startBox, endBox, elementSubType, connectsGroup);
					if (link.getConnectsGroup())
					{
						startBox.addSiblingLink(link);
						endBox.addSiblingLink(link);
					}
					else
					{
						startBox.addChildLink(link);
						endBox.addParentLink(link);
					}

					ArgumentThread startBoxThread = argModel.getBoxThread(startBox);

					ArgumentThread endBoxThread = argModel.getBoxThread(endBox);

					if (!startBoxThread.equals(endBoxThread))
					{
						startBoxThread.addBoxes(endBoxThread.getBoxes());
						argModel.removeArgThread(endBoxThread);
					}

					String siblingsAlreadyUpdated = a.getParameterValue(ParameterTypes.SiblingsAlreadyUpdated);

					if (siblingsAlreadyUpdated != null)
					{
						if (!Boolean.parseBoolean(siblingsAlreadyUpdated))
						{
							autoOrganizer.updateSiblingLinks(link);
						}
					}
				}

				// End Kevin Loughlin

				if (elementType.equalsIgnoreCase("FEEDBACK-CLUSTER")) {
					FeedbackPanelArgument feedbackPanel = LASAD_Client.getMapTab(controller.getMapID()).getMyMapSpace().getFeedbackPanel();
					if (feedbackPanel == null) {
						Logger.log("WARNING: There is no feedback panel in the map with id=" +controller.getMapID(), Logger.DEBUG);
						return;
					}
					feedbackPanel.addFeedbackMessage(elementID, a.getParameterValue(ParameterTypes.Message));
//					LASAD_Client.getMapTab(controller.getMapID()).getMyMapSpace().getFeedbackPanel()
//							.addFeedbackMessage(elementID, a.getParameterValue(ParameterTypes.Message));
				}

			} else if (a.getCmd().equals(Commands.UpdateElement)) {

				Logger.log("[lasad.gwt.client.communication.LASADActionReceiver.processMapAction] UPDATE-ELEMENT", Logger.DEBUG);

				if (a.getParameterValue(ParameterTypes.UserName) != null
						&& a.getParameterValue(ParameterTypes.UserName).equalsIgnoreCase(LASAD_Client.getInstance().getUsername())
						&& a.getParameterValue(ParameterTypes.Status) != null && a.getParameterValue(ParameterTypes.Text) == null) {
					Action.removeParameter(a, ParameterTypes.Status);
				} else {

					String elementIDString = a.getParameterValue(ParameterTypes.Id);
					int elementID = Integer.parseInt(elementIDString);
					LinkedBox boxToUpdate = argModel.getBoxByBoxID(elementID);

					if (boxToUpdate != null)
					{
						String newWidthString = a.getParameterValue(ParameterTypes.Width);
						String newXLeftString = a.getParameterValue(ParameterTypes.PosX);

						if (newWidthString != null)
						{
							boxToUpdate.setWidth(Integer.parseInt(newWidthString));
							boxToUpdate.setHeight(Integer.parseInt(a.getParameterValue(ParameterTypes.Height)));
						}
						if (newXLeftString != null)
						{
							boxToUpdate.setXLeft(Double.parseDouble(newXLeftString));
							boxToUpdate.setYTop(Double.parseDouble(a.getParameterValue(ParameterTypes.PosY)));
						}
					}
					
					controller.updateElement(Integer.parseInt(a.getParameterValue(ParameterTypes.Id)), a.getParameters());
				}

			}
			else if (a.getCmd().equals(Commands.StartAutoOrganize))
			{
				Logger.log("[lasad.gwt.client.communication.LASADActionReceiver.processMapAction] STARTING AUTO-ORGANIZE", Logger.DEBUG);
			}
			else if (a.getCmd().equals(Commands.FinishAutoOrganize))
			{
				Logger.log("[lasad.gwt.client.communication.LASADActionReceiver.processMapAction] FINISHING AUTO-ORGANIZE", Logger.DEBUG);
				this.handleAutoOrganization(a);
			}
			else if (a.getCmd().equals(Commands.UpdateCursorPosition)) {

				Logger.log("[lasad.gwt.client.communication.LASADActionReceiver.processMapAction] UPDATE-CURSOR-POSITION", Logger.DEBUG);
				Logger.log(a.toString(), Logger.DEBUG_DETAILS);
				controller.updateElement(Integer.parseInt(a.getParameterValue(ParameterTypes.Id)), a.getParameters());

			}
			else if (a.getCmd().equals(Commands.DeleteElement))
			{
				Logger.log("[lasad.gwt.client.communication.LASADActionReceiver.processMapAction] DELETE-ELEMENT", Logger.DEBUG);
				int elementID = Integer.parseInt(a.getParameterValue(ParameterTypes.Id));
				
				if (controller.getElement(elementID) != null) {
					String elementType = controller.getElement(elementID).getType();
					if (controller.getElement(elementID).getType().equalsIgnoreCase("FEEDBACK-AGENT")) {
						processRemoveFeedbackAgent(controller, a);
					}
					try {
						controller.deleteElement(Integer.parseInt(a.getParameterValue(ParameterTypes.Id)),
								a.getParameterValue(ParameterTypes.UserName));
					}catch (Exception e) {
						e.printStackTrace();
						Logger.log("Can not delete element, because ID is not int!", Logger.DEBUG_ERRORS);
					}

					// Kevin Loughlin
					// Keep in mind that relations are removed before boxes
					if (elementType.equalsIgnoreCase(RELATION))
					{
						OrganizerLink removedLink = argModel.removeLinkByLinkID(elementID);
						if (removedLink != null)
						{
							argModel.createNewThreadIfNecessary(removedLink);

							if (!Boolean.parseBoolean(a.getParameterValue(ParameterTypes.LinksAlreadyRemoved)) 
								&& Integer.parseInt(a.getParameterValue(ParameterTypes.NumActions)) == 1)
							{
								autoOrganizer.determineLinksToRemove(removedLink);
							}
						}
						else
						{
							Logger.log("ERROR: Removed link is null", Logger.DEBUG);
						}
					}
					else if (elementType.equalsIgnoreCase(BOX))
					{
						LinkedBox removedBox = argModel.removeBoxByBoxID(elementID);
						if (removedBox != null)
						{
							argModel.removeLinksTo(removedBox);
						}
						else
						{
							Logger.log("ERROR: Removed box is null", Logger.DEBUG);
						}
					}
					// End Kevin Loughlin

				} else {
					// This occurs when another user deletes his/her feedback
					Logger.log("Cannot delete, because element ID is unknown.", Logger.DEBUG_ERRORS);
				}
			} 
			else if (a.getCategory().equals(Categories.Error)) {
				LASADInfo.display("Error", a.getParameterValue(ParameterTypes.Message));
			}

		}
	}

	private void processRegisterFeedbackAgent(MVController controller, Action a) {

		Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][processRegisterFeedbackAgent] Processing new feedback agent...",
				Logger.DEBUG);

		Logger.log("[Details] " + a.toString(), Logger.DEBUG_DETAILS);
		ArgumentMapMenuBar currentMenu = (ArgumentMapMenuBar) LASAD_Client.getMapTab(controller.getMapID()).getMyMapSpace().getMenuBar();
		if (a.getParameterValue(ParameterTypes.AgentType) != null) {
			currentMenu.addFeedbackEngine(a.getParameterValue(ParameterTypes.AgentId), a.getParameterValue(ParameterTypes.TypeId),
					a.getParameterValue(ParameterTypes.AgentType));
		} else {
			currentMenu.addFeedbackEngine(a.getParameterValue(ParameterTypes.AgentId), a.getParameterValue(ParameterTypes.TypeId));
		}
	}

	private void processRemoveFeedbackAgent(MVController controller, Action a) {

		Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][processRegisterFeedbackAgent] Processing new feedback agent...",
				Logger.DEBUG);

		Logger.log("[Details] " + a.toString(), Logger.DEBUG_DETAILS);
		ArgumentMapMenuBar currentMenu = (ArgumentMapMenuBar) LASAD_Client.getMapTab(controller.getMapID()).getMyMapSpace().getMenuBar();
		currentMenu.removeFeedbackEngine(
				controller.getElement(Integer.parseInt(a.getParameterValue(ParameterTypes.Id))).getValue(ParameterTypes.AgentId),
				controller.getElement(Integer.parseInt(a.getParameterValue(ParameterTypes.Id))).getValue(ParameterTypes.TypeId));
	}

	private void processHeartbeatAction(MVController controller, Action a) {
		Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][processHeartbeatAction] Processing heartbeat action...",
				Logger.DEBUG);
	}

	private void processUserEventAction(MVController controller, Action a) {

		Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][processUserEventAction] Processing userevent action...",
				Logger.DEBUG);
		Logger.log("[Details] " + a.toString(), Logger.DEBUG_DETAILS);
		
		Commands command = a.getCmd();

		// Generate new LASAD Event
		LasadEvent newEvent = new LasadEvent();

		newEvent.setType("USER_" + command);

		if (command.equals(Commands.UserJoin)) {
			// A New User Joined the map
			HashMap<ParameterTypes, String> data = new HashMap<ParameterTypes, String>();
			data.put(ParameterTypes.UserName, a.getParameterValue(ParameterTypes.UserName));
			data.put(ParameterTypes.Client, LASAD_Client.getInstance().getUsername());
			newEvent.setData(data);
		} else if (command.equals(Commands.UserLeave)) {
			newEvent.setData(a.getParameterValue(ParameterTypes.UserName));
		} else if (command.equals(Commands.UserList)) {
			Vector<String> client = new Vector<String>();
			client.addElement(LASAD_Client.getInstance().getUsername());
			// A complete list of the actual users arrived
			Vector<String> userList = a.getParameterValues(ParameterTypes.UserName);
			HashMap<ParameterTypes, Vector<String>> data = new HashMap<ParameterTypes, Vector<String>>();
			data.put(ParameterTypes.Client, client);
			data.put(ParameterTypes.UserList, userList);

			newEvent.setData(data);
		}

		if (command != null) {
			controller.fireLasadEvent(newEvent);
		}
	}

	private void processQuestionnaireAction(MVController controller, Action a) {

		Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][processQuestionnaireAction] Processing questionnaire action...",
				Logger.DEBUG);

		Logger.log("[Details] " + a.toString(), Logger.DEBUG_DETAILS);

		// Questionnaire related Stuff
		if (a.getCmd().equals(Commands.Answer)) {
			String questionID = a.getParameterValue(ParameterTypes.QuestionId);
			String answer = a.getParameterValue(ParameterTypes.QuestionAnswer);

			if (questionID != null) {
				LASAD_Client.getMapTab(controller.getMapID()).getMyMapSpace().getTutorial().getQuestionnaireHandler()
						.publishQuestionnaireAnswer(questionID, answer);
			}
		}
	}

	// David Drexler Edit-BEGIN
	// Code outsourcing by Marcel Bergmann

	/**
	 * Processes replay actions
	 * 
	 * @param a Action to be processed
	 */
	private void processReplayAction(Action a) {
		switch (a.getCmd()){
		case InitStart:
			// Start to initialize the HashMaps needed for replay...
			// Initialize HashMaps
			this.treeForwReplay = new TreeMap<Integer, ActionPackage>();
			this.treeUserReplay = new TreeMap<String, List<Integer>>();
			this.elementReplay = new ArrayList<Integer>();
			init = new ReplayInitializer(treeForwReplay, treeUserReplay, elementReplay);
			break;
		case InitEnd:
			init.finishInitialization(a);
			break;
		case CreateElement:
		case UpdateElement:
		case DeleteElement:
		case ChangeFontSize:
		case StartAutoOrganize:
		case FinishAutoOrganize:
		case ChatMsg:
			init.createAllReplayActions(a);
			break;
//		case ChatMsg:
//			System.err.println("There are no Methods to handle the Chat Messages!");
//			System.err.println(a.toString());
//			break;
		default:
			// An error has occured
			// Close wait-MessageBox
			LoadingReplayDialogue.getInstance().closeLoadingScreen();
			// Show error
			MapLoginTab.getInstance().infoReplayError(a.getCmd());
		}
	}

	// David Drexler Edit-END

	/**
	 * Work on ActionSets with no valid/registered MAPIDs
	 * 
	 * @param actionSet
	 */
	private void workOnGlobalAction(Action a) {
		//TODO Vereinfachen!!! Switch und so...
		Logger.log("[lasad.gwt.client.communication.LASADActionReceiver][workOnGlobalAction]", Logger.DEBUG_DETAILS);
//		Logger.log(a.toString(), Logger.DEBUG_DETAILS);

		if (a.getCategory().equals(Categories.Auth) || a.getCategory().equals(Categories.Map)
				|| a.getCategory().equals(Categories.Management)) {
			processManagementAction(a);
		} else if (a.getCategory().equals(Categories.FeedbackAuthoring)) {
			processFeedbackAuthoringAction(a);
		}
		else if (a.getCategory().equals(Categories.Error)) {
			LASADInfo.display("Error", a.getParameterValue(ParameterTypes.Message));
		} else if (a.getCategory().equals(Categories.Notify)) {
			
			if(a.getCmd().equals(Commands.UserCreated)) {
				CreateAndDeleteUsers.resetUserData();
			} else if (a.getCmd().equals(Commands.OntologyCreated)) {
				CreateModifyAndDeleteOntology.resetOntologyData();
			} else if (a.getCmd().equals(Commands.TemplateCreated)) {
				CreateAndDeleteTemplate.resetTemplateData();
			} else if (a.getCmd().equals(Commands.SessionCreated)) {
				CreateAndDeleteSessions.resetSessionData();
			} else if (a.getCmd().equals(Commands.ErrorInfo)) {
				String msg = " Please check with the administrator, or reload the page and login again.";
				MessageBox.wait("Account problem", a.getParameterValue(ParameterTypes.Message) + msg, null);
			}
			
			LASADInfo.display("Notification", a.getParameterValue(ParameterTypes.Message));
		} else if (a.getCategory().equals(Categories.Session)) {
			processSessionAction(a);
		} else if (a.getCategory().equals(Categories.Authoring)) {
			processAuthoringAction(a);
		} else if (a.getCategory().equals(Categories.Replay)) {
			processReplayAction(a);
		} else if (a.getCategory().equals(Categories.Info)) {
			if (a.getCmd().equals(Commands.JoinComplete)) {
				LoadingMapDialogue.getInstance().closeLoadingScreen();
				this.centerMap(a);

			} else if (a.getCmd().equals(Commands.Ready)) {
				if (a.getParameterValue(ParameterTypes.ImportType).equalsIgnoreCase("LASAD")) {
					LoadingMapFromFileDialogue.getInstance().closeLoadingScreen();					
				}
				if (a.getParameterValue(ParameterTypes.ImportType).equalsIgnoreCase("LARGO")
						|| a.getParameterValue(ParameterTypes.ImportType).equalsIgnoreCase("ARGUNAUT")) {
					ImportingMapDialogue.getInstance().closeLoadingScreen();
				}
			} else if (a.getCmd().equals(Commands.AuthoringFailed)) {
				LASADInfo.display("Error", a.getParameterValue(ParameterTypes.Message));
			}
		} else {
			Logger.log(
					"[lasad.gwt.client.communication.LASADActionReceiver][workOnGlobalAction] Error: Unknown action category! [CATEGORY: "
							+ a.getCategory() + "]", Logger.DEBUG);
		}
	}

	/**
	 * Calculates the center of all existing boxes and scrolls the map to this position
	 * 
	 * @param a the join-complete action with the map information
	 */
	private void centerMap(Action a) {
		// Scroll the map to the center of all boxes
		int xsum = 0;
		int ysum = 0;
		int numberOfObjects = 0;
		
		//Map-Id-Hack... normal tag would be filtered by ActionReceiver, that's why this action has its own mapid-attribute
		AbstractGraphMap map = LASAD_Client.getMapTab(a.getParameterValue(ParameterTypes.MapId)).getMyMapSpace()
				.getMyMap();
		List<Component> mapComponents = map.getItems();
		Iterator<Component> iter = mapComponents.iterator();
		while (iter.hasNext()) {
			Component component = iter.next();
			if (component instanceof AbstractBox) {
				AbstractBox box = (AbstractBox) component;
				xsum += box.getPosition(true).x + box.getWidth() / 2;
				ysum += box.getPosition(true).y + box.getHeight() / 2;
				numberOfObjects++;
			}
		}

		if (numberOfObjects > 0) {
			map.getLayoutTarget().dom.setScrollLeft(xsum / numberOfObjects - map.getInnerWidth() / 2);
			map.getLayoutTarget().dom.setScrollTop(ysum / numberOfObjects - map.getInnerHeight() / 2);
		} else {
			map.getLayoutTarget().dom.setScrollLeft(map.getMapDimensionSize().width / 2 - map.getInnerWidth() / 2);
			map.getLayoutTarget().dom.setScrollTop(map.getMapDimensionSize().height / 2 - map.getInnerHeight() / 2);
		}
	}

	private void handleAutoOrganization(Action a)
	{
		final String MAP_ID = a.getParameterValue(ParameterTypes.MapId);
		final boolean IS_DOWNWARD = Boolean.parseBoolean(a.getParameterValue(ParameterTypes.OrganizerOrientation));
		AutoOrganizer organizer = LASAD_Client.getMapTab(MAP_ID).getMyMapSpace().getMyMap().getAutoOrganizer();
		organizer.setOrientation(IS_DOWNWARD);
		organizer.setBoxWidth(Integer.parseInt(a.getParameterValue(ParameterTypes.OrganizerBoxWidth)));
		organizer.setMinBoxHeight(Integer.parseInt(a.getParameterValue(ParameterTypes.OrganizerBoxHeight)));
		AbstractGraphMap map = LASAD_Client.getMapTab(MAP_ID).getMyMapSpace().getMyMap();
		double edgeCoordY = Double.parseDouble(a.getParameterValue(ParameterTypes.ScrollEdgeY));
		double edgeCoordX = Double.parseDouble(a.getParameterValue(ParameterTypes.ScrollEdgeX));
		if (IS_DOWNWARD)
		{
			map.getLayoutTarget().dom.setScrollTop((int) Math.round(edgeCoordY) + 10 - map.getInnerHeight());
		}
		else
		{
			map.getLayoutTarget().dom.setScrollTop((int) Math.round(edgeCoordY) - 10);
		}
		map.getLayoutTarget().dom.setScrollLeft((int) Math.round(edgeCoordX - map.getInnerWidth() / 2.0));
	}

	private void processAuthoringAction(final Action a) {
		if (AuthoringTab.active) {
			switch (a.getCmd()) {
			case AddUserToList:
				CreateAndDeleteUsers.addUserItem(a.getParameterValue(ParameterTypes.UserName), a.getParameterValue(ParameterTypes.Role),
						a.getParameterValue(ParameterTypes.UserId), a.getParameterValue(ParameterTypes.UserMaxId));
				break;
			case AddTemplateToList:
				CreateAndDeleteTemplate.addTemplateItem(a.getParameterValue(ParameterTypes.Ontology),
						a.getParameterValue(ParameterTypes.Template), a.getParameterValue(ParameterTypes.TemplateId),
						a.getParameterValue(ParameterTypes.TemplateMaxId));
				break;
			case AddMapToList:
				CreateAndDeleteSessions.addSessionItem(a.getParameterValue(ParameterTypes.MapName),
						a.getParameterValue(ParameterTypes.Template), a.getParameterValue(ParameterTypes.MapId),
						a.getParameterValue(ParameterTypes.MapMaxId));
				break;
			case DeleteTemplateFromList:
				CreateAndDeleteTemplate.removeTemplateItem(a.getParameterValue(ParameterTypes.Ontology),
						a.getParameterValue(ParameterTypes.Template));
				break;
			case DeleteUserFromList:
				CreateAndDeleteUsers.removeUserItem(a.getParameterValue(ParameterTypes.Role), a.getParameterValue(ParameterTypes.UserName));
				break;
			case DeleteMapFromList:
				CreateAndDeleteSessions.removeSessionItem(a.getParameterValue(ParameterTypes.MapName),
						a.getParameterValue(ParameterTypes.Template));
				break;
			case DeleteOntologyFromList:
				CreateModifyAndDeleteOntology.removeOntologyItem(a.getParameterValue(ParameterTypes.OntologyName));
				break;
			case ListUsers:
				CreateAndDeleteSessions.createUserList(a.getParameterValues(ParameterTypes.UserName));
				break;
			case ListOntologies:
				CreateModifyAndDeleteOntology.updateOntologyView(a.getParameterValues(ParameterTypes.OntologyName));
				break;
			case OntologyDetails:
				CreateModifyAndDeleteOntology.updateOntologyDetailsView(a.getParameterValue(ParameterTypes.Ontology));
				break;
			case ConfirmationRequest:
				final Listener<MessageBoxEvent> l = new Listener<MessageBoxEvent>() {
					public void handleEvent(MessageBoxEvent ce) {
						Button btn = ce.getButtonClicked();

						if (btn.getItemId().equals(Dialog.YES)) {
							ActionPackage p = ActionFactory.getInstance().recoverRequestFromConfirmation(a);
							LASADActionSender.getInstance().sendActionPackage(p);
						}
					}
				};

				MessageBox.confirm("Confirm", a.getParameterValue(ParameterTypes.Message), l);
				break;
			default:
				break;
			}
		}
	}
}