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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LoaderParameters {

    public static final String PROP_FILE = "file";
    public static final String PROP_FIRMWARE_LIST = "firmwareList";
    public static final String PROP_FIRMWARE = "firmware";
    public static final String PROP_UPDATE_ALL = "updateAll";
    public static final String PROP_DEVICES = "devices";
    public static final String PROP_ENABLE_LOCAL = "enableLocal";
    public static final String PROP_ENABLE_NETWORK = "enableNetwork";

    public static final String PROP_DEVICE_SELECTION = "deviceSelection";

    File file;

    List<Firmware> firmwareList;

    boolean updateAll;
    List<Device> devices;

    boolean enableLocal;
    boolean enableNetwork;

    Firmware firmware;

    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    public LoaderParameters() {
        updateAll = true;
        enableLocal = true;
        devices = new ArrayList<>();
        firmwareList = new ArrayList<>();
    }

    public void updateFrom(Firmware firmware) {
        setFirmwareList(new ArrayList<>());
        setFirmware(firmware);
    }

    public void updateFrom(FirmwarePack pack) {
        setFirmwareList(pack.getFirmwareList());
        if (pack.getFirmwareList().size() != 0) {
            setFirmware(pack.getFirmwareList().get(0));
        }
        setEnableLocal(pack.isEnableLocal());
        setEnableNetwork(pack.isEnableNetwork());
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(propertyName, listener);
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        changeSupport.firePropertyChange(PROP_FILE, this.file, this.file = file);
    }

    public boolean isUpdateAll() {
        return updateAll;
    }

    public void setUpdateAll(boolean updateAll) {
        changeSupport.firePropertyChange(PROP_UPDATE_ALL, this.updateAll, this.updateAll = updateAll);
    }

    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        Iterator<Device> iter = devices.iterator();
        while (iter.hasNext()) {
            Device device = iter.next();
            if (!this.devices.contains(device)) {
                this.devices.add(device);
            }
        }

        iter = this.devices.iterator();
        while (iter.hasNext()) {
            Device device = iter.next();
            if (!devices.contains(device)) {
                iter.remove();
            }
        }

        changeSupport.firePropertyChange(PROP_DEVICES, null, this.devices);
    }

    public void setDeviceSelection(Device device, boolean selection) {
        if (device.isSelected() != selection) {
            device.setSelected(selection);
            changeSupport.firePropertyChange(PROP_DEVICE_SELECTION, null, device);
        }
    }

    public boolean isEnableLocal() {
        return enableLocal;
    }

    public void setEnableLocal(boolean enableLocal) {
        changeSupport.firePropertyChange(PROP_ENABLE_LOCAL, this.enableLocal, this.enableLocal = enableLocal);
    }

    public boolean isEnableNetwork() {
        return enableNetwork;
    }

    public void setEnableNetwork(boolean enableNetwork) {
        changeSupport.firePropertyChange(PROP_ENABLE_NETWORK, this.enableNetwork, this.enableNetwork = enableNetwork);
    }

    public List<Firmware> getFirmwareList() {
        return firmwareList;
    }

    public void setFirmwareList(List<Firmware> firmwareList) {
        this.firmwareList.clear();
        this.firmwareList.addAll(firmwareList);
        changeSupport.firePropertyChange(PROP_FIRMWARE_LIST, null, this.firmwareList);
    }

    public Firmware getFirmware() {
        return firmware;
    }

    public void setFirmware(Firmware firmware) {
        changeSupport.firePropertyChange(PROP_FIRMWARE, this.firmware, this.firmware = firmware);
    }

    public boolean canDoUpdate() {
        if (getFirmware() == null || getFirmware().getBinaryVersion() == 0) {
            return false;
        }
        return isUpdateAll() || getSelectedDevices().size() != 0;
    }

    public List<Device> getSelectedDevices() {
        List<Device> list = new ArrayList<>();

        if (isUpdateAll()) {
            list.addAll(devices);
        }
        else {
            Iterator<Device> iter = devices.iterator();
            while (iter.hasNext()) {
                Device device = iter.next();
                if (device.isSelected()) {
                    list.add(device);
                }
            }
        }

        return list;
    }

}
