package de.thm.mni.mote.mode.config.model;

import de.thm.mni.mote.mode.config.Settings;
import de.thm.mni.mote.mode.config.xml.MyMatcher;
import de.thm.mni.mote.mode.util.Utilities;
import lombok.*;
import lombok.experimental.Accessors;
import org.apache.commons.io.IOUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Created by Marcel Hoppe on 11.09.16.
 */
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
@Getter
@Accessors(chain = true)
@ToString
@NoArgsConstructor
public class Project {
  private static final String importFileName = "package.imports";
  
  
  @Setter private Path projectPath;
  @Element
  @NonNull private String name;
  @Element
  @NonNull private Path moFile;
  @Element
  @NonNull private Date lastOpened;
  @ElementList(required = false)
  @NonNull private List<String> systemLibraries = new ArrayList<>();
  @NonNull private List<Path> projectLibraries = new ArrayList<>();
  
  @Getter(AccessLevel.PRIVATE) private Boolean isNewProject = false;
  @Getter(AccessLevel.PRIVATE) private String comment;
  @Getter(AccessLevel.PRIVATE) private String documentation;
  
  @Builder
  public Project(Boolean isNewProject, String name, String comment, String documentation, Path moFile, Path projectPath, @Singular List<String> systemLibraries, @Singular List<Path> projectLibraries) {
    this.isNewProject = (isNewProject != null && isNewProject);
    this.name = name;
    this.comment = (comment == null) ? "" : comment;
    this.documentation = (documentation == null) ? "" : documentation;
    this.moFile = moFile;
    this.projectPath = projectPath;
    this.lastOpened = new Date();
    this.systemLibraries.clear();
    this.systemLibraries.addAll(systemLibraries);
    this.projectLibraries.clear();
    this.projectLibraries.addAll(projectLibraries);
  }
  
  public Path getMoFile() {
    if (this.moFile.isAbsolute())
      return this.moFile;
    return projectPath.resolveSibling(this.moFile).normalize();
  }
  
  public void updateLastOpened() {
    this.lastOpened = new Date();
  }
  
  public void save() throws Exception {
    if (isNewProject) {
      isNewProject = false;
      List<String> fileContent = IOUtils.readLines(Utilities.getTemplate("base_package.mo"), StandardCharsets.UTF_8);
      for (int i = 0; i < fileContent.size(); i++) {
        fileContent.set(i, fileContent.get(i).replaceAll("<name>", this.name.replaceAll("\\s", "")).replaceFirst("<comment>", this.comment).replaceFirst("<documentation>", this.documentation));
      }
      if (!Files.exists(this.projectPath.getParent()))
        Files.createDirectory(this.projectPath.getParent());
      Files.write(this.getMoFile(), fileContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }
    Serializer serializer = new Persister(new MyMatcher());
    serializer.write(this, this.projectPath.toFile());
    this.saveProjectLibraries();
    Settings.load().getRecent().remove(this.getProjectPath());
    Settings.load().getRecent().add(this.getProjectPath());
    Settings.load().save();
  }
  
  private void saveProjectLibraries() throws IOException {
    Path importsFile = this.getMoFile().resolveSibling(importFileName);
    if (projectLibraries.isEmpty()) {
      if (Files.exists(importsFile)) Files.delete(importsFile);
      return;
    }
    
    Files.write(importsFile, projectLibraries.stream().map(Path::toString).collect(Collectors.toList()), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }
  
  private void loadProjectLibraries() throws IOException {
    Path importsFile = this.getMoFile().resolveSibling(importFileName);
    if (!Files.exists(importsFile)) return;
    projectLibraries.clear();
    projectLibraries.addAll(Files.lines(importsFile).map(Paths::get).collect(Collectors.toList()));
  }
  
  public static Project load(Path projectFile) throws Exception {
    Serializer serializer = new Persister(new MyMatcher());
    Project p = serializer.read(Project.class, projectFile.toFile()).setProjectPath(projectFile);
    p.loadProjectLibraries();
    return p;
  }
}
