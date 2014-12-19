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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;

public class AjpCommandLineClient
{
    public static void main(String[] args) throws Exception {
        int timeout = 0;
        if ((args.length >= 2) && (args[0].equals("--timeout")))
        {
          try {
            timeout = Integer.parseInt(args[1]);
            args = ArrayUtils.removeAll(args, 0, 1);
          }
          catch (NumberFormatException e) {
            System.err.println("timeout argument must be an integer");
            System.exit(1);
          }
        }
        if (args.length < 2) {
          System.err.println("Requires at least two arguments");
          System.exit(1);
        }
        String host = args[0];
        int port = 0;  // Only so that "variable port might not have been initialized" error will go away
        try {
          port = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException e) {
          System.err.println("port argument must be an integer");
          System.exit(1);
        }

        try {
					AjpClient ac = AjpClient.newInstance(host, port, timeout);
					if (args.length == 2) {
							System.out.printf("CPing %s:%s: %s%n", host, port, ac.cping() ? "OK" : "NOK");
					} else {
							URL url = new URL(args[2]);
							if (args.length == 5) {
									ac.setAuthentication(args[3], args[4]);
							} else if (args.length == 6) {
									ac.setAuthentication(args[4], args[5]);
							}
							
							AjpResponse resp;
							if (args.length == 3 || args.length == 5)
									resp = ac.get(url);
							else 
									resp = ac.post(url, read(args[3]));
							
							System.out.printf("%s %s%n", resp.getResponseCode(), resp.getResponseMessage());
							for (Map.Entry<String, List<String>> e : resp.getHeaderFields().entrySet()) {
									System.out.printf("%s: %s%n", e.getKey(), e.getValue());
							}
							System.out.println();
							System.out.print(new String(resp.getContent(), "ISO-8859-1"));
					}
					ac.close();
        }
        catch (java.net.SocketTimeoutException e) {
          System.err.println("Timeout occurred");
          System.exit(2);
        }
    }

    private static byte[] read(String filename) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        InputStream in = new FileInputStream(filename);
        byte[] buffer = new byte[2048];
        int read;
        while ((read = in.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }
        in.close();
        
        return bos.toByteArray();
    }
}
