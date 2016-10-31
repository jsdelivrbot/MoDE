package de.thm.mni.mhpp11.util.parser.models.graphics;

import de.thm.mni.mhpp11.util.config.model.Point;
import de.thm.mni.mhpp11.util.parser.models.annotations.MoIcon;
import de.thm.mni.mhpp11.util.parser.models.graphics.Utilities.Smooth;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hobbypunk on 11.10.16.
 */
public class MoDefaults {
  
  private static MoIcon moPackage = null;
  private static MoIcon moModel = null;
  private static MoIcon moFunction = null;
  
  private static MoGraphic getMoGraphic() {
    return new MoGraphic(null, null, null);
  }
  
  private static MoFilledShape getMoFilledShape(Color fillColor, Color borderColor) {
    return new MoFilledShape(getMoGraphic(), fillColor, borderColor, Utilities.LinePattern.SOLID, Utilities.FillPattern.SOLID, 2.);
  }
  
  private static MoIcon newAnything(Color c, List<Point<Double, Double>> points, Smooth smooth) {
    MoCoordinateSystem mcs = new MoCoordinateSystem();
    List<MoGraphic> list = new ArrayList<>();
    
    list.add(new MoRectangle(getMoFilledShape(c, Color.TRANSPARENT), new Point<>(-100., 100.), new Point<>(100., -100.), MoRectangle.BorderPattern.NONE, 0.));
    list.add(new MoPolygon(getMoFilledShape(Color.BLACK, c), points, smooth));
    return new MoIcon(mcs, list);
    
  }
  
  public static MoIcon newPackage() {
    if (moPackage == null) {
      List<Point<Double, Double>> points = new ArrayList<>();
      points.add(new Point<>(-40., -70.));
      points.add(new Point<>(-40., 70.));
      points.add(new Point<>(-20., 70.));
      points.add(new Point<>(0., 70.));
      points.add(new Point<>(30., 35.));
      points.add(new Point<>(0., 0.));
      points.add(new Point<>(-20., 0.));
    
      points.add(new Point<>(-20., 50.));
      points.add(new Point<>(-5., 50.));
      points.add(new Point<>(10., 35.));
      points.add(new Point<>(-5., 20.));
      points.add(new Point<>(-20., 20.));
    
      points.add(new Point<>(-20., 0.));
      points.add(new Point<>(-20., -70.));
      points.add(new Point<>(-40., -70.));
    
      moPackage = newAnything(Color.DARKRED, points, Utilities.Smooth.BEZIER);
    }
    return moPackage;
  }
  
  public static MoIcon newModel() {
    if (moModel == null) {
      List<Point<Double, Double>> points = new ArrayList<>();
      points.add(new Point<>(-60., -70.));
      points.add(new Point<>(-60., 70.));
      points.add(new Point<>(-40., 70.));
    
      points.add(new Point<>(0., 0.));
    
      points.add(new Point<>(40., 70.));
      points.add(new Point<>(60., 70.));
      points.add(new Point<>(60., -70.));
      points.add(new Point<>(40., -70.));
      points.add(new Point<>(40., 40.));
    
      points.add(new Point<>(0., -30.));
    
      points.add(new Point<>(-40., 40.));
      points.add(new Point<>(-40., -70.));
      points.add(new Point<>(-60., -70.));
      moModel = newAnything(Color.BLUE, points, Utilities.Smooth.NONE);
    }
    return moModel;
  }
  
  public static MoIcon newFunction() {
    if (moFunction == null) moFunction = newAnything(Color.GREEN, new ArrayList<>(), Utilities.Smooth.NONE);
    return moFunction;
  }
}