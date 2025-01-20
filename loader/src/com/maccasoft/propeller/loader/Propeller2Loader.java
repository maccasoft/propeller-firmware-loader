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

package com.maccasoft.propeller.loader;

import java.io.InputStream;
import java.util.Base64;
import java.util.Base64.Encoder;

import com.maccasoft.propeller.port.ComPort;
import com.maccasoft.propeller.port.ComPortException;

import jssc.SerialPort;

public class Propeller2Loader extends PropellerLoader {

    public static final int DOWNLOAD_RUN_RAM = 0;
    public static final int DOWNLOAD_RUN_FLASH = 1;

    ComPort comPort;

    public Propeller2Loader(ComPort serialPort) {
        this.comPort = serialPort;
    }

    public Propeller2Loader(ComPort serialPort, PropellerLoaderListener listener) {
        super(listener);
        this.comPort = serialPort;
    }

    public String getPortName() {
        return comPort.getPortName();
    }

    @Override
    public void upload(byte[] binaryImage, boolean writeFlash) throws ComPortException {

        try {
            comPort.openPort();
            comPort.setParams(2000000, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            int rc = hwfind();
            if (rc == 0) {
                throw new ComPortException("No propeller chip on port " + comPort.getPortName());
            }

            bufferUpload(writeFlash ? DOWNLOAD_RUN_FLASH : DOWNLOAD_RUN_RAM, binaryImage, "binary image");

        } finally {
            comPort.closePort();
        }
    }

    protected int hwfind() throws ComPortException {

        comPort.hwreset(15);
        comPort.writeString("> \r> Prop_Chk 0 0 0 0\r");

        readStringWithTimeout(comPort, 50);

        String result = readStringWithTimeout(comPort, 50);
        if (result.startsWith("Prop_Ver ")) {
            return 2;
        }

        return 0;
    }

    private String readStringWithTimeout(ComPort comPort, int timeout) throws ComPortException {
        int b;
        StringBuilder sb = new StringBuilder();

        do {
            b = comPort.readByteWithTimeout(timeout);
            if (b != -1) {
                if (b == '\r') {
                    b = comPort.readByteWithTimeout(timeout);
                    break;
                }
                sb.append((char) b);
            }
        } while (b != -1);

        return sb.toString();
    }

    protected void bufferUpload(int type, byte[] binaryImage, String text) throws ComPortException {
        int n;

        if (listener != null) {
            listener.bufferUpload(type, binaryImage, text);
        }

        if (type == DOWNLOAD_RUN_FLASH) {
            InputStream is = getClass().getResourceAsStream("flash_loader.binary");
            try {
                byte[] loader = new byte[is.available()];
                is.read(loader);

                byte[] loaderImage = new byte[loader.length + ((binaryImage.length + 3) & ~3)];
                System.arraycopy(loader, 0, loaderImage, 0, loader.length);
                System.arraycopy(binaryImage, 0, loaderImage, loader.length, binaryImage.length);
                binaryImage = loaderImage;

                binaryImage[8] = (byte) binaryImage.length;
                binaryImage[9] = (byte) (binaryImage.length >> 8);
                binaryImage[10] = (byte) (binaryImage.length >> 16);
                binaryImage[11] = (byte) (binaryImage.length >> 24);

                int sum = 0;
                for (n = 0; n < binaryImage.length; n += 4) {
                    int data = binaryImage[n] & 0xFF;
                    if ((n + 1) < binaryImage.length) {
                        data |= (binaryImage[n + 1] << 8) & 0xFF00;
                        if ((n + 2) < binaryImage.length) {
                            data |= (binaryImage[n + 2] << 16) & 0xFF0000;
                            if ((n + 3) < binaryImage.length) {
                                data |= (binaryImage[n + 3] << 24) & 0xFF000000;
                            }
                        }
                    }
                    sum -= data;
                }

                binaryImage[4] = (byte) sum;
                binaryImage[5] = (byte) (sum >> 8);
                binaryImage[6] = (byte) (sum >> 16);
                binaryImage[7] = (byte) (sum >> 24);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        base64Upload(binaryImage);
        verifyRam();

        if (type == DOWNLOAD_RUN_FLASH) {
            flashWrite();
        }
    }

    void hexUpload(byte[] binaryImage) throws ComPortException {
        int n;

        comPort.writeString("> Prop_Hex 0 0 0 0");

        int sum = 0;
        for (n = 0; n < binaryImage.length; n += 4) {
            if (n > 0 && (n % 64) == 0) {
                comPort.writeString("\r>");
                notifyProgress(n, binaryImage.length);
            }
            int data = binaryImage[n] & 0xFF;
            if ((n + 1) < binaryImage.length) {
                data |= (binaryImage[n + 1] << 8) & 0xFF00;
                if ((n + 2) < binaryImage.length) {
                    data |= (binaryImage[n + 2] << 16) & 0xFF0000;
                    if ((n + 3) < binaryImage.length) {
                        data |= (binaryImage[n + 3] << 24) & 0xFF000000;
                    }
                }
            }
            comPort.writeString(String.format(" %x %x %x %x", data & 0xFF, (data >> 8) & 0xFF, (data >> 16) & 0xFF, (data >> 24) & 0xFF));
            sum += data;
        }
        notifyProgress(n, binaryImage.length);

        sum = 0x706F7250 - sum;
        comPort.writeString(String.format(" %x %x %x %x ?", sum & 0xFF, (sum >> 8) & 0xFF, (sum >> 16) & 0xFF, (sum >> 24) & 0xFF));
    }

    void base64Upload(byte[] binaryImage) throws ComPortException {
        int n, sent;
        Encoder encoder = Base64.getEncoder();

        int sum = 0;
        for (n = 0; n < binaryImage.length; n += 4) {
            int data = binaryImage[n] & 0xFF;
            if ((n + 1) < binaryImage.length) {
                data |= (binaryImage[n + 1] << 8) & 0xFF00;
                if ((n + 2) < binaryImage.length) {
                    data |= (binaryImage[n + 2] << 16) & 0xFF0000;
                    if ((n + 3) < binaryImage.length) {
                        data |= (binaryImage[n + 3] << 24) & 0xFF000000;
                    }
                }
            }
            sum += data;
        }
        sum = 0x706F7250 - sum;

        byte[] image = new byte[(binaryImage.length + 7) & ~3];
        System.arraycopy(binaryImage, 0, image, 0, binaryImage.length);
        image[image.length - 4] = (byte) sum;
        image[image.length - 3] = (byte) (sum >> 8);
        image[image.length - 2] = (byte) (sum >> 16);
        image[image.length - 1] = (byte) (sum >> 24);
        binaryImage = image;

        comPort.writeString("> Prop_Txt 0 0 0 0");

        String encodedImage = encoder.encodeToString(binaryImage);
        for (n = 0, sent = 0; n < encodedImage.length(); n += 64, sent += 48) {
            comPort.writeString("\r> ");
            comPort.writeString(encodedImage.substring(n, n + Math.min(64, encodedImage.length() - n)));
            notifyProgress(sent, binaryImage.length);
        }
        notifyProgress(sent, binaryImage.length);

        comPort.writeString(" ?");
    }

    protected void notifyProgress(int sent, int total) {
        // Do nothing
    }

    protected void flashWrite() throws ComPortException {

        if (listener != null) {
            listener.eepromWrite();
        }

    }

    protected void verifyRam() throws ComPortException {

        if (listener != null) {
            listener.verifyRam();
        }

        int rc = comPort.readByteWithTimeout(10_000);

        if (rc == -1) {
            throw new ComPortException("Timeout");
        }

        if (rc != '.') {
            throw new ComPortException("Checksum error");
        }
    }

}
