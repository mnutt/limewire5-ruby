package com.limegroup.gnutella.gui.xml.editor;

import com.limegroup.gnutella.library.FileDesc;

public interface MetaDataEventListener /*extends EventListener<FileManagerEvent> */ {

	void metaDataUnchanged(FileDesc fd);
	
}
