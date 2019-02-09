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
package io.github.lizhangqu.intellij.android.bundle.apkview.dex;

import com.android.tools.adtui.common.ColumnTreeBuilder;
import com.android.tools.apk.analyzer.FilteredTreeModel;
import com.android.tools.apk.analyzer.dex.*;
import com.android.tools.apk.analyzer.dex.tree.DexElementNode;
import com.android.tools.apk.analyzer.dex.tree.DexPackageNode;
import com.android.tools.apk.analyzer.internal.ProguardMappingFiles;
import io.github.lizhangqu.intellij.android.bundle.apkview.ApkFileEditorComponent;
import io.github.lizhangqu.intellij.android.bundle.apkview.ApkViewPanel;
import com.android.tools.idea.concurrent.EdtExecutor;
import com.android.tools.proguard.ProguardMap;
import com.android.tools.proguard.ProguardSeedsMap;
import com.android.tools.proguard.ProguardUsagesMap;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.*;
import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.tree.TreeModelAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.PooledThreadExecutor;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.List;

public class DexFileViewer implements ApkFileEditorComponent {
  private final Disposable myDisposable;
  private final JBLoadingPanel myLoadingPanel;

  private final Tree myTree;
  private final JPanel myTopPanel;

  @NotNull private final Path[] myDexFiles;
  @NotNull private final Project myProject;
  @NotNull private final VirtualFile myApkFolder;
  @NotNull private final DexViewFilters myDexFilters;
  private final DexTreeNodeRenderer myDexTreeRenderer;

  @Nullable private ProguardMappings myProguardMappings;
  private boolean myDeobfuscateNames;
  private ListenableFuture<DexReferences> myDexReferences;

  @NotNull public static final NotificationGroup LOGGING_NOTIFICATION = NotificationGroup.logOnlyGroup("APK Analyzer (Info)");
  @NotNull public static final NotificationGroup BALLOON_NOTIFICATION = NotificationGroup.balloonGroup("APK Analyzer (Important)");

  public DexFileViewer(@NotNull Project project, @NotNull Path[] dexFiles, @NotNull VirtualFile apkFolder) {
    myDexFiles = dexFiles;
    myProject = project;
    myApkFolder = apkFolder;

    //noinspection Convert2Lambda // we need a new instance of this disposable every time, not just a lambda method
    //noinspection AnonymousInnerClassMayBeStatic
    myDisposable = new Disposable() {
      @Override
      public void dispose() {
      }
    };

    myLoadingPanel = new JBLoadingPanel(new BorderLayout(), myDisposable);
    myLoadingPanel.startLoading();

    myTree = new Tree(new DefaultTreeModel(new LoadingNode()));
    myTree.setRootVisible(true);
    myTree.setShowsRootHandles(true);

    new TreeSpeedSearch(myTree, path -> {
      Object o = path.getLastPathComponent();
      if (!(o instanceof DexElementNode)) {
        return "";
      }

      DexElementNode node = (DexElementNode)o;
      return node.getName();
    }, true);

    myDexTreeRenderer = new DexTreeNodeRenderer();

    ColumnTreeBuilder builder = new ColumnTreeBuilder(myTree)
      .addColumn(new ColumnTreeBuilder.ColumnBuilder()
                   .setName("Class")
                   .setPreferredWidth(500)
                   .setHeaderAlignment(SwingConstants.LEFT)
                   .setComparator(Comparator.comparing(DexElementNode::getName).reversed())
                   .setRenderer(myDexTreeRenderer))
      .addColumn(new ColumnTreeBuilder.ColumnBuilder()
                   .setName("Defined Methods")
                   .setPreferredWidth(100)
                   .setHeaderAlignment(SwingConstants.LEFT)
                   .setComparator(Comparator.comparing(DexElementNode::getMethodDefinitionsCount))
                   .setRenderer(new MethodCountRenderer(true)))
      .addColumn(new ColumnTreeBuilder.ColumnBuilder()
                   .setName("Referenced Methods")
                   .setPreferredWidth(100)
                   .setHeaderAlignment(SwingConstants.LEFT)
                   .setComparator(Comparator.comparing(DexElementNode::getMethodReferencesCount))
                   .setRenderer(new MethodCountRenderer(false)))
      .addColumn(new ColumnTreeBuilder.ColumnBuilder()
                   .setName("Size")
                   .setPreferredWidth(50)
                   .setHeaderAlignment(SwingConstants.LEFT)
                   .setComparator(Comparator.comparing(DexElementNode::getSize))
                   .setRenderer(new SizeRenderer()));

    builder.setTreeSorter((Comparator<DexElementNode> comparator, SortOrder order) -> {
      if (comparator != null) {
        TreeModel model = myTree.getModel();
        TreePath selectionPath = myTree.getSelectionPath();

        Object root = model.getRoot();
        if (root instanceof DexElementNode) {
          ((DexElementNode)root).sort(comparator.reversed());
        }

        if (model instanceof DefaultTreeModel) {
          ((DefaultTreeModel)model).reload();
        }

        myTree.setSelectionPath(selectionPath);
        myTree.scrollPathToVisible(selectionPath);
      }
    });

    JComponent columnTree = builder.build();
    myLoadingPanel.add(columnTree, BorderLayout.CENTER);
    myTopPanel = new JPanel(new BorderLayout());
    myLoadingPanel.add(myTopPanel, BorderLayout.NORTH);

    myDexFilters = new DexViewFilters();

    DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(new ShowFieldsAction(myTree, myDexFilters));
    actionGroup.add(new ShowMethodsAction(myTree, myDexFilters));
    actionGroup.add(new ShowReferencedAction(myTree, myDexFilters));
    actionGroup.addSeparator();
    actionGroup.add(new ShowRemovedNodesAction(myTree, myDexFilters));
    actionGroup.add(new DeobfuscateNodesAction());
    actionGroup.add(new LoadProguardAction());
    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, actionGroup, true);
    myTopPanel.add(toolbar.getComponent(), BorderLayout.WEST);

    ActionGroup group = createPopupActionGroup(myTree);
    PopupHandler
      .installPopupHandler(myTree, group, ActionPlaces.UNKNOWN, ActionManager.getInstance());

    initDex();
  }

  @NotNull
  private ActionGroup createPopupActionGroup(@NotNull Tree tree) {
    final DefaultActionGroup group = new DefaultActionGroup();
    group.add(new ShowDisassemblyAction(tree));
    group.add(new ShowReferencesAction(tree, this));
    group.add(new GenerateProguardKeepRuleAction(tree));
    return group;
  }

  public void selectProguardMapping() {
    SelectProguardMapsDialog dialog = new SelectProguardMapsDialog(myProject, myApkFolder);
    try {
      if (!dialog.showAndGet()) { // user cancelled
        return;
      }

      ProguardMappingFiles mappingFiles = dialog.getMappingFiles();
      List<String> loaded = new ArrayList<>(3);
      List<String> errors = new ArrayList<>(3);

      Path mappingFile = mappingFiles.mappingFile;
      ProguardMap proguardMap = new ProguardMap();
      if (mappingFile != null) {
        try {
          proguardMap.readFromReader(new InputStreamReader(Files.newInputStream(mappingFile), Charsets.UTF_8));
          loaded.add(mappingFile.getFileName().toString());
        }
        catch (IOException | ParseException e) {
          errors.add(mappingFile.getFileName().toString());
          proguardMap = null;
        }
      }

      Path seedsFile = mappingFiles.seedsFile;
      ProguardSeedsMap seeds = null;
      if (seedsFile != null) {
        try {
          seeds = ProguardSeedsMap.parse(new InputStreamReader(Files.newInputStream(seedsFile), Charsets.UTF_8));
          loaded.add(seedsFile.getFileName().toString());
        }
        catch (IOException e) {
          errors.add(seedsFile.getFileName().toString());
        }
      }

      //automatically enable deobfuscation if loading mapping file for the first time
      if ((myProguardMappings == null || myProguardMappings.map == null) && proguardMap != null) {
        myDeobfuscateNames = true;
      }

      Path usageFile = mappingFiles.usageFile;
      ProguardUsagesMap usage = null;
      if (usageFile != null) {
        try {
          usage = ProguardUsagesMap.parse(new InputStreamReader(Files.newInputStream(usageFile), Charsets.UTF_8));
          loaded.add(usageFile.getFileName().toString());
        }
        catch (IOException e) {
          errors.add(usageFile.getFileName().toString());
        }
      }

      myProguardMappings = loaded.isEmpty() ? null : new ProguardMappings(proguardMap, seeds, usage);
      if (errors.isEmpty() && loaded.isEmpty()) {
        BALLOON_NOTIFICATION.createNotification("APK Analyzer couldn't find any ProGuard mapping files. " +
                                                "The filenames must match one of: mapping.txt, seeds.txt, usage.txt",
                                                MessageType.ERROR).notify(myProject);
      }
      else if (errors.isEmpty()) {
        LOGGING_NOTIFICATION.createNotification("APK Analyzer successfully loaded maps from: " + StringUtil.join(loaded, ", "),
                                                MessageType.INFO).notify(myProject);
      }
      else {
        BALLOON_NOTIFICATION.createNotification("APK Analyzer successfully loaded maps from: " + StringUtil.join(loaded, ",") + "\n"
                                                + "There were problems loading: " + StringUtil.join(errors, ", "),
                                                MessageType.WARNING).notify(myProject);
      }

      myDexTreeRenderer.setMappings(myProguardMappings);
      initDex();
    }
    catch (IOException e) {
      Messages.showErrorDialog(e.getMessage(), "Error Loading Mappings...");
    }
  }

  public void initDex() {
    ListeningExecutorService pooledThreadExecutor = MoreExecutors.listeningDecorator(PooledThreadExecutor.INSTANCE);
    ListenableFuture<Map<Path, DexBackedDexFile>> dexFileFuture = pooledThreadExecutor.submit(() -> {
      Map<Path, DexBackedDexFile> dexFiles = Maps.newHashMapWithExpectedSize(myDexFiles.length);
      for (int i = 0; i < myDexFiles.length; i++) {
        dexFiles.put(myDexFiles[i], DexFiles.getDexFile(myDexFiles[i]));
      }
      return dexFiles;
    });

    ListenableFuture<DexPackageNode> treeNodeFuture =
      Futures.transform(dexFileFuture, new Function<Map<Path, DexBackedDexFile>, DexPackageNode>() {
        @NotNull
        @Override
        public DexPackageNode apply(@Nullable Map<Path, DexBackedDexFile> input) {
          assert input != null;
          PackageTreeCreator treeCreator = new PackageTreeCreator(myProguardMappings, myDeobfuscateNames);
          return treeCreator.constructPackageTree(input);
        }
      }, pooledThreadExecutor);

    Futures.addCallback(treeNodeFuture, new FutureCallback<DexPackageNode>() {
      @Override
      public void onSuccess(DexPackageNode result) {
        myLoadingPanel.stopLoading();
        myTree.setRootVisible(false);
        TreeModel treeModel = new FilteredTreeModel<>(result, myDexFilters);
        myTree.setModel(treeModel);

        //this has to be added AFTER the Model is added to the Tree because change events are sent to listeners in order from last to first
        //otherwise, any root change event would wipe out the expandedDescendants list before we have a chance to read it
        treeModel.addTreeModelListener(new TreeModelAdapter() {
          @Override
          protected void process(TreeModelEvent event, EventType type) {
            Enumeration<TreePath> expanded = myTree.getExpandedDescendants(new TreePath(myTree.getModel().getRoot()));
            if (expanded == null) {
              return;
            }
            // Schedule a runnable to expand the gathered paths later,
            // so that all the other listeners get a chance to process the tree changes first.
            ApplicationManager.getApplication().invokeLater(() -> {
              for (TreePath path : Collections.list(expanded)) {
                myTree.expandPath(path);
              }
            });
          }
        });
      }

      @Override
      public void onFailure(@NotNull Throwable t) {
        myLoadingPanel.stopLoading();
      }
    }, EdtExecutor.INSTANCE);

    ListenableFuture<DexFileStats> dexStatsFuture =
      Futures.transform(dexFileFuture, new Function<Map<Path, DexBackedDexFile>, DexFileStats>() {
        @NotNull
        @Override
        public DexFileStats apply(@Nullable Map<Path, DexBackedDexFile> input) {
          assert input != null;
          return DexFileStats.create(input.values());
        }
      }, pooledThreadExecutor);

    //this will never change for a given dex file, regardless of proguard mappings
    //so it doesn't make sense to recompute every time
    if (((BorderLayout)myTopPanel.getLayout()).getLayoutComponent(BorderLayout.EAST) == null) {
      SimpleColoredComponent titleComponent = new SimpleColoredComponent();
      titleComponent.setIcon(AllIcons.Actions.Refresh);
      titleComponent.append("Loading dex stats");
      myTopPanel.add(titleComponent, BorderLayout.EAST);

      Futures.addCallback(dexStatsFuture, new FutureCallback<DexFileStats>() {
        @Override
        public void onSuccess(DexFileStats result) {
          titleComponent.clear();
          titleComponent.setIcon(AllIcons.General.Information);
          titleComponent.append(myDexFiles.length == 1 ? "This dex file defines " : "These dex files define ");
          titleComponent.append(Integer.toString(result.classCount), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
          titleComponent.append(" classes with ");
          titleComponent.append(Integer.toString(result.definedMethodCount), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
          titleComponent.append(" methods, and references ");
          titleComponent.append(Integer.toString(result.referencedMethodCount), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
          titleComponent.append(" methods.");
        }

        @Override
        public void onFailure(@NotNull Throwable t) {
          titleComponent.clear();
          titleComponent.setIcon(AllIcons.General.Error);
          titleComponent.append("Error parsing dex file: " + t.getMessage());
        }
      }, EdtExecutor.INSTANCE);
    }
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myLoadingPanel;
  }

  @Override
  public void dispose() {
    Disposer.dispose(myDisposable);
  }

  @Nullable
  public ProguardMappings getProguardMappings() {
    return myProguardMappings;
  }

  public boolean isDeobfuscateNames() {
    return myDeobfuscateNames;
  }

  @Nullable
  ListenableFuture<DexReferences> getDexReferences() {
    if (myDexReferences == null) {
      ListeningExecutorService pooledThreadExecutor = MoreExecutors.listeningDecorator(PooledThreadExecutor.INSTANCE);
      ListenableFuture<DexBackedDexFile[]> dexFileFuture = pooledThreadExecutor.submit(() -> {
        DexBackedDexFile[] files = new DexBackedDexFile[myDexFiles.length];
        for (int i = 0; i < files.length; i++) {
          files[i] = DexFiles.getDexFile(myDexFiles[i]);
        }
        return files;
      });
      myDexReferences = Futures.transform(dexFileFuture, new Function<DexBackedDexFile[], DexReferences>() {
        @Override
        public DexReferences apply(@Nullable DexBackedDexFile[] inputs) {
          assert inputs != null;
          return new DexReferences(inputs);
        }
      }, pooledThreadExecutor);
    }

    return myDexReferences;
  }

  private static class DexTreeNodeRenderer extends ColoredTreeCellRenderer {

    @Nullable private ProguardMappings myMappings;

    public void setMappings(@Nullable ProguardMappings mappings) {
      myMappings = mappings;
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
      if (!(value instanceof DexElementNode)) {
        return;
      }

      DexElementNode node = (DexElementNode)value;

      if (myMappings != null && node.isSeed(myMappings.seeds, myMappings.map, true)) {
        append(node.getName(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, null));
      }
      else if (node.isRemoved()) {
        append(node.getName(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_STRIKEOUT | SimpleTextAttributes.STYLE_ITALIC, null));
      }
      else if (!node.isDefined()) {
        append(node.getName(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, null));
      }
      else {
        append(node.getName());
      }

      setIcon(DexNodeIcons.forNode(node));
    }
  }

  private static class MethodCountRenderer extends ColoredTreeCellRenderer {
    private final boolean myShowDefinedCount;

    public MethodCountRenderer(boolean showDefinedCount) {
      myShowDefinedCount = showDefinedCount;
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
      if (value instanceof DexElementNode) {
        DexElementNode node = (DexElementNode)value;
        int count = myShowDefinedCount ? node.getMethodDefinitionsCount() : node.getMethodReferencesCount();
        if (count != 0) {
          append(Integer.toString(count));
        }
      }
    }
  }

  private static class SizeRenderer extends ColoredTreeCellRenderer {
    @Override
    public void customizeCellRenderer(@NotNull JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
      if (value instanceof DexElementNode) {
        DexElementNode node = (DexElementNode)value;
        append(ApkViewPanel.getHumanizedSize(node.getSize()));
      }
    }
  }

  private static class ShowFieldsAction extends ToggleAction {
    private final Tree myTree;
    private final DexViewFilters myDexViewFilters;

    public ShowFieldsAction(@NotNull Tree tree, @NotNull DexViewFilters options) {
      super("Show fields", "Toggle between show/hide fields", PlatformIcons.FIELD_ICON);
      myTree = tree;
      myDexViewFilters = options;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return myDexViewFilters.isShowFields();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      myDexViewFilters.setShowFields(state);
      ((DefaultTreeModel)myTree.getModel()).reload();
    }
  }

  private static class ShowMethodsAction extends ToggleAction {
    private final Tree myTree;
    private final DexViewFilters myDexViewFilters;

    public ShowMethodsAction(@NotNull Tree tree, @NotNull DexViewFilters options) {
      super("Show methods", "Toggle between show/hide methods", PlatformIcons.METHOD_ICON);
      myTree = tree;
      myDexViewFilters = options;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return myDexViewFilters.isShowMethods();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      myDexViewFilters.setShowMethods(state);
      ((DefaultTreeModel)myTree.getModel()).reload();
    }
  }

  private static class ShowReferencedAction extends ToggleAction {
    private final Tree myTree;
    private final DexViewFilters myDexViewFilters;

    public ShowReferencedAction(@NotNull Tree tree, @NotNull DexViewFilters options) {
      super("Show referenced-only nodes", "Toggle between show/hide referenced-only nodes", AllIcons.ObjectBrowser.ShowMembers);
      myTree = tree;
      myDexViewFilters = options;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      super.update(e);
      String text =
        myDexViewFilters.isShowReferencedNodes() ? "Show all referenced methods and fields" : "Show defined methods and fields";
      e.getPresentation().setText(text);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return myDexViewFilters.isShowReferencedNodes();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      myDexViewFilters.setShowReferencedNodes(state);
      ((DefaultTreeModel)myTree.getModel()).reload();
    }
  }

  private class ShowRemovedNodesAction extends ToggleAction {
    private final Tree myTree;
    private final DexViewFilters myDexViewFilters;

    public ShowRemovedNodesAction(@NotNull Tree tree, @NotNull DexViewFilters options) {
      super("Show removed nodes", "Toggle between show/hide nodes removed by Proguard", AllIcons.ObjectBrowser.CompactEmptyPackages);
      myTree = tree;
      myDexViewFilters = options;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return myDexViewFilters.isShowRemovedNodes();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      myDexViewFilters.setShowRemovedNodes(state);
      ((DefaultTreeModel)myTree.getModel()).reload();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      super.update(e);
      e.getPresentation().setEnabled(myProguardMappings != null && myProguardMappings.usage != null);
    }
  }

  private class DeobfuscateNodesAction extends ToggleAction {
    public DeobfuscateNodesAction() {
      super("Deobfuscate names", "Deobfuscate names using Proguard mapping", AllIcons.ObjectBrowser.AbbreviatePackageNames);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return myDeobfuscateNames;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      myDeobfuscateNames = state;
      initDex();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      super.update(e);
      e.getPresentation().setEnabled(myProguardMappings != null && myProguardMappings.map != null);
    }
  }

  private class LoadProguardAction extends AnAction {
    public LoadProguardAction() {
      super("Load Proguard mappings...", null, EmptyIcon.ICON_0);
      getTemplatePresentation().setDisabledIcon(EmptyIcon.ICON_0);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      selectProguardMapping();
    }

    @Override
    public boolean displayTextInToolbar() {
      return true;
    }

    @Override
    public void update(AnActionEvent e) {
      super.update(e);
      if (myProguardMappings != null) {
        e.getPresentation().setText("Change Proguard mappings...");
      }
      else {
        e.getPresentation().setText("Load Proguard mappings...");
      }
    }
  }
}

