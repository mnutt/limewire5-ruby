package org.limewire.core.impl.inspections;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

import org.limewire.bittorrent.bencoding.Token;
import org.limewire.io.InvalidDataException;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;


/**
 * inspections communicator encapsulates individual inspection results
 *
 * map
 *    "t" -> 173627467343
 *    "i" -> list
 *              map0
 *                 "k" -> "inspection point1"
 *                 "d" -> Map
 *                           "data point1" -> Map<String, Object>, ... primitive
 *                           "data_point2" -> Map<String, whatever>...
 *                           "data_point3" -> 3
 *
 *              map1
 *                 "k" -> "inspection point2"
 *                 "d" -> Map
 *                           "data pointA" -> Map<String, Object>, ... primitive
 *                           "data_pointB" -> Map<String, whatever>...
 *                           "data_pointC" -> 3
 *              map2 ...
 *
 */
public class InspectionDataContainer {

    private static final Log LOG = LogFactory.getLog(InspectionDataContainer.class);
    
    /**
     * Map of inspection keys to inspection data.
     */
    private final Map<String, Object> inspectionResults = new ConcurrentHashMap<String, Object>();

    /**
     * Timestamp of when the inspection data in this class was collected.
     */
    private final long collectionTimestamp;

    /**
     * Creates with current timestamp, no inspections results
     */
    public InspectionDataContainer() {
        this(System.currentTimeMillis());
    }

    /**
     * Creates with timestamp parameter, no inspections results
     * 
     * @param collectionTimestamp milliseconds since epoch
     */
    public InspectionDataContainer(long collectionTimestamp) {
        this.collectionTimestamp = collectionTimestamp;
    }

    /**
     * Creates this object by decoding wire protocol data
     * (bencoded and gzipped bytes)
     * 
     * @param bencodedGzipBytes BEncoded and gzipped bytes
     * @throws InvalidDataException if input byte[] contains invalid data
     */
    public InspectionDataContainer(byte[] bencodedGzipBytes) throws InvalidDataException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bencodedGzipBytes);
        try {
            GZIPInputStream gzip = new GZIPInputStream(bais);
            ReadableByteChannel rbc = Channels.newChannel(gzip);
            Map<?, ?> containerMap = (Map<?, ?>) Token.parse(rbc, "UTF-8");
            this.collectionTimestamp = (Long)containerMap.get("t");
            
            List<?> listOfInspectionMaps = (List<?>)containerMap.get("i");
            for (Object inspDataContainer : listOfInspectionMaps) {
                Map<?, ?> inspDataContainerMap = (Map<?, ?>)inspDataContainer;
                String inspKey = new String((byte[])inspDataContainerMap.get("k"));            
                Object inspDataMap = inspDataContainerMap.get("d");
                addInspectionResult(inspKey, inspDataMap);
            }
        } catch (IOException e) {
            LOG.debugf(e, "Error in parsing bytes into inspection data");
            throw new InvalidDataException(e);    
        } catch (ClassCastException e) {
            LOG.debugf(e, "Invalid encoding in data");
            throw new InvalidDataException(e);    
        }
    }

    Map<String, Object> asBencodedMap() {
        Map<String, Object> bencodingMap = new HashMap<String, Object>();
        List<Map<String, Object>> inspPointsMapList = new ArrayList<Map<String, Object>>();
        bencodingMap.put("t", collectionTimestamp);
        bencodingMap.put("i", inspPointsMapList);
        for (Map.Entry<String, Object> entry : inspectionResults.entrySet()) {
            Map<String, Object> inspKeyAndData = new HashMap<String, Object>();
            inspKeyAndData.put("k", entry.getKey());
            inspKeyAndData.put("d", entry.getValue());
            inspPointsMapList.add(inspKeyAndData);
        }
        return bencodingMap;
    }

    /**
     * Add inspection result data.
     * 
     * @param key String identifying inspection point.
     * @param inspectionData bencodable inspection data.
     */
    public void addInspectionResult(String key, Object inspectionData) {
        inspectionResults.put(key, inspectionData);
    }
    
    long getTimestamp() {
        return collectionTimestamp;
    }
    
    Object getData(String key) {
        return inspectionResults.get(key);
    }
    
    int getResultCount() {
        return inspectionResults.size();
    }
}
