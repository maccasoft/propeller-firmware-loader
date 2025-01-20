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

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LoaderParametersTest {

    @Test
    void testAddNewDevices() {
        LoaderParameters subject = new LoaderParameters();

        subject.setDevices(new ArrayList<>(Arrays.asList(new Device[] {
            new Device("test1", 1, "/dev/ttyUSB0"),
            new Device("test2", 1, "/dev/ttyUSB1"),
        })));

        Assertions.assertEquals(2, subject.getDevices().size());
    }

    @Test
    void testRemoveOldDevices() {
        LoaderParameters subject = new LoaderParameters();
        subject.devices.addAll(Arrays.asList(new Device[] {
            new Device("test1", 1, "/dev/ttyUSB0"),
            new Device("test2", 1, "/dev/ttyUSB1"),
        }));

        subject.setDevices(new ArrayList<>(Arrays.asList(new Device[] {
            new Device("test2", 1, "/dev/ttyUSB1"),
        })));

        Assertions.assertEquals(1, subject.getDevices().size());
    }

    @Test
    void testKeepExistingDevice() {
        LoaderParameters subject = new LoaderParameters();

        Device device = new Device("test", 1, "/dev/ttyUSB0");
        subject.devices.add(device);

        subject.setDevices(new ArrayList<>(Arrays.asList(new Device[] {
            new Device("test", 1, "/dev/ttyUSB0")
        })));

        Assertions.assertEquals(1, subject.getDevices().size());
        Assertions.assertSame(device, subject.getDevices().get(0));

    }

}
