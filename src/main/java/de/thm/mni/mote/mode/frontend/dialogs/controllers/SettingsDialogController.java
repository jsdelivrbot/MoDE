package de.thm.mni.mote.mode.frontend.dialogs.controllers;

import de.thm.mni.mote.mode.config.Settings;
import de.thm.mni.mote.mode.frontend.controllers.Controller;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.controlsfx.control.PopOver;
import org.slf4j.event.Level;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.ResourceBundle;

import static de.thm.mni.mote.mode.util.Translator.tr;

/**
 * Created by Marcel Hoppe on 14.09.16.
 */
public class SettingsDialogController extends Controller {
  
  @FXML private ChoiceBox<Locale> cbLanguage;
  
  @FXML private TextField tfRecentCount;
  @FXML private Slider sRecentCount;
  
  @FXML private TextField tfNotifySeconds;
  @FXML private Slider sNotifySeconds;
  
  @FXML private ChoiceBox<Level> cbLoggerLevel;
  @FXML private ChoiceBox<Level> cbLoggerNotifyLevel;
  
  @FXML private TextField tfModelicaCompiler;
  
  @FXML private Label lModelicaCompiler;
  @FXML private Button btnModelicaCompiler;
  
  private final PopOver po = new PopOver();
  
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    super.initialize(location, resources);
    po.setContentNode(new Label(tr(null, "settings", "global.select_modelica_compiler.popover")));
    po.setArrowLocation(PopOver.ArrowLocation.LEFT_CENTER);
    po.setDetachable(false);
    po.setAutoHide(false);
    po.setHideOnEscape(false);
  
    cbLanguage.setItems(FXCollections.observableArrayList(Settings.SUPPORTED_LANGUAGES));
    cbLanguage.setValue(getSettings().getLang());
    cbLanguage.setConverter(new StringConverter<Locale>() {
      @Override
      public String toString(Locale locale) {
        return tr(null, "settings", "global.supported_languages." + locale.toString().toLowerCase());
      }
  
      @Override
      public Locale fromString(String string) {
        for (Locale locale : Settings.SUPPORTED_LANGUAGES) {
          if (toString(locale).equals(string))
            return locale;
        }
        return null;
      }
    });
    cbLanguage.valueProperty().addListener((observable, oldValue, newValue) -> getSettings().setLang(newValue));
  
    cbLoggerLevel.setItems(FXCollections.observableArrayList(Level.values()));
    cbLoggerLevel.setValue(getSettings().getLogger().getLevel());
    cbLoggerLevel.valueProperty().addListener((observable, oldValue, newValue) -> getSettings().getLogger().setLevel(newValue));
  
    cbLoggerNotifyLevel.setItems(FXCollections.observableArrayList(Level.values()));
    cbLoggerNotifyLevel.getItems().removeAll(Level.TRACE);
    cbLoggerNotifyLevel.setValue(getSettings().getLogger().getNotifyLevel());
    cbLoggerNotifyLevel.valueProperty().addListener((observable, oldValue, newValue) -> getSettings().getLogger().setNotifyLevel(newValue));
  
    if (getSettings().getModelica().getCompiler() != null) {
      Path f = getSettings().getModelica().getCompiler();
      tfModelicaCompiler.setText(f.toString());
      tfModelicaCompiler.positionCaret(f.toString().length() + 1);
    }
    tfModelicaCompiler.textProperty().addListener((observable, oldValue, newValue) -> getSettings().getModelica().setCompiler(Paths.get(newValue)));
  
    initSlider(sRecentCount, tfRecentCount, getSettings().getRecent().getCount(), (observable, oldValue, newValue) -> {
      if (oldValue.intValue() != newValue.intValue()) getSettings().getRecent().setCount(newValue.intValue());
    });
  
    initSlider(sNotifySeconds, tfNotifySeconds, getSettings().getNotification().getSeconds(), (observable, oldValue, newValue) -> {
      if (oldValue.intValue() != newValue.intValue()) getSettings().getNotification().setSeconds(newValue.intValue());
    });
  }
  
  private void initSlider(Slider s, TextField tf, Integer i, ChangeListener<Number> cl) {
    s.setValue(i);
    tf.setText("" + i);
    s.valueProperty().addListener(cl);
    s.valueProperty().addListener((observable, oldValue, newValue) -> tf.setText("" + newValue.intValue()));
  }
  
  @FXML
  private void onModelicaCompilerClick() {
    Path f = fileDialog(
        getSettings().getModelica().getCompiler(),
        tr(null, "settings", "global.select_modelica_compiler")
    );
    
    if (f == null) return;
    tfModelicaCompiler.setText(f.toString());
    tfModelicaCompiler.positionCaret(f.toString().length() + 1);
  }
  
  private Path fileDialog(Path init, String title) {
    title = title.replaceAll(":$", "");
    File f;
    FileChooser fc = new FileChooser();
    fc.setTitle(title);
    if (init != null) {
      fc.setInitialDirectory(init.getParent().toFile());
      fc.setInitialFileName(init.getFileName().toString());
    }
    po.hide();
    f = fc.showOpenDialog(this.getStage());
    return (f == null) ? null : f.toPath();
  }
  
  public void updateUI(Boolean omcStarted) {
    if (!omcStarted) {
      lModelicaCompiler.getStyleClass().add("red-text");
      new Thread(() -> {
        try {
          Thread.sleep(250);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        Platform.runLater(() -> po.show(btnModelicaCompiler));
      }).start();
    } else lModelicaCompiler.getStyleClass().remove("red-text");
  }
}
