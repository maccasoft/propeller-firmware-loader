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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maccasoft.propeller.port.ComPort;
import com.maccasoft.propeller.port.ComPortException;
import com.maccasoft.propeller.port.DeviceDescriptor;
import com.maccasoft.propeller.port.NetworkComPort;
import com.maccasoft.propeller.port.SerialComPort;

import jssc.SerialPort;
import jssc.SerialPortList;

public class DeviceDiscover {

    public static final int HTTP_PORT = 80;
    public static final int TELNET_PORT = 23;
    public static final int DISCOVER_PORT = 32420;

    public static final int CONNECT_TIMEOUT = 3000;
    public static final int RESPONSE_TIMEOUT = 3000;
    public static final int DISCOVER_REPLY_TIMEOUT = 250;
    public static final int DISCOVER_ATTEMPTS = 3;

    byte LFSR;

    public DeviceDiscover() {

    }

    public void find(boolean local, boolean network, DeviceDiscoverListener listener) {
        List<Device> list = new ArrayList<>();
        if (local) {
            list.addAll(findLocalDevices());
        }
        if (network) {
            list.addAll(findNetworkDevices());
        }
        Collections.sort(list);
        listener.discoverCompleted(list);
    }

    List<Device> findLocalDevices() {
        List<Device> list = new ArrayList<>();

        String[] portNames = SerialPortList.getPortNames();
        for (int i = 0; i < portNames.length; i++) {
            SerialComPort comPort = new SerialComPort(portNames[i]);
            try {
                Device device = find(comPort);
                if (device != null) {
                    list.add(device);
                }
            } catch (Exception e) {
                // Do nothing
            }
        }

        return list;
    }

    Device find(SerialComPort comPort) throws ComPortException {
        int rc = 0;

        comPort.openPort();

        try {
            rc = probeP2(comPort);
            if (rc == 0) {
                rc = probeP1(comPort);
            }
        } catch (Exception e) {
            // Do nothing
        }

        comPort.closePort();

        if (rc != 0) {
            return new Device(getVersionText(rc), rc == 1 ? 1 : 2, comPort.getPortName());
        }

        return null;
    }

    int probeP1(SerialComPort comPort) throws ComPortException {
        int n, ii, jj;
        byte[] buffer;

        try {
            comPort.setParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            comPort.hwreset(90);

            // send the calibration pulse
            comPort.writeInt(0xF9);

            // send the magic propeller LFSR byte stream.
            LFSR = 'P';
            buffer = new byte[250];
            for (n = 0; n < 250; n++) {
                buffer[n] = (byte) (iterate() | 0xFE);
            }
            comPort.writeBytes(buffer);

            // Send 258 0xF9 for LFSR and Version ID
            // These bytes clock the LSFR bits and ID from propeller back to us.
            buffer = new byte[258];
            for (n = 0; n < 258; n++) {
                buffer[n] = (byte) (0xF9);
            }
            comPort.writeBytes(buffer);

            // Wait at least 100ms for the first response. Allow some margin.
            // Some chips may respond < 50ms, but there's no guarantee all will.
            // If we don't get it, we can assume the propeller is not there.
            if ((ii = getBit(comPort, 110)) == -1) {
                throw new ComPortException("timeout waiting for first response bit");
            }

            // wait for response so we know we have a Propeller
            for (n = 1; n < 250; n++) {
                jj = iterate();

                if (ii != jj) {
                    for (n = 0; n < 300; n++) {
                        if (comPort.readByteWithTimeout(50) == -1) {
                            break;
                        }
                    }
                    return 0;
                }

                int to = 0;
                do {
                    if ((ii = getBit(comPort, 110)) != -1) {
                        break;
                    }
                } while (to++ < 100);

                if (to > 100) {
                    throw new ComPortException("timeout waiting for response bit");
                }
            }

            int rc = 0;
            for (n = 0; n < 8; n++) {
                rc >>= 1;
                if ((ii = getBit(comPort, 110)) != -1) {
                    rc += (ii != 0) ? 0x80 : 0;
                }
            }

            return rc;

        } catch (Exception e) {
            // Do nothing
        }

        return 0;
    }

    int getBit(SerialComPort comPort, int timeout) {
        try {
            int rx = comPort.readByteWithTimeout(timeout);
            return rx & 1;
        } catch (ComPortException e) {

        }
        return -1;
    }

    int iterate() {
        int bit = LFSR & 1;
        LFSR = (byte) ((LFSR << 1) | (((LFSR >> 7) ^ (LFSR >> 5) ^ (LFSR >> 4) ^ (LFSR >> 1)) & 1));
        return bit;
    }

    int probeP2(ComPort comPort) throws ComPortException {

        comPort.setParams(2000000, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

        comPort.hwreset(15);
        comPort.writeString("> \r> Prop_Chk 0 0 0 0\r");

        readStringWithTimeout(comPort, 50);

        String result = readStringWithTimeout(comPort, 50);
        if (result.startsWith("Prop_Ver ")) {
            return result.charAt(9);
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

    public List<Device> findNetworkDevices() {
        List<Device> list = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()) {
                NetworkInterface nif = nets.nextElement();
                if (nif.isUp() && !nif.isLoopback()) {
                    for (InterfaceAddress addr : nif.getInterfaceAddresses()) {
                        InetAddress inetAddr = addr.getBroadcast();
                        if (inetAddr != null) {
                            Device result = probe(inetAddr, true);
                            if (result != null) {
                                list.add(result);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    Device probe(InetAddress inetAddr, boolean broadcast) throws IOException {
        Device found = null;
        byte[] buffer = new byte[2048];

        DatagramSocket socket = new DatagramSocket(DISCOVER_PORT);
        if (broadcast) {
            socket.setBroadcast(true);
        }
        socket.setSoTimeout(DISCOVER_REPLY_TIMEOUT);

        for (int i = 0; i < DISCOVER_ATTEMPTS && found == null; i++) {
            socket.send(new DatagramPacket(new byte[] {
                0x00, 0x00, 0x00, 0x00
            }, 4, inetAddr, DISCOVER_PORT));

            try {
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                do {
                    socket.receive(response);
                    if (response.getLength() > 0) {
                        byte[] data = response.getData();
                        if (data[0] != 0x00) {
                            String json = new String(data, 0, response.getLength());
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                DeviceDescriptor descriptor = mapper.readValue(json, DeviceDescriptor.class);
                                if (descriptor != null) {
                                    NetworkComPort serialPort = new NetworkComPort(descriptor.name, inetAddr, descriptor.mac_address, descriptor.reset_pin);
                                    try {
                                        serialPort.openPort();
                                        int rc = probeP2(serialPort);
                                        if (rc == 0) {
                                            rc = probeP2(serialPort);
                                        }
                                        if (rc == 0) {
                                            rc = probeP2(serialPort);
                                        }
                                        if (rc != 0) {
                                            String version = getVersionText(rc);
                                            if (version != null) {
                                                found = new Device(version, 2, response.getAddress(), descriptor.mac_address, descriptor.reset_pin);
                                            }
                                        }
                                    } catch (Exception e) {
                                        // Do nothing
                                    }
                                    if (serialPort.isOpened()) {
                                        serialPort.closePort();
                                    }

                                    if (found == null) {
                                        found = new Device(descriptor.name, 1, response.getAddress(), descriptor.mac_address, descriptor.reset_pin);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                } while (found == null);
            } catch (SocketTimeoutException e) {
                // Do nothing
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }

        socket.close();

        return found;
    }

    String getVersionText(int version) {
        switch (version) {
            case 0:
                // Not found
                return null;
            case 1:
                return "P8X32A";
            case 'G':
                return "P2X8C4M64P Rev B/C";
            default:
                return "Unknown version " + (Character.isLetterOrDigit(version) ? "'" + (char) version + "'" : version);
        }
    }

    public static void main(String[] args) {
        try {
            DeviceDiscover discover = new DeviceDiscover();
            discover.find(true, true, (list) -> {
                for (Device device : list) {
                    System.out.println(device.getName() + " - " + device.getPortDescription());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
