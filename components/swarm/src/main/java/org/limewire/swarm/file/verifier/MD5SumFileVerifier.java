package org.limewire.swarm.file.verifier;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.swarm.SwarmBlockVerifier;
import org.limewire.swarm.SwarmFile;
import org.limewire.swarm.SwarmFileSystem;
import org.limewire.swarm.VerificationException;
import org.limewire.util.FileUtils;

/**
 * This class allows for adding various ranges to check against an MD5Sum.
 * 
 */
public class MD5SumFileVerifier implements SwarmBlockVerifier {

    private Map<Range, String> rangeMD5s = new HashMap<Range, String>();

    public MD5SumFileVerifier() {

    }

    public MD5SumFileVerifier(Range range, String md5String) {
        addMD5Check(range, md5String);
    }

    public void addMD5Check(Range range, String md5String) {
        rangeMD5s.put(range, md5String);
    }

    public List<Range> scanForVerifiableRanges(IntervalSet writtenBlocks, long completeSize) {
        List<Range> ret = new ArrayList<Range>();
        for (Map.Entry<Range, String> entry : rangeMD5s.entrySet()) {
            Range range = entry.getKey();

            if (writtenBlocks.contains(range)) {
                ret.add(range);
            }
        }

        return ret;
    }

    public boolean verify(Range range, SwarmFileSystem swarmFileSystem)
            throws VerificationException {
        SwarmFile swarmFile = swarmFileSystem.getSwarmFile(range.getLow());
        String md5String = rangeMD5s.get(range);

        String testMd5;

        try {
            testMd5 = FileUtils.getMD5(swarmFile.getFile());
        } catch (NoSuchAlgorithmException e) {
            throw new VerificationException(e, range);
        } catch (IOException e) {
            throw new VerificationException(e, range);
        }

        return testMd5.equals(md5String);
    }

}
