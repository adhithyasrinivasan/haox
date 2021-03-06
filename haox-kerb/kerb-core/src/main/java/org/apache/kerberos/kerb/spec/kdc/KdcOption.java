package org.apache.kerberos.kerb.spec.kdc;

import org.apache.kerberos.kerb.spec.KrbEnum;

public enum KdcOption implements KrbEnum {
    NONE(-1),
    //RESERVED(0x80000000),
    FORWARDABLE(0x40000000),
    FORWARDED(0x20000000),
    PROXIABLE(0x10000000),
    PROXY(0x08000000),
    ALLOW_POSTDATE(0x04000000),
    POSTDATED(0x02000000),
    //UNUSED(0x01000000),
    RENEWABLE(0x00800000),
    //UNUSED(0x00400000),
    //RESERVED(0x00200000),
    //RESERVED(0x00100000),
    //RESERVED(0x00080000),
    //RESERVED(0x00040000),
    CNAME_IN_ADDL_TKT(0x00020000),
    CANONICALIZE(0x00010000),
    REQUEST_ANONYMOUS(0x00008000),
    //RESERVED(0x00004000),
    //RESERVED(0x00002000),
    //RESERVED(0x00001000),
    //RESERVED(0x00000800),
    //RESERVED(0x00000400),
    //RESERVED(0x00000200),
    //RESERVED(0x00000100),
    //RESERVED(0x00000080),
    //RESERVED(0x00000040),
    DISABLE_TRANSITED_CHECK(0x00000020),
    RENEWABLE_OK(0x00000010),
    ENC_TKT_IN_SKEY(0x00000008),
    //UNUSED(0x00000004),
    RENEW(0x00000002),
    VALIDATE(0x00000001);

    private final int value;

    private KdcOption(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return value;
    }

    public static KdcOption fromValue(int value) {
        for (KrbEnum e : values()) {
            if (e.getValue() == value) {
                return (KdcOption) e;
            }
        }

        return NONE;
    }
}
