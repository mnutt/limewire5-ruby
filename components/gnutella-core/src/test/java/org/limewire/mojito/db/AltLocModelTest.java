package org.limewire.mojito.db;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.Collection;

import org.limewire.core.settings.DHTSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.inject.AbstractModule;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.dht.db.AltLocModel;
import com.limegroup.gnutella.dht.db.AltLocValueFactory;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.FileManagerTestUtils;
import com.limegroup.gnutella.library.RareFileStrategy;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.HashTreeCache;

public class AltLocModelTest extends LimeTestCase {
    private FileDesc fileDesc;
    private Injector injector;

    public AltLocModelTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        injector = LimeTestUtils.createInjectorAndStart(new AbstractModule() {
            @Override
            protected void configure() {
                bind(RareFileStrategy.class).to(AlwaysRareStrategy.class);
            }
        });
        FileManager fileManager = injector.getInstance(FileManager.class);

        FileManagerTestUtils.waitForLoad(fileManager, 5000);
        
        File testMp3 = TestUtils.getResourceFile("com/limegroup/gnutella/resources/berkeley.mp3");
        fileDesc = fileManager.getGnutellaFileList().add(testMp3).get(1, TimeUnit.SECONDS);
        assertNotNull(fileDesc);
    }

    public void testDontPublish() {
        Injector injector = LimeTestUtils.createInjectorAndStart();
        AltLocModel model = injector.getInstance(AltLocModel.class);
        DHTSettings.PUBLISH_ALT_LOCS.setValue(false);
        assertEquals(0, model.getStorables().size());
    }
    
    public void testGetStorables() throws Exception {
        AltLocModel model = injector.getInstance(AltLocModel.class);
        DHTSettings.PUBLISH_ALT_LOCS.setValue(true);

        Collection<Storable> storables = model.getStorables();
        assertEquals(1, storables.size());
        Storable storable = storables.iterator().next();
        assertEquals(KUIDUtils.toKUID(fileDesc.getSHA1Urn()), storable.getPrimaryKey());
        assertEquals(0, storable.getLocationCount());
        assertEquals(0, storable.getPublishTime());
        DHTValue value = getDHTValue(injector, fileDesc);
        assertEquals(value, storable.getValue());
    }

    private DHTValue getDHTValue(Injector injector, FileDesc fileDesc) {
        long fileSize = fileDesc.getFileSize();
        HashTreeCache tigerTreeCache = injector.getInstance(HashTreeCache.class);
        HashTree hashTree = tigerTreeCache.getHashTree(fileDesc.getSHA1Urn());
        byte[] ttroot = null;
        if (hashTree != null) {
            ttroot = hashTree.getRootHashBytes();
        }
        AltLocValueFactory altLocValueFactory = injector.getInstance(AltLocValueFactory.class);
        return altLocValueFactory.createAltLocValueForSelf(fileSize, ttroot);
    }

    private static class AlwaysRareStrategy implements RareFileStrategy {
        @Override
        public boolean isRareFile(FileDesc fd) {
            return true;
        }
    }
}
