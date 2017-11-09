package de.thm.mni.mote.mode.uiactor.editor.statemachine;

import de.thm.mni.mhpp11.smbj.actors.messagebus.MessageBus;
import de.thm.mni.mhpp11.smbj.messages.logging.TraceMessage;
import de.thm.mni.mote.mode.modelica.MoContainer;
import de.thm.mni.mote.mode.uiactor.control.MainTabControl;
import de.thm.mni.mote.mode.uiactor.editor.MenuManager;
import de.thm.mni.mote.mode.uiactor.editor.actionmanager.ActionManager;
import de.thm.mni.mote.mode.uiactor.editor.actionmanager.commands.Command;
import de.thm.mni.mote.mode.uiactor.editor.elementmanager.ElementManager;
import de.thm.mni.mote.mode.uiactor.editor.elementmanager.interfaces.Hoverable;
import de.thm.mni.mote.mode.uiactor.editor.elementmanager.interfaces.Selectable;
import de.thm.mni.mote.mode.uiactor.editor.statemachine.interfaces.*;
import de.thm.mni.mote.mode.uiactor.editor.statemachine.states.KeyState;
import de.thm.mni.mote.mode.uiactor.editor.statemachine.states.State;
import de.thm.mni.mote.mode.uiactor.editor.statemachine.states.States;
import de.thm.mni.mote.mode.util.Utilities;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Created by hobbypunk on 15.11.16.
 */

@Getter
public class StateMachine implements EventHandler<InputEvent> {
  private static Map<MoContainer, StateMachine> INSTANCES = new HashMap<>();
  private EventType<? extends Event> eventToSkip = null;
  
  public static StateMachine getInstance(MoContainer container) {
    if (!INSTANCES.containsKey(container)) throw new NoSuchElementException("No such StateMachine");
    return INSTANCES.get(container);
  }
  
  public static StateMachine getInstance(Scene scene, MainTabControl tab, MoContainer container) {
    if (!INSTANCES.containsKey(container)) INSTANCES.put(container, new StateMachine(scene, tab, container));
    return INSTANCES.get(container);
  }
  
  public static void removeInstance(MoContainer data) {
    INSTANCES.remove(data);
  }
  
  
  public BooleanProperty active = new SimpleBooleanProperty(false);
  private Boolean freezeState = false;
  private Node target;
  private final MoContainer data;

  private Scene scene;
  private MainTabControl tab;
  @NonNull private ObjectProperty<KeyState> keyState = new SimpleObjectProperty<>(KeyState.NONE);
  
  @NonNull private State state = States.NONE;
  
  
  private StateMachine(Scene scene, MainTabControl tab, MoContainer container) {
    this.scene = scene;
    this.tab = tab;
    this.data = container;
  }
  
  @Override
  public void handle(InputEvent event) {
    handleElementManagment(event, freezeState);
    if (eventToSkip != null && event.getEventType() == eventToSkip) {
      eventToSkip = null;
      event.consume();
      return;
    }
    if (!freezeState) {
      //if (event.getSource() instanceof FXMoDiagramMoGroup && event.getEventType().equals(MouseEvent.MOUSE_MOVED)) return;
      //if (!event.getEventType().equals(MouseEvent.MOUSE_MOVED)) MessageBus.getInstance().send(new TraceMessage(StateMachine.class, tab.getText() + " Event: " + event.getSource().getClass().getSimpleName() + " : " + event.getEventType()));
      if (event instanceof MouseEvent) updateKeyState((MouseEvent) event);
      if (event instanceof ScrollEvent) updateKeyState((ScrollEvent) event);
      
      updateState(event);
    }
    
    Node target = findTarget(event);
    
    if (this.freezeState) {
      if (target == null || !this.target.getClass().isAssignableFrom(target.getClass()))
        target = this.target;
    } else {
      this.target = target;
    }

//    mousePositionHelp(event);
        
    Command c = state.handle(this, target, event);
    if (c != null) {
      ActionManager.getInstance(data).addUndo(c);
      event.consume();
    }
  }
  
  public void switchToNone() {
    this.state = States.NONE;
    ElementManager.getInstance(this.data).clearSelectedElement();
  }
  
  private void updateKeyState(MouseEvent event) {
    updateKeyState(event.isShiftDown(), event.isAltDown(), event.isMetaDown(), event.isControlDown());
  }
  
  private void updateKeyState(ScrollEvent event) {
    updateKeyState(event.isShiftDown(), event.isAltDown(), event.isMetaDown(), event.isControlDown());
  }
  
  private void updateKeyState(Boolean isShiftDown, Boolean isAltDown, Boolean isMetaDown, Boolean isControlDown) {
    if (isShiftDown) keyState.setValue(KeyState.SHIFT);
    else if (isAltDown) keyState.setValue(KeyState.ALT);
    else if (isMetaDown) keyState.setValue(KeyState.META);
    else if (isControlDown) keyState.setValue(KeyState.CTRL);
    else keyState.setValue(KeyState.NONE);
  }
  
  private void updateState(InputEvent event) {
    Node target = (Node) event.getTarget();
    State state = States.NONE;
    if (event instanceof MouseEvent) {
      MouseEvent e = (MouseEvent) event;
      if (!(this.freezeState && this.state == States.MOVE) && e.getEventType().equals(MouseEvent.MOUSE_CLICKED) && (keyState.get().equals(KeyState.SHIFT) || e.getClickCount() % 2 == 0) && hasMatchingParent(target, Actionable.class))
        state = States.ACTION;
    }
    if (state == States.NONE) {
      if (keyState.get().equals(KeyState.CTRL) && hasMatchingParent(target, Deletable.class)) state = States.DELETE;
      else if (keyState.get().equals(KeyState.NONE) && hasMatchingParent(target, Resizeable.class)) state = States.RESIZE;
      else if (keyState.get().equals(KeyState.SHIFT) && hasMatchingParent(target, Rotateable.class)) state = States.ROTATE;
      else if ((keyState.get().equals(KeyState.NONE) || keyState.get().equals(KeyState.SHIFT)) && hasMatchingParent(target, Moveable.class)) state = States.MOVE;
      else if ((Utilities.isMac() && keyState.get().equals(KeyState.META)) || (!Utilities.isMac() && keyState.get().equals(KeyState.CTRL))) state = States.ZOOM; //TODO
    }
    changeState(state);
  }
  
  private void handleElementManagment(InputEvent event, Boolean fixedSelection) {
    handleElementManagment(event, (Node) event.getTarget(), fixedSelection);
  }
  
  private void handleElementManagment(InputEvent event, Node target, Boolean fixedSelection) {
    EventType<? extends Event> type = event.getEventType();
  
    if (type.equals(MouseEvent.MOUSE_ENTERED) || type.equals(MouseEvent.MOUSE_MOVED) || type.equals(MouseEvent.MOUSE_EXITED)) {
      if (hasMatchingParent(target, Hoverable.class)) {
        if (type.equals(MouseEvent.MOUSE_ENTERED) || type.equals(MouseEvent.MOUSE_MOVED))
          ElementManager.getInstance(tab.getData()).setHoveredElement((Hoverable) getMatchingParent(target, Hoverable.class));
        if (type.equals(MouseEvent.MOUSE_EXITED))
          ElementManager.getInstance(tab.getData()).clearHoveredElement();
      } else {
        if(type.equals(MouseEvent.MOUSE_MOVED))
          ElementManager.getInstance(tab.getData()).clearHoveredElement();
      }
    }
  
    if (type.equals(MouseEvent.MOUSE_PRESSED)) {
      if (!fixedSelection) {
        if (hasMatchingParent(target, Selectable.class)) {
          ElementManager.getInstance(tab.getData()).setSelectedElement((Selectable) getMatchingParent(target, Selectable.class));
        } else {
          ElementManager.getInstance(tab.getData()).clearSelectedElement();
        }
      }
    }
  }
  
  
  private Node findTarget(InputEvent event) {
    if (state.equals(States.ACTION)) return getMatchingParent((Node) event.getTarget(), Actionable.class);
    if (state.equals(States.DELETE)) return getMatchingParent((Node) event.getTarget(), Deletable.class);
    if (state.equals(States.MOVE)) return getMatchingParent((Node) event.getTarget(), Moveable.class);
    if (state.equals(States.RESIZE)) return getMatchingParent((Node) event.getTarget(), Resizeable.class);
    if (state.equals(States.ROTATE)) return getMatchingParent((Node) event.getTarget(), Rotateable.class);
    if (state.equals(States.ZOOM)) return getMatchingParent((Node) event.getTarget(), Zoomable.class);
    return (Node) event.getTarget();
  }
  
  private boolean hasMatchingParent(Node target, Class<?> thatClass) {
    return getMatchingParent(target, thatClass) != null;
  }
  
  private Node getMatchingParent(Node target, Class<?> thatClass) {
    if (target == null) return null;
    if (thatClass.isAssignableFrom(target.getClass())) return target;
    return getMatchingParent(target.getParent(), thatClass);
  }
  
  private void changeState(State newState) {
    if (this.state == newState) return;
    this.state.leave(this.scene);
    MessageBus.getInstance().send(new TraceMessage(StateMachine.class, "Switch State: " + this.state.getClass().getSimpleName() + " -> " + newState.getClass().getSimpleName()));
    this.state = newState;
    this.state.enter(this.scene);
  }
  
  
  public void leave() {
    MenuManager.getInstance(this.data).getActive().set(false);
    ActionManager.getInstance(this.data).getActive().set(false);
    this.active.set(false);
    state.leave(this.scene);
    MessageBus.getInstance().send(new TraceMessage(StateMachine.class, tab.getText() + " leave"));
  }
  
  public void enter() {
    MessageBus.getInstance().send(new TraceMessage(StateMachine.class, tab.getText() + " enter"));
  
    state.enter(this.scene);
    this.active.set(true);
    ActionManager.getInstance(this.data).getActive().set(true);
    MenuManager.getInstance(this.data).getActive().set(true);
  }
  
  public void freeze() {
    this.freezeState = true;
  }
  
  public void unfreeze() {
    this.freezeState = false;
  }
  
  public void skipNextEvent(EventType<? extends Event> type) {
    this.eventToSkip = type;
  }
}