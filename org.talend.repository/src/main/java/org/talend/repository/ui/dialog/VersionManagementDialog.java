// ============================================================================
//
// Copyright (C) 2006-2010 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.ui.dialog;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.progress.ProgressMonitorJobsDialog;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.image.EImage;
import org.talend.commons.ui.image.IImage;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.commons.ui.swt.advanced.composite.ThreeCompositesSashForm;
import org.talend.commons.utils.VersionUtils;
import org.talend.core.CorePlugin;
import org.talend.core.model.general.Project;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.RepositoryManager;
import org.talend.core.ui.images.CoreImageProvider;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.ERepositoryStatus;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.IRepositoryNode;
import org.talend.repository.model.ItemVersionObject;
import org.talend.repository.model.RepositoryConstants;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.IRepositoryNode.ENodeType;
import org.talend.repository.model.nodes.IProjectRepositoryNode;
import org.talend.repository.ui.views.CheckboxRepositoryTreeViewer;
import org.talend.repository.ui.views.RepositoryCheckBoxView;
import org.talend.repository.ui.views.RepositoryView;

/**
 * ggu class global comment. Detailled comment
 * 
 * @deprecated see ProjectSettingDialog
 */
public class VersionManagementDialog extends Dialog {

    private static final String TITLE = Messages.getString("VersionManagementDialog.Title"); //$NON-NLS-1$

    private static final IProxyRepositoryFactory FACTORY = CorePlugin.getDefault().getProxyRepositoryFactory();

    private static final String ITEM_EDITOR_KEY = "ITEM_EDITOR_KEY"; //$NON-NLS-1$

    private Map<IImage, Image> cacheItemImages = new HashMap<IImage, Image>();

    private Project project;

    private CheckboxRepositoryTreeViewer treeViewer;

    private Button removeBtn;

    private Table itemTable;

    private Button fixedVersionBtn;

    private Text maxVersionText;

    private Button majorBtn;

    private Button minorBtn;

    private Button revertBtn;

    private Button eachVersionBtn;

    private List<ItemVersionObject> checkedObjects = new ArrayList<ItemVersionObject>();

    public VersionManagementDialog(Shell parentShell, Project project) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
        this.project = project;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(TITLE);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        ThreeCompositesSashForm compositesSachForm = new ThreeCompositesSashForm(composite, SWT.NONE);
        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.horizontalSpacing = 0;
        composite.setLayout(gridLayout);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 400;
        gridData.widthHint = 570;
        composite.setLayoutData(gridData);

        //
        addRepositoryTreeViewer(compositesSachForm.getLeftComposite());

        addButtons(compositesSachForm.getMidComposite());

        addItemTableViewer(compositesSachForm.getRightComposite());

        return composite;
    }

    /**
     * repository tree viewer.
     */
    private void addRepositoryTreeViewer(Composite leftComposite) {
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.widthHint = 210;
        leftComposite.setLayoutData(gridData);
        RepositoryCheckBoxView view = new RepositoryCheckBoxView();
        try {
            view.init(RepositoryView.show().getViewSite());
        } catch (PartInitException e) {
            ExceptionHandler.process(e);
        }
        view.createPartControl(leftComposite);
        treeViewer = (CheckboxRepositoryTreeViewer) view.getViewer();
        // filter
        treeViewer.addFilter(new ViewerFilter() {

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                RepositoryNode node = (RepositoryNode) element;
                return filterRepositoryNode(node);
            }
        });
        // event
        treeViewer.addCheckStateListener(new ICheckStateListener() {

            public void checkStateChanged(CheckStateChangedEvent event) {
                RepositoryNode node = (RepositoryNode) event.getElement();
                List<ItemVersionObject> objects = new ArrayList<ItemVersionObject>();
                processItems(objects, node);
                if (!objects.isEmpty()) {
                    if (event.getChecked()) {
                        checkedObjects.addAll(objects);
                    } else {
                        checkedObjects.removeAll(objects);
                        removeItemElements(objects);
                    }
                    researchMaxVersion();
                    refreshTableItems();
                }
            }
        });
        treeViewer.addTreeListener(new ITreeViewerListener() {

            public void treeCollapsed(TreeExpansionEvent event) {
                //
            }

            public void treeExpanded(TreeExpansionEvent event) {
                refreshCheckedTreeView();
            }
        });

        expandSomeNodes(view);
    }

    private void expandSomeNodes(RepositoryCheckBoxView view) {
        if (view == null) {
            return;
        }
        final RepositoryNode root = view.getRoot();
        if (root instanceof IProjectRepositoryNode) {
            final IProjectRepositoryNode rootNode = (IProjectRepositoryNode) root;
            final TreeViewer viewer = view.getViewer();
            // metadata
            IRepositoryNode metadataConNode = rootNode.getMetadataNode();
            if (metadataConNode != null) {
                viewer.expandToLevel(metadataConNode, 1);
            }
            // code
            IRepositoryNode codeNode = rootNode.getCodeNode();
            if (codeNode != null) {
                viewer.expandToLevel(codeNode, 1);
            }
        }
    }

    private boolean filterRepositoryNode(RepositoryNode node) {
        if (node == null) {
            return false;
        }
        if (node.isBin()) {
            return false;
        }
        if (node.getObject() != null) {
            ERepositoryStatus status = FACTORY.getStatus(node.getObject());
            if (status == ERepositoryStatus.LOCK_BY_OTHER
                    || (status == ERepositoryStatus.LOCK_BY_USER && RepositoryManager.isOpenedItemInEditor(node.getObject()))) {
                return false;
            }
            // table
            if (node.getObject() instanceof org.talend.core.model.metadata.MetadataTable) {
                return false;
            }
            // opened items
            // TODO

            ERepositoryObjectType objectType = node.getObjectType();
            switch (objectType) {
            case SQLPATTERNS:
            case ROUTINES:
                RepositoryNode systemNode = node.getParent();
                if (systemNode != null) {
                    // for system folder
                    if (systemNode.getType() == ENodeType.STABLE_SYSTEM_FOLDER
                            && systemNode.getLabel().equalsIgnoreCase(RepositoryConstants.SYSTEM_DIRECTORY)) {
                        return false;
                    }
                }
            default:
            }
        }
        ERepositoryObjectType contentType = node.getContentType();
        if (contentType != null) {
            switch (contentType) {
            case REFERENCED_PROJECTS: // referenced project.
            case GENERATED: // generated documentation
                return false;
            case SQLPATTERNS:
            case ROUTINES:

                // for system folder
                if (node.getType() == ENodeType.STABLE_SYSTEM_FOLDER
                        && node.getLabel().equalsIgnoreCase(RepositoryConstants.SYSTEM_DIRECTORY)) {
                    return false;
                }
            default:
            }
        }

        // for sub folder in db connection
        if (node.getType() == ENodeType.STABLE_SYSTEM_FOLDER) {
            RepositoryNode parentNode = node.getParent();
            if (parentNode != null) {
                if (parentNode.getObjectType() == ERepositoryObjectType.METADATA_CONNECTIONS) {
                    return false;
                }
            }
        }
        return true;
    }

    private void processItems(List<ItemVersionObject> objects, RepositoryNode node) {
        if (node == null) {
            return;
        }
        if (node.getType() == ENodeType.REPOSITORY_ELEMENT) {
            if (node.getObject() != null) {
                Property property = node.getObject().getProperty();
                Item item = property.getItem();
                if (item != null && filterRepositoryNode(node)) { // must be item
                    ItemVersionObject object = new ItemVersionObject(property, node, property.getVersion());
                    objects.add(object);
                }
            }
        } else {
            for (IRepositoryNode child : node.getChildren()) {
                processItems(objects, (RepositoryNode) child);
            }
        }
    }

    private void addButtons(Composite middleComposite) {
        Composite btnComposite = new Composite(middleComposite, SWT.NONE);
        btnComposite.setLayout(new GridLayout(1, true));
        GridData data = new GridData(GridData.FILL_BOTH);
        data.verticalAlignment = GridData.CENTER;
        btnComposite.setLayoutData(data);

        removeBtn = new Button(btnComposite, SWT.NONE);
        removeBtn.setImage(ImageProvider.getImage(EImage.LEFT_ICON));
        removeBtn.setToolTipText(Messages.getString("VersionManagementDialog.RemoveTip")); //$NON-NLS-1$
        removeBtn.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                TableItem[] selections = itemTable.getSelection();
                itemTable.setRedraw(false);
                for (TableItem item : selections) {
                    Object data = item.getData();
                    checkedObjects.remove(data);
                    removeTableItem(item);
                }
                itemTable.setRedraw(true);
                refreshCheckedTreeView();
                refreshTableItems();
            }
        });

        removeBtn.setEnabled(false);
    }

    private void addItemTableViewer(Composite rightComposite) {
        Composite composite = new Composite(rightComposite, SWT.NONE);
        composite.setLayout(new GridLayout());
        GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);

        addItemsOption(composite);

        itemTable = new Table(composite, SWT.MULTI | SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(itemTable);
        itemTable.setHeaderVisible(true);
        itemTable.setLinesVisible(true);

        //
        TableColumn itemColumn = new TableColumn(itemTable, SWT.CENTER);
        itemColumn.setText(Messages.getString("VersionManagementDialog.Items")); //$NON-NLS-1$
        itemColumn.setWidth(110);

        TableColumn oldVersionColumn = new TableColumn(itemTable, SWT.CENTER);
        oldVersionColumn.setText(Messages.getString("VersionManagementDialog.Version")); //$NON-NLS-1$
        oldVersionColumn.setWidth(60);

        TableColumn versionColumn = new TableColumn(itemTable, SWT.CENTER);
        versionColumn.setText(Messages.getString("VersionManagementDialog.NewVersion")); //$NON-NLS-1$
        versionColumn.setWidth(82);

        final TableColumn delColumn = new TableColumn(itemTable, SWT.CENTER);
        delColumn.setText(""); //$NON-NLS-1$
        delColumn.setWidth(26);
        delColumn.setResizable(false);

        versionColumn.addControlListener(new ControlListener() {

            public void controlMoved(ControlEvent e) {
                //
            }

            public void controlResized(ControlEvent e) {
                if (!isFixedVersion()) {
                    refreshTableItems();
                }
            }
        });
        itemTable.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                checkButtonsState();
            }
        });
    }

    private void addItemsOption(Composite parent) {
        Group option = new Group(parent, SWT.NONE);
        option.setLayout(new GridLayout());
        option.setText(Messages.getString("VersionManagementDialog.Options")); //$NON-NLS-1$
        GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).applyTo(option);
        fixedVersionBtn = new Button(option, SWT.RADIO);
        fixedVersionBtn.setText(Messages.getString("VersionManagementDialog.FixedVersion")); //$NON-NLS-1$
        fixedVersionBtn.setSelection(true); // default

        Composite versionComposit = new Composite(option, SWT.NONE);
        GridLayout layout = new GridLayout(5, false);
        layout.horizontalSpacing = 1;
        layout.verticalSpacing = 0;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        versionComposit.setLayout(layout);
        GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(versionComposit);

        maxVersionText = new Text(versionComposit, SWT.BORDER | SWT.READ_ONLY);
        GridData data = new GridData();
        data.widthHint = 50;
        data.minimumWidth = 50;
        maxVersionText.setLayoutData(data);
        maxVersionText.setEnabled(false);
        maxVersionText.setText(VersionUtils.DEFAULT_VERSION);

        majorBtn = new Button(versionComposit, SWT.NONE);
        majorBtn.setText("M"); //$NON-NLS-1$
        majorBtn.setToolTipText(Messages.getString("VersionManagementDialog.MajorVersionTip")); //$NON-NLS-1$
        minorBtn = new Button(versionComposit, SWT.NONE);
        minorBtn.setText("m"); //$NON-NLS-1$
        minorBtn.setToolTipText(Messages.getString("VersionManagementDialog.MinorVersionTip")); //$NON-NLS-1$

        Label label = new Label(versionComposit, SWT.NONE);
        label.setText(""); //$NON-NLS-1$
        data = new GridData();
        data.minimumWidth = 20;
        data.widthHint = 20;
        label.setLayoutData(data);
        label.setVisible(false);

        revertBtn = new Button(versionComposit, SWT.NONE);
        revertBtn.setText(Messages.getString("VersionManagementDialog.Revert")); //$NON-NLS-1$
        revertBtn.setToolTipText(Messages.getString("VersionManagementDialog.RevertTip")); //$NON-NLS-1$

        eachVersionBtn = new Button(option, SWT.RADIO);
        eachVersionBtn.setText(Messages.getString("VersionManagementDialog.EachVersion")); //$NON-NLS-1$

        // event
        fixedVersionBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                checkFixedButtons();
                researchMaxVersion();
                refreshTableItems();
            }
        });
        majorBtn.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                String version = maxVersionText.getText();
                version = VersionUtils.upMajor(version);
                maxVersionText.setText(version);
                refreshTableItems();
            }
        });
        minorBtn.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                String version = maxVersionText.getText();
                version = VersionUtils.upMinor(version);
                maxVersionText.setText(version);
                refreshTableItems();
            }
        });
        revertBtn.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                maxVersionText.setText(VersionUtils.DEFAULT_VERSION); // set min version
                researchMaxVersion();
                refreshTableItems();
            }
        });
        eachVersionBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                checkFixedButtons();
                refreshTableItems();
            }
        });
        checkFixedButtons();
    }

    private void refreshTableItems() {
        removeItemElements(checkedObjects);
        addItemElements(checkedObjects);
        // if (checkedObjects.isEmpty()) {
        // maxVersionText.setText(VersionUtils.DEFAULT_VERSION);
        // }
        checkButtonsState();
    }

    private void researchMaxVersion() {
        String version = maxVersionText.getText();
        if ("".equals(version.trim())) { //$NON-NLS-1$
            version = VersionUtils.DEFAULT_VERSION;
        }
        for (ItemVersionObject object : checkedObjects) {
            if (VersionUtils.compareTo(version, object.getOldVersion()) < 0) {
                version = object.getOldVersion();
            }
        }
        maxVersionText.setText(version);
    }

    private void checkButtonsState() {
        TableItem[] selections = itemTable.getSelection();
        if (selections != null && selections.length > 0) {
            removeBtn.setEnabled(true);
        } else {
            removeBtn.setEnabled(false);
        }
    }

    private boolean isFixedVersion() {
        return fixedVersionBtn.getSelection();
    }

    private void checkFixedButtons() {
        majorBtn.setEnabled(isFixedVersion());
        minorBtn.setEnabled(isFixedVersion());
        revertBtn.setEnabled(isFixedVersion());
    }

    private void removeTableItem(TableItem item) {
        if (item == null) {
            return;
        }
        TableEditor[] editors = (TableEditor[]) item.getData(ITEM_EDITOR_KEY);
        if (editors != null) {
            for (int j = 0; j < editors.length; j++) {
                editors[j].getEditor().dispose();
                editors[j].dispose();
            }
        }
        item.dispose();
    }

    private void removeItemElements(List<ItemVersionObject> objects) {
        itemTable.setRedraw(false);
        TableItem[] items = itemTable.getItems();
        for (TableItem item : items) {
            if (objects.contains(item.getData())) {
                removeTableItem(item);
            }
        }
        itemTable.setRedraw(true);
    }

    private void refreshCheckedTreeView() {
        List<RepositoryNode> nodes = new ArrayList<RepositoryNode>();

        for (TableItem item : itemTable.getItems()) {
            Object data = item.getData();
            if (data instanceof ItemVersionObject) {
                nodes.add(((ItemVersionObject) data).getRepositoryNode());
            }
        }
        treeViewer.setCheckedElements(nodes.toArray());
        // treeViewer.refresh();
    }

    private Image getItemsImage(IImage iImage) {
        if (iImage == null) {
            iImage = EImage.DEFAULT_IMAGE;
        }
        Image image = cacheItemImages.get(iImage);
        if (image == null) {
            Image oImage = ImageProvider.getImage(iImage);
            ImageData imageData = oImage.getImageData();
            // enlarge image
            final int larger = 4;
            ImageData newData = imageData.scaledTo(imageData.width + larger, imageData.height + larger);
            image = new Image(oImage.getDevice(), newData);
            cacheItemImages.put(iImage, image);
        }
        return image;
    }

    private void addItemElements(List<ItemVersionObject> elements) {
        if (elements == null || elements.isEmpty()) {
            return;
        }
        itemTable.setRedraw(false);
        final Color redColor = Display.getDefault().getSystemColor(SWT.COLOR_RED);

        for (final ItemVersionObject object : elements) {
            final TableItem tableItem = new TableItem(itemTable, SWT.NONE);
            tableItem.setData(object);
            Item item = object.getItem();

            ERepositoryObjectType itemType = ERepositoryObjectType.getItemType(item);

            tableItem.setImage(getItemsImage(CoreImageProvider.getIcon(itemType)));
            tableItem.setText(item.getProperty().getLabel());
            // old version
            tableItem.setText(1, object.getOldVersion());

            TableEditor versionEditor = null;

            if (isFixedVersion()) {
                String version = maxVersionText.getText();
                tableItem.setText(2, version);
                if (VersionUtils.compareTo(version, object.getOldVersion()) > 0) {
                    tableItem.setForeground(2, redColor);
                } else {
                    tableItem.setForeground(2, Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
                }
            } else {
                // new version
                versionEditor = new TableEditor(itemTable);
                Composite versionComposit = new Composite(itemTable, SWT.NONE);
                GridLayout layout = new GridLayout(3, false);
                layout.horizontalSpacing = 1;
                layout.verticalSpacing = 0;
                layout.marginHeight = 0;
                layout.marginWidth = 0;
                versionComposit.setLayout(layout);

                final Text text = new Text(versionComposit, SWT.BORDER | SWT.READ_ONLY);
                GridData data = new GridData(GridData.FILL_HORIZONTAL);
                text.setLayoutData(data);
                text.setEditable(false);
                text.setText(object.getNewVersion());
                if (VersionUtils.compareTo(object.getNewVersion(), object.getOldVersion()) > 0) {
                    text.setForeground(redColor);
                } else {
                    text.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
                }

                Button tableMajorBtn = new Button(versionComposit, SWT.NONE);
                tableMajorBtn.setText("M"); //$NON-NLS-1$
                tableMajorBtn.setToolTipText(Messages.getString("VersionManagementDialog.MajorVersionTip")); //$NON-NLS-1$
                Button tableMinorBtn = new Button(versionComposit, SWT.NONE);
                tableMinorBtn.setText("m"); //$NON-NLS-1$
                tableMinorBtn.setToolTipText(Messages.getString("VersionManagementDialog.MinorVersionTip")); //$NON-NLS-1$

                tableMajorBtn.addSelectionListener(new SelectionAdapter() {

                    public void widgetSelected(SelectionEvent e) {
                        String version = object.getNewVersion();
                        version = VersionUtils.upMajor(version);
                        text.setText(version);
                        text.setForeground(redColor);
                        object.setNewVersion(version);
                    }
                });
                tableMinorBtn.addSelectionListener(new SelectionAdapter() {

                    public void widgetSelected(SelectionEvent e) {
                        String version = object.getNewVersion();
                        version = VersionUtils.upMinor(version);
                        text.setText(version);
                        text.setForeground(redColor);
                        object.setNewVersion(version);
                    }
                });

                versionEditor.minimumWidth = itemTable.getColumn(2).getWidth();
                versionEditor.setEditor(versionComposit, tableItem, 2);
            }
            TableEditor delEditor = new TableEditor(itemTable);
            Label delLabel = new Label(itemTable, SWT.CENTER);
            delLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            delLabel.setImage(ImageProvider.getImage(EImage.DELETE_ICON));
            delLabel.setToolTipText(Messages.getString("VersionManagementDialog.DeletedTip")); //$NON-NLS-1$
            delLabel.pack();
            delLabel.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseDown(MouseEvent e) {
                    checkedObjects.remove(object);
                    removeTableItem(tableItem);
                    refreshCheckedTreeView();
                    checkButtonsState();
                }

            });

            delEditor.minimumWidth = 25;
            delEditor.horizontalAlignment = SWT.CENTER;
            delEditor.setEditor(delLabel, tableItem, 3);
            if (isFixedVersion()) {
                tableItem.setData(ITEM_EDITOR_KEY, new TableEditor[] { delEditor });
            } else if (versionEditor != null) {
                tableItem.setData(ITEM_EDITOR_KEY, new TableEditor[] { versionEditor, delEditor });
            }
            itemTable.setRedraw(true);
        }
    }

    public List<ItemVersionObject> getModifiedVersionItems() {
        return this.checkedObjects;
    }

    @Override
    protected void okPressed() {
        boolean modified = false;
        String newVersion = maxVersionText.getText();
        for (ItemVersionObject object : getModifiedVersionItems()) {
            if (!isFixedVersion()) {
                newVersion = object.getNewVersion();
            }
            if (!object.getOldVersion().equals(newVersion)) {
                modified = true;
                break;
            }
        }
        if (modified) {
            boolean confirm = false;
            if (isFixedVersion()) {
                confirm = MessageDialog.openConfirm(getShell(), Messages.getString("VersionManagementDialog.ConfirmTitle"), //$NON-NLS-1$
                        Messages.getString("VersionManagementDialog.ConfirmMessage", newVersion)); //$NON-NLS-1$
                if (confirm) {
                    // set all items for new version
                    for (ItemVersionObject object : getModifiedVersionItems()) {
                        object.setNewVersion(newVersion);
                    }
                }
            } else {
                ItemsVersionConfirmDialog chanedDialog = new ItemsVersionConfirmDialog(getShell(), getModifiedVersionItems());
                confirm = (chanedDialog.open() == Window.OK);
            }

            if (confirm) {
                updateItemsVersion();
                super.okPressed();
            }
        } else {
            if (!getModifiedVersionItems().isEmpty()) {
                MessageDialog.openWarning(getShell(), Messages.getString("VersionManagementDialog.WarningTitle"), //$NON-NLS-1$
                        Messages.getString("VersionManagementDialog.WarningMessages")); //$NON-NLS-1$
            }
            super.okPressed();
        }

    }

    @SuppressWarnings("restriction")
    private void updateItemsVersion() {
        IRunnableWithProgress runnable = new IRunnableWithProgress() {

            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                monitor.beginTask("", getModifiedVersionItems().size() * 100); //$NON-NLS-1$
                Set<ERepositoryObjectType> types = new HashSet<ERepositoryObjectType>();
                for (ItemVersionObject object : getModifiedVersionItems()) {
                    if (!object.getOldVersion().equals(object.getNewVersion())) {
                        final Item item = object.getItem();
                        item.getProperty().setVersion(object.getNewVersion());
                        types.add(object.getRepositoryNode().getObjectType());

                        try {
                            FACTORY.save(project, item);
                        } catch (PersistenceException e) {
                            ExceptionHandler.process(e);
                        }
                    }
                    monitor.worked(100);
                }
                RepositoryManager.refresh(types);
                monitor.done();
            }
        };

        final ProgressMonitorJobsDialog dialog = new ProgressMonitorJobsDialog(null);
        try {
            dialog.run(false, false, runnable);
        } catch (InvocationTargetException e) {
            ExceptionHandler.process(e);
        } catch (InterruptedException e) {
            ExceptionHandler.process(e);
        }
    }
}