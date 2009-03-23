package org.limewire.core.impl.updates;

import org.limewire.util.BaseTestCase;

public class UpdateInformationImplTest extends BaseTestCase {

    public UpdateInformationImplTest(String name) {
        super(name);
    }

    public void testGetterss() {
        String button1Text = "1";
        String button2Text = "2";
        String commandText = "command";
        String text = "text";
        String title = "title";
        String url = "url";
        
        UpdateInformationImpl updateInformationImpl = new UpdateInformationImpl(button1Text,
                button2Text, commandText, text, title, url);
        assertEquals(button1Text, updateInformationImpl.getButton1Text());
        assertEquals(button2Text, updateInformationImpl.getButton2Text());
        assertEquals(commandText, updateInformationImpl.getUpdateCommand());
        assertEquals(text, updateInformationImpl.getUpdateText());
        assertEquals(title, updateInformationImpl.getUpdateTitle());
        assertEquals(url, updateInformationImpl.getUpdateURL());

        assertNotNull(updateInformationImpl.toString());
    }

}
