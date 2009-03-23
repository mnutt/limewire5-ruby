package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

import org.limewire.inspection.InspectionException;
import org.limewire.inspection.Inspector;
import org.limewire.io.GGEP;
import org.limewire.io.IOUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.BEncoder;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.FECUtils;

@Singleton
public class InspectionResponseFactoryImpl implements InspectionResponseFactory {

    private static final String INSPECTION_FILE = "inspection.props";
    
    private static final int OLD_VERSION = 1;
    private static final int GGEP_VERSION = 2;
    
    public static final float REDUNDANCY = 1.2f;
    
    private final Inspector inspector;
    
    private final FECUtils fecUtils;
    
    @Inject
    public InspectionResponseFactoryImpl(Inspector inspector, FECUtils fecUtils) {
        this.inspector = inspector;
        this.inspector.load(new File(CommonUtils.getCurrentDirectory(),INSPECTION_FILE));
        this.fecUtils = fecUtils;
    }
    
    public InspectionResponse[] createResponses(InspectionRequest request) {
        byte [] payload = derivePayload(request);
        if (payload.length < InspectionResponse.PACKET_SIZE || !request.supportsEncoding())
            return new InspectionResponse[]{new InspectionResponse(OLD_VERSION, request.getGUID(), payload)};
        
        
        // package responses
        List<byte []> chunks = fecUtils.encode(payload, InspectionResponse.PACKET_SIZE, REDUNDANCY); 
        List<InspectionResponse> ret = new ArrayList<InspectionResponse>(chunks.size());
        for (int i = 0; i < chunks.size() ; i++ ) {
            
            GGEP g = new GGEP();
            g.put(InspectionResponse.DATA_KEY,chunks.get(i));
            g.put(InspectionResponse.CHUNK_ID_KEY,i);
            
            // need to be in every packet
            g.put(InspectionResponse.TOTAL_CHUNKS_KEY,chunks.size());
            g.put(InspectionResponse.LENGTH_KEY, payload.length);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                g.write(baos);
            } catch (IOException impossible) {
                continue; // don't disturb the user
            }
            
            ret.add(new InspectionResponse(GGEP_VERSION, request.getGUID(), baos.toByteArray()));
        }
        
        InspectionResponse [] rett = new InspectionResponse[ret.size()];
        return ret.toArray(rett);
    }

    private byte[] derivePayload(InspectionRequest request) {
        /*
         * The format is a deflated bencoded mapping from
         * the indices of the inspected fields to their values.
         */
        String [] requested = request.getRequestedFields();
        Map<Integer, Object> responses = 
            new HashMap<Integer, Object>(requested.length);
        
        // if a timestamp was requested, it is put under the "-1" key.
        if (request.requestsTimeStamp())
            responses.put(-1,System.currentTimeMillis());
        
        for (int i = 0; i < requested.length; i++) {
            try {
                responses.put(i, inspector.inspect(requested[i]));
            } catch (InspectionException skip){}
        }
        
        if (responses.isEmpty())
            return DataUtils.EMPTY_BYTE_ARRAY;
        
        
        // since the inspected values may contain any character, bencoding is safest
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(baos);
        try {
            try {
                BEncoder.getEncoder(dos, false, true,"UTF-8").encodeDict(responses);
            } catch (Throwable bencoding) {
                // a BEInspectable returned invalid object - report the error.
                String msg = bencoding.toString();
                String ret = "d5:error"+msg.length()+":"+msg+"e";
                dos.write(ret.getBytes("UTF-8"));
            }
            dos.flush();
        } catch (IOException impossible) {
            ErrorService.error(impossible);
        } finally {
            IOUtils.close(dos);
        }
        return baos.toByteArray();
    }
}
