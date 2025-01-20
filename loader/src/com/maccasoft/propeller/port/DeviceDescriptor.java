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

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class DeviceDescriptor {

    public String name;
    public String description;

    @JsonProperty("reset pin")
    public String reset_pin;

    @JsonProperty("rx pullup")
    @JsonDeserialize(using = CustomBooleanDeserializer.class)
    public boolean rx_pullup;

    @JsonProperty("mac address")
    public String mac_address;

    static class CustomBooleanDeserializer extends StdDeserializer<Boolean> {

        private static final long serialVersionUID = -7905274166200998028L;

        public CustomBooleanDeserializer() {
            this(null);
        }

        public CustomBooleanDeserializer(Class<Boolean> vc) {
            super(vc);
        }

        @Override
        public Boolean deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

            if (List.of("1", "active", "true", "enabled").contains(p.getText())) {
                return Boolean.TRUE;
            }
            else if (List.of("0", "inactive", "false", "disabled").contains(p.getText())) {
                return Boolean.FALSE;
            }
            return null;
        }

    }

    public DeviceDescriptor() {

    }

    @Override
    public String toString() {
        return "DeviceDescriptor [name=" + name + ", description=" + description
            + ", reset_pin=" + reset_pin
            + ", rx_pullup=" + rx_pullup
            + ", mac_address=" + mac_address
            + "]";
    }

}