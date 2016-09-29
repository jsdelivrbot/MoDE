package de.thm.mni.mhpp11.controller;

import de.thm.mni.mhpp11.util.UTF8ResourceBundleControl;
import de.thm.mni.mhpp11.util.config.model.Logger.LEVEL;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.util.Pair;
import javafx.util.StringConverter;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by hobbypunk on 14.09.16.
 */
public class SettingsController extends Controller {
  
  @FXML private ChoiceBox<Pair<String, String>> cbLanguage;
  
  @FXML private TextField tfRecentCount;
  @FXML private Slider sRecentCount;
  
  @FXML private TextField tfNotifySeconds;
  @FXML private Slider sNotifySeconds;
  
  @FXML private ChoiceBox<LEVEL> cbLoggerLevel;
  @FXML private ChoiceBox<LEVEL> cbLoggerNotifyLevel;
  
  @FXML private TextField tfOMLibrary;
  
  
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    super.initialize(location, resources);
  
    ResourceBundle languages = ResourceBundle.getBundle("de/thm/mni/mhpp11/i18n/Languages", settings.getLang(), new UTF8ResourceBundleControl());
    Pair<String, String> lang = new Pair<>(settings.getLang().toString(), languages.getString(settings.getLang().toString()));
    
    List<Pair<String, String>> langs = new ArrayList<>();
    for(String l: languages.keySet())
      langs.add(new Pair<>(l, languages.getString(l)));
        
    cbLanguage.setItems(FXCollections.observableArrayList(langs));
    cbLanguage.setValue(lang);
    cbLanguage.setConverter(new StringConverter<Pair<String, String>>() {
      @Override
      public String toString(Pair<String, String> object) {
        return object.getValue();
      }
  
      @Override
      public Pair<String, String> fromString(String string) {
        for(Pair<String, String> p: langs) {
          if(p.getValue().equals(string))
            return p;
        }
        return null;
      }
    });
    cbLanguage.valueProperty().addListener((observable, oldValue, newValue) -> settings.setLang(new Locale(newValue.getKey())));
  
    cbLoggerLevel.setItems(FXCollections.observableArrayList(LEVEL.values()));
    cbLoggerLevel.setValue(settings.getLogger().getLevel());
    cbLoggerLevel.valueProperty().addListener((observable, oldValue, newValue) -> settings.getLogger().setLevel(newValue));
    
    cbLoggerNotifyLevel.setItems(FXCollections.observableArrayList(LEVEL.values()));
    cbLoggerNotifyLevel.setValue(settings.getLogger().getNotifyLevel());
    cbLoggerNotifyLevel.valueProperty().addListener((observable, oldValue, newValue) -> settings.getLogger().setNotifyLevel(newValue));
  
    if (settings.getModelica().getOmc() != null) {
      File f = settings.getModelica().getOmc();
      tfOMLibrary.setText(f.getPath());
      tfOMLibrary.positionCaret(f.getPath().length() + 1);
    }
    
    tfOMLibrary.textProperty().addListener((observable, oldValue, newValue) -> settings.getModelica().setModelicaLibrary(new File(newValue)));
  
    initSlider(sRecentCount, tfRecentCount, settings.getRecent().getCount(), (observable, oldValue, newValue) -> {
      if(oldValue.intValue() != newValue.intValue()) settings.getRecent().setCount(newValue.intValue());
    });
    initSlider(sNotifySeconds, tfNotifySeconds, settings.getNotification().getSeconds(), (observable, oldValue, newValue) -> {
      if(oldValue.intValue() != newValue.intValue()) settings.getNotification().setSeconds(newValue.intValue());
    });
  }
  
  private void initSlider(Slider s, TextField tf, Integer i, ChangeListener<Number> cl) {
    s.setValue(i);
    tf.setText("" + i);
    s.valueProperty().addListener(cl);
    s.valueProperty().addListener((observable, oldValue, newValue) -> tf.setText("" + newValue.intValue()));
  }
  
  @FXML
  private void onOMLibraryClick() {
    DirectoryChooser dc = new DirectoryChooser();
    File f = settings.getModelica().getOmc();
    if (f != null) dc.setInitialDirectory(f);
    f = dc.showDialog(this.stage);
    
    if (f == null) return;
    tfOMLibrary.setText(f.getPath());
    tfOMLibrary.positionCaret(f.getPath().length() + 1);
  }
}