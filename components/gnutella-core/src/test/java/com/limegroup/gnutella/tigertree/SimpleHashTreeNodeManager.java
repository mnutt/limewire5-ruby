package com.limegroup.gnutella.tigertree;

import java.util.List;

import com.limegroup.gnutella.security.Tiger;

public class SimpleHashTreeNodeManager implements HashTreeNodeManager {
    
    public List<List<byte[]>> getAllNodes(HashTree tree) {
        return HashTreeUtils.createAllParentNodes(tree.getNodes(), new Tiger());
    }
    
    public void register(HashTree tree, List<List<byte[]>> nodes) {
        
    }

}
