package org.limewire.core.impl.inspections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.limewire.bittorrent.bencoding.Token;
import org.limewire.io.IOUtils;
import org.limewire.io.InvalidDataException;
import org.limewire.util.BEncoder;

/**
 * helper parser class
 * todo: consider putting the logic here inside the inspections data/spec objects themselves.
 */
public class InspectionsParser {
    
    /**
     * Parse gzipped bencoded byte[] into list of inspections specifications
     * 
     * list
     *    map0
     *      "startdelay" -> integer
     *      "interval"   -> integer
     *      "insp"       -> list of inspection point strings
     *         point1,
     *         point2, point3, ...
     *    map1
     *       "startdelay" -> integer
     *  ...
     *  
     * @param rawInspectionSpecs byte[] raw data
     * @return List<InspectionsSpec> inspection specifications
     * @throws IOException if error occurs during parsing
     */
    // todo: consider putting all the parsing logic in a class that contains a List<InspectionsSpec>
    public List<InspectionsSpec> parseInspectionSpecs(byte[] rawInspectionSpecs) throws IOException,
                                                                                        InvalidDataException {
        ByteArrayInputStream bais = new ByteArrayInputStream(rawInspectionSpecs);
        GZIPInputStream gzip = new GZIPInputStream(bais);
        ReadableByteChannel rbc = Channels.newChannel(gzip);
        List<InspectionsSpec> specs = new ArrayList<InspectionsSpec>();

        List<?> inspSpecs = (List<?>) Token.parse(rbc, "UTF-8");
        for (Object inspSpec : inspSpecs) {
            specs.add(new InspectionsSpec(inspSpec));
        }
        return specs;
    }

    /**
     * 
     * @param insps inspection results
     * @return byte[] bencoded and compressed representation of inspection results
     * @throws IOException if error occurs during conversion
     */
    // todo: revisit whether the parsing logic even belongs in here, or in InspectionDataContainer
    // Same with the List<InspectionsSpec>, consider making an object that contains the List<InspectionsSpec>
    public byte[] inspectionResultToByteArray(InspectionDataContainer insps) throws IOException {

        Map<String, Object> bencodedMap = insps.asBencodedMap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream dos = null;
        try {
            dos = new GZIPOutputStream(baos);
            try {
                BEncoder.getEncoder(dos, false, true, "UTF-8").encodeDict(bencodedMap);
            } catch (Throwable bencoding) {
                // a BEInspectable returned invalid object - report the error.
                String msg = bencoding.toString();
                String ret = "d5:error" + msg.length() + ":" + msg + "e";
                dos.write(ret.getBytes("UTF-8"));
            }
            dos.flush();
        } finally {
            IOUtils.close(dos);
        }
        return baos.toByteArray();
    }
}
