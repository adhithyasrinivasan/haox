package org.haox.kerb.spec.type.kdc;

import org.haox.kerb.spec.type.common.KrbFlags;

public class KdcOptions extends KrbFlags {

    public KdcOptions() {
        this(0);
    }

    public KdcOptions(int value) {
        setFlags(value);
    }
}