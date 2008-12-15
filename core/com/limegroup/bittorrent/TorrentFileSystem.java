package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.limewire.collection.MultiCollection;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SaveLocationException.LocationCode;
import org.limewire.core.settings.SharingSettings;
import org.limewire.util.Base32;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.downloader.serial.TorrentFileSystemMemento;
import com.limegroup.gnutella.downloader.serial.TorrentFileSystemMementoImpl;

/**
 * Information about the file hierarchy contained in the torrent.
 */
public class TorrentFileSystem {

	/* the name of the torrent */
	private final String _name;
	
	/*
	 * the total length of this torrent.
	 */
	private final long _totalSize;
	
	/**
	 * a list of <tt>TorrentFile</tt> for every file in this torrent
	 * LOCKING: this
	 */
	private final List<TorrentFile> _files;
	
	/**
	 * Unmodifiable view over _files to be exposed to the outside world.
	 */
	private final List<TorrentFile>_unmodFiles;
	
	/**
	 * any folders that are contained in the torrent
	 * LOCKING: this
	 */
	private final Collection<File> _folders = new HashSet<File>();
	
	/**
	 * a view of the files and folders contained in this torrent
	 */
	private Collection<File> _filesAndFolders;
	
	/*
	 * A <tt>File</tt> pointing to the location where the incomplete
	 * torrent is written.
	 */
	private final File _incompleteFile;

	/*
	 * A <tt> File </tt> pointing to the file/directory where the completed
	 * torrent will be moved.
	 */
	private File _completeFile;
	
	public TorrentFileSystem(int numHashes, TorrentFileSystemMemento torrentFileSystemMemento) {
        this._name = torrentFileSystemMemento.getName();
        this._totalSize = torrentFileSystemMemento.getTotalSize();
        this._files = torrentFileSystemMemento.getFiles();
        
        //BEGIN TODO remove this code after releasing a few versions Sept 2nd 2008.
        //There was a bug, where the file end piece would be set to numhashes instead of numhashes-1
        //the piece index is zero based. old code used to ignore these propblems, some of the newer 
        //code throws an illegal argument exception.
        for(TorrentFile file : _files) {
            if(file.getEndPiece() == numHashes) {
                file.setEndPiece(numHashes -1);
            }
        }
        //END TODO
        
        this._unmodFiles = Collections.unmodifiableList(_files);
        _folders.addAll(torrentFileSystemMemento.getFolders());
        this._incompleteFile = torrentFileSystemMemento.getIncompleteFile();
        this._completeFile = torrentFileSystemMemento.getCompleteFile();
    }

	
    /**
     * Constructs the file system using the given BTData & hash information.
     * If any of the information is malformed, throws a ValueException.
     * 
     * @param data contains all the data about a .torrent file
     * @param numHashes number of pieces the torrent was divided into
     * @param pieceLength size of divided up torrent file
     * @param infoHash a string of alphanumeric characters in the .torrent file 
     * that the client uses to verify the data that is being transferred
     * @throws ValueException
     */
	TorrentFileSystem(BTData data, int numHashes, long pieceLength, byte [] infoHash) 
	throws IOException {
	    // name of the torrent, also specifying the directory under which to save the torrents.
		_name = CommonUtils.convertFileName(data.getName());

		// we need to check the name of the torrent, security risk!
		if (_name.length() == 0)
			throw new ValueException("bad torrent name");

		_incompleteFile = new File(SharingSettings.INCOMPLETE_DIRECTORY.getValue(), 
				Base32.encode(infoHash)+File.separator+_name);
		_completeFile = new File(SharingSettings.getSaveDirectory(_name), _name);
		
		if (!FileUtils.isReallyParent(SharingSettings.getSaveDirectory(_name), _completeFile)) {
		    throw new SaveLocationException(LocationCode.SECURITY_VIOLATION, _completeFile);
		}
		
        if(data.getFiles() != null) {
            List<BTData.BTFileData> files = data.getFiles();
            List<TorrentFile> torrents = new ArrayList<TorrentFile>(files.size());
            long position = 0;
            for(BTData.BTFileData file : files) {
                String torrentPath = _name + file.getPath();
            	TorrentFile f = new TorrentFile(file.getLength(), new File(_completeFile, file.getPath()).getAbsolutePath(), torrentPath);
            	f.setBeginPiece((int) (position / pieceLength));
                f.setStartByte(position);
                position += f.length();
                f.setEndPiece((int) (position / pieceLength));
                f.setEndByte(position-1);

            	if (!FileUtils.isReallyInParentPath(_completeFile, f))
            		throw new SaveLocationException(LocationCode.SECURITY_VIOLATION, f);
                torrents.add(f);
            }
            
			if (files.size() == 0)
				throw new ValueException("bad metainfo, no files!");
			
			_files = torrents;
            
            // add folders, for easier conflict checking later on
            for(String folderPath : data.getFolders())
                _folders.add(new File(_completeFile, folderPath));
			_folders.add(_completeFile);
		} else {
		    String torrentPath = data.getName();
            TorrentFile f = new TorrentFile(data.getLength(), _completeFile.getAbsolutePath(), torrentPath);
            f.setBeginPiece(0);
            f.setStartByte(0);
            f.setEndPiece(numHashes-1);
            f.setEndByte(f.length());
            _files = new ArrayList<TorrentFile>(1);
            _files.add(f);
		}
        
		_unmodFiles = Collections.unmodifiableList(_files);
		_totalSize = calculateTotalSize(_files);
        if (_totalSize <= 0)
            throw new ValueException("invalid size "+_totalSize);
	}
	
	public TorrentFileSystemMemento toMemento() {
	    TorrentFileSystemMemento memento = new TorrentFileSystemMementoImpl();
	    memento.setCompleteFile(_completeFile);
	    List<TorrentFile> files;
	    Collection<File> folders;
	    synchronized(this) {
	        files = new ArrayList<TorrentFile>(_files);
	        folders = new HashSet<File>(_folders); 
	    }
	    memento.setFiles(files);
	    memento.setFolders(folders);
	    memento.setIncompleteFile(_incompleteFile);
	    memento.setName(_name);
	    memento.setTotalSize(_totalSize);
	    return memento;
	}
	
	/**
	 * Accessor for the total size of this torrent
	 * 
	 * @return long size of the torrent
	 */
	public long getTotalSize() {
		return _totalSize;
	}

	/**
	 * @return the name of this torrent
	 */
	public String getName() {
		return _name;
	}

	/**
	 * @return a list of Files that will be used to write to during download
	 */
	public List<TorrentFile> getIncompleteFiles() {
		List<TorrentFile> ret = new ArrayList<TorrentFile>(_files);
		updateReferences(_incompleteFile,ret);
		return ret;
	}
	
	/**
	 * @return list of files holding the complete torrent.
	 */
	public List<TorrentFile> getFiles() {
		return _unmodFiles;
	}
	
	/**
	 * @return view of the files and folders that will be created when
	 * the torrent download completes
	 */
	public Collection<File> getFilesAndFolders() {
		if (_filesAndFolders == null)
			_filesAndFolders = new MultiCollection<File>(_unmodFiles,Collections.unmodifiableCollection(_folders));
		return _filesAndFolders;
	}
	
	/**
	 * @return true if the given file would conflict with any of the
	 * files that will be used by this torrent when it completes
	 */
	public boolean conflicts(File f) {
		return getFilesAndFolders().contains(f);
	}
	
	/**
	 * @return true if the given file conflicts with any of the files
	 * used by this torrent during download
	 */
	public boolean conflictsIncomplete(File f) {
		return getIncompleteFile().getParentFile().equals(f);
	}

	/**
	 * @return <tt>File</tt> the parent file for all files in this torrent.
	 */
	public File getIncompleteFile() {
		return _incompleteFile;
	}

	/**
	 * @return <tt>File</tt> the parent file for all files in this torrent or
	 *         the only file in this torrent
	 */
	public File getCompleteFile() {
		return _completeFile;
	}
	
	/**
	 * changes the location where this torrent will be saved.
	 */
	public void setCompleteFile(File f) {
		updateReferences(f, _files);
		updateFolderReferences(f);
		_completeFile = f;
	}
	
	/**
	 * Updates the files in the specified collection to start at the 
	 * new base path.
	 * The files must already be absolute and be children of _completeFile.
	 * 
	 * @param completeBase the new base path
	 *
	 */
	private void updateReferences(File completeBase, List<TorrentFile> l) {
		int offset = _completeFile.getAbsolutePath().length();
		String newPath = completeBase.getAbsolutePath();
		for (int i = 0; i < l.size(); i++) {
			TorrentFile current = l.get(i);
			TorrentFile updated = new TorrentFile(current.length(), newPath
					+ current.getPath().substring(offset), current.getTorrentPath());
			updated.setBeginPiece(current.getBeginPiece());
			updated.setEndPiece(current.getEndPiece());
			updated.setStartByte(current.getStartByte());
			updated.setEndByte(current.getEndByte());
			l.set(i,updated);
		}
	}
	
	/**
	 * Updates the folders in the torrent to start at the 
	 * new base path.
	 */
	private synchronized void updateFolderReferences(File completeBase) {
		int offset = _completeFile.getAbsolutePath().length();
		String newPath = completeBase.getAbsolutePath();
		Collection<File> newFolders = new HashSet<File>(_folders.size());
		for (File f: _folders) 
			newFolders.add(new File(newPath + f.getPath().substring(offset)));
		_folders.clear();
		_folders.addAll(newFolders); 
	}
	
	void moveToCompleteFolder() {
		File parent = _incompleteFile.getParentFile();
		boolean success = _incompleteFile.renameTo(_completeFile);
		if (!success) {
			success = FileUtils.copy(_incompleteFile, _completeFile);
			if (success)
				_incompleteFile.delete();
		}
		
		if (success)
			FileUtils.deleteRecursive(parent);
	}
	
	private static long calculateTotalSize(List<TorrentFile> files) {
		long ret = 0;
		for (File f : files)
			ret += f.length();
		return ret;
	}
}
