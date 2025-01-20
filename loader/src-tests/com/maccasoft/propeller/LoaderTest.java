/*
 * Copyright (c) 2025 Marco Maccaferri and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.maccasoft.propeller;

import java.io.File;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
class LoaderTest {

    Display display;
    Shell shell;
    Loader app;

    @BeforeAll
    void initialize() {
        display = Display.getDefault();
    }

    @BeforeEach
    void setUp() {
        shell = new Shell(display);
        app = new Loader(shell);
        runEvents();
    }

    @AfterEach
    void tearDown() {
        runEvents();
        shell.dispose();
        runEvents();
    }

    @AfterAll
    void terminate() {
        display.dispose();
    }

    void runEvents() {
        while (display.readAndDispatch()) {

        }
    }

    @Test
    void testDefaults() {
        app.createContents(shell);
        runEvents();

        Assertions.assertEquals("", app.firmwareFile.getText());
        Assertions.assertEquals(0, app.firmwareList.getCombo().getItemCount());
        Assertions.assertEquals("", app.firmwareInfo.getText());
        Assertions.assertSame(app.firmwareInfoContainer, app.selectionGroupStack.topControl);
        Assertions.assertTrue(app.allAvailable.getSelection());
        Assertions.assertFalse(app.selectedOnly.getSelection());
        Assertions.assertEquals(0, app.devicesViewer.getTable().getItemCount());
        Assertions.assertTrue(app.enableLocal.getSelection());
        Assertions.assertFalse(app.enableNetwork.getSelection());
        Assertions.assertFalse(app.updateButton.getEnabled());
    }

    @Test
    void testBinaryFirmwareStartup() {
        File file = new File("test.binary");
        Firmware firmware = new Firmware(1, new byte[0], "firmware description");
        app.updateFrom(file, firmware);

        app.createContents(shell);
        runEvents();

        Assertions.assertEquals(file.getName(), app.firmwareFile.getText());
        Assertions.assertEquals(0, app.firmwareList.getCombo().getItemCount());
        Assertions.assertEquals(firmware.getDescription(), app.firmwareInfo.getText());
        Assertions.assertSame(app.firmwareInfoContainer, app.selectionGroupStack.topControl);
        Assertions.assertTrue(app.allAvailable.getSelection());
        Assertions.assertFalse(app.selectedOnly.getSelection());
        Assertions.assertEquals(0, app.devicesViewer.getTable().getItemCount());
        Assertions.assertTrue(app.enableLocal.getSelection());
        Assertions.assertFalse(app.enableNetwork.getSelection());
        Assertions.assertTrue(app.updateButton.getEnabled());
    }

    @Test
    void testFirmwarePackStartup() {
        File file = new File("test.json");
        Firmware firmware = new Firmware(1, new byte[0], "firmware description");
        FirmwarePack firmwarePack = new FirmwarePack();
        firmwarePack.addFirmware(firmware);
        firmwarePack.setEnableLocal(false);
        firmwarePack.setEnableNetwork(true);
        app.updateFrom(file, firmwarePack);

        app.createContents(shell);
        runEvents();

        Assertions.assertEquals(file.getName(), app.firmwareFile.getText());
        Assertions.assertEquals(1, app.firmwareList.getCombo().getItemCount());
        Assertions.assertSame(firmware, app.firmwareList.getStructuredSelection().getFirstElement());
        Assertions.assertEquals(firmware.getDescription(), app.firmwareInfo.getText());
        Assertions.assertSame(app.firmwareListContainer, app.selectionGroupStack.topControl);
        Assertions.assertTrue(app.allAvailable.getSelection());
        Assertions.assertFalse(app.selectedOnly.getSelection());
        Assertions.assertEquals(0, app.devicesViewer.getTable().getItemCount());
        Assertions.assertFalse(app.enableLocal.getSelection());
        Assertions.assertTrue(app.enableNetwork.getSelection());
        Assertions.assertTrue(app.updateButton.getEnabled());
    }

    @Test
    void testEmbeddedFirmwarePackStartup() {
        File file = new File("test.json");
        Firmware firmware = new Firmware(1, new byte[0], "firmware description");
        FirmwarePack firmwarePack = new FirmwarePack();
        firmwarePack.addFirmware(firmware);
        firmwarePack.setEnableLocal(false);
        firmwarePack.setEnableNetwork(true);

        app.updateFrom(file, firmwarePack);
        app.setEmbeddedFirmware(true);

        app.createContents(shell);
        runEvents();

        Assertions.assertEquals(file.getName(), app.firmwareFile.getText());
        Assertions.assertEquals(1, app.firmwareList.getCombo().getItemCount());
        Assertions.assertSame(firmware, app.firmwareList.getStructuredSelection().getFirstElement());
        Assertions.assertEquals(firmware.getDescription(), app.firmwareInfo.getText());
        Assertions.assertSame(app.firmwareListContainer, app.selectionGroupStack.topControl);
        Assertions.assertTrue(app.allAvailable.getSelection());
        Assertions.assertFalse(app.selectedOnly.getSelection());
        Assertions.assertEquals(0, app.devicesViewer.getTable().getItemCount());
        Assertions.assertFalse(app.enableLocal.getSelection());
        Assertions.assertTrue(app.enableNetwork.getSelection());
        Assertions.assertTrue(app.updateButton.getEnabled());
    }

    @Test
    void testLoadBinaryFirmware() {
        File file = new File("test.binary");
        Firmware firmware = new Firmware(1, new byte[0], "firmware description");

        app.createContents(shell);
        runEvents();

        app.updateFrom(file, firmware);
        runEvents();

        Assertions.assertEquals(file.getName(), app.firmwareFile.getText());
        Assertions.assertEquals(0, app.firmwareList.getCombo().getItemCount());
        Assertions.assertEquals(firmware.getDescription(), app.firmwareInfo.getText());
        Assertions.assertSame(app.firmwareInfoContainer, app.selectionGroupStack.topControl);
        Assertions.assertTrue(app.allAvailable.getSelection());
        Assertions.assertFalse(app.selectedOnly.getSelection());
        Assertions.assertEquals(0, app.devicesViewer.getTable().getItemCount());
        Assertions.assertTrue(app.enableLocal.getSelection());
        Assertions.assertFalse(app.enableNetwork.getSelection());
        Assertions.assertTrue(app.updateButton.getEnabled());
    }

    @Test
    void testLoadFirmwarePack() {
        File file = new File("test.json");
        Firmware firmware = new Firmware(1, new byte[0], "firmware description");
        FirmwarePack firmwarePack = new FirmwarePack();
        firmwarePack.addFirmware(firmware);
        firmwarePack.setEnableLocal(false);
        firmwarePack.setEnableNetwork(true);

        app.createContents(shell);
        runEvents();

        app.updateFrom(file, firmwarePack);
        runEvents();

        Assertions.assertEquals(file.getName(), app.firmwareFile.getText());
        Assertions.assertEquals(1, app.firmwareList.getCombo().getItemCount());
        Assertions.assertSame(firmware, app.firmwareList.getStructuredSelection().getFirstElement());
        Assertions.assertEquals(firmware.getDescription(), app.firmwareInfo.getText());
        Assertions.assertSame(app.firmwareListContainer, app.selectionGroupStack.topControl);
        Assertions.assertTrue(app.allAvailable.getSelection());
        Assertions.assertFalse(app.selectedOnly.getSelection());
        Assertions.assertEquals(0, app.devicesViewer.getTable().getItemCount());
        Assertions.assertFalse(app.enableLocal.getSelection());
        Assertions.assertTrue(app.enableNetwork.getSelection());
        Assertions.assertTrue(app.updateButton.getEnabled());
    }

    @Test
    void testSwitchBinaryFirmware() {
        File file = new File("test.binary");
        Firmware firmware1 = new Firmware(1, new byte[0], "firmware description 1");
        Firmware firmware2 = new Firmware(1, new byte[0], "firmware description 2");

        app.createContents(shell);
        runEvents();

        app.updateFrom(file, firmware1);
        runEvents();

        Assertions.assertEquals(firmware1.getDescription(), app.firmwareInfo.getText());

        app.updateFrom(file, firmware2);
        runEvents();

        Assertions.assertEquals(file.getName(), app.firmwareFile.getText());
        Assertions.assertEquals(0, app.firmwareList.getCombo().getItemCount());
        Assertions.assertEquals(firmware2.getDescription(), app.firmwareInfo.getText());
        Assertions.assertSame(app.firmwareInfoContainer, app.selectionGroupStack.topControl);
        Assertions.assertTrue(app.allAvailable.getSelection());
        Assertions.assertFalse(app.selectedOnly.getSelection());
        Assertions.assertEquals(0, app.devicesViewer.getTable().getItemCount());
        Assertions.assertTrue(app.enableLocal.getSelection());
        Assertions.assertFalse(app.enableNetwork.getSelection());
        Assertions.assertTrue(app.updateButton.getEnabled());
    }

}
