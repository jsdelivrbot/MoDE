package de.thm.mni.mote.mode.modelica;

/**
 * Created by Marcel Hoppe on 16.09.16.
 */
public class MoPackage extends MoClass {
  
  MoPackage(ClassInformation classInformation, MoLater that) {
    super(classInformation, that);
    PREFIX = "p";
  }
}
