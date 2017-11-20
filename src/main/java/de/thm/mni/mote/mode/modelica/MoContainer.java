package de.thm.mni.mote.mode.modelica;

import de.thm.mni.mhpp11.smbj.logging.messages.ErrorMessage;
import de.thm.mni.mhpp11.smbj.manager.ActorManager;
import de.thm.mni.mote.mode.backend.omc.OMCompiler;
import de.thm.mni.mote.mode.parser.ParserException;
import de.thm.mni.mote.mode.util.HierarchyData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static de.thm.mni.mote.mode.util.Translator.tr;

/**
 * Created by Marcel Hoppe on 27.01.17.
 */
@Getter
public class MoContainer implements Comparable<MoContainer>, HierarchyData<MoContainer> {
  private static final int MAX_LOADING_DEPTH = 2;
  
  @Getter static final Set<MoRoot> ROOTS = new HashSet<>();
  
  public static MoContainer staticFind(String s) {
    MoContainer that;
    for (MoContainer root : ROOTS) {
      that = root.find(s);
      if (that != null) return that;
    }
    return null;
  }
  
  private OMCompiler omc = null;
  private MoContainer parent = null;
  protected String name = null;
  private MoClass element = null;
  
  private final List<MoContainer> inheritedClasses = new ArrayList<>();
  private final ObservableList<MoContainer> children = FXCollections.observableArrayList();
  
  public MoContainer(OMCompiler omc, MoContainer parent, @NonNull String name) {
    this.omc = omc;
    this.parent = parent;
    this.name = name;
  }
  
  public MoContainer setElement(MoClass element) {
    this.element = element;
    element.setContainer(this);
    return this;
  }
  
  public synchronized void moveTo(MoContainer newParent) {
    moveTo(-1, newParent);
  }
  
  private synchronized void moveTo(Integer index, MoContainer newParent) {
    if (this.parent != null) this.parent.getChildren().remove(this);
    if (index > -1) newParent.getChildren().set(index, this);
    else newParent.getChildren().add(this);
    this.parent = newParent;
  }
  
  public Boolean contains(MoContainer elem) {
    return contains(elem.getElement());
  }
  
  private Boolean contains(MoClass elem) {
    if (element.equals(elem)) return true;
    
    for (MoContainer c : children)
      if (c.contains(elem)) return true;
    
    return false;
  }
  
  public MoContainer find(String s) {
    if (this.getName().equals(s)) return this;
    
    if (!s.startsWith(this.getName())) return null;
  
    for (MoContainer child : this.getChildren()) {
      MoContainer result = child.find(s);
      if (result != null) return result;
    }
  
    List<String> childrenList = omc.getChildren(this.getName());
    if (childrenList.size() != this.getChildren().size()) {
      //noinspection ResultOfMethodCallIgnored
      childrenList.stream().anyMatch(child -> {
        String childName = this.getName() + "." + child;
        if (!s.startsWith(childName)) return false;
        this.addChild(new MoContainer(omc, this, child)).setElement(new MoLater());
        return true;
      });
    }
    return null;
  }
  
  synchronized MoContainer addChild(MoContainer container) {
    for (MoContainer c : this.getChildren()) {
      if (c.similar(container)) return c;
    }
    this.getChildren().add(container);
    return container;
  }
  
  public void removeNotExistingChildren(List<String> names) {
    List<MoContainer> notExistingChildren = this.getChildren().stream().filter(moContainer -> !names.contains(moContainer.getSimpleName())).collect(Collectors.toList());
    notExistingChildren.forEach(this::removeChild);
  }
  
  synchronized private MoContainer removeChild(MoContainer container) {
    int pos = -1;
    for (int i = 0; i < this.getChildren().size(); i++) {
      MoContainer c = this.getChildren().get(i);
      if (c.similar(container)) {
         pos = i;
      }
    }
    if(pos >= 0) this.getChildren().remove(pos);
    return container;
  }
  
  public MoClass getElement() {
    if (this.element instanceof MoLater) {
      try {
        this.element = MoClass.parse(omc, (MoLater) this.element);
      } catch (ParserException e) {
        ActorManager.getInstance().send(new ErrorMessage(MoContainer.class, omc.getID(), new ParserException(tr("Error", "error", "modelica.error_in", getSimpleName()), e)));
      }
    }
    return this.element;
  }
  
  public String getName() {
    if (this.parent == null || this.parent instanceof MoRoot) return this.name;
    return this.parent.getName() + "." + this.name;
  }
  
  public String getSimpleName() {
    return this.name;
  }
  
  
  private boolean similar(MoContainer that) {
    return this.equals(that) || this.getName().equals(that.getName());
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof MoContainer) {
      return this.equals((MoContainer) obj);
    }
    if (MoClass.class.isAssignableFrom(obj.getClass())) {
      MoClass that = (MoClass) obj;
      return (this.element == that);
    }
    return false;
  }
  
  private boolean equals(MoContainer that) {
    return (this == that || this.element == that.element);
  }
  
  @Override
  public int compareTo(MoContainer that) {
    return element.compareTo(that.element);
  }
  
  private void update(OMCompiler omc, int depth) {
    this.omc = omc;
    this.getElement();
    if (depth > 0) {
      for (MoContainer child : this.getChildren()) {
        child.update(omc, depth - 1);
      }
    }
  }
  
  public void update(OMCompiler omc) {
    update(omc, MAX_LOADING_DEPTH);
  }
}
