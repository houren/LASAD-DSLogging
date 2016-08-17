package lasad.gwt.client.ui.workspace.tabs.feedbackauthoring.util.widget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import lasad.gwt.client.ui.workspace.tabs.feedbackauthoring.data.ElementModel;
import lasad.gwt.client.ui.workspace.tabs.feedbackauthoring.data.GridElementLabel;
import lasad.gwt.client.ui.workspace.tabs.feedbackauthoring.util.FeedbackAuthoringStrings;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.dnd.ListViewDragSource;
import com.extjs.gxt.ui.client.dnd.ListViewDropTarget;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.IconButtonEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.button.IconButton;
import com.extjs.gxt.ui.client.widget.form.AdapterField;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.ListField;
import com.extjs.gxt.ui.client.widget.form.MultiField;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Element;

/**
 * Combines and input field and a list field and allows adding text in input field to the list field. Also, allows
 * selection in list field to be moved to input field using buttons.
 * 
 * @param <D> the model type
 */
@SuppressWarnings("deprecation")
public class InputFieldAndListField<D extends ModelData> extends MultiField<Field<?>>  {

	 /**
	   * The input field list field messages.
	   */
	  public class DualListFieldMessages extends FieldMessages {

//	    private String addAll;
	    private String addSelected;
	    private String moveDown;
	    private String moveUp;
	    private String removeAll;
	    private String removeSelected;

//	    /**
//	     * Returns the add all tooltip.
//	     * 
//	     * @return the add all tooltip
//	     */
//	    public String getAddAll() {
//	      return addAll;
//	    }

	    /**
	     * Returns the add selected tooltip.
	     * 
	     * @return the add selected tooltip
	     */
	    public String getAddSelected() {
	      return addSelected;
	    }

	    /**
	     * Returns the move down tooltip.
	     * 
	     * @return the move down tooltip
	     */
	    public String getMoveDown() {
	      return moveDown;
	    }

	    /**
	     * Sets the move up tooltip.
	     * 
	     * @return the move up tooltip
	     */
	    public String getMoveUp() {
	      return moveUp;
	    }

	    /**
	     * Returns the remove all tooltip.
	     * 
	     * @return the remove all tooltip
	     */
	    public String getRemoveAll() {
	      return removeAll;
	    }

	    /**
	     * Returns the remove selected tooltip.
	     * 
	     * @return the remove selected tooltip
	     */
	    public String getRemoveSelected() {
	      return removeSelected;
	    }

//	    /**
//	     * Sets the add all tooltip (defaults to 'Add all').
//	     * 
//	     * @param addAll the add all tooltip
//	     */
//	    public void setAddAll(String addAll) {
//	      this.addAll = addAll;
//	    }

	    /**
	     * Sets the add selected tooltip (defaults to 'Add selected').
	     * 
	     * @param addSelected the add selected tooltip
	     */
	    public void setAddSelected(String addSelected) {
	      this.addSelected = addSelected;
	    }

	    /**
	     * Sets the move selected down tooltip (defaults to 'Move selected down').
	     * 
	     * @param moveDown the move down tooltip
	     */
	    public void setMoveDown(String moveDown) {
	      this.moveDown = moveDown;
	    }

	    /**
	     * Sets the move selected up tooltip (defaults to 'Move selected up').
	     * 
	     * @param moveUp the move selected up text
	     */
	    public void setMoveUp(String moveUp) {
	      this.moveUp = moveUp;
	    }

	    /**
	     * Sets the remove all tooltip (defaults to 'Remove all').
	     * 
	     * @param removeAll the remove all tooltip
	     */
	    public void setRemoveAll(String removeAll) {
	      this.removeAll = removeAll;
	    }

	    /**
	     * Sets the remove selected tooltip (defaults to 'Remove selected').
	     * 
	     * @param removeSelected the remove selected tooltip
	     */
	    public void setRemoveSelected(String removeSelected) {
	      this.removeSelected = removeSelected;
	    }

	  }
	  
	  final Random generator = new Random(System.currentTimeMillis());
	  public static final int LEFT_BUTTON = 0x1;//remove-edit
	  public static final int RIGHT_BUTTON = 0x2;//add
	  public static final int ALL_LEFT_BUTTON = 0x3;
//	  public static final int ALL_RIGHT_BUTTON = 0x4;
	  public static final int UP_BUTTON = 0x5;
	  public static final int DOWN_BUTTON = 0x6;
	  
	  private List<Integer> buttonsToRemoveList;

	  /**
	   * The DND mode enumeration.
	   */
	  public enum Mode {
	    APPEND, INSERT;
	  }

	  protected AdapterField buttonAdapter;
	  protected AdapterField rightButtonAdapter;
	  protected VerticalPanel buttonBar;
	  protected VerticalPanel rightButtonBar;
	  protected TextField<String> fromField;
	  protected Mode mode = Mode.APPEND;
	  protected ListField<D> toField;
	  protected ListViewDragSource sourceFromField;
	  protected ListViewDragSource sourceToField;
	  protected ListViewDropTarget targetFromField;
	  protected ListViewDropTarget targetToField;
	  protected IconButton left, right, allLeft, allRight;
	  protected IconButton up, down;

//	  private String dndGroup;
//	  private boolean enableDND = true;

	  public InputFieldAndListField(List<Integer> buttonsToRemoveList) {
		this.buttonsToRemoveList = buttonsToRemoveList;
	    fromField = new TextField<String>();
	    toField = new ListField<D>();
	    toField.setStyleAttribute("color", "#000000");
	    setSize(200, 125);
	    messages = new DualListFieldMessages();

	    buttonBar = new VerticalPanel();
	    buttonBar.setStyleAttribute("margin", "7px");
	    buttonBar.setHorizontalAlign(HorizontalAlignment.CENTER);
	    buttonAdapter = new AdapterField(buttonBar);
	    
	    rightButtonBar = new VerticalPanel();
	    rightButtonBar.setStyleAttribute("margin", "7px");
	    rightButtonBar.setHorizontalAlign(HorizontalAlignment.CENTER);
	    rightButtonAdapter = new AdapterField(rightButtonBar);
	    
	    

	    add(fromField);
	    add(buttonAdapter);
	    add(toField);
	    add(rightButtonAdapter);
	  }

	  private void configureButton(IconButton btn, String msg, String tip) {
	    btn.setHeight(18);
	    tip = msg != null ? msg : tip;
	    if (GXT.isAriaEnabled()) {
	      btn.setTitle(tip);
	      btn.getFocusSupport().setIgnore(true);
	    } else {
	      btn.setToolTip(tip);
	    }
	  }

//	  /**
//	   * Returns the DND group name.
//	   * 
//	   * @return the group name
//	   */
//	  public String getDNDGroup() {
//	    return dndGroup;
//	  }

	  /**
	   * Returns the from field's drag source instance.
	   * 
	   * @return the drag source
	   */
	  public ListViewDragSource getDragSourceFromField() {
	    assert rendered : "Can only be called post-render";
	    return sourceFromField;
	  }

	  /**
	   * Returns the to field's drag source instance.
	   * 
	   * @return the drag source
	   */
	  public ListViewDragSource getDragSourceToField() {
	    assert rendered : "Can only be called post-render";
	    return sourceToField;
	  }

	  /**
	   * Returns the from field's drop target instance.
	   * 
	   * @return the drag source
	   */
	  public ListViewDropTarget getDropTargetFromField() {
	    assert rendered : "Can only be called post-render";
	    return targetFromField;
	  }

	  /**
	   * Returns the to field's drop target instance.
	   * 
	   * @return the drag source
	   */
	  public ListViewDropTarget getDropTargetToField() {
	    assert rendered : "Can only be called post-render";
	    return targetToField;
	  }

	  /**
	   * Returns the from text field.
	   * 
	   * @return the from text field
	   */
	  public TextField<String> getFromField() {
	    return fromField;
	  }

	  @SuppressWarnings("unchecked")
	  @Override
	  public DualListFieldMessages getMessages() {
	    return (DualListFieldMessages) messages;
	  }

	  /**
	   * Returns the list field's mode.
	   * 
	   * @return the mode
	   */
	  public Mode getMode() {
	    return mode;
	  }

	  /**
	   * Return the to list field.
	   * 
	   * @return the to list field
	   */
	  public ListField<D> getToField() {
	    return toField;
	  }

	  /**
	   * Returns the to list field.
	   * 
	   * @return the field
	   */
	  public ListField<D> getToList() {
	    return toField;
	  }

	  protected void initButtons() {
	    if (mode == Mode.INSERT) {
	      up = new IconButton("arrow-up");
	      configureButton(up, getMessages().getMoveUp(), GXT.MESSAGES.listField_moveSelectedUp());
	      up.addListener(Events.Select, new Listener<IconButtonEvent>() {
	        public void handleEvent(IconButtonEvent be) {
	          onButtonUp(be);
	        }
	      });
	      if(buttonsToRemoveList != null && !buttonsToRemoveList.contains(UP_BUTTON)){
//	    	  buttonBar.add(up);
	    	  rightButtonBar.add(up);
	      }
	    }

//	    allRight = new IconButton("arrow-double-right");
//	    configureButton(allRight, getMessages().getAddAll(), GXT.MESSAGES.listField_addAll());
//	    allRight.addListener(Events.Select, new Listener<IconButtonEvent>() {
//	      public void handleEvent(IconButtonEvent be) {
//	        onButtonAllRight(be);
//	      }
//	    });
//	    buttonBar.add(allRight);

	    right = new IconButton("arrow-right");
	    configureButton(right, getMessages().getAddSelected(), FeedbackAuthoringStrings.ADD_LABEL);
	    right.addListener(Events.Select, new Listener<IconButtonEvent>() {
	      public void handleEvent(IconButtonEvent be) {
	        onButtonRight(be);
	      }
	    });

	    left = new IconButton("arrow-left");
	    configureButton(left, getMessages().getRemoveSelected(), FeedbackAuthoringStrings.EDIT_LABEL + " " +
	    				FeedbackAuthoringStrings.SELECT_LABEL + "ed");//GXT.MESSAGES.listField_removeSelected()
	    left.addListener(Events.Select, new Listener<IconButtonEvent>() {
	      public void handleEvent(IconButtonEvent be) {
	        onButtonLeft(be);
	      }
	    });
	    if(buttonsToRemoveList != null && !buttonsToRemoveList.contains(RIGHT_BUTTON)){
	    	buttonBar.add(right);
	    }
	    if(buttonsToRemoveList != null && !buttonsToRemoveList.contains(LEFT_BUTTON)){
	    	buttonBar.add(left);
	    }

	    allLeft = new IconButton("button-delete-circle");//former CSS-> arrow-double-left
	    configureButton(allLeft, getMessages().getRemoveAll(), GXT.MESSAGES.listField_removeSelected());//GXT.MESSAGES.listField_removeAll()
	    allLeft.addListener(Events.Select, new Listener<IconButtonEvent>() {
	      public void handleEvent(IconButtonEvent be) {
	        onButtonAllLeft(be);
	      }
	    });

	    if (mode == Mode.INSERT) {
	      down = new IconButton("arrow-down");
	      configureButton(down, getMessages().getMoveDown(), GXT.MESSAGES.listField_moveSelectedDown());
	      down.addListener(Events.Select, new Listener<IconButtonEvent>() {
	        public void handleEvent(IconButtonEvent be) {
	          onButtonDown(be);
	        }
	      });
	      if(buttonsToRemoveList != null && !buttonsToRemoveList.contains(DOWN_BUTTON)){
//	    	  buttonBar.add(down);
	    	  rightButtonBar.add(down);
	      }
	    }
	    if(buttonsToRemoveList != null && !buttonsToRemoveList.contains(ALL_LEFT_BUTTON)){
//	    	buttonBar.add(allLeft);
	    	rightButtonBar.add(allLeft);
	    }
	  }
	  
//	  /**
//	   * Returns true if drag and drop is enabled.
//	   * 
//	   * @return true if drag and drop is enabled
//	   */
//	  public boolean isEnableDND() {
//	    return enableDND;
//	  }
	  
	  protected void onButtonAllLeft(IconButtonEvent be) {
	    List<D> sel = toField.getSelection();
	    for (D model : sel) {
	      toField.getStore().remove(model);
	    }
		  
//	    List<D> sel = toField.getStore().getModels();
//	    toField.getStore().removeAll();
	  }

//	  protected void onButtonAllRight(IconButtonEvent be) {
//	    List<D> sel = fromField.getStore().getModels();
//	    toField.getStore().add(sel);
//	    fromField.getStore().removeAll();
//	  }

	  protected void onButtonDown(IconButtonEvent be) {
	    toField.getListView().moveSelectedDown();
	  }

	  /*
	   * This is gonna be used to delete elements
	   */
	  protected void onButtonLeft(IconButtonEvent be) {
	    List<D> sel = toField.getSelection();
	    for (D model : sel) {
	      toField.getStore().remove(model);
	    }
	    if(sel != null && sel.size() > 0)
	    	fromField.setValue((String) sel.get(0).get(GridElementLabel.NAME));
	    
//	    fromField.getStore().add(sel);
//	    select(fromField, sel);
	  }
	  
	  protected void onButtonRight(IconButtonEvent be) {
		String value = fromField.getValue();
		fromField.setValue("");
		Map<String, String> tmp = new HashMap<String, String>();
    	tmp.put(GridElementLabel.ID, Integer.toString(getNewId()));
    	tmp.put(GridElementLabel.NAME, value);
    	@SuppressWarnings("unchecked")
    	D model = (D) new ElementModel(tmp);
    	
		toField.getStore().add(model);
		List<D> sel = new Vector<D>();
		sel.add(model);
		select(toField, sel);
//	    List<D> sel = fromField.getSelection();
//	    for (D model : sel) {
//	      fromField.getStore().remove(model);
//	    }
//	    toField.getStore().add(sel);
//	    select(toField, sel);
	  }
	  
	  protected void onButtonUp(IconButtonEvent be) {
	    toField.getListView().moveSelectedUp();
	  }
	  
	  @Override
	  protected void onFocus(ComponentEvent ce) {
	    super.onFocus(ce);
	    fromField.focus();
	  }
	  
	  @Override
	  protected void onRender(Element target, int index) {
	    initButtons();

	    super.onRender(target, index);
//	    getElement().removeAttribute("tabindex");

//	    if (enableDND) {
//	      enableDND = false;
//	      setEnableDND(true);
//	    }
	  }

	  @Override
	  protected void onResize(int width, int height) {
	    super.onResize(width, height);
	    if (orientation == Orientation.HORIZONTAL) {
	      int w = (width - buttonAdapter.el().getParent().getWidth()
	    		  		- rightButtonAdapter.el().getParent().getWidth()) / 2;
	      w -= (fields.size() * spacing);
	      fromField.setSize(w, 25);
	      toField.setSize(w, height);
	    } else {
	      for (Field<?> f : fields) {
	        f.setWidth(width);
	      }
	    }
	  }

	  @SuppressWarnings({"unchecked", "rawtypes"})
	  private void select(final ListField<?> field, final List list) {
	  	// DeferredCommand is deprecated, use Scheduler once we upgrade GWT
	    com.google.gwt.user.client.DeferredCommand.addCommand(new Command() {
	      public void execute() {
	        field.getListView().getSelectionModel().select(list, false);
	      }
	    });
	  }

//	  /**
//	   * Sets the drag and drop group name. A group name will be generated if none
//	   * is specified.
//	   * 
//	   * @param group the group name
//	   */
//	  public void setDNDGroup(String group) {
//	    if (group == null) {
//	      group = getId() + "-group";
//	    }
//	    this.dndGroup = group;
//	    if (sourceFromField != null) {
//	      sourceFromField.setGroup(dndGroup);
//	    }
//	    if (sourceToField != null) {
//	      sourceToField.setGroup(dndGroup);
//	    }
//	    if (targetFromField != null) {
//	      targetFromField.setGroup(dndGroup);
//	    }
//	    if (targetToField != null) {
//	      targetToField.setGroup(dndGroup);
//	    }
//	  }

//	  /**
//	   * True to allow selections to be dragged and dropped between lists (defaults
//	   * to true).
//	   * 
//	   * @param enableDND true to enable drag and drop
//	   */
//	  public void setEnableDND(boolean enableDND) {
//	    if (rendered) {
//	      if (enableDND && !this.enableDND) {
//	        sourceFromField = new ListViewDragSource(fromField.getListView());
//	        sourceToField = new ListViewDragSource(toField.getListView());
//
//	        targetFromField = new ListViewDropTarget(fromField.getListView());
//	        targetFromField.setAutoSelect(true);
//	        targetToField = new ListViewDropTarget(toField.getListView());
//	        targetToField.setAutoSelect(true);
//
//	        if (mode == Mode.INSERT) {
//	          targetToField.setAllowSelfAsSource(true);
//	          targetFromField.setFeedback(Feedback.INSERT);
//	          targetToField.setFeedback(Feedback.INSERT);
//	        }
//	        setDNDGroup(dndGroup);
//	      } else if (!enableDND) {
//	        if (sourceFromField != null) {
//	          sourceFromField.release();
//	          sourceFromField = null;
//	        }
//	        if (sourceToField != null) {
//	          sourceToField.release();
//	          sourceToField = null;
//	        }
//	        if (targetFromField != null) {
//	          targetFromField.release();
//	          targetFromField = null;
//	        }
//	        if (targetToField != null) {
//	          targetToField.release();
//	          targetToField = null;
//	        }
//	      }
//	    }
//	    this.enableDND = enableDND;
//	  }

	  /**
	   * Specifies if selections are either inserted or appended when moving between
	   * lists.
	   * 
	   * @param mode the mode
	   */
	  public void setMode(Mode mode) {
	    this.mode = mode;
	  }
	  
	  private int getNewId(){
			return Math.abs(generator.nextInt());
		}
	
}
