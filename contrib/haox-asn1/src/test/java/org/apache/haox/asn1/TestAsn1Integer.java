package org.apache.haox.asn1;

import org.apache.haox.asn1.type.Asn1Integer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class TestAsn1Integer {

    @Test
    public void testEncoding() {
        testEncodingWith(0, "0x02 01 00");
        testEncodingWith(1, "0x02 01 01");
        testEncodingWith(2, "0x02 01 02");
        testEncodingWith(127, "0x02 01 7F");
        testEncodingWith(128, "0x02 02 00 80");
        testEncodingWith(-1, "0x02 01 FF");
        testEncodingWith(-128, "0x02 01 80");
        testEncodingWith(-32768, "0x02 02 80 00");
        testEncodingWith(1234567890, "0x02 04 49 96 02 D2");
    }

    private void testEncodingWith(int value, String expectedEncoding) {
        byte[] expected = Util.hex2bytes(expectedEncoding);
        Asn1Integer aValue = new Asn1Integer(value);
        aValue.setEncodingOption(EncodingOption.DER);
        byte[] encodingBytes = aValue.encode();
        Assert.assertArrayEquals(expected, encodingBytes);
    }

    @Test
    public void testDecoding() throws IOException {
        testDecodingWith(0, "0x02 01 00");
        testDecodingWith(1, "0x02 01 01");
        testDecodingWith(2, "0x02 01 02");
        testDecodingWith(127, "0x02 01 7F");
        testDecodingWith(128, "0x02 02 00 80");
        testDecodingWith(-1, "0x02 01 FF");
        testDecodingWith(-128, "0x02 01 80");
        testDecodingWith(-32768, "0x02 02 80 00");
        testDecodingWith(1234567890, "0x02 04 49 96 02 D2");
    }

    private void testDecodingWith(Integer expectedValue, String content) throws IOException {
        Asn1Integer decoded = new Asn1Integer();
        decoded.setEncodingOption(EncodingOption.DER);
        decoded.decode(Util.hex2bytes(content));
        Assert.assertEquals(expectedValue, decoded.getValue());
    }
}