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

import com.maccasoft.propeller.port.ComPortException;

public abstract class PropellerLoader {

    protected PropellerLoaderListener listener;

    public PropellerLoader() {

    }

    public PropellerLoader(PropellerLoaderListener listener) {
        this.listener = listener;
    }

    public void setListener(PropellerLoaderListener listener) {
        this.listener = listener;
    }

    public abstract void upload(byte[] binaryImage, boolean eeprom) throws ComPortException;

}
