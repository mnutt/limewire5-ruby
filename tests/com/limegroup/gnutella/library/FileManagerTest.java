package com.limegroup.gnutella.library;

import junit.framework.Test;


public class FileManagerTest extends FileManagerTestCase {

//    private static final int MAX_LOCATIONS = 10;

    public FileManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FileManagerTest.class);
    }
//    
//    /**
//     * Tests adding incomplete files to the FileManager.
//     */
//    public void testAddIncompleteFile() throws Exception {
//        VerifyingFileFactory verifyingFileFactory = injector.getInstance(VerifyingFileFactory.class);
//        
//        assertEquals("unexected shared files", 0, fman.getGnutellaSharedFileList().size());
//        assertEquals("unexpected shared incomplete", 0, fman.getIncompleteFileList().size());
//        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());    
//        
//        // add one incomplete file and make sure the numbers go up.
//        Set<URN> urns = new UrnSet();
//        urns.add( UrnHelper.URNS[0] );
//        fman.getIncompleteFileList().addIncompleteFile(new File("a"), urns, "a", 0, verifyingFileFactory.createVerifyingFile(0));
//
//        assertEquals("unexected shared files", 0, fman.getGnutellaSharedFileList().size());
//        assertEquals("unexpected shared incomplete", 1, fman.getIncompleteFileList().size());
//        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
//            
//        // add another incomplete file with the same hash and same
//        // name and make sure it's not added.
//        fman.getIncompleteFileList().addIncompleteFile(new File("a"), urns, "a", 0, verifyingFileFactory.createVerifyingFile(0));
//
//        assertEquals("unexected shared files", 0, fman.getGnutellaSharedFileList().size());
//        assertEquals("unexpected shared incomplete", 1, fman.getIncompleteFileList().size());
//        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
//            
//        // add another incomplete file with another hash, it should be added.
//        urns = new UrnSet();
//        urns.add( UrnHelper.URNS[1] );
//        fman.getIncompleteFileList().addIncompleteFile(new File("c"), urns, "c", 0, verifyingFileFactory.createVerifyingFile(0));
//
//        assertEquals("unexected shared files", 0, fman.getGnutellaSharedFileList().size());
//        assertEquals("unexpected shared incomplete", 2, fman.getIncompleteFileList().size());
//        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
//    }
//    
//    public void testShareIncompleteFile() throws Exception {
//        VerifyingFileFactory verifyingFileFactory = injector.getInstance(VerifyingFileFactory.class);
//        
//        assertEquals("unexected shared files", 0, fman.getGnutellaSharedFileList().size());
//        assertEquals("unexpected shared incomplete", 0, fman.getIncompleteFileList().size());
//        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
//
//        File f;
//        VerifyingFile vf;
//        UrnSet urns;
//        IncompleteFileDesc ifd;
//        Mockery mockery = new Mockery();
//        final QueryRequest qrDesiring = mockery.mock(QueryRequest.class);
//        final QueryRequest notDesiring = mockery.mock(QueryRequest.class);
//        mockery.checking(MessageTestUtils.createDefaultMessageExpectations(qrDesiring, QueryRequest.class));
//        mockery.checking(MessageTestUtils.createDefaultMessageExpectations(notDesiring, QueryRequest.class));
//        mockery.checking(MessageTestUtils.createDefaultQueryExpectations(qrDesiring));
//        mockery.checking(MessageTestUtils.createDefaultQueryExpectations(notDesiring));
//        mockery.checking(new Expectations(){{
//            atLeast(1).of(qrDesiring).getQuery();
//            will(returnValue("asdf"));
//            atLeast(1).of(notDesiring).getQuery();
//            will(returnValue("asdf"));
//            atLeast(1).of(qrDesiring).desiresPartialResults();
//            will(returnValue(true));
//            atLeast(1).of(notDesiring).desiresPartialResults();
//            will(returnValue(false));
//            allowing(qrDesiring).shouldIncludeXMLInResponse();
//            will(returnValue(true));
//        }});
//        
//        // a) single urn, not enough data written -> not shared
//        vf = verifyingFileFactory.createVerifyingFile(1024 * 1024);
//        vf.addInterval(Range.createRange(0,100));
//        assertEquals(101,vf.getBlockSize());
//        urns = new UrnSet();
//        urns.add(UrnHelper.URNS[0]);
//        f = new File("asdf");
//        
//        addIncompleteFile(fman, f, urns, "asdf", 1024 * 1024, vf);
//        ifd = (IncompleteFileDesc) fman.getFileDesc(UrnHelper.URNS[0]);
//        assertFalse(ifd.hasUrnsAndPartialData());
//        assertEquals(0,keywordIndex.query(qrDesiring).length);
//        assertFalse(qrpUpdater.getQRT().contains(qrDesiring));
//        
//        // b) single urn, enough data written -> not shared
//        fman.removeFile(f);
//        vf = verifyingFileFactory.createVerifyingFile(1024 * 1024);
//        vf.addInterval(Range.createRange(0,1024 * 512));
//        assertGreaterThan(102400,vf.getBlockSize());
//        urns = new UrnSet();
//        urns.add(UrnHelper.URNS[0]);
//        f = new File("asdf");
//        
//        addIncompleteFile(fman, f, urns, "asdf", 1024 * 1024, vf);
//        ifd = (IncompleteFileDesc) fman.getFileDesc(UrnHelper.URNS[0]);
//        assertFalse(ifd.hasUrnsAndPartialData());
//        assertEquals(0,keywordIndex.query(qrDesiring).length);
//        assertFalse(qrpUpdater.getQRT().contains(qrDesiring));
//        
//        // c) two urns, not enough data written -> not shared
//        fman.removeFile(f);
//        vf = verifyingFileFactory.createVerifyingFile(1024 * 1024);
//        vf.addInterval(Range.createRange(0,1024 ));
//        assertLessThan(102400,vf.getBlockSize());
//        urns = new UrnSet();
//        urns.add(UrnHelper.URNS[0]);
//        urns.add(UrnHelper.TTROOT);
//        f = new File("asdf");
//        
//        addIncompleteFile(fman, f, urns, "asdf", 1024 * 1024, vf);
//        ifd = (IncompleteFileDesc) fman.getFileDesc(UrnHelper.URNS[0]);
//        assertFalse(ifd.hasUrnsAndPartialData());
//        assertEquals(0,keywordIndex.query(qrDesiring).length);
//        assertFalse(qrpUpdater.getQRT().contains(qrDesiring));
//
//        // d) two urns, enough data written -> shared
//        fman.removeFile(f);
//        vf = verifyingFileFactory.createVerifyingFile(1024 * 1024);
//        vf.addInterval(Range.createRange(0,1024 * 512));
//        assertGreaterThan(102400,vf.getBlockSize());
//        urns = new UrnSet();
//        urns.add(UrnHelper.URNS[0]);
//        urns.add(UrnHelper.TTROOT);
//        assertGreaterThan(1, urns.size());
//        f = new File("asdf");
//        
//        addIncompleteFile(fman, f, urns, "asdf", 1024 * 1024, vf);
//        ifd = (IncompleteFileDesc) fman.getFileDesc(UrnHelper.URNS[0]);
//        assertTrue(ifd.hasUrnsAndPartialData());
//        assertGreaterThan(0,keywordIndex.query(qrDesiring).length);
//        assertEquals(0,keywordIndex.query(notDesiring).length);
//        assertTrue(qrpUpdater.getQRT().contains(qrDesiring));
//        double qrpFull = qrpUpdater.getQRT().getPercentFull();
//        assertGreaterThan(0,qrpFull);
//        
//        // now remove the file and qrt should get updated
//        fman.removeFile(f);
//        assertEquals(0,keywordIndex.query(qrDesiring).length);
//        assertEquals(0,keywordIndex.query(notDesiring).length);
//        assertFalse(qrpUpdater.getQRT().contains(qrDesiring));
//        assertLessThan(qrpFull,qrpUpdater.getQRT().getPercentFull());
//        
//        // e) two urns, enough data written, sharing disabled -> not shared
//        SharingSettings.ALLOW_PARTIAL_SHARING.setValue(false);
//        fman.removeFile(f);
//        vf = verifyingFileFactory.createVerifyingFile(1024 * 1024);
//        vf.addInterval(Range.createRange(0,1024 * 512));
//        assertGreaterThan(102400,vf.getBlockSize());
//        urns = new UrnSet();
//        urns.add(UrnHelper.URNS[0]);
//        urns.add(UrnHelper.TTROOT);
//        assertGreaterThan(1, urns.size());
//        f = new File("asdf");
//        
//        addIncompleteFile(fman, f, urns, "asdf", 1024 * 1024, vf);
//        ifd = (IncompleteFileDesc) fman.getFileDesc(UrnHelper.URNS[0]);
//        assertTrue(ifd.hasUrnsAndPartialData());
//        assertEquals(0,keywordIndex.query(qrDesiring).length);
//        assertEquals(0,keywordIndex.query(notDesiring).length);
//        assertFalse(qrpUpdater.getQRT().contains(qrDesiring));
//        SharingSettings.ALLOW_PARTIAL_SHARING.setValue(true);
//        
//        // f) start with one urn, add a second one -> becomes shared
//        fman.removeFile(f);
//        vf = verifyingFileFactory.createVerifyingFile(1024 * 1024);
//        vf.addInterval(Range.createRange(0,1024 * 512));
//        assertGreaterThan(102400,vf.getBlockSize());
//        urns = new UrnSet();
//        urns.add(UrnHelper.URNS[0]);
//        f = new File("asdf");
//        
//        addIncompleteFile(fman, f, urns, "asdf", 1024 * 1024, vf);
//        ifd = (IncompleteFileDesc) fman.getFileDesc(UrnHelper.URNS[0]);
//        assertFalse(ifd.hasUrnsAndPartialData());
//        assertEquals(0,keywordIndex.query(qrDesiring).length);
//        assertFalse(qrpUpdater.getQRT().contains(qrDesiring));
//        
//        ifd.setTTRoot(UrnHelper.TTROOT);
//        assertTrue(ifd.hasUrnsAndPartialData());
//        assertGreaterThan(0,keywordIndex.query(qrDesiring).length);
//        assertEquals(0,keywordIndex.query(notDesiring).length);
//        assertTrue(qrpUpdater.getQRT().contains(qrDesiring));
//        
//        // g) start with two urns, add data -> becomes shared
//        // actually this is on the one scenario that won't work
//        // because we do not have a callback mechanism for file
//        // verification.  However, given that the default chunks size
//        // we request is 128kb, we're bound to have more data downloaded
//        // by the time we get the tree root.
//        // This will change once we start using roots from replies.
//    }
//    
//    private void addIncompleteFile(FileManager fileManager, final File f, Set<URN> urns, String name,
//            long size, VerifyingFile vf) throws Exception {
//        final CountDownLatch latch = new CountDownLatch(1);
//        fileManager.getIncompleteFileList().addFileListListener(new EventListener<FileListChangedEvent>() {
//            @Override
//            public void handleEvent(FileListChangedEvent event) {
//                switch(event.getType()) {
//                case ADDED:
//                    try {
//                        if(event.getFileDesc().getFile().getCanonicalFile().equals(f.getCanonicalFile())) {
//                            latch.countDown();
//                            event.getList().removeFileListListener(this);
//                        }
//                    } catch(IOException iox) {
//                        throw new RuntimeException(iox);
//                    }
//                    break;
//                }
//            }
//        });
//        fileManager.getIncompleteFileList().addIncompleteFile(f, urns, name, size, vf);
//        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
//    }
//    
//    /**
//     * Tests the removeFileIfShared for incomplete files.
//     */
//    public void testRemovingIncompleteFiles() {
//        VerifyingFileFactory verifyingFileFactory = injector.getInstance(VerifyingFileFactory.class);
//        
//        assertEquals("unexected shared files", 0, fman.getGnutellaSharedFileList().size());
//        assertEquals("unexpected shared incomplete", 0, fman.getIncompleteFileList().size());
//        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
//            
//        Set<URN> urns = new UrnSet();
//        urns.add( UrnHelper.URNS[0] );
//        fman.getIncompleteFileList().addIncompleteFile(new File("a"), urns, "a", 0, verifyingFileFactory.createVerifyingFile(0));
//        urns = new UrnSet();
//        urns.add( UrnHelper.URNS[1] );
//        fman.getIncompleteFileList().addIncompleteFile(new File("b"), urns, "b", 0, verifyingFileFactory.createVerifyingFile(0));
//        assertEquals("unexpected shared incomplete", 2, fman.getIncompleteFileList().size());
//            
//        fman.removeFile( new File("a") );
//        assertEquals("unexpected shared incomplete", 1, fman.getIncompleteFileList().size());
//        
//        fman.removeFile( new File("c") );
//        assertEquals("unexpected shared incomplete", 1, fman.getIncompleteFileList().size());
//        
//        fman.removeFile( new File("b") );
//        assertEquals("unexpected shared incomplete", 0, fman.getIncompleteFileList().size());
//    }
//    
//    /**
//     * Tests that responses are not returned for zero size IncompleteFiles.
//     */
//    public void testQueryRequestsDoNotReturnZeroSizeIncompleteFiles() {
//        VerifyingFileFactory verifyingFileFactory = injector.getInstance(VerifyingFileFactory.class);
//        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
//        
//        assertEquals("unexected shared files", 0, fman.getGnutellaSharedFileList().size());
//        assertEquals("unexpected shared incomplete", 0, fman.getIncompleteFileList().size());
//        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
//            
//        Set<URN> urns = new UrnSet();
//        URN urn = UrnHelper.URNS[0];
//        urns.add( urn );
//        fman.getIncompleteFileList().addIncompleteFile(new File("sambe"), urns, "a", 0, verifyingFileFactory.createVerifyingFile(0));
//        assertEquals("unexpected shared incomplete", 1, fman.getIncompleteFileList().size());            
//            
//        QueryRequest qr = queryRequestFactory.createQuery(urn, "sambe");
//        assertTrue(qr.desiresPartialResults());
//        Response[] hits = keywordIndex.query(qr);
//        assertNotNull(hits);
//        assertEquals("unexpected number of resp.", 0, hits.length);
//    }
//        
//    /**
//     * Tests URN requests on the FileManager.
//     */
//    public void testUrnRequests() throws Exception {
//        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
//        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
//        
//        addFilesToLibrary();
//        for(FileDesc fd : fman.getGnutellaSharedFileList().iterable()) {
//            Response testResponse = responseFactory.createResponse(fd);
//            URN urn = fd.getSHA1Urn();
//            assertEquals("FileDescs should match", fd, 
//                         fman.getFileDesc(urn));
//            // first set does not include any requested types
//            // third includes both
//            Set<URN.Type> requestedUrnSet1 = new HashSet<URN.Type>();
//            Set<URN.Type> requestedUrnSet2 = new HashSet<URN.Type>();
//            Set<URN.Type> requestedUrnSet3 = new HashSet<URN.Type>();
//            requestedUrnSet1.add(URN.Type.ANY_TYPE);
//            requestedUrnSet2.add(URN.Type.SHA1);
//            requestedUrnSet3.add(URN.Type.ANY_TYPE);
//            requestedUrnSet3.add(URN.Type.SHA1);
//            Set[] requestedUrnSets = {URN.Type.NO_TYPE_SET, requestedUrnSet1, 
//                                      requestedUrnSet2, requestedUrnSet3};
//            Set<URN> queryUrnSet = new UrnSet();
//            queryUrnSet.add(urn);
//            for(int j = 0; j < requestedUrnSets.length; j++) { 
//                QueryRequest qr = queryRequestFactory.createQuery(queryUrnSet);
//                Response[] hits = keywordIndex.query(qr);
//                assertEquals("there should only be one response", 1, hits.length);
//                assertEquals("responses should be equal", testResponse, hits[0]);       
//            }
//        }
//    }
//
//    /**
//     * Tests sending request that do not explicitly request any URNs -- traditional
//     * requests -- to make sure that they do return URNs in their responses.
//     */
//    public void testThatUrnsAreReturnedWhenNotRequested() throws Exception {
//        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
//        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
//        
//        addFilesToLibrary();
//        
//        boolean checked = false;
//        for(FileDesc fd : fman.getGnutellaSharedFileList().iterable()) {
//            Response testResponse = responseFactory.createResponse(fd);
//            URN urn = fd.getSHA1Urn();
//            String name = I18NConvert.instance().getNorm(fd.getFileName());
//            
//            char[] illegalChars = SearchSettings.ILLEGAL_CHARS.getValue();
//            Arrays.sort(illegalChars);
//
//            if (name.length() > SearchSettings.MAX_QUERY_LENGTH.getValue()
//                    || StringUtils.containsCharacters(name, illegalChars)) {
//                continue;
//            }
//            
//            QueryRequest qr = queryRequestFactory.createQuery(name);
//            Response[] hits = keywordIndex.query(qr);
//            assertNotNull("didn't get a response for query " + qr, hits);
//            // we can only do this test on 'unique' names, so if we get more than
//            // one response, don't test.
//            if ( hits.length != 1 ) continue;
//            checked = true;
//            assertEquals("responses should be equal", testResponse, hits[0]);
//            Set<URN> urnSet = hits[0].getUrns();
//            URN[] responseUrns = urnSet.toArray(new URN[0]);
//            // this is just a sanity check
//            assertEquals("urns should be equal for " + fd, urn, responseUrns[0]);       
//        }
//        assertTrue("wasn't able to find any unique classes to check against.", checked);
//    }
//    
//    /**
//     * Tests that alternate locations are returned in responses.
//     */
//    public void testThatAlternateLocationsAreReturned() throws Exception {
//        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
//        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
//        AlternateLocationFactory alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
//        AltLocManager altLocManager = injector.getInstance(AltLocManager.class);
//        
//        addFilesToLibrary();
//
//        for(FileDesc fd : fman.getGnutellaSharedFileList().iterable()) {
//            URN urn = fd.getSHA1Urn();
//            for(int j = 0; j < MAX_LOCATIONS + 5; j++) {
//                altLocManager.add(alternateLocationFactory.create("1.2.3." + j, urn), null);
//            }
//        }
//        
//        boolean checked = false;
//        for(FileDesc fd : fman.getGnutellaSharedFileList().iterable()) {
//            Response testResponse = responseFactory.createResponse(fd);
//            String name = I18NConvert.instance().getNorm(fd.getFileName());
//            
//            char[] illegalChars = SearchSettings.ILLEGAL_CHARS.getValue();
//            Arrays.sort(illegalChars);
//
//            if (name.length() > SearchSettings.MAX_QUERY_LENGTH.getValue()
//                    || StringUtils.containsCharacters(name, illegalChars)) {
//                continue;
//            }
//            
//            QueryRequest qr = queryRequestFactory.createQuery(name);
//            Response[] hits = keywordIndex.query(qr);
//            assertNotNull("didn't get a response for query " + qr, hits);
//            // we can only do this test on 'unique' names, so if we get more than
//            // one response, don't test.
//            if ( hits.length != 1 ) continue;
//            checked = true;
//            assertEquals("responses should be equal", testResponse, hits[0]);
//            assertEquals("should have 10 other alts", 10, testResponse.getLocations().size());
//            assertEquals("should have equal alts",
//                testResponse.getLocations(), hits[0].getLocations());
//        }
//        assertTrue("wasn't able to find any unique classes to check against.", checked);
//        altLocManager.purge();
//    }   
//    
//    /**
//     * tests for the QRP thats kept by the FileManager 
//     * tests that the function getQRP of FileManager returns
//     * the correct table after addition, removal, and renaming
//     * of shared files.
//     */
//    public void testFileManagerQRP() throws Exception {
//        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
//        
//        f1 = createNewNamedTestFile(10, "hello");
//        f2 = createNewNamedTestFile(10, "\u5bae\u672c\u6b66\u8535\u69d8");
//        f3 = createNewNamedTestFile(10, "\u00e2cc\u00e8nts");
//        waitForLoad(); 
//
//        //get the QRT from QRTUpdater
//        QueryRouteTable qrt = qrpUpdater.getQRT();
//
//        //test that QRT doesn't contain random keyword
//        QueryRequest qr = queryRequestFactory.createQuery("asdfasdf");
//        assertFalse("query should not be in qrt",
//                   qrt.contains(qr));
//
//        //test that the qrt contains the three files 
//        qr = get_qr(f1);
//        assertTrue("query not in QRT", qrt.contains(qr));
//        
//        qr = get_qr(f2);
//        assertTrue("query not in QRT", qrt.contains(qr));
//
//        qr = get_qr(f3);
//        assertTrue("query not in QRT", qrt.contains(qr));
//
//
//        //now remove one of the files
//        fman.removeFile(f3);
//        
//        qrt = qrpUpdater.getQRT();
//        
//        //make sure the removed file is no longer in qrt
//        assertFalse("query should not be in qrt", qrt.contains(qr));
//        
//        //just check that the one of the other files is still 
//        //in the qrt
//        qr = get_qr(f2);
//        assertTrue("query not in QRT", qrt.contains(qr));
//
//        
//
//        //test rename
//        f4 = createNewNamedTestFile(10, "miyamoto_musashi_desu");
//        
//        //check that this file doesn't hit
//        qr = get_qr(f4);
//        assertFalse("query should not be in qrt", qrt.contains(qr));
//
//        //now rename one of the files
//        FileManagerEvent result = renameFile(f2, f4);
//        assertTrue(result.toString(), result.isRenameEvent());
//        fman.fileRenamed(f2, f4);
//        qrt = qrpUpdater.getQRT();
//        
//        //check hit with new name
//        qr = get_qr(f4);
//        assertTrue("query not in qrt", qrt.contains(qr));
//        
//        //check old name
//        qr = get_qr(f2);
//        assertFalse("query should not be in qrt", qrt.contains(qr));
//    }
//    

//

//    
//    public void testAddSharedFoldersWithBlackList() throws Exception {
//        File[] dirs = LimeTestUtils.createDirs(_sharedDir, 
//                "recursive1",
//                "recursive1/sub1",
//                "recursive1/sub1/subsub",
//                "recursive1/sub2",
//                "recursive2");
//        
//        // create files in all folders, so we can see if they were shared
//        for (int i = 0; i < dirs.length; i++) {
//            createNewNamedTestFile(i + 1, "recshared" + i, dirs[i]);
//        }
//        
//        List<File> whiteList = Arrays.asList(dirs[0], dirs[1], dirs[4]);
//        List<File> blackList = Arrays.asList(dirs[2], dirs[3]);
//        fman.addSharedFolders(new HashSet<File>(whiteList), new HashSet<File>(blackList));
//        waitForLoad();
//        
//        // assert blacklist worked
//        for (File dir : blackList) {
//            assertEquals(0, fman.getGnutellaSharedFileList().getFilesInDirectory(dir).size());
//        }
//        
//        // assert others were shared
//        for (File dir : whiteList) {
//            assertEquals(1, fman.getGnutellaSharedFileList().getFilesInDirectory(dir).size());
//        }
//    }
//    
//
//    public void testSymlinksAreResolvedInBlacklist() throws Exception {
//        if (OSUtils.isWindows()) {
//            return;
//        }
//        
//        File[] dirs = LimeTestUtils.createTmpDirs(
//                "resolvedshare",
//                "resolvedshare/resolvedsub",
//                "resolvedshare/other/shared"
//        );
//        // create files in all folders, so we can see if they were shared
//        for (int i = 0; i < dirs.length; i++) {
//            createNewNamedTestFile(i + 1, "shared" + i, dirs[i]);
//        }
//        File[] pointedTo = LimeTestUtils.createTmpDirs(
//                "notshared",
//                "notshared/sub",
//                "notshared/other/sub"
//        );
//        // create files in all folders, so we can see if they were shared
//        for (int i = 0; i < dirs.length; i++) {
//            createNewNamedTestFile(i + 1, "linkshared" + i, pointedTo[i]);
//        }
//        // add symlinks in shared folders to pointedTo
//        for (int i = 0; i < dirs.length; i++) {
//            createSymLink(dirs[i], "link", pointedTo[i]);
//        }
//        // create blacklist set
//        Set<File> blackListSet = new HashSet<File>();
//        for (File dir : dirs) {
//            blackListSet.add(new File(dir, "link"));
//        }
//        fman.addSharedFolders(Collections.singleton(dirs[0]), blackListSet);
//        waitForLoad();
//        
//        // assert blacklisted were not shared
//        for (File excluded : blackListSet) {
//            assertEquals("excluded was shared: " + excluded, 0, fman.getGnutellaSharedFileList().getFilesInDirectory(excluded).size());
//        }
//        // same for pointed to
//        for (File excluded : pointedTo) {
//            assertEquals(0, fman.getGnutellaSharedFileList().getFilesInDirectory(excluded).size());
//        }
//        // ensure other files were shared
//        for (File shared: dirs) {
//            assertEquals(1, fman.getGnutellaSharedFileList().getFilesInDirectory(shared).size());
//        }
//        
//        // clean up
//        for (File dir : dirs) {
//            cleanFiles(dir, true);
//        }
//        for (File dir : pointedTo) {
//            cleanFiles(dir, true);
//        }
//    }
//    
//    private static void createSymLink(File parentDir, String name, File pointedTo) throws Exception {
//        assertEquals(0, 
//                Runtime.getRuntime().exec(new String[] { 
//                        "ln", 
//                        "-s",
//                        pointedTo.getAbsolutePath(),
//                        parentDir.getAbsolutePath() + File.separator + name
//                }).waitFor());
//    }
//        
//    public void testExplicitlySharedSubfolderUnsharedDoesntStayShared() throws Exception {
//        File[] dirs = LimeTestUtils.createDirs(_sharedDir, 
//                "recursive1",
//                "recursive1/sub1");
//        
//        assertEquals(2, dirs.length);
//        assertEquals("sub1", dirs[1].getName()); // make sure sub1 is second!
//        
//        // create files in all folders, so we can see if they were shared
//        for (int i = 0; i < dirs.length; i++) {
//            createNewNamedTestFile(i + 1, "recshared" + i, dirs[i]);
//        }
//        
//        fman.removeSharedFolder(_sharedDir);
//        fman.addSharedFolder(dirs[1]);
//        fman.addSharedFolder(dirs[0]);
//        
//        waitForLoad();
//        
//        assertEquals(1, fman.getGnutellaSharedFileList().getFilesInDirectory(dirs[0]).size());
//        assertEquals(1, fman.getGnutellaSharedFileList().getFilesInDirectory(dirs[1]).size());
//        
//        // Now unshare sub1
//        fman.removeSharedFolder(dirs[1]);
//        assertEquals(1, fman.getGnutellaSharedFileList().getFilesInDirectory(dirs[0]).size());
//        assertEquals(0, fman.getGnutellaSharedFileList().getFilesInDirectory(dirs[1]).size());
//        
//        // Now reload fman and make sure it's still not shared!
//        FileManagerTestUtils.waitForLoad(fman,10000);
//
//        assertEquals(1, fman.getGnutellaSharedFileList().getFilesInDirectory(dirs[0]).size());
//        assertEquals(0, fman.getGnutellaSharedFileList().getFilesInDirectory(dirs[1]).size());
//    }
//    
//    public void testExplicitlySharedSubSubfolderUnsharedDoesntStayShared() throws Exception {
//        File[] dirs = LimeTestUtils.createDirs(_sharedDir, 
//                "recursive1",
//                "recursive1/sub1",
//                "recursive1/sub1/sub2");
//        
//        assertEquals(3, dirs.length);
//        assertEquals("sub2", dirs[2].getName()); // make sure sub2 is third!
//        
//        // create files in all folders, so we can see if they were shared
//        for (int i = 0; i < dirs.length; i++) {
//            createNewNamedTestFile(i + 1, "recshared" + i, dirs[i]);
//        }
//        
//        fman.removeSharedFolder(_sharedDir);
//        fman.addSharedFolder(dirs[2]);
//        fman.addSharedFolder(dirs[0]);
//        
//        waitForLoad();
//        
//        assertEquals(1, fman.getGnutellaSharedFileList().getFilesInDirectory(dirs[0]).size());
//        assertEquals(1, fman.getGnutellaSharedFileList().getFilesInDirectory(dirs[1]).size());
//        assertEquals(1, fman.getGnutellaSharedFileList().getFilesInDirectory(dirs[2]).size());
//        
//        // Now unshare sub2
//        fman.removeSharedFolder(dirs[2]);
//        assertEquals(1, fman.getGnutellaSharedFileList().getFilesInDirectory(dirs[0]).size());
//        assertEquals(1, fman.getGnutellaSharedFileList().getFilesInDirectory(dirs[1]).size());
//        assertEquals(0, fman.getGnutellaSharedFileList().getFilesInDirectory(dirs[2]).size());
//        assertFalse(fman.isFolderShared(dirs[2]));
//        
//        // Now reload fman and make sure it's still not shared!
//        FileManagerTestUtils.waitForLoad(fman, 10000);
//
//        assertEquals(1, fman.getGnutellaSharedFileList().getFilesInDirectory(dirs[0]).size());
//        assertEquals(1, fman.getGnutellaSharedFileList().getFilesInDirectory(dirs[1]).size());
//        assertEquals(0, fman.getGnutellaSharedFileList().getFilesInDirectory(dirs[2]).size());
//    }
//
//    

//    
//    public void testIsSharableFolder() throws Exception {
//        //  check that system roots are sensitive directories
//        File[] faRoots = File.listRoots();
//        if(faRoots != null && faRoots.length > 0) {
//            for(int i = 0; i < faRoots.length; i++) {
//                assertFalse("root directory "+faRoots[i]+ " should not be sharable", 
//                           fman.isFolderShareable(faRoots[i], false));
//                assertFalse("root directory "+faRoots[i]+ " should not be sharable", 
//                        fman.isFolderShareable(faRoots[i], true));
//            }
//        }
//    }
//    
//    
//    public void testMetaQueriesWithConflictingMatches() throws Exception {
//        waitForLoad();
//        
//        // test a query where the filename is meaningless but XML matches.
//        File f1 = createNewNamedTestFile(10, "meaningless");
//        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(FileManagerTestUtils.buildAudioXMLString(
//            "artist=\"Sammy B\" album=\"Jazz in G\""));
//        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>(); 
//        l1.add(d1);
//        FileManagerEvent result = addIfShared(f1, l1);
//        assertTrue(result.toString(), result.isAddEvent());
//        assertEquals(d1, result.getNewFileDesc().getLimeXMLDocuments().get(0));
//        
//        Response[] r1 = keywordIndex.query(queryRequestFactory.createQuery("sam",
//                FileManagerTestUtils.buildAudioXMLString("artist=\"sam\"")));
//        assertNotNull(r1);
//        assertEquals(1, r1.length);
//        assertEquals(d1.getXMLString(), r1[0].getDocument().getXMLString());
//        
//        // test a match where 50% matches -- should get no matches.
//        Response[] r2 = keywordIndex.query(queryRequestFactory.createQuery("sam jazz in c",
//                FileManagerTestUtils.buildAudioXMLString("artist=\"sam\" album=\"jazz in c\"")));
//        assertNotNull(r2);
//        assertEquals(0, r2.length);
//            
//            
//        // test where the keyword matches only.
//        Response[] r3 = keywordIndex.query(queryRequestFactory.createQuery("meaningles"));
//        assertNotNull(r3);
//        assertEquals(1, r3.length);
//        assertEquals(d1.getXMLString(), r3[0].getDocument().getXMLString());
//                                  
//        // test where keyword matches, but xml doesn't.
//        Response[] r4 = keywordIndex.query(queryRequestFactory.createQuery("meaningles",
//                FileManagerTestUtils.buildAudioXMLString("artist=\"bob\"")));
//        assertNotNull(r4);
//        assertEquals(0, r4.length);
//            
//        // more ambiguous tests -- a pure keyword search for "jazz in d"
//        // will work, but a keyword search that included XML will fail for
//        // the same.
//        File f2 = createNewNamedTestFile(10, "jazz in d");
//        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(FileManagerTestUtils.buildAudioXMLString(
//            "album=\"jazz in e\""));
//        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>(); l2.add(d2);
//        result = addIfShared(f2, l2);
//        assertTrue(result.toString(), result.isAddEvent());
//        assertEquals(d2, result.getNewFileDesc().getLimeXMLDocuments().get(0));
//        
//        // pure keyword.
//        Response[] r5 = keywordIndex.query(queryRequestFactory.createQuery("jazz in d"));
//        assertNotNull(r5);
//        assertEquals(1, r5.length);
//        assertEquals(d2.getXMLString(), r5[0].getDocument().getXMLString());
//        
//        // keyword, but has XML to check more efficiently.
//        Response[] r6 = keywordIndex.query(queryRequestFactory.createQuery("jazz in d",
//                FileManagerTestUtils.buildAudioXMLString("album=\"jazz in d\"")));
//        assertNotNull(r6);
//        assertEquals(0, r6.length);
//                            
//        
//                                   
//    }
//    
//    public void testMetaQueriesStoreFiles() throws Exception{
//    
//        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
//        
//        waitForLoad();
//        
//        // create a store audio file with limexmldocument preventing sharing
//        File f1 = createNewNamedTestFile(12, "small town hero");
//        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(FileManagerTestUtils.buildAudioXMLString(storeAudio));
//        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
//        l1.add(d1);
//        FileManagerEvent result = addIfShared(f1, l1);
//        assertTrue(result.toString(), result.isAddEvent());
//        assertEquals(d1, result.getNewFileDesc().getLimeXMLDocuments().get(0));
//        
//        //create a query with just a file name match, should get no responses
//        Response[] r0 = keywordIndex.query(queryRequestFactory.createQuery("small town hero"));
//        assertNotNull(r0);
//        assertEquals(0, r0.length);
//        
//        // create a query where keyword matches and partial xml matches, should get no
//        // responses
//        Response[] r1 = keywordIndex.query(queryRequestFactory.createQuery("small town hero",
//                FileManagerTestUtils.buildAudioXMLString("title=\"Alive\"")));    
//        assertNotNull(r1);
//        assertEquals(0, r1.length);
//        
//        // test 100% matches, should get no results
//        Response[] r2 = keywordIndex.query(queryRequestFactory.createQuery("small town hero",
//                FileManagerTestUtils.buildAudioXMLString(storeAudio)));
//        assertNotNull(r2);
//        assertEquals(0, r2.length);
//        
//        // test xml matches 100% but keyword doesn't, should get no matches
//        Response[] r3 = keywordIndex.query(queryRequestFactory.createQuery("meaningless",
//                FileManagerTestUtils.buildAudioXMLString(storeAudio)));
//        assertNotNull(r3);
//        assertEquals(0, r3.length);
//        
//        //test where nothing matches, should get no results
//        Response[] r4 = keywordIndex.query(queryRequestFactory.createQuery("meaningless",
//                FileManagerTestUtils.buildAudioXMLString("title=\"some title\" artist=\"unknown artist\" album=\"this album name\" genre=\"Classical\"")));
//        assertNotNull(r4);
//        assertEquals(0, r4.length);
//        
//        
//        // create a store audio file with xmldocument preventing sharing with video xml attached also
//        File f2 = createNewNamedTestFile(12, "small town hero 2");
//        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(FileManagerTestUtils.buildAudioXMLString(storeAudio));
//        LimeXMLDocument d3 = limeXMLDocumentFactory.createLimeXMLDocument(FileManagerTestUtils.buildVideoXMLString("director=\"francis loopola\" title=\"Alive\""));
//        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
//        l2.add(d3);
//        l2.add(d2);
//        FileManagerEvent result2 = addIfShared(f2, l2);
//        assertTrue(result2.toString(), result2.isAddEvent());
//            
//          //create a query with just a file name match, should get no responses
//        Response[] r5 = keywordIndex.query(queryRequestFactory.createQuery("small town hero 2"));
//        assertNotNull(r5);
//        assertEquals(0, r5.length);
//        
//        // query with videoxml matching. This SHOULDNT return results. The new Meta-data parsing
//        //  is fixed to disallow adding new XML docs to files
//        Response[] r6 = keywordIndex.query(queryRequestFactory.createQuery("small town hero 2",
//                FileManagerTestUtils.buildVideoXMLString("director=\"francis loopola\" title=\"Alive\"")));
//        assertNotNull(r6);
//        assertEquals(0, r6.length);
//        
//        // query with videoxml partial matching. This in SHOULDNT return results. The new Meta-data parsing
//        //  is fixed to disallow adding new XML docs to files, this in theory shouldn't be 
//        //  possible
//        Response[] r7 = keywordIndex.query(queryRequestFactory.createQuery("small town hero 2",
//                FileManagerTestUtils.buildVideoXMLString("title=\"Alive\"")));
//        assertNotNull(r7);
//        assertEquals(0, r7.length);
//        
//        // test 100% matches minus VideoXxml, should get no results
//        Response[] r8 = keywordIndex.query(queryRequestFactory.createQuery("small town hero 2",
//                FileManagerTestUtils.buildAudioXMLString(storeAudio)));
//        assertNotNull(r8);
//        assertEquals(0, r8.length);
//        
//        fman.removeFile(f2);
//    }
//
//    public void testMetaQRT() throws Exception {
//        String dir2 = "director=\"francis loopola\"";
//
//        File f1 = createNewNamedTestFile(10, "hello");
//        QueryRouteTable qrt = qrpUpdater.getQRT();
//        assertFalse("should not be in QRT", qrt.contains(get_qr(f1)));
//        waitForLoad();
//        
//        //make sure QRP contains the file f1
//        qrt = qrpUpdater.getQRT();
//        assertTrue("expected in QRT", qrt.contains(get_qr(f1)));
//
//        //now test xml metadata in the QRT
//        File f2 = createNewNamedTestFile(11, "metadatafile2");
//        LimeXMLDocument newDoc2 = limeXMLDocumentFactory.createLimeXMLDocument(FileManagerTestUtils.buildVideoXMLString(dir2));
//        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
//        l2.add(newDoc2);
//        
//        FileManagerEvent result = addIfShared(f2, l2);
//        assertTrue(result.toString(), result.isAddEvent());
//        assertEquals(newDoc2, result.getNewFileDesc().getLimeXMLDocuments().get(0));
//        qrt = qrpUpdater.getQRT();
//        
//        assertTrue("expected in QRT", qrt.contains (get_qr(FileManagerTestUtils.buildVideoXMLString(dir2))));
//        assertFalse("should not be in QRT", qrt.contains(get_qr(FileManagerTestUtils.buildVideoXMLString("director=\"sasami juzo\""))));
//        
//        //now remove the file and make sure the xml gets deleted.
//        fman.removeFile(f2);
//        qrt = qrpUpdater.getQRT();
//       
//        assertFalse("should not be in QRT", qrt.contains(get_qr(FileManagerTestUtils.buildVideoXMLString(dir2))));
//    }
//    
//    public void testMetaQRTStoreFiles() throws Exception {
//        
//        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
//        
//        // share a file
//        File f1 = createNewNamedTestFile(10, "hello");
//        QueryRouteTable qrt = qrpUpdater.getQRT();
//        assertFalse("should not be in QRT", qrt.contains(get_qr(f1)));
//        waitForLoad();
//        
//        //make sure QRP contains the file f1
//        qrt = qrpUpdater.getQRT();
//        assertTrue("expected in QRT", qrt.contains(get_qr(f1)));
//        
//        // create a store audio file with xml preventing sharing
//        File f2 = createNewNamedTestFile(12, "small town hero");
//        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(FileManagerTestUtils.buildAudioXMLString(storeAudio));
//        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
//        l1.add(d1);
//        
//        FileManagerEvent result = addIfShared(f2, l1);
//        assertTrue(result.toString(), result.isAddEvent());
//        assertEquals(d1, result.getNewFileDesc().getLimeXMLDocuments().get(0));
//        qrt = qrpUpdater.getQRT();
//        
//        assertFalse("should not be in QRT", qrt.contains (get_qr(FileManagerTestUtils.buildAudioXMLString(storeAudio))));
//   
//        waitForLoad();
//   
//        //store file should not be in QRT table
//        qrt = qrpUpdater.getQRT();
//        assertFalse("should not be in QRT", qrt.contains (get_qr(FileManagerTestUtils.buildAudioXMLString(storeAudio))));
//        assertTrue("expected in QRT", qrt.contains(get_qr(f1)));
//    }
//
//    public void testMetaQueries() throws Exception {
//        waitForLoad();
//        String dir1 = "director=\"loopola\"";
//
//        //make sure there's nothing with this xml query
//        Response[] res = keywordIndex.query(queryRequestFactory.createQuery("", FileManagerTestUtils.buildVideoXMLString(dir1)));
//        
//        assertEquals("there should be no matches", 0, res.length);
//        
//        File f1 = createNewNamedTestFile(10, "test_this");
//        
//        LimeXMLDocument newDoc1 = limeXMLDocumentFactory.createLimeXMLDocument(FileManagerTestUtils.buildVideoXMLString(dir1));
//        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
//        l1.add(newDoc1);
//
//
//        String dir2 = "director=\"\u5bae\u672c\u6b66\u8535\u69d8\"";
//        File f2 = createNewNamedTestFile(11, "hmm");
//
//        LimeXMLDocument newDoc2 = limeXMLDocumentFactory.createLimeXMLDocument(FileManagerTestUtils.buildVideoXMLString(dir2));
//        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
//        l2.add(newDoc2);
//
//        
//        String dir3 = "director=\"\u5bae\u672c\u6b66\u8535\u69d8\"";
//        File f3 = createNewNamedTestFile(12, "testtesttest");
//        
//        LimeXMLDocument newDoc3 = 
//            limeXMLDocumentFactory.createLimeXMLDocument(FileManagerTestUtils.buildVideoXMLString(dir3));
//        List<LimeXMLDocument> l3 = new ArrayList<LimeXMLDocument>();
//        l3.add(newDoc3);
//        
//        //add files and check they are returned as responses
//        FileManagerEvent result = addIfShared(f1, l1);
//        assertTrue(result.toString(), result.isAddEvent());
//        assertEquals(newDoc1, result.getNewFileDesc().getLimeXMLDocuments().get(0));
//        
//        result = addIfShared(f2, l2);
//        assertTrue(result.toString(), result.isAddEvent());
//        assertEquals(newDoc2, result.getNewFileDesc().getLimeXMLDocuments().get(0));
//        
//        result = addIfShared(f3, l3);
//        assertTrue(result.toString(), result.isAddEvent());
//        assertEquals(newDoc3, result.getNewFileDesc().getLimeXMLDocuments().get(0));
//        Thread.sleep(100);
//        res = keywordIndex.query(queryRequestFactory.createQuery("", FileManagerTestUtils.buildVideoXMLString(dir1)));
//        assertEquals("there should be one match", 1, res.length);
//
//        res = keywordIndex.query(queryRequestFactory.createQuery("", FileManagerTestUtils.buildVideoXMLString(dir2)));
//        assertEquals("there should be two matches", 2, res.length);
//        
//        //remove a file
//        fman.removeFile(f1);
//
//        res = keywordIndex.query(queryRequestFactory.createQuery("", FileManagerTestUtils.buildVideoXMLString(dir1)));
//        assertEquals("there should be no matches", 0, res.length);
//        
//        //make sure the two other files are there
//        res = keywordIndex.query(queryRequestFactory.createQuery("", FileManagerTestUtils.buildVideoXMLString(dir2)));
//        assertEquals("there should be two matches", 2, res.length);
//
//        //remove another and check we still have on left
//        fman.removeFile(f2);
//        res = keywordIndex.query(queryRequestFactory.createQuery("",FileManagerTestUtils.buildVideoXMLString(dir3)));
//        assertEquals("there should be one match", 1, res.length);
//
//        //remove the last file and make sure we get no replies
//        fman.removeFile(f3);
//        res = keywordIndex.query(queryRequestFactory.createQuery("", FileManagerTestUtils.buildVideoXMLString(dir3)));
//        assertEquals("there should be no matches", 0, res.length);
//    }
//    
//    /**
//     * Helper function to set the operating system so that multiple OSs can be partially-checked
//     * by testing on one platform.
//     */
//    private static void setOSName(String name) throws Exception {
//        System.setProperty("os.name", name);
//        PrivilegedAccessor.invokeMethod(OSUtils.class, "setOperatingSystems");
//    }
//
//
//

//    
//
//    
//    protected LibraryData getLibraryData() throws Exception {
//        return (LibraryData)PrivilegedAccessor.getValue(fman, "_data");
//    }
//

//
//    private static class MultiListener implements EventListener<FileManagerEvent> {
//        private List<FileManagerEvent> evtList = new ArrayList<FileManagerEvent>();
//        public synchronized void handleEvent(FileManagerEvent fme) {
//            evtList.add(fme);
//        }
//
//        public synchronized List<FileManagerEvent> getFileManagerEventList() {
//            return evtList;
//        }
//    }
}