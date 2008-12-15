package com.limegroup.gnutella.gui.xml.editor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.xml.sax.SAXException;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.library.LibraryMediator;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.LimeXMLUtils;
import com.limegroup.gnutella.xml.SchemaNotFoundException;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;

import static com.limegroup.gnutella.xml.LimeXMLReplyCollection.MetaDataState;

public class MetaDataSaver {

	private FileDesc[] fds;
	private String input;
	private LimeXMLSchema schema;

    public MetaDataSaver(FileDesc[] fds, LimeXMLSchema schema, String input) {
    	this.fds = fds;
    	this.schema = schema;
    	this.input = input;
    }

    public void saveMetaData() {
    	saveMetaData(null);
    }
    
    public void saveMetaData(final MetaDataEventListener listener) {
    	if (listener != null) {
//            GuiCoreMediator.getFileManager().addFileEventListener(listener);
        }
        BackgroundExecutorService.schedule(new Runnable() {
            public void run() {
                try {
                    GUIMediator.safeInvokeAndWait(new Runnable() {
                        public void run(){
                            LibraryMediator.instance().setAnnotateEnabled(false);
                        }
                    });
                    saveMetaDataInternal(listener);
                } finally{
                    GUIMediator.safeInvokeAndWait(new Runnable() {
                        public void run() {
                            LibraryMediator.instance().setAnnotateEnabled(true);
                        }
                    });
            		if (listener != null) {
            			Runnable runner = new Runnable() {
            				public void run() {
//            					GuiCoreMediator.getFileManager().removeFileEventListener(listener);
            				}
            			};
            			// FIXME crude work around to improve changes that the listener was actually notified before being removed
            			GuiCoreMediator.getCoreBackgroundExecutor().schedule(runner, 30 * 1000, TimeUnit.MILLISECONDS);
            		}
                }
            }
        });
    }

    private void saveMetaDataInternal(MetaDataEventListener listener) {
    	if (input == null) {
            return;
        } else if (fds.length == 1 && input.trim().length() == 0) {
            removeMeta(fds, schema);
            return;
        }

        LimeXMLDocument newDoc = null;

        try {
            newDoc = GuiCoreMediator.getLimeXMLDocumentFactory().createLimeXMLDocument(input);
        } catch (SAXException e) {
            GUIMediator.showError(I18n.tr("Internal Document Error. Data could not be saved."));
            return;
        } catch (SchemaNotFoundException e) {
            GUIMediator.showError(I18n.tr("Internal Document Error. Data could not be saved."));
            return;
        } catch (IOException e) {
            GUIMediator.showError(I18n.tr("Internal Document Error. Data could not be saved."));
            return;
        }

        //OK we have the new LimeXMLDocument
        SchemaReplyCollectionMapper map = GuiCoreMediator.getSchemaReplyCollectionMapper();
        String schemaURI = newDoc.getSchemaURI();
        LimeXMLReplyCollection collection = map.getReplyCollection(schemaURI);

        //This is a really bad case!
        assert collection != null :
                "Cant add doc to nonexistent collection";

        for(int i = 0; i < fds.length; i++) {            
            LimeXMLDocument oldDoc = fds[i].getXMLDocument(schemaURI);
            LimeXMLDocument result = null;

            if (oldDoc != null) {
                result = MetaEditorUtil.merge(oldDoc, newDoc);
                oldDoc = collection.replaceDoc(fds[i], result);
            } else {
                result = newDoc;
                collection.addReply(fds[i], result);
            }

            if (LimeXMLUtils.isSupportedFormat(fds[i].getFileName())) {
                final MetaDataState committed = collection.mediaFileToDisk(fds[i], result);
                if (committed == MetaDataState.UNCHANGED) {
                	if (listener != null) {
                		listener.metaDataUnchanged(fds[i]);
                	}
                } else if (committed != MetaDataState.NORMAL) {

                    GUIMediator.safeInvokeAndWait(new Runnable() {
                        public void run() {
                            showCommitError(committed);
                        }
                    });
                	
                    // clean up
                	switch (committed) {
                	case FAILED_ARTIST:
                        cleanUpChanges(fds[i], LimeXMLNames.AUDIO_ARTIST, collection, oldDoc);
                        break;
                    case FAILED_ALBUM:
                        cleanUpChanges(fds[i], LimeXMLNames.AUDIO_ALBUM, collection, oldDoc);
                        break;
                    case FAILED_YEAR:
                        cleanUpChanges(fds[i], LimeXMLNames.AUDIO_YEAR, collection, oldDoc);
                        break;
                    case FAILED_COMMENT:
                        cleanUpChanges(fds[i], LimeXMLNames.AUDIO_COMMENTS, collection, oldDoc);
                        break;
                    case FAILED_TRACK:
                        cleanUpChanges(fds[i], LimeXMLNames.AUDIO_TRACK, collection, oldDoc);
                        break;
                    case FAILED_GENRE:
                        cleanUpChanges(fds[i], LimeXMLNames.AUDIO_GENRE, collection, oldDoc);
                        break;
                    }
                }
            } else if (!collection.writeMapToDisk()) {
                GUIMediator.safeInvokeAndWait(new Runnable() {
                    public void run() {
                    	GUIMediator.showError(I18n.tr("Internal Error. Data could not be saved."));
                    }
                });
            }
        }
    }

    private void showCommitError(MetaDataState committed) {
        switch (committed) {
        case UNCHANGED:
            GUIMediator.showMessage(I18n.tr("Nothing to save."));
            break;
        case FILE_DEFECTIVE:
            GUIMediator.showError(I18n.tr("File not found or corrupt file. Data could not be saved"));
            break;
        case RW_ERROR:
            GUIMediator.showError(I18n.tr("Read Write Error. Data could not be saved."));
            break;
        case BAD_ID3:
            GUIMediator.showError(I18n.tr("File Corrupt. Data could not be saved."));
            break;
        case FAILED_TITLE:
            GUIMediator.showError(I18n.tr("File Corrupt. Data could not be saved."));
            break;
        case FAILED_ARTIST:
        case FAILED_ALBUM:
        case FAILED_YEAR:
        case FAILED_COMMENT:
        case FAILED_TRACK:
        case FAILED_GENRE:
            GUIMediator.showError(I18n.tr("Incorrect format entered. Changes will not be saved."));
            break;
        case HASH_FAILED:
            GUIMediator.showError(I18n.tr("Internal Error. Data could not be saved."));
            break;
        }
    }
    
    private void cleanUpChanges(FileDesc fd, String canonicalFieldName,
            LimeXMLReplyCollection collection, LimeXMLDocument oldDoc) {
        if (oldDoc == null) {
        	// it was added....just remove
            collection.removeDoc(fd);
        } else {
            // older one was replaced....replace back
            collection.replaceDoc(fd, oldDoc);
        }
    }

    private void removeMeta(FileDesc[] fds, LimeXMLSchema schema) {

        String uri = schema.getSchemaURI();
        LimeXMLReplyCollection collection = GuiCoreMediator.getSchemaReplyCollectionMapper().getReplyCollection(uri);

        assert collection != null :
                "Trying to remove data from a non-existent collection";

        for(int i = 0; i < fds.length; i++) {
            if (!collection.removeDoc(fds[i])) {// unable to remove or write to disk
                GUIMediator.showError(I18n.tr("Unable to remove data."));
            }
        }
    }
}
