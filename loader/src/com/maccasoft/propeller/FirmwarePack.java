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
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class FirmwarePack {

    @JsonInclude(Include.ALWAYS)
    List<Firmware> firmwareList;

    @JsonInclude(Include.ALWAYS)
    boolean enableLocal;
    @JsonInclude(Include.ALWAYS)
    boolean enableNetwork;

    public FirmwarePack() {
        enableLocal = true;
        firmwareList = new ArrayList<>();
    }

    public boolean isEnableLocal() {
        return enableLocal;
    }

    public void setEnableLocal(boolean enableLocal) {
        this.enableLocal = enableLocal;
    }

    public boolean isEnableNetwork() {
        return enableNetwork;
    }

    public void setEnableNetwork(boolean enableNetwork) {
        this.enableNetwork = enableNetwork;
    }

    public List<Firmware> getFirmwareList() {
        return firmwareList;
    }

    public void addFirmware(Firmware firmware) {
        this.firmwareList.add(firmware);
    }

    public void removeFirmware(Firmware firmware) {
        this.firmwareList.remove(firmware);
    }

    public static void main(String[] args) {
        try {
            FirmwarePack pack = new FirmwarePack();

            pack.addFirmware(Firmware.fromFile(new File("/home/marco/workspace/spin-tools-ide/examples/P1/jm_i2c_devices.binary")));
            pack.addFirmware(Firmware.fromFile(new File("/home/marco/workspace/spin-tools-ide/examples/P2/jm_i2c_devices.binary")));

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            mapper.setSerializationInclusion(Include.NON_DEFAULT);
            mapper.writeValue(new File("firmware-pack.json"), pack);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
