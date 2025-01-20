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

import java.net.InetAddress;
import java.util.Objects;

public class Device implements Comparable<Device> {

    String name;
    int version;

    String serialPort;

    InetAddress inetAddr;
    String macAddr;
    String resetPin;

    boolean selected;
    Integer status;

    public Device(String name, int version, String serialPort) {
        this.name = name;
        this.version = version;
        this.serialPort = serialPort;
    }

    public Device(String name, int version, InetAddress inetAddr, String macAddr, String resetPin) {
        this.name = name;
        this.version = version;
        this.inetAddr = inetAddr;
        this.macAddr = macAddr;
        this.resetPin = resetPin;
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

    public String getSerialPort() {
        return serialPort;
    }

    public InetAddress getInetAddr() {
        return inetAddr;
    }

    public String getMacAddr() {
        return macAddr;
    }

    public String getResetPin() {
        return resetPin;
    }

    public String getPortDescription() {
        if (serialPort != null) {
            return serialPort;
        }
        return inetAddr.getHostAddress();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void clearStatus() {
        this.status = null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inetAddr, macAddr, serialPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Device other = (Device) obj;
        return Objects.equals(inetAddr, other.inetAddr) && Objects.equals(macAddr, other.macAddr) && Objects.equals(serialPort, other.serialPort);
    }

    @Override
    public int compareTo(Device o) {
        if (getSerialPort() != null && o.getSerialPort() == null) {
            return -1;
        }
        if (getSerialPort() == null && o.getSerialPort() != null) {
            return 1;
        }

        if (getSerialPort() != null && o.getSerialPort() != null) {
            return getSerialPort().compareTo(o.getSerialPort());
        }

        if (getInetAddr() != null && o.getInetAddr() != null) {
            byte[] our = getInetAddr().getAddress();
            byte[] other = o.getInetAddr().getAddress();
            for (int i = 0; i < our.length; i++) {
                if (our[i] != other[i]) {
                    return our[i] - other[i];
                }
            }
        }

        return 0;
    }

}