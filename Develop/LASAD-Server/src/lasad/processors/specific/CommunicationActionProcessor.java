package lasad.processors.specific;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.Calendar;

import lasad.controller.ManagementController;
import lasad.entity.ActionParameter;
import lasad.entity.Element;
import lasad.entity.Map;
import lasad.entity.Revision;
import lasad.entity.User;
import lasad.helper.ActionPackageFactory;
import lasad.logging.Logger;
import lasad.processors.ActionObserver;
import lasad.processors.ActionProcessor;
import lasad.shared.communication.objects.Action;
import lasad.shared.communication.objects.ActionPackage;
import lasad.shared.communication.objects.Parameter;
import lasad.shared.communication.objects.categories.Categories;
import lasad.shared.communication.objects.commands.Commands;
import lasad.shared.communication.objects.parameters.ParameterTypes;
import edu.cmu.pslc.logging.ContextMessage;
import edu.cmu.pslc.logging.OliDatabaseLogger;
import edu.cmu.pslc.logging.ToolMessage;
import edu.cmu.pslc.logging.element.DatasetElement;
import edu.cmu.pslc.logging.element.LevelElement;
import edu.cmu.pslc.logging.element.MetaElement;
import edu.cmu.pslc.logging.element.ProblemElement;

/** 
 * this class records all communications between users
 * 
 * @author ?
 */
public class CommunicationActionProcessor extends AbstractActionObserver
		implements ActionObserver {

	private boolean DS_LOGGING_IS_ON;

	private OliDatabaseLogger dsLogger;

	// user, sessionID
	private java.util.Map<String, Set<String>> loggedSessions;

	// session + user + mapID --> contextMSG
	private java.util.Map<String, ContextMessage> loggedContexts;

	private String DATASET;
	private String className;
	private String school;
	private String period;
	private String instructor;

	public CommunicationActionProcessor(){
		super();

		// These settings correspond to whether or not PSLC DataShop Logging will be used
        String settingsFileName = "./ds_settings.txt";

        try
        {
            FileReader fr = new FileReader(settingsFileName);
            BufferedReader reader = new BufferedReader(fr);

            // first line of file is whether to do DS Logging
            DS_LOGGING_IS_ON = true;// Boolean.parseBoolean(reader.readLine().replaceAll("\\s",""));
            if (DS_LOGGING_IS_ON)
            {
            	String url = reader.readLine();
            	dsLogger = OliDatabaseLogger.create(url, "UTF-8");
            	// second line of file is dataset
            	DATASET = reader.readLine();
            	className = reader.readLine();
            	school = reader.readLine();
            	period = reader.readLine();
            	instructor = reader.readLine(); 

				loggedSessions = new ConcurrentHashMap<String, Set<String>>();
				loggedContexts = new ConcurrentHashMap<String, ContextMessage>();
            }
            reader.close();         
        }
        catch(Exception ex) {
        	DS_LOGGING_IS_ON = false;
        	DATASET = "garbage";
        	className = "garbage";
        	school = "garbage";
        	period = "garbage";
        	instructor = "garbage";
            Logger.debugLog("ERROR: can't read DS settings, DS Logging deactivated.");
        	StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			Logger.debugLog(sw.toString());              
        }
	}

	/**
     * Logs user map actions to PSLC datashop
     */
    private void logToDataShop(Action a, String userName, String sessionID)
    {
    	try
    	{
			//String userName = u.getNickname();
	        //String sessionID = u.getSessionID();
	        Integer mapID = ActionProcessor.getMapIDFromAction(a);

	        final String CONTEXT_REF = sessionID + userName + String.valueOf(mapID);

	        if (loggedSessions.get(userName) == null)
	        {
	        	dsLogger.logSession(userName, sessionID);
	        	Set<String> sessIDs = new HashSet<String>();
	        	sessIDs.add(sessionID);
	        	loggedSessions.put(userName, sessIDs);
	        }
	        else if (!loggedSessions.get(userName).contains(sessionID))
	        {
	        	dsLogger.logSession(userName, sessionID);
	        	loggedSessions.get(userName).add(sessionID);
	        }

	        boolean shouldLogContext = false;
	        ProblemElement problem = new ProblemElement(Map.getMapName(mapID));

	        String timeString = Long.toString(a.getTimeStamp());
			String timeZone = Calendar.getInstance().getTimeZone().getID();
		        
			ContextMessage contextMsg = loggedContexts.get(CONTEXT_REF);
			if (contextMsg == null)
			{
			    MetaElement metaElement = new MetaElement(userName, sessionID, timeString, timeZone);
				shouldLogContext = true;
				contextMsg = ContextMessage.createStartProblem(metaElement);

				LevelElement sectionLevel;
	        	sectionLevel = new LevelElement("Section", "01", problem);

		        contextMsg.setClassName(className);
		        contextMsg.setSchool(school);
		        contextMsg.setPeriod(period);
		        contextMsg.addInstructor(instructor);
		        contextMsg.setDataset(new DatasetElement(DATASET, sectionLevel));	        
			}	

	        ToolMessage toolMsg = ToolMessage.create(contextMsg);
	        toolMsg.setTimeString(timeString);

	        String selection;
	        if (a.getParameterValue(ParameterTypes.Id) == null)
	        {
	        	selection = "";
	        }
	        else
	        {
	        	selection = a.getParameterValue(ParameterTypes.Id);
	        }

	        final String input;
	        final String action;
	        String eltType = a.getParameterValue(ParameterTypes.Type);

	        switch (a.getCmd())
	        {
	        	case ChatMsg:
	        		toolMsg.setAsAttempt("");
	        		String msg = "";
	        		if (a.getParameterValue(ParameterTypes.Message) != null) {
	        			msg = a.getParameterValue(ParameterTypes.Message);
	        		}
	        		input = msg;
	        		action = "Chat Message";
	        		break;
	        	default:
	        		return;
	        }

	        if (selection != null && action != null && input != null)
	        {
	        	toolMsg.addSai(selection, action, input);
	        }
	        else
	        {
	        	Logger.debugLog("ERROR: cannot log becase sai is null for following action!");
	        	Logger.debugLog(a.toString());
	        	return;
	        }

	        if (shouldLogContext)
	        {
	        	if (dsLogger.log(contextMsg))
		        {
		        	loggedContexts.put(CONTEXT_REF, contextMsg);
		        }
		        else
		        {
		        	Logger.debugLog("ERROR: context Message log failed for following action!");
		        	Logger.debugLog(a.toString());
		        	return;
		        }
	        }

	        if (!dsLogger.log(toolMsg))
	        {
	        	Logger.debugLog("ERROR: tool Message log failed for following action!");
	        	Logger.debugLog(a.toString());
	        	return;
	        }
		}	    	
        catch(Exception e)
        {
        	Logger.debugLog("ERROR: Exception thrown for following action!");
        	Logger.debugLog(a.toString());
        	Logger.debugLog("EXCEPTION INFO...");
        	StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			Logger.debugLog(sw.toString());
        } 	
    }

	/** 
	 * After a communication happening each time, the revision is updated to record this communication
	 * 
	 * @param a      a specific LASAD action  
	 * @param u      the User,who owns this map
	 * @param sessionID  the Session ID 
	 * @author ZGE
	 */
	@Override
	public boolean processAction(Action a, User u, String sessionID) {
		boolean returnValue = false;
		if (u != null&&a.getCategory().equals(Categories.Communication)){
			// Currently, there are only chat messages in this category, thus, a
			// further differentiation is not needed
			int mapID = ActionProcessor.getMapIDFromAction(a);

			Revision r = new Revision(mapID, "Chat message", u.getUserID());
			r.saveToDatabase();

			ActionParameter.saveParametersForRevision(r.getId(), a);
			a.addParameter(ParameterTypes.UserName, u.getNickname());
			a.addParameter(ParameterTypes.Time, System.currentTimeMillis() + "");

			ActionPackage ap = ActionPackage.wrapAction(a);
			Logger.doCFLogging(ap);
			ManagementController.addToAllUsersOnMapActionQueue(ap, mapID);
			
			returnValue = true;
		}

		if (DS_LOGGING_IS_ON)
		{
			logToDataShop(a, u.getNickname(), u.getSessionID());
		}

		return returnValue;
	}

}
