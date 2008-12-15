package com.limegroup.gnutella.gui;

import junit.framework.Test;

import org.limewire.core.settings.QuestionsHandler;
import org.limewire.util.BaseTestCase;


public class JavaVersionNoticeTest extends BaseTestCase {

    public JavaVersionNoticeTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(JavaVersionNoticeTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testUpgradeRequired() {
        assertTrue(JavaVersionNotice.upgradeRequired("1.4.0"));
        assertFalse(JavaVersionNotice.upgradeRequired("1.5.0"));
        assertFalse(JavaVersionNotice.upgradeRequired(JavaVersionNotice.REQUIRED));
    }

    public void testUpgradeRecommended() {
        assertNull(JavaVersionNotice.getUpgradeRecommendedDialog("1.4.0"));
        assertNull(JavaVersionNotice.getUpgradeRecommendedDialog("1.5.0"));
        assertNull(JavaVersionNotice.getUpgradeRecommendedDialog("1.6.0"));
        assertNotNull(JavaVersionNotice.getUpgradeRecommendedDialog("1.6.0-beta"));
        assertNotNull(JavaVersionNotice.getUpgradeRecommendedDialog("1.6.0-rc"));
        assertNotNull(JavaVersionNotice.getUpgradeRecommendedDialog("1.6.0-beta1"));
        assertNotNull(JavaVersionNotice.getUpgradeRecommendedDialog("1.6.0-rc2"));
    }

    public void testUpgradeRecommendedSetting() {
        String orginalValue = QuestionsHandler.LAST_CHECKED_JAVA_VERSION.getValue();
        try {
            QuestionsHandler.LAST_CHECKED_JAVA_VERSION.setValue("1.4.0");
            assertNotNull(JavaVersionNotice.getUpgradeRecommendedDialog("1.6.0-beta"));
            QuestionsHandler.LAST_CHECKED_JAVA_VERSION.setValue("1.6.0-beta");
            assertNull(JavaVersionNotice.getUpgradeRecommendedDialog("1.6.0-beta"));
            QuestionsHandler.LAST_CHECKED_JAVA_VERSION.setValue("");
            assertNotNull(JavaVersionNotice.getUpgradeRecommendedDialog("1.6.0-beta"));
        } finally {
            QuestionsHandler.LAST_CHECKED_JAVA_VERSION.setValue(orginalValue);
        }
    }

}
