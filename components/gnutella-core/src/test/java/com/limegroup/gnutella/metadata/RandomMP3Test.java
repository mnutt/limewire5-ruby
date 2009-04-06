package com.limegroup.gnutella.metadata;


import java.io.File;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.util.TestUtils;

import com.limegroup.gnutella.metadata.audio.AudioMetaData;

/**
 * Test MP3Info class
 *
 *
 * @author  cHANCE mOORE, ctmoore@gottapee.com - 30 July 2002
 */
//34567890123456789012345678901234567890123456789012345678901234567890123456789
public class RandomMP3Test extends LimeTestCase {
      
    
    private static final String file[] = new String[] {
    "mpg1layIII_96k-RIFFWAV_441000hz_joint_Xing.wav",
    "mpg1layI_0h_448k_frame14_48000hz_dual_fl5Orig.mp3",
    "mpg1layI_43h_32k_f256_44100hz_dual_50-15emph_CRCcopyOrig_sectest.mp3",
    "mpg1layII_0h_384k_frame24_48000hz_stereo_CRCOrig_Gogo1sectest.mp3",
    "mpg1layIII_212k-VBRq0_f1655_441hz_stereo_FhgTAGID3v1_waterson.mp3",
    "mpg1layIII_0h_58k-VBRq30_frame1211_44100hz_joint_XingTAG_sample.mp3",
    "mpg1layIII_138k-VBRq44_f2895_44100hz_joint_XingTAG_ID3v2_Lame.mp3",
    "mpg1layIII_170k-VBRq66_f1721_441hz_stereo_XingTAGcopyID3v1_mutter.mp3",  
    "mpg1layII_0h_192k_frame44_44100hz_joint_CRCfl11.mp2",
    "mpg2.5layIII_8k-VBRq95_f149_8000khz_mono_ID3v2Orig_XingTAG_Lame.mp3",
    "mpg2layI_0h_128k_frame54_22050hz_joint_CRCOrig_test33.mp3",
    "mpg2layII_1504h_16k_frame56_24000hz_joint_CRCOrigID3v1&2_test27.mp3",
    "mpg2layIII_0h_40k_764or744frame_22050hz_mono_Orig_Fhgl3Frank.mp3",
    "mpg2layIII_12kABRq55_f1202_16hz_stereo_XingLameTAG_CRCOrigwaterson.mp3",
    "mpgPlus_210k-VBR_f1149_441hz_joint_copy__NOTmp1lay_448k_f540_5015.mpc",
    "mpgPRO2layIII_0h_64k_frame2036or2015_22050hz_joint_thomson.mp3",
    "mpg4_golem160x90first120.avi",
    "corruptFileWithBadHeaders.mp3"};

    public RandomMP3Test(String name) {
        super(name);
    }

    /**
     * 
     * @param args java.lang.String[]
     */
    public static void main(String[] args) {        
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return buildTestSuite(RandomMP3Test.class);
    }
    
    private AudioMetaData newMP3Info(String resource) throws Exception {
        MetaDataFactory factory = new MetaDataFactoryImpl();
        File file = TestUtils.getResourceInPackage(resource, RandomMP3Test.class);
        MetaData data = factory.parse(file);
        return (AudioMetaData)data;
    }
    
    /**
     *
     * All values (where possible) have been validated by
     *  Winamp  winamp.com
     *  EncSpot guerillasoft.com
     *  LAME
     *  MusicMatch musicmatch.com
     *          
     */
    public void testMP3() throws Exception {
        
        AudioMetaData info = null;     
        
        //  mpg1layI_0h_448k_frame14_48000hz_dual_fl5Orig.mp3
        info  = newMP3Info(file[1]);
        assertEquals(448, info.getBitrate());    
        assertEquals(0, info.getLength()); 
        assertTrue(!info.isVBR());
        
//          //mpg1layI_32k_f256_441hz_dual_5015emph_CRCcopyOrig_sectest.mp3
//        info  = newMP3Info(file[2]);
//        assertEquals(32, info.getBitrate());   
//        assertEquals(7, info.getLength()); 
//        assertTrue(!info.isVBR());
        
          //mpg1layII_384k_f24_48hz_stereo_CRCOrig_Gogo1sectest.mp3
        info  = newMP3Info(file[3]);
        assertEquals(384, info.getBitrate()); 
        assertEquals(0, info.getLength()); 
        assertTrue(!info.isVBR());

        //mpg1layIII_58k-VBRq30_f1211_441hz_joint_XingTAG_sample.mp3
        info  = newMP3Info(file[5]);
        assertEquals(58, info.getBitrate()); //59-winamp(wrong) 58-encspot 
        assertEquals(31, info.getLength()); 
        assertTrue(info.isVBR());
            
     // TODO: mp2 extension??
//          mpg1layII_0h_192k_frame44_44100hz_joint_CRCfl11.mp2
//        info  = newMP3Info(file[8]);
//        assertEquals(192, info.getBitrate());    
//        assertEquals(1, info.getLength());    
//        assertTrue(!info.isVBR());
    
        //  mpg2.5layIII_8k-VBRq95_f149_8khz_mono_ID3v2Orig_XingTAGLame.mp3
        info  = newMP3Info(file[9]);
        assertEquals(8, info.getBitrate()); //18-winamp 8-encspot/////////////////////////////////////////////
        assertEquals(10, info.getLength()); //5-winamp 10-encspot/////////////////////////////////////
        assertTrue(info.isVBR());
        
        //  mpg2layI_128k_f54_22050hz_joint_CRCOrig_test33.mp3
        info  = newMP3Info(file[10]);
        assertEquals(128, info.getBitrate());     
        assertEquals(1, info.getLength());   
        assertTrue(!info.isVBR());
    
        //  mpg2layII_16k_f56_24000hz_joint_CRCOrigID3v1&2_test27.mp3
        info  = newMP3Info(file[11]);
        assertEquals(16, info.getBitrate()); 
        assertEquals(1, info.getLength()); //1-winamp 43-musicmatch(correct)//////////////////////////
        assertTrue(!info.isVBR());
    
     
    // TODO: mpc extension???
        //  mpgPlus_210k-VBR_f1149_441hz_joint_protectedUnt_NOTm1lay-448k-f540-5015.mpc
//        info  = newMP3Info(file[14]);
//        assertEquals(448, info.getBitrate()); //448-winamp 210-encspot
//        assertEquals(14, info.getLength()); //30-encspot
//        assertTrue(!info.isVBR()); //encspot true */
    
//          mpg4_golem160x90first120.avi
//          MPEG 4 files contain a 'possible' MP3 header
//
//          info  = newMP3Info(file[18] );
//          assertEquals(info.getFileSize(), 743933L);
//          assertEquals(info.getBitrate() , 40);     
//          assertTrue(!info.isCoprighted()); 
//          assertTrue(!info.isOriginal());   
//          assertTrue(!info.isPadded());     
//          assertTrue(info.isPrivate()); 
//          assertTrue(!info.isProtected());  
//          assertTrue(!info.isRiffWav());        
//          assertEquals(info.getFrequency() , 8000); 
//          assertEquals(info.getHeaderBitRate() , 40);
//          assertEquals(info.getLayer_Numeric() , 2);        
//          assertEquals(info.getLength() , 555);    
//          assertEquals(info.getMode() , "Joint Stereo");    
//          assertEquals(info.getVersion_Numeric() , 2.5d, 0D);
//          assertEquals(info.getNumberOfFrames() , 3857); //39626us - 39624 winamp
//          assertEquals(info.getEmphasis() , "none");
//          assertTrue(!info.isVBR());

        //  corruptFileWithBadHeaders.mp3
//        try {
//            info  = newMP3Info(file[19]);
//            fail("Corrupt file with headers passed!"); //shouldn't make it
//        }
//        catch (IOException e) {}
    
    }
}

