// Copyright (c) 2010 Espen Wiborg <espenhw@grumblesmurf.org>
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.grumblesmurf.ajp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

final class AjpReader
{
    private AjpReader() {
    }

    static AjpMessage readReplyMessage(InputStream in) throws IOException {
        consume('A', in);
        consume('B', in);
        int length = readInt(in);
        int type = in.read();
        byte[] bytes = new byte[length - 1];
        fullyRead(bytes, in);

        switch (type) {
        case Constants.PACKET_TYPE_CPONG:
            return new CPongMessage();
        case Constants.PACKET_TYPE_SEND_HEADERS:
            return SendHeadersMessage.readFrom(new ByteArrayInputStream(bytes));
        case Constants.PACKET_TYPE_SEND_BODY_CHUNK:
            return SendBodyChunkMessage.readFrom(new ByteArrayInputStream(bytes));
        case Constants.PACKET_TYPE_GET_BODY_CHUNK:
            return GetBodyChunkMessage.readFrom(new ByteArrayInputStream(bytes));
        case Constants.PACKET_TYPE_END_RESPONSE:
            return EndResponseMessage.readFrom(new ByteArrayInputStream(bytes));
        default:
            // FIXME: This just consumes the data
            System.err.printf("Unknown packet type %x%n", type);
            System.err.println(new String(bytes, "ISO-8859-1"));
            return null;
        }
    }

    static int readInt(InputStream in) throws IOException {
        byte[] buf = new byte[2];
        fullyRead(buf, in);
        return makeInt(buf[0], buf[1]);
    }

    static int makeInt(int b1, int b2) {
        return b1 << 8 | (b2 & 0xff);
    }

    static String readString(InputStream in) throws IOException {
        int len = readInt(in);
        return readString(len, in);
    }
    
    static String readString(int len, InputStream in) throws IOException {
        if (len == -1) {
            // XXX:  Will this ever occur?
            return null;
        }
        byte[] buf = new byte[len];
        fullyRead(buf, in);
        // Skip the terminating \0
        in.read();
        
        // XXX: Encoding?
        return new String(buf, "UTF-8");
    }

    static void fullyRead(byte[] buffer, InputStream in) throws IOException {
        int totalRead = 0;
        int read = 0;
        while ((read = in.read(buffer, totalRead, buffer.length - totalRead)) > 0) {
            totalRead += read;
        }
        if (totalRead != buffer.length) {
            throw new ShortAjpReadException(buffer.length, totalRead);
        }
    }

    static int readByte(InputStream in) throws IOException {
        return in.read();
    }
    
    static boolean readBoolean(InputStream in) throws IOException {
        return readByte(in) > 0;
    }
    
    private static void consume(int expected, InputStream in) throws IOException {
        int readByte = readByte(in);
        if (readByte != expected) {
            throw new UnexpectedAjpByteException(expected, readByte);
        }
    }
}
