package de.thm.mni.mote.mode.config.xml;

import org.simpleframework.xml.transform.Transform;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Marcel Hoppe on 08.10.16.
 */
class PathTransform implements Transform<Path> {
  @Override
  public Path read(String value) throws Exception {
    return Paths.get(value);
  }
  
  @Override
  public String write(Path value) throws Exception {
    return value.toString();
  }
}
