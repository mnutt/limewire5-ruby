package com.limegroup.gnutella.performance;

/**
 * Used to store config options for {@link KeywordIndexTestPerformance}
 *
 */
public class KeywordIndexPerformanceConfig {

    static final int DEFAULT_NUMBER_FILES_INDEXED = 1000;
    static final int DEFAULT_NUMBER_OF_SEARCHES_PER_SEARCH_TERM = 10000;
    static final String[] DEFAULT_SEARCH_TERMS = {"failure swjdfg", "an to do", "an", "ac se fi gu"};
    static final String DEFAULT_SHARED_FILES_DIRECTORY = "/tmp";
    static final String DEFAULT_RAW_DATA_FILENAME = DEFAULT_SHARED_FILES_DIRECTORY + "/rawData.txt";
    static final int DEFAULT_NUMBER_TO_DISCARD_DUE_TO_JIT = 20000;


    private SearchConfig searchConfig;
    private AnalyzeConfig analyzeConfig;


    // TODO: leave open other ways of generating this config - such as thru ant properties, xml (dom nodes), etc


    KeywordIndexPerformanceConfig() {
        this.searchConfig = new SearchConfig();
        searchConfig.setNumberOfFilesIndexed(DEFAULT_NUMBER_FILES_INDEXED);
        searchConfig.setNumberOfSearchesToPerform(DEFAULT_NUMBER_OF_SEARCHES_PER_SEARCH_TERM);
        searchConfig.setRawDataFileName(DEFAULT_RAW_DATA_FILENAME);
        searchConfig.setSearchTerms(DEFAULT_SEARCH_TERMS);
        searchConfig.setSharedFilesDirectory(DEFAULT_SHARED_FILES_DIRECTORY);

        this.analyzeConfig = new AnalyzeConfig();
        analyzeConfig.setNumberOfQueriesToThrowAwayDueToJit(DEFAULT_NUMBER_TO_DISCARD_DUE_TO_JIT);
        analyzeConfig.setRawDataFileName(DEFAULT_RAW_DATA_FILENAME);
    }


    SearchConfig getSearchConfig() {
        return searchConfig;
    }

    AnalyzeConfig getAnalyzeConfig() {
        return analyzeConfig;
    }



    // convenience get and set methods that delegate
    int getNumberOfFilesIndexed() {
        return searchConfig.getNumberOfFilesIndexed();
    }

    void setNumberOfFilesIndexed(int numberOfFilesIndexed) {
        searchConfig.setNumberOfFilesIndexed(numberOfFilesIndexed);
    }


    void setRawDataOutputFileName(String rawDataFileName) {
        // TODO: duplication!  can we get rid of it?
        this.searchConfig.setRawDataFileName(rawDataFileName);
        this.analyzeConfig.setRawDataFileName(rawDataFileName);
    }

    int getNumberOfSearchesToPerform() {
        return searchConfig.getNumberOfSearchesToPerform();
    }

    void setNumberOfSearchesToPerform(int numberOfSearchesToPerform) {
        searchConfig.setNumberOfSearchesToPerform(numberOfSearchesToPerform);
    }

    String[] getSearchTerms() {
        return searchConfig.getSearchTerms();
    }

    void setSearchTerms(String[] searchTerms) {
        searchConfig.setSearchTerms(searchTerms);
    }

    int getNumberOfQueriesToThrowAwayDueToJit() {
        return analyzeConfig.getNumberOfQueriesToThrowAwayDueToJit();
    }

    void setNumberOfQueriesToThrowAwayDueToJit(int numberOfQueriesToThrowAwayDueToJit) {
        analyzeConfig.setNumberOfQueriesToThrowAwayDueToJit(numberOfQueriesToThrowAwayDueToJit);
    }

    String getSharedFilesDirectory() {
        return searchConfig.getSharedFilesDirectory();
    }

    void setSharedFilesDirectory(String sharedFilesDirectory) {
        searchConfig.setSharedFilesDirectory(sharedFilesDirectory);
    }
}


class SearchConfig {

    private int numberOfFilesIndexed;
    private String rawDataFileName;
    private String sharedFilesDirectory;
    private int numberOfSearchesToPerform;
    private String[] searchTerms;

    int getNumberOfFilesIndexed() {
        return numberOfFilesIndexed;
    }

    void setNumberOfFilesIndexed(int numberOfFilesIndexed) {
        this.numberOfFilesIndexed = numberOfFilesIndexed;
    }

    String getRawDataFileName() {
        return rawDataFileName;
    }

    void setRawDataFileName(String rawDataOutputFileName) {
        this.rawDataFileName = rawDataOutputFileName;
    }

    int getNumberOfSearchesToPerform() {
        return numberOfSearchesToPerform;
    }

    void setNumberOfSearchesToPerform(int numberOfSearchesToPerform) {
        this.numberOfSearchesToPerform = numberOfSearchesToPerform;
    }

    String[] getSearchTerms() {
        return searchTerms;
    }

    void setSearchTerms(String[] searchTerms) {
        this.searchTerms = searchTerms;
    }

    String getSharedFilesDirectory() {
        return sharedFilesDirectory;
    }

    void setSharedFilesDirectory(String sharedFilesDirectory) {
        this.sharedFilesDirectory = sharedFilesDirectory;
    }
}

class AnalyzeConfig {

    // performance test will throw away the first X number of query results
    private int numberOfQueriesToThrowAwayDueToJit;
    private String rawDataFileName;

    int getNumberOfQueriesToThrowAwayDueToJit() {
        return numberOfQueriesToThrowAwayDueToJit;
    }

    void setNumberOfQueriesToThrowAwayDueToJit(int numberOfQueriesToThrowAwayDueToJit) {
        this.numberOfQueriesToThrowAwayDueToJit = numberOfQueriesToThrowAwayDueToJit;
    }

    String getRawDataFileName() {
        return rawDataFileName;
    }

    void setRawDataFileName(String rawDataFileName) {
        this.rawDataFileName = rawDataFileName;
    }
}