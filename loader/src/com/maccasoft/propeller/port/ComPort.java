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

package com.maccasoft.propeller.port;

public abstract class ComPort {

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getPortName();

    public abstract boolean isOpened();

    public abstract boolean openPort() throws ComPortException;

    public abstract boolean setParams(int baudRate, int dataBits, int stopBits, int parity) throws ComPortException;

    public abstract void closePort() throws ComPortException;

    public abstract void hwreset(int delay);

    public abstract int readByteWithTimeout(int timeout) throws ComPortException;

    public abstract boolean writeInt(int singleInt) throws ComPortException;

    public abstract boolean writeByte(byte singleByte) throws ComPortException;

    public abstract boolean writeBytes(byte[] buffer) throws ComPortException;

    public abstract boolean writeString(String string) throws ComPortException;

    public abstract byte[] readBytes() throws ComPortException;

    public abstract void setRTS(boolean enable) throws ComPortException;

    public abstract void setDTR(boolean enable) throws ComPortException;

    public abstract boolean isCTS() throws ComPortException;

    public abstract boolean isDSR() throws ComPortException;

}
