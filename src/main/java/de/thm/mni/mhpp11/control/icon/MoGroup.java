package de.thm.mni.mhpp11.control.icon;

import de.thm.mni.mhpp11.shape.*;
import de.thm.mni.mhpp11.shape.interfaces.HasInitialStroke;
import de.thm.mni.mhpp11.util.parser.models.MoClass;
import de.thm.mni.mhpp11.util.parser.models.MoVariable;
import de.thm.mni.mhpp11.util.parser.models.graphics.*;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hobbypunk on 26.10.16.
 */

@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
public abstract class MoGroup extends Group {
  private final Group basis = new Group();
  javafx.scene.shape.Rectangle coordianteSystem = null; //TODO: add InitialStroke Interface
  @Getter private final MoClass moClass;
  
  private Map<MoVariable, MoIconGroup> data = new HashMap<>();
  
  private Double moveX = 100.0;
  private Double moveY = 100.0;
  
  private Double initialScale = 1.0;
  private Boolean preserveAspectRatio = true;
  
  @Getter private Scale scale = new Scale();
  @Getter private Affine flipping = new Affine();
  @Getter private Translate position = new Translate();
  
  @Getter private Boolean flippedX = false; //X axis is flipped
  @Getter private Boolean flippedY = false; //Y axis is flipped
  
  MoGroup(@NonNull MoClass moClass) {
    this.moClass = moClass;
    this.getChildren().add(basis);
    basis.getTransforms().addAll(scale, flipping, position);
  }
  
  final void init() {
    if (moClass.getIcon() != null) {
      initCoordinateSystem();
      initImage();
    }
  }
  
  //TODO: prevent scaling of stroke!
  private void preventScaling(Double scaleX, Double scaleY) {
    getBasis().getChildren().forEach(node -> {
      if (node instanceof MoGroup) ((MoGroup) node).preventScaling(scaleX * scale.getX(), scaleY * scale.getY());
      else if (node instanceof HasInitialStroke) {
        HasInitialStroke s = (HasInitialStroke) node;
        Double factor = Math.max(scaleX * scale.getX(), scaleY * scale.getY());
        factor = (factor < 1.) ? 1. : factor; //dirty hack
        s.setStrokeWidth(s.getInitialStrokeWidth() * (1 / factor));
      }
    });
  }
  
  public MoGroup scaleTo(Double newWidth, Double newHeight) {
    Point2D extent0 = this.getMoClass().getIcon().getMoCoordinateSystem().getExtent().get(0).getValue();
    Point2D extent1 = this.getMoClass().getIcon().getMoCoordinateSystem().getExtent().get(1).getValue();
  
  
    Double oldWidth = Math.max(extent0.getX(), extent1.getX()) - Math.min(extent0.getX(), extent1.getX());
    Double oldHeight = Math.max(extent0.getY(), extent1.getY()) - Math.min(extent0.getY(), extent1.getY());
    
    
    scale.setX(newWidth / oldWidth);
    scale.setY(newHeight / oldHeight);
    preventScaling(1., 1.);
    return this;
  }
  
  private void initCoordinateSystem() {
    MoCoordinateSystem mcs = this.getMoClass().getIcon().getMoCoordinateSystem();
  
    Point2D extent0 = mcs.getExtent().get(0).getValue();
    Point2D extent1 = mcs.getExtent().get(1).getValue();
  
    Double minX = Math.min(extent0.getX(), extent1.getX());
    Double minY = Math.min(extent0.getY(), extent1.getY());
    Double width = Math.max(extent0.getX(), extent1.getX()) - Math.min(extent0.getX(), extent1.getX());
    Double height = Math.max(extent0.getY(), extent1.getY()) - Math.min(extent0.getY(), extent1.getY());
  
    coordianteSystem = new javafx.scene.shape.Rectangle(minX, minY, width, height);
    coordianteSystem.setStrokeWidth(1.);
    coordianteSystem.setStroke(Color.BLACK);
    coordianteSystem.setFill(Color.BLACK);
    coordianteSystem.setOpacity(.1);
    basis.getChildren().add(coordianteSystem);
  
  
    flipping.append(Transform.scale(1., -1.));
    flipping.append(Transform.translate(0, -height));
  
    extentChange(extent0, extent1);
  
    position.setX(-minX);
    position.setY(-minY);
  
    if (mcs.getPreserveAspectRatio() != null) this.setPreserveAspectRatio(mcs.getPreserveAspectRatio());
    if (mcs.getInitialScale() != null) this.setInitialScale(mcs.getInitialScale());
  
  }
  
  private void extentChange(Point2D extent0, Point2D extent1) {
    if (!flippedX && extent1.getX() < extent0.getX()) {
      flipping.append(Transform.scale(-1., 1.));
      flipping.append(Transform.translate(coordianteSystem.getWidth(), 0));
      flippedX = true;
    } else if (flippedX && extent0.getX() < extent1.getX()) {
      flipping.append(Transform.scale(-1., 1.));
      flipping.append(Transform.translate(coordianteSystem.getWidth(), 0).createInverse());
      flippedX = false;
    }
    
    if (!flippedY && extent1.getY() < extent0.getY()) {
      flipping.append(Transform.scale(1., -1.));
      flipping.append(Transform.translate(0, -coordianteSystem.getHeight()));
      flippedY = true;
    } else if (flippedY && extent0.getY() < extent1.getY()) {
      flipping.append(Transform.scale(1., -1.));
      flipping.append(Transform.translate(0, -coordianteSystem.getHeight()).createInverse());
      flippedY = false;
    }
  }
  
  void initVariable(MoVariable mv) {
    if (mv.getPlacement() == null || (mv.getPlacement().getIconTransformation() == null && mv.getPlacement().getDiagramTransformation() == null)) return;
    MoIconGroup mip = new MoIconGroup(mv, false);
    data.put(mv, mip);
    this.add(mip);
  }
  
  protected abstract void initImage();
  
  public void add(Node node) {
    basis.getChildren().add(node);
  }
  
  void initImage(MoGraphic mg) {
    if (mg instanceof MoText) this.add(new Text(this, (MoText) mg));
    else if (mg instanceof MoRectangle) {
      this.add(new Rectangle(this, (MoRectangle) mg));
    } else if (mg instanceof MoEllipse) {
      MoEllipse me = (MoEllipse) mg;
      if (me.getEndAngle() + me.getStartAngle() == 360) this.add(new Ellipse(this, me));
      else this.add(new Arc(this, me));
    } else if (mg instanceof MoLine) {
      this.add(new Line(this, (MoLine) mg));
    } else if (mg instanceof MoPolygon) {
      this.add(new Polygon(this, (MoPolygon) mg));
    }
  }
  
  public void setInternalStyle(String style) {
    basis.setStyle(style);
  }
  
}
