package de.thm.mni.mote.mode.modelica;

import de.thm.mni.mote.mode.backend.omc.OMCompiler;
import de.thm.mni.mote.mode.modelica.annotations.MoAnnotation;
import de.thm.mni.mote.mode.modelica.annotations.MoPlacement;
import de.thm.mni.mote.mode.modelica.graphics.MoText;
import de.thm.mni.mote.mode.modelica.interfaces.Changeable;
import de.thm.mni.mote.mode.modelica.interfaces.MoElement;
import de.thm.mni.mote.mode.util.ImmutableListCollector;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.FilteredList;
import lombok.*;
import lombok.experimental.NonFinal;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Marcel Hoppe on 20.10.16.
 */
@Value
@EqualsAndHashCode(callSuper = true, exclude = {"parent"})
public class MoVariable extends MoElement implements Changeable {
  ObjectProperty<Change> unsavedChanges = new SimpleObjectProperty<>(Change.NONE);
  
  static Pattern PATTERN = Pattern.compile("(^\"|\"$)");
  
  MoClass parent;
  Boolean isParameter;
  String name;
  Specification kind;
  @NonFinal @Setter MoContainer type = null;
  String line;
  @NonFinal @Setter(AccessLevel.PRIVATE) String value = "";
  
  private final Map<MoVariable, String> paramValues = new HashMap<>();
  
  public enum Specification {
    NONE,
    INPUT,
    OUTPUT,
    FLOW
  }
  
  public MoVariable(@NonNull MoClass parent, MoContainer type, String name) {
    this(parent, Specification.NONE, false, type, name, "", null, "");
  }
  
  @Builder
  @SuppressWarnings("SameParameterValue")
  private MoVariable(@NonNull MoClass parent, Specification kind, Boolean isParameter, MoContainer type, String name, String comment, @Singular List<MoAnnotation> annotations, String line) {
    super("v", comment);
    this.name = name;
    this.parent = parent;
    this.kind = kind;
    this.isParameter = (isParameter != null && isParameter);
    this.line = line;
  
    if (type != null) {
      this.type = type;
      if(this.line != null) {
        String l = this.line.replace("\"" + this.getComment() + "\"", "");
        if (l.matches(".*?" + type.getSimpleName() + "\\s+" + this.getName() + "\\s+=.*")) parseValue(l);
        else if (l.matches(".*?" + this.getName() + "\\s*\\(.*")) this.parseParameter(l);
      }
    }
    if (annotations != null) this.addAllAnnotations(annotations);
    if (getPlacement() != null) getPlacement().setChangeParent(this);
    initChangeListener();
  }
  
  private void parseParameter(String line) {
    line = line.replaceFirst("^.*?" + this.getName() + "\\s*\\(", "");
    line = line.replaceFirst("\\s*//.+$", "");
    line = line.replaceFirst("\\)[^)]*?;$", "");
    line = line.replaceFirst("\\)[^)]*?annotation.*$", "");
    String[] params = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); //http://stackoverflow.com/questions/1757065/java-splitting-a-comma-separated-string-but-ignoring-commas-in-quotes
    for (String param : params) {
      for (MoVariable p : getParameters()) {
        if (param.matches("^" + p.getName() + "\\s*=.*$")) {
          paramValues.put(p, param.replaceFirst(p.getName() + "\\s*=\\s*", ""));
        }
      }
    }
  }
  
  private void parseValue(String line) {
    this.value = line;
    this.value = this.value.replaceFirst("^.*?" + this.getName() + "\\s+=\\s+", "");
    this.value = this.value.replace("\"" + this.getComment() + "\"", "");
    this.value = this.value.replaceFirst("\\s*//.+$", "");
    this.value = this.value.replaceFirst("\\s+;$", "");
    
    this.value = this.value.replaceFirst("annotation.*$", "");
  }
  
  @Override
  public Changeable getChangeParent() {
    return getParent();
  }
  
  @Override
  public List<Changeable> getChangeChildren() {
    List<Changeable> list = new ArrayList<>();
    if (this.getPlacement() != null)
      list.add(this.getPlacement());
    return list;
  }
  
  private List<MoVariable> getVariables() {
    return getType().getElement().getVariables();
  }
  
  public List<MoVariable> getParameters() {
    try {
      return getVariables().stream().filter(MoVariable::getIsParameter).collect(Collectors.toList());
    } catch (NullPointerException e) {
      e.printStackTrace();
      return Collections.emptyList();
    }
  }
  
  public FilteredList<MoConnection> getConnections() {
    return getConnections(this.parent);
  }
  
  public FilteredList<MoConnection> getFromConnections() {
    return getConnections().filtered(conn -> conn.fromContains(this));
  }
  
  public FilteredList<MoConnection> getToConnections() {
    return getConnections().filtered(conn -> conn.toContains(this));
  }
  
  public FilteredList<MoConnection> getConnections(MoClass baseParent) {
    return baseParent.getConnections().filtered(moConnection -> moConnection.contains(this));
  }
  
  public MoPlacement getPlacement() {
    for (MoAnnotation ma : getAnnotations())
      if (ma instanceof MoPlacement) return (MoPlacement) ma;
    return null;
  }
  
  public String getIndicator() {
    return String.format("%s %s", type.getName(), getName());
  }
  
  public void initReplaceText() {
    type.getElement().getIcon().getMoGraphics().stream().filter(mg -> mg instanceof MoText).forEach(mg -> ((MoText) mg).initReplaceText(this));
  }
  
  @Override
  public String toString() {
    if (MoClass.getBases().contains(type)) return "";
    
    return String.format("%s() \"%s\" annotation(%s);", getIndicator(), getComment(), MoAnnotation.toString(getAnnotations()));
  }
  
  @Override
  public String getSimpleName() {
    return getName();
  }
  
  public static List<MoVariable> parse(OMCompiler omc, MoClass parent) {
    List<MoVariable> list = new ArrayList<>();
    try {
      for (Map<String, String> m : omc.getVariables(parent.getName(), parent.getClassInformation())) {
        list.add(parse(omc, parent, m));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  
    return list.stream().filter(Objects::nonNull).collect(ImmutableListCollector.toImmutableList());
  }
  
  private static MoVariable parse(OMCompiler omc, MoClass parent, Map<String, String> m) {
    if (Boolean.parseBoolean(m.get("isProtected"))) return null;
    
    MoVariableBuilder mb = builder();
    if (Boolean.parseBoolean(m.get("isFlow"))) {
      mb.kind(Specification.FLOW);
    } else if (!m.get("inputOutput").isEmpty()) {
      if (m.get("inputOutput").contains("input")) mb.kind(Specification.INPUT);
      else mb.kind(Specification.OUTPUT);
    } else mb.kind(Specification.NONE);
    mb.isParameter(Boolean.parseBoolean(m.get("isParam")));
    mb.parent(parent);
    mb.type(MoClass.findClass(m.get("type")));
    mb.name(m.get("name"));
    mb.comment(m.get("comment"));
    mb.line(m.get("line"));
    if (m.containsKey("annotation"))
      MoAnnotation.parse(omc, m.get("annotation")).forEach(mb::annotation);
    return mb.build();
  }
}