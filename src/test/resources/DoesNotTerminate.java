import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Code snippet from a Defects4J bug that caused the analysis to hang.
 */
class DoesNotTerminate {
    Map<String, String> parsePaxHeaders(InputStream i) throws IOException {
        Map<String, String> headers = new HashMap<String, String>();
        // Format is "length keyword=value\n";
        while(true){ // get length
            int ch;
            int len = 0;
            int read = 0;
            while((ch = i.read()) != -1) {
                read++;
                if (ch == ' '){ // End of length string
                    // Get keyword
                    ByteArrayOutputStream coll = new ByteArrayOutputStream();
                    while((ch = i.read()) != -1) {
                        read++;
                        if (ch == '='){ // end of keyword
                            String keyword = coll.toString("UTF_8");
                            // Get rest of entry
                            final int restLen = len - read;
                            byte[] rest = new byte[restLen];
                            int got = 1;
                            if (got != restLen) {
                                throw new IOException("Failed to read "
                                        + "Paxheader. Expected "
                                        + restLen
                                        + " bytes, read "
                                        + got);
                            }
                            // Drop trailing NL
                            String value = new String(rest, 0,
                                    restLen - 1, "UTF_8");
                            headers.put(keyword, value);
                            break;
                        }
                        coll.write((byte) ch);
                    }
                    break; // Processed single header
                }
                len *= 10;
                len += ch - '0';
            }
            if (ch == -1){ // EOF
                break;
            }
        }
        return headers;
    }
}
