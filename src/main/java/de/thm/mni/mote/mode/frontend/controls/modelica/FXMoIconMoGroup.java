package de.thm.mni.mote.mode.frontend.controls.modelica;

import de.thm.mni.mote.mode.modelica.MoContainer;
import de.thm.mni.mote.mode.modelica.annotations.MoIcon;
import de.thm.mni.mote.mode.modelica.graphics.MoText;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Created by Marcel Hoppe on 19.09.16.
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class FXMoIconMoGroup extends FXMoGroup {
  
  @Getter private final MoIcon image;
  
  public FXMoIconMoGroup(MoContainer parent) {
    super(parent);
    this.image = parent.getElement().getIcon();
    init();
  }
  
  public FXMoIconMoGroup(MoIcon image) {
    super(new MoContainer(null, null, ""));
    this.image = image;
    init();
  }
  
  protected void initImage() {
    this.image.getMoGraphics().stream().filter(mg -> !(mg instanceof MoText) || ((MoText) mg).getShowAlways()).forEach(this::initImage);
  }
}
