package org.limewire.core.impl.inspections;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

import org.limewire.util.BEncoder;
import org.limewire.io.IOUtils;


/**
 * utils class for push inspections
 */
public final class InspectionsTestUtils {
    
    private InspectionsTestUtils() {}
    
    public static byte[] getGzippedAndBencoded(List<InspectionsSpec> specs) throws IOException {
        List<Map<String, Object>> listOfSpecMap = new ArrayList<Map<String, Object>>();
        for (InspectionsSpec spec : specs) {
            Map<String, Object> bencodingMap = spec.asBencodedMap();
            listOfSpecMap.add(bencodingMap);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(baos);
        try {
            BEncoder.getEncoder(gzip, false, true,"UTF-8").encodeList(listOfSpecMap);
            gzip.flush();
        } finally {
            IOUtils.close(gzip);
        }                
        return baos.toByteArray();
    }
    
    private static Map<String, Object> inspectionsSpecToMap(InspectionsSpec spec) {
        Map<String, Object> specMap1 = new HashMap<String, Object>();
        specMap1.put("startdelay", spec.getInitialDelay());
        specMap1.put("interval", spec.getInterval());
        specMap1.put("insp", spec.getInspectionPoints());
        return specMap1;
    }
}
