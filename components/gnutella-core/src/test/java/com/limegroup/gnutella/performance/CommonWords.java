package com.limegroup.gnutella.performance;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.limewire.util.TestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Class to get and manage access to a list of common english words.  
 */
public class CommonWords {

    private static final Log LOG = LogFactory.getLog(CommonWords.class);

    private static final String COMMON_WORDS_FILE_NAME = "commonWords.txt";

    private final String[] commonWords;
    private int currentWordIndex;

    public CommonWords() throws IOException {
        commonWords = gatherWords();
        currentWordIndex = 0;
    }

    private static String[] gatherWords() throws IOException {
        String dir = "tests/com/limegroup/gnutella/performance";
        File wordsFile = TestUtils.getResourceFile(dir + "/" + COMMON_WORDS_FILE_NAME);
        BufferedReader reader = new BufferedReader(new FileReader(wordsFile));

        String currentWord;
        List<String> words = new ArrayList<String>();
        while ((currentWord = reader.readLine()) != null) {
            words.add(currentWord);
        }
        return words.toArray(new String[words.size()]);
    }
    

    public static void main(String[] args) throws IOException {
        CommonWords commonWords = new CommonWords();
        String[] words = commonWords.commonWords;
        LOG.info("Most common words in the English language:\n\n");

        for (String word : words) {
            System.out.println(word);
        }
    }

    public String getNextWord() {
        return commonWords[currentWordIndex++ % commonWords.length];
    }

    public String getWordByIndex(int index) {
        return commonWords[index % commonWords.length];
    }

}
