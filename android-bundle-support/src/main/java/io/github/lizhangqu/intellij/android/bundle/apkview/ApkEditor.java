/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.lizhangqu.intellij.android.bundle.apkview;

import com.android.SdkConstants;
import com.android.tools.apk.analyzer.*;
import com.android.tools.apk.analyzer.internal.ArchiveTreeNode;
import com.google.common.primitives.Shorts;
import com.google.devrel.gmscore.tools.apk.arsc.Chunk;
import io.github.lizhangqu.intellij.android.bundle.apkview.arsc.ArscViewer;
import io.github.lizhangqu.intellij.android.bundle.apkview.dex.DexFileViewer;
import io.github.lizhangqu.intellij.android.bundle.apkview.diff.ApkDiffPanel;
import com.google.common.base.Charsets;
import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.android.tools.idea.FileEditorUtil.DISABLE_GENERATED_FILE_NOTIFICATION_KEY;

public class ApkEditor extends UserDataHolderBase implements FileEditor, ApkViewPanel.Listener {
  private final Project myProject;
  private final VirtualFile myBaseFile;
  private final VirtualFile myRoot;
  private ApkViewPanel myApkViewPanel;
  private ArchiveContext myArchiveContext;

  private JBSplitter mySplitter;
  private ApkFileEditorComponent myCurrentEditor;

  public ApkEditor(@NotNull Project project, @NotNull VirtualFile baseFile, @NotNull VirtualFile root) {
    myProject = project;
    myBaseFile = baseFile;
    myRoot = root;

    DISABLE_GENERATED_FILE_NOTIFICATION_KEY.set(this, true);

    mySplitter = new JBSplitter(true, "android.apk.viewer", 0.62f);
    mySplitter.setName("apkViwerContainer");

    // Setup focus root for a11y purposes
    // Given that
    // 1) IdeFrameImpl sets up a custom focus traversal policy that unconditionally set te focus to the preferred component
    //    of the editor windows
    // 2) IdeFrameImpl is the default focus cycle root for editor windows
    // (see https://github.com/JetBrains/intellij-community/commit/65871b384739b52b1c0450235bc742d2ba7fb137#diff-5b11919bab177bf9ab13c335c32874be)
    //
    // We need to declare the root component of this custom editor to be a focus cycle root and
    // setup the default focus traversal policy (layout) to ensure the TAB key cycles through all the
    // components of this custom panel.
    mySplitter.setFocusCycleRoot(true);
    mySplitter.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());

    // The APK Analyzer uses a copy of the APK to open it as an Archive. It does so far two reasons:
    // 1. We don't want the editor holding a lock on an APK (applies only to Windows)
    // 2. Since an Archive creates a FileSystem under the hood, we don't want the zip file's contents
    // to change while the FileSystem is open, since this may lead to JVM crashes
    // But if we do a copy, we need to update it whenever the real file changes. So we listen to changes
    // in the VFS as long as this editor is open.
    MessageBusConnection connection = project.getMessageBus().connect(this);
    connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
          if (myBaseFile.equals(event.getFile())) {
            if (myBaseFile.isValid()) {
              refreshApk(baseFile);
            } else {
              // if the file is deleted, the editor is automatically closed..
            }
          }
        }
      }
    });

    refreshApk(myBaseFile);
    mySplitter.setSecondComponent(new JPanel());
  }

  @NotNull
  private static Logger getLog() {
    return Logger.getInstance(ApkEditor.class);
  }

  private void refreshApk(@NotNull VirtualFile apkVirtualFile) {
    disposeArchive();

    try {
      // this temporary copy is destroyed while disposing the archive, see #disposeArchive
      Path copyOfApk = Files.createTempFile(apkVirtualFile.getNameWithoutExtension(), "." + apkVirtualFile.getExtension());
      Files.copy(VfsUtilCore.virtualToIoFile(apkVirtualFile).toPath(), copyOfApk, StandardCopyOption.REPLACE_EXISTING);
      myArchiveContext = Archives.open(copyOfApk);
      myApkViewPanel = new ApkViewPanel(myProject, new ApkParser(myArchiveContext, ApkSizeCalculator.getDefault()));
      myApkViewPanel.setListener(this);
      mySplitter.setFirstComponent(myApkViewPanel.getContainer());
      selectionChanged(null);
    }
    catch (IOException e) {
      getLog().error(e);
      disposeArchive();
      mySplitter.setFirstComponent(new JBLabel(e.toString()));
    }
  }

  /**
   * Changes the editor displayed based on the path selected in the tree.
   */
  @Override
  public void selectionChanged(@Nullable ArchiveTreeNode[] entries) {
    if (myCurrentEditor != null) {
      Disposer.dispose(myCurrentEditor);
      // Null out the field immediately after disposal, in case an exception is thrown later in the method
      myCurrentEditor = null;
    }

    myCurrentEditor = getEditor(entries);
    mySplitter.setSecondComponent(myCurrentEditor.getComponent());
  }

  @Override
  public void selectApkAndCompare() {
    FileChooserDescriptor desc = new FileChooserDescriptor(true, false, false, false, false, false);
    desc.withFileFilter(file -> ApkFileSystem.EXTENSIONS.contains(file.getExtension()));
    VirtualFile file = FileChooser.chooseFile(desc, myProject, null);
    if(file == null) {
      // user canceled
      return;
    }
    VirtualFile oldApk = ApkFileSystem.getInstance().getRootByLocal(file);
    assert oldApk != null;

    DialogBuilder builder = new DialogBuilder(myProject);
    builder.setTitle(oldApk.getName() + " (old) vs " + myRoot.getName() + " (new)");
    ApkDiffPanel panel = new ApkDiffPanel(oldApk, myRoot);
    builder.setCenterPanel(panel.getContainer());
    builder.setPreferredFocusComponent(panel.getPreferredFocusedComponent());
    builder.show();
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return mySplitter;
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myApkViewPanel.getPreferredFocusedComponent();
  }

  @NotNull
  @Override
  public String getName() {
    return myBaseFile.getName();
  }

  @Override
  public void setState(@NotNull FileEditorState state) {
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public boolean isValid() {
    return myBaseFile.isValid();
  }

  @Override
  public void selectNotify() {
  }

  @Override
  public void deselectNotify() {
  }

  @Override
  public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
  }

  @Override
  public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
  }

  @Nullable
  @Override
  public BackgroundEditorHighlighter getBackgroundHighlighter() {
    return null;
  }

  @Nullable
  @Override
  public FileEditorLocation getCurrentLocation() {
    return null;
  }

  @Nullable
  @Override
  public StructureViewBuilder getStructureViewBuilder() {
    return null;
  }

  @Override
  public void dispose() {
    if (myCurrentEditor != null) {
      Disposer.dispose(myCurrentEditor);
      myCurrentEditor = null;
    }
    getLog().info("Disposing ApkEditor with ApkViewPanel: " + myApkViewPanel);
    disposeArchive();
  }

  private void disposeArchive() {
    if (myApkViewPanel != null){
      myApkViewPanel.clearArchive();
    }
    if (myArchiveContext != null) {
      try {
        myArchiveContext.close();
        // the archive was constructed out of a temporary file
        Files.deleteIfExists(myArchiveContext.getArchive().getPath());
      }
      catch (IOException e) {
        getLog().warn(e);
      }
      myArchiveContext = null;
    }
  }

  @NotNull
  private ApkFileEditorComponent getEditor(@Nullable ArchiveTreeNode[] nodes) {
    if (nodes == null || nodes.length == 0) {
      return new EmptyPanel();
    }

    //check if multiple dex files are selected
    //and return a multiple dex viewer
    boolean allDex = true;
    for (ArchiveTreeNode path : nodes) {
       if (!path.getData().getPath().getFileName().toString().endsWith(SdkConstants.EXT_DEX)){
        allDex = false;
        break;
      }
    }

    if (allDex){
      Path[] paths = new Path[nodes.length];
      for (int i = 0; i < nodes.length; i++) {
        paths[i] = nodes[i].getData().getPath();
      }
      return new DexFileViewer(myProject, paths, myBaseFile.getParent());
    }

    //only one file or many files with different extensions are selected
    //we can only show a single editor for a single filetype,
    //so arbitrarily pick the first file:
    ArchiveTreeNode n = nodes[0];
    Path p = n.getData().getPath();
    Path fileName = p.getFileName();
    if ("resources.arsc".equals(fileName.toString())) {
      byte[] arscContent;
      try {
        arscContent = Files.readAllBytes(p);
      }
      catch (IOException e) {
        return new EmptyPanel();
      }
      return new ArscViewer(arscContent);
    }

    if (p.toString().endsWith(SdkConstants.EXT_DEX)) {
      return new DexFileViewer(myProject, new Path[]{p}, myBaseFile.getParent());
    }

    VirtualFile file = createVirtualFile(n.getData().getArchive(), p);
    Optional<FileEditorProvider> providers = getFileEditorProviders(file);
    if (!providers.isPresent()) {
      return new EmptyPanel();
    }
    else if (file != null) {
      FileEditor editor = providers.get().createEditor(myProject, file);
      return new ApkFileEditorComponent() {
        @NotNull
        @Override
        public JComponent getComponent() {
          return editor.getComponent();
        }

        @Override
        public void dispose() {
          Disposer.dispose(editor);
        }
      };
    } else {
      return new EmptyPanel();
    }
  }

  @Nullable
  private VirtualFile createVirtualFile(@NotNull Archive archive, @NotNull Path p) {
    Path name = p.getFileName();
    if (name == null) {
      return null;
    }

    // No virtual file for directories
    if (Files.isDirectory(p)) {
      return null;
    }

    // Read file contents and decode it
    byte[] content;
    try {
      content = Files.readAllBytes(p);
    }
    catch (IOException e) {
      getLog().warn(String.format("Error loading entry \"%s\" from archive", p.toString()), e);
      return null;
    }

    if (archive.isBinaryXml(p, content) || isBinaryXml(p, content)) {
      content = BinaryXmlParser.decodeXml(name.toString(), content);
      return ApkVirtualFile.create(p, content);
    }

    if (archive.isProtoXml(p, content)) {
      try {
        ProtoXmlPrettyPrinter prettyPrinter = new ProtoXmlPrettyPrinterImpl();
        content = prettyPrinter.prettyPrint(content).getBytes(Charsets.UTF_8);
      }
      catch (IOException e) {
        // Ignore error, show encoded content
        getLog().warn(String.format("Error decoding XML entry \"%s\" from archive", p.toString()), e);
      }
      return ApkVirtualFile.create(p, content);
    }

    VirtualFile file = JarFileSystem.getInstance().findLocalVirtualFileByPath(archive.getPath().toString());
    if (file != null) {
      return file.findFileByRelativePath(p.toString());
    }
    else {
      return ApkVirtualFile.create(p, content);
    }
  }

  boolean isBinaryXml(Path p, byte[] content) {
    if (!p.toString().endsWith(SdkConstants.DOT_XML)) {
      return false;
    } else {
      Path name = p.getFileName();
      if (name == null) {
        return false;
      } else {
        Path contents = p.getFileSystem().getPath("/");
        boolean manifest = p.equals(contents.resolve(SdkConstants.FN_ANDROID_MANIFEST_XML));
        boolean insideResFolder = p.startsWith(contents.resolve(SdkConstants.FD_RES));
        boolean insideResRaw = p.startsWith(contents.resolve(SdkConstants.FD_RES).resolve(SdkConstants.FD_RES_RAW));
        boolean xmlResource = insideResFolder && !insideResRaw;
        if (!manifest && !xmlResource) {
          return false;
        } else {
          short code = Shorts.fromBytes(content[1], content[0]);
          return code == Chunk.Type.XML.code();
        }
      }
    }
  }

  @NotNull
  private Optional<FileEditorProvider> getFileEditorProviders(@Nullable VirtualFile file) {
    if (file == null || file.isDirectory()) {
      return Optional.empty();
    }

    FileEditorProvider[] providers = FileEditorProviderManager.getInstance().getProviders(myProject, file);

    // skip 9 patch editor since nine patch information has been stripped out
    return Arrays.stream(providers).filter(
      fileEditorProvider -> !fileEditorProvider.getClass().getName().equals("com.android.tools.idea.editors.NinePatchEditorProvider")).findFirst();
  }
}
