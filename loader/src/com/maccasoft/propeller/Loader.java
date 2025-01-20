/*
 * Copyright (c) 2025 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 */

package com.maccasoft.propeller;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maccasoft.propeller.internal.ImageRegistry;
import com.maccasoft.propeller.internal.InternalErrorDialog;
import com.maccasoft.propeller.loader.Propeller1Loader;
import com.maccasoft.propeller.loader.Propeller2Loader;
import com.maccasoft.propeller.loader.PropellerLoader;
import com.maccasoft.propeller.loader.PropellerLoaderListener;
import com.maccasoft.propeller.port.ComPort;
import com.maccasoft.propeller.port.NetworkComPort;
import com.maccasoft.propeller.port.SerialComPort;

public class Loader {

    public static final String APP_TITLE = "Propeller Firmware Loader";
    public static final String APP_VERSION = "0.1.0";

    private static final int HORIZONTAL_DIALOG_UNIT_PER_CHAR = 4;
    private static final int VERTICAL_DIALOG_UNITS_PER_CHAR = 8;

    public static final String[] filterNames = new String[] {
        "All Firmware Files",
        "Binary Files",
        "Firmware Packs",
    };
    public static final String[] filterExtensions = new String[] {
        "*.binary;*.bin;*.json",
        "*.binary;*.bin",
        "*.json",
    };

    Display display;
    Shell shell;
    FontMetrics fontMetrics;

    Font boldFont;

    Composite selectionGroup;
    StackLayout selectionGroupStack;
    Composite firmwareListContainer;
    Composite firmwareInfoContainer;

    Text firmwareFile;
    Button browseFile;
    ComboViewer firmwareList;
    Label firmwareInfo;

    Button allAvailable;
    Button selectedOnly;
    CheckboxTableViewer devicesViewer;

    Button enableLocal;
    Button enableNetwork;
    Button discoverButton;

    Button updateButton;

    LoaderParameters parameters;
    boolean embeddedFirmware;

    public Loader(Shell shell) {
        this.shell = shell;
        this.display = shell.getDisplay();
        this.parameters = new LoaderParameters();

        GC gc = new GC(shell);
        try {
            fontMetrics = gc.getFontMetrics();
        } finally {
            gc.dispose();
        }

        FontData[] fontData = shell.getFont().getFontData();
        boldFont = new Font(display, fontData[0].getName(), fontData[0].getHeight(), SWT.BOLD);

        this.shell.addDisposeListener(new DisposeListener() {

            @Override
            public void widgetDisposed(DisposeEvent event) {
                boldFont.dispose();
            }

        });
    }

    public void createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = layout.marginHeight = 10;
        container.setLayout(layout);

        createFirmwareSelectionGroup(container);
        createDeviceSelectionGroup(container);
        createButtonsGroup(container);

        hookListeners();
    }

    void hookListeners() {

        parameters.addPropertyChangeListener((event) -> {

            switch (event.getPropertyName()) {
                case LoaderParameters.PROP_FILE:
                    firmwareFile.setText(getRelativeFilePath(parameters.getFile()));
                    break;

                case LoaderParameters.PROP_FIRMWARE_LIST:
                    if (!isEmbeddedFirmware()) {
                        if (parameters.getFirmwareList().size() != 0) {
                            selectionGroupStack.topControl = firmwareListContainer;
                        }
                        else {
                            selectionGroupStack.topControl = firmwareInfoContainer;
                        }
                    }
                    selectionGroup.layout();
                    firmwareList.refresh();
                    devicesViewer.refresh();
                    break;

                case LoaderParameters.PROP_FIRMWARE:
                    if (event.getNewValue() == null || parameters.getFirmwareList().contains(event.getNewValue())) {
                        if (event.getNewValue() != firmwareList.getStructuredSelection().getFirstElement()) {
                            firmwareList.setSelection(event.getNewValue() != null ? new StructuredSelection(event.getNewValue()) : StructuredSelection.EMPTY);
                        }
                    }
                    firmwareInfo.setText(event.getNewValue() != null ? ((Firmware) event.getNewValue()).getDescription() : "");
                    devicesViewer.refresh();
                    break;

                case LoaderParameters.PROP_UPDATE_ALL:
                    devicesViewer.getControl().setEnabled(!((Boolean) event.getNewValue()).booleanValue());
                    break;

                case LoaderParameters.PROP_DEVICES:
                    devicesViewer.refresh();
                    break;

                case LoaderParameters.PROP_ENABLE_LOCAL: {
                    boolean selection = ((Boolean) event.getNewValue()).booleanValue();
                    if (enableLocal.getSelection() != selection) {
                        enableLocal.setSelection(selection);
                    }
                    break;
                }

                case LoaderParameters.PROP_ENABLE_NETWORK:
                    boolean selection = ((Boolean) event.getNewValue()).booleanValue();
                    if (enableNetwork.getSelection() != selection) {
                        enableNetwork.setSelection(selection);
                    }
                    break;
            }

            updateButton.setEnabled(parameters.canDoUpdate());
        });
    }

    void createFirmwareSelectionGroup(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = layout.marginHeight = 0;
        container.setLayout(layout);
        GridData containerGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        container.setLayoutData(containerGridData);

        Label label = new Label(container, SWT.NONE);
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
        label.setText("Firmware");

        Composite group = new Composite(parent, SWT.NONE);
        layout = new GridLayout(2, false);
        layout.marginWidth = layout.marginHeight = 0;
        group.setLayout(layout);
        GridData groupLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
        group.setLayoutData(groupLayoutData);

        if (isEmbeddedFirmware()) {
            group.setVisible(false);
            groupLayoutData.exclude = true;
        }

        firmwareFile = new Text(group, SWT.BORDER | SWT.READ_ONLY);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        gridData.widthHint = convertWidthInCharsToPixels(50);
        firmwareFile.setLayoutData(gridData);
        firmwareFile.setBackground(display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        firmwareFile.setText(getRelativeFilePath(parameters.getFile()));

        browseFile = new Button(group, SWT.PUSH | SWT.FLAT);
        browseFile.setImage(ImageRegistry.getImageFromResources("folder-horizontal-open.png"));
        browseFile.setToolTipText("Browse files");
        browseFile.setLayoutData(new GridData(convertHorizontalDLUsToPixels(18), convertHorizontalDLUsToPixels(18)));
        browseFile.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                FileDialog dlg = new FileDialog(container.getShell(), SWT.OPEN);
                dlg.setText("Open Firmware File");
                dlg.setFilterNames(filterNames);
                dlg.setFilterExtensions(filterExtensions);
                dlg.setFilterIndex(0);

                String filterPath = null;
                if (parameters.getFile() != null) {
                    File file = parameters.getFile().getAbsoluteFile().getParentFile();
                    if (file != null) {
                        filterPath = file.getAbsolutePath();
                    }
                }
                if (filterPath == null) {
                    filterPath = System.getProperty("APP_DIR");
                }
                if (filterPath != null) {
                    dlg.setFilterPath(filterPath);
                }

                String fileName = dlg.open();
                if (fileName != null) {
                    handleFileSelection(new File(fileName));
                }
            }

        });

        selectionGroup = new Composite(parent, SWT.NONE);
        selectionGroupStack = new StackLayout();
        selectionGroupStack.marginWidth = selectionGroupStack.marginHeight = 0;
        selectionGroup.setLayout(selectionGroupStack);
        selectionGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        firmwareListContainer = createFirmwareListGroup(selectionGroup);
        firmwareInfoContainer = createFirmwareInfoGroup(selectionGroup);

        if (isEmbeddedFirmware()) {
            selectionGroupStack.topControl = firmwareListContainer;
        }
        else {
            if (parameters.getFirmwareList().size() != 0) {
                selectionGroupStack.topControl = firmwareListContainer;
            }
            else {
                selectionGroupStack.topControl = firmwareInfoContainer;
            }
        }
    }

    String getRelativeFilePath(File file) {
        String appDir = System.getenv("APP_DIR");
        if (appDir == null) {
            appDir = new File("").getAbsolutePath();
        }

        if (file != null) {
            String text = file.getAbsolutePath();
            if (appDir != null && text.startsWith(appDir)) {
                text = text.substring(appDir.length());
                if (text.startsWith("/") || text.startsWith("\\")) {
                    text = text.substring(1);
                }
            }
            return text;
        }

        return "";
    }

    void handleFileSelection(File file) {

        parameters.setFile(file);

        try {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".json")) {
                if (file.getName().toLowerCase().endsWith(".json")) {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                    FirmwarePack pack = mapper.readValue(file, FirmwarePack.class);
                    updateFrom(pack);
                }
            }
            else if (name.endsWith(".binary") || name.endsWith(".bin")) {
                Firmware firmware = Firmware.fromFile(file);
                updateFrom(firmware);
            }
        } catch (Exception e) {
            // Do nothing
        }

        selectionGroupStack.topControl = parameters.getFirmwareList().size() != 0 ? firmwareListContainer : firmwareInfoContainer;
        selectionGroup.layout();
    }

    public void updateFrom(FirmwarePack pack) {
        parameters.updateFrom(pack);
    }

    public void updateFrom(File file, FirmwarePack pack) {
        parameters.setFile(file);
        parameters.updateFrom(pack);
    }

    public void updateFrom(Firmware firmware) {
        parameters.updateFrom(firmware);
    }

    public void updateFrom(File file, Firmware firmware) {
        parameters.setFile(file);
        parameters.updateFrom(firmware);
    }

    Composite createFirmwareListGroup(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = layout.marginHeight = 0;
        container.setLayout(layout);
        GridData containerGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        container.setLayoutData(containerGridData);

        firmwareList = new ComboViewer(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        firmwareList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        firmwareList.setContentProvider(new ArrayContentProvider());
        firmwareList.setLabelProvider(new LabelProvider() {

            @Override
            public String getText(Object element) {
                return ((Firmware) element).getDescription();
            }

        });
        firmwareList.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                Firmware firmware = (Firmware) event.getStructuredSelection().getFirstElement();
                if (parameters.getFirmwareList().contains(firmware)) {
                    parameters.setFirmware(firmware);
                }
            }

        });

        firmwareList.setInput(parameters.getFirmwareList());
        if (parameters.getFirmware() != null) {
            firmwareList.setSelection(new StructuredSelection(parameters.getFirmware()));
        }

        return container;
    }

    Composite createFirmwareInfoGroup(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = layout.marginHeight = 0;
        container.setLayout(layout);
        GridData containerGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        container.setLayoutData(containerGridData);

        firmwareInfo = new Label(container, SWT.NONE);
        firmwareInfo.setFont(boldFont);
        firmwareInfo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        firmwareInfo.setAlignment(SWT.CENTER);

        if (parameters.getFirmware() != null) {
            firmwareInfo.setText(parameters.getFirmware().getDescription());
        }

        return container;
    }

    void createDeviceSelectionGroup(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = layout.marginHeight = 0;
        container.setLayout(layout);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Composite group = new Composite(container, SWT.NONE);
        layout = new GridLayout(3, false);
        layout.marginWidth = layout.marginHeight = 0;
        group.setLayout(layout);
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Label label = new Label(group, SWT.NONE);
        label.setText("Target Devices");
        allAvailable = new Button(group, SWT.RADIO);
        allAvailable.setText("All available");
        allAvailable.setSelection(parameters.isUpdateAll());
        allAvailable.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                parameters.setUpdateAll(((Button) event.widget).getSelection());
            }

        });

        selectedOnly = new Button(group, SWT.RADIO);
        selectedOnly.setText("Selected only");
        selectedOnly.setSelection(!parameters.isUpdateAll());

        devicesViewer = CheckboxTableViewer.newCheckList(container, SWT.V_SCROLL | SWT.BORDER);
        devicesViewer.setContentProvider(new ArrayContentProvider());
        devicesViewer.setLabelProvider(new StyledCellLabelProvider() {

            @Override
            public void update(ViewerCell cell) {
                StringBuilder sb = new StringBuilder();
                List<StyleRange> styles = new ArrayList<StyleRange>();

                Device element = (Device) cell.getElement();
                sb.append(element.getName());
                sb.append(" ");

                String description = element.getPortDescription();
                styles.add(new StyleRange(sb.length(), description.length(), new Color(0x80, 0x80, 0x00), null));
                sb.append(description);

                if (element.getStatus() != null) {
                    sb.append(" - ");
                    switch (element.getStatus()) {
                        case 0:
                            description = "Ok";
                            styles.add(new StyleRange(sb.length(), description.length(), new Color(0x00, 0xC0, 0x00), null));
                            sb.append(description);
                            break;
                        default:
                            description = "Error";
                            styles.add(new StyleRange(sb.length(), description.length(), new Color(0x0C, 0x00, 0x00), null));
                            sb.append(description);
                            break;
                    }
                }

                cell.setText(sb.toString());
                cell.setStyleRanges(styles.toArray(new StyleRange[styles.size()]));
            }

        });
        devicesViewer.setCheckStateProvider(new ICheckStateProvider() {

            @Override
            public boolean isGrayed(Object element) {
                return false;
            }

            @Override
            public boolean isChecked(Object element) {
                return ((Device) element).isSelected();
            }
        });
        devicesViewer.addFilter(new ViewerFilter() {

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                Device deviceElement = (Device) element;
                Firmware firmware = parameters.getFirmware();
                if (firmware == null || firmware.getBinaryVersion() == 0) {
                    return true;
                }
                return deviceElement.version == 0 || deviceElement.version == firmware.getBinaryVersion();
            }

        });
        devicesViewer.addCheckStateListener(new ICheckStateListener() {

            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                parameters.setDeviceSelection((Device) event.getElement(), event.getChecked());
            }

        });
        devicesViewer.setInput(parameters.getDevices());
        devicesViewer.getControl().setEnabled(!parameters.isUpdateAll());

        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        gridData.widthHint = convertWidthInCharsToPixels(65);
        gridData.heightHint = convertHeightInCharsToPixels(15);
        devicesViewer.getControl().setLayoutData(gridData);

        group = new Composite(parent, SWT.NONE);
        layout = new GridLayout(4, false);
        layout.marginWidth = layout.marginHeight = 0;
        group.setLayout(layout);
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        label = new Label(group, SWT.NONE);
        label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        enableLocal = new Button(group, SWT.CHECK);
        enableLocal.setText("Local");
        enableLocal.setSelection(parameters.isEnableLocal());
        enableLocal.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                parameters.setEnableLocal(((Button) event.widget).getSelection());
            }

        });
        enableNetwork = new Button(group, SWT.CHECK);
        enableNetwork.setText("Network");
        enableNetwork.setSelection(parameters.isEnableNetwork());
        enableNetwork.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                parameters.setEnableNetwork(((Button) event.widget).getSelection());
            }

        });

        discoverButton = new Button(group, SWT.PUSH);
        discoverButton.setText("Discover");
        discoverButton.setLayoutData(new GridData(convertHorizontalDLUsToPixels(60), SWT.DEFAULT));
        discoverButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                setControlsEnable(false);

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        DeviceDiscover discover = new DeviceDiscover();
                        discover.find(parameters.isEnableLocal(), parameters.isEnableNetwork(), (list) -> {
                            display.asyncExec(() -> {
                                parameters.setDevices(list);
                            });
                        });
                    } catch (Exception ex) {
                        // Do nothing
                        ex.printStackTrace();
                    }
                });
                BusyIndicator.showWhile(future);

                setControlsEnable(true);
            }

        });
    }

    void createButtonsGroup(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = layout.marginHeight = 0;
        container.setLayout(layout);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Label label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
        ((GridData) label.getLayoutData()).heightHint = convertVerticalDLUsToPixels(5);

        updateButton = new Button(container, SWT.PUSH);
        updateButton.setText("Update");
        updateButton.setLayoutData(new GridData(convertHorizontalDLUsToPixels(60), SWT.DEFAULT));
        updateButton.setEnabled(parameters.canDoUpdate());
        updateButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                setControlsEnable(false);
                try {
                    boolean writeToFlash = System.getenv("LOADER_WRITE_TO_RAM") == null;
                    startUpdate(writeToFlash);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                setControlsEnable(true);
            }

        });

        label = new Label(container, SWT.NONE);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button button = new Button(container, SWT.PUSH);
        button.setImage(ImageRegistry.getImageFromResources("help.png"));
        button.setLayoutData(new GridData(convertHorizontalDLUsToPixels(18), convertHorizontalDLUsToPixels(18)));
        //button.setLayoutData(new GridData(convertHorizontalDLUsToPixels(60), SWT.DEFAULT));
        button.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    AboutDialog dlg = new AboutDialog(shell);
                    dlg.open();
                } catch (Exception e) {
                    // Do nothing
                }
            }

        });

        shell.setDefaultButton(updateButton);
    }

    public void setEmbeddedFirmware(boolean embeddedFirmware) {
        this.embeddedFirmware = embeddedFirmware;
    }

    boolean isEmbeddedFirmware() {
        return embeddedFirmware;
    }

    void setControlsEnable(boolean enable) {
        if (firmwareFile != null) {
            firmwareFile.setEnabled(enable);
        }
        if (browseFile != null) {
            browseFile.setEnabled(enable);
        }

        allAvailable.setEnabled(enable);
        selectedOnly.setEnabled(enable);
        devicesViewer.getControl().setEnabled(enable && !parameters.isUpdateAll());

        enableLocal.setEnabled(enable);
        enableNetwork.setEnabled(enable);
        discoverButton.setEnabled(enable);

        updateButton.setEnabled(enable && parameters.canDoUpdate());
    }

    void startUpdate(boolean writeFlash) {
        List<Device> selectedDevices = new ArrayList<>();

        if (parameters.getDevices().size() == 0) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    DeviceDiscover discover = new DeviceDiscover();
                    discover.find(parameters.isEnableLocal(), parameters.isEnableNetwork(), (list) -> {
                        selectedDevices.addAll(list);
                        display.asyncExec(() -> {
                            parameters.setDevices(list);
                        });
                    });
                } catch (Exception ex) {
                    // Do nothing
                    ex.printStackTrace();
                }
            });
            BusyIndicator.showWhile(future);
        }
        else {
            selectedDevices.addAll(parameters.getSelectedDevices());
        }

        Firmware firmware = parameters.getFirmware();

        Iterator<Device> iter = selectedDevices.iterator();
        while (iter.hasNext()) {
            if (iter.next().getVersion() != firmware.getBinaryVersion()) {
                iter.remove();
            }
        }

        if (selectedDevices.size() == 0) {
            MessageDialog.openInformation(shell, APP_TITLE, "Found 0 device(s) to update.");
            return;
        }

        boolean doUpdate = MessageDialog.openConfirm(shell, APP_TITLE, "Found " + selectedDevices.size() + " device(s). Confirm firmware update?");
        if (doUpdate) {
            ProgressMonitorDialog dlg = new ProgressMonitorDialog(shell);

            for (Device device : selectedDevices) {
                device.clearStatus();
            }
            devicesViewer.refresh();

            IRunnableWithProgress thread = new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

                    PropellerLoaderListener listener = new PropellerLoaderListener() {

                        @Override
                        public void bufferUpload(int type, byte[] binaryImage, String text) {
                            monitor.subTask("Loading " + text + " to RAM");
                        }

                        @Override
                        public void verifyRam() {
                            monitor.subTask("Verifying RAM ... ");
                        }

                        @Override
                        public void eepromWrite() {
                            monitor.subTask("Writing EEPROM ... ");
                        }

                        @Override
                        public void eepromVerify() {
                            monitor.subTask("Verifying EEPROM ... ");
                        }

                    };

                    monitor.beginTask("Firmware upload", selectedDevices.size());

                    Display.getDefault().syncExec(new Runnable() {

                        @Override
                        public void run() {
                            dlg.open();
                        }
                    });

                    ComPort comPort;
                    for (Device device : selectedDevices) {
                        String portName = device.getSerialPort();
                        if (portName != null && !portName.isBlank()) {
                            comPort = new SerialComPort(portName);
                        }
                        else {
                            comPort = new NetworkComPort(device.getName(), device.getInetAddr(), device.getMacAddr(), device.getResetPin());
                        }
                        monitor.setTaskName("Firmware upload to " + comPort.getDescription());

                        try {
                            PropellerLoader loader = firmware.getBinaryVersion() == 1 ? new Propeller1Loader(comPort) : new Propeller2Loader(comPort);
                            loader.setListener(listener);
                            loader.upload(firmware.getBinaryImage(), writeFlash);
                            device.setStatus(0);
                        } catch (Exception e) {
                            e.printStackTrace();
                            device.setStatus(1);
                        }

                        Display.getDefault().syncExec(new Runnable() {

                            @Override
                            public void run() {
                                devicesViewer.update(device, null);
                            }
                        });

                        monitor.worked(1);
                    }
                }

            };

            try {
                dlg.setOpenOnRun(false);
                dlg.run(true, true, thread);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected int convertHorizontalDLUsToPixels(int dlus) {
        if (fontMetrics == null) {
            return 0;
        }
        return (int) ((fontMetrics.getAverageCharacterWidth() * dlus + HORIZONTAL_DIALOG_UNIT_PER_CHAR / 2) / HORIZONTAL_DIALOG_UNIT_PER_CHAR);
    }

    protected int convertVerticalDLUsToPixels(int dlus) {
        if (fontMetrics == null) {
            return 0;
        }
        return (fontMetrics.getHeight() * dlus + VERTICAL_DIALOG_UNITS_PER_CHAR / 2) / VERTICAL_DIALOG_UNITS_PER_CHAR;
    }

    protected int convertWidthInCharsToPixels(int chars) {
        if (fontMetrics == null) {
            return 0;
        }
        return (int) (fontMetrics.getAverageCharacterWidth() * chars);
    }

    protected int convertHeightInCharsToPixels(int chars) {
        if (fontMetrics == null) {
            return 0;
        }
        return fontMetrics.getHeight() * chars;
    }

    public LoaderParameters getParameters() {
        return parameters;
    }

    static {
        Display.setAppName(APP_TITLE);
        Display.setAppVersion(APP_VERSION);
    }

    public static void main(String[] args) {
        final Display display = new Display();

        display.setErrorHandler(new Consumer<Error>() {

            @Override
            public void accept(Error e) {
                openInternalError(display.getActiveShell(), "An unexpected error has occured.", e);
            }

        });
        display.setRuntimeExceptionHandler(new Consumer<RuntimeException>() {

            @Override
            public void accept(RuntimeException e) {
                openInternalError(display.getActiveShell(), "An unexpected error has occured.", e);
            }

        });

        Realm.runWithDefault(DisplayRealm.getRealm(display), new Runnable() {

            @Override
            public void run() {
                try {
                    Shell shell = new Shell(display);
                    shell.setText(APP_TITLE);
                    shell.setImages(new Image[] {
                        ImageRegistry.getImageFromResources("app64.png"),
                        ImageRegistry.getImageFromResources("app48.png"),
                        ImageRegistry.getImageFromResources("app32.png"),
                        ImageRegistry.getImageFromResources("app16.png"),
                    });

                    FillLayout layout = new FillLayout();
                    layout.marginWidth = layout.marginHeight = 0;
                    shell.setLayout(layout);

                    Loader app = new Loader(shell);
                    app.createContents(shell);

                    shell.pack();

                    Rectangle screen = display.getClientArea();

                    Rectangle rect = shell.getBounds();
                    rect.x = (screen.width - rect.width) / 2;
                    rect.y = (screen.height - rect.height) / 2;
                    shell.setLocation(rect.x, rect.y);

                    shell.open();

                    while (display.getShells().length != 0) {
                        if (!display.readAndDispatch()) {
                            display.sleep();
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        });

        display.dispose();
    }

    static boolean internalErrorRunning;

    public static void openInternalError(Shell shell, String message, Throwable details) {
        if (internalErrorRunning) {
            return;
        }
        internalErrorRunning = true;
        try {
            details.printStackTrace();
            InternalErrorDialog dlg = new InternalErrorDialog(shell, APP_TITLE, null, "An unexpected error has occured.", details, MessageDialog.ERROR, new String[] {
                IDialogConstants.OK_LABEL, IDialogConstants.SHOW_DETAILS_LABEL
            }, 0);
            dlg.setDetailButton(1);
            dlg.open();
        } finally {
            internalErrorRunning = false;
        }
    }

}
