package com.limegroup.bittorrent.disk;


import java.io.IOException;
import java.io.InterruptedIOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.AndView;
import org.limewire.collection.BitField;
import org.limewire.collection.BitFieldSet;
import org.limewire.collection.BitSet;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.NECallable;
import org.limewire.collection.NotView;
import org.limewire.collection.OrView;
import org.limewire.collection.RRProcessingQueue;
import org.limewire.io.DiskException;
import org.limewire.service.ErrorService;
import org.limewire.util.SystemUtils;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.BTPiece;
import com.limegroup.bittorrent.PieceReadListener;
import com.limegroup.bittorrent.TorrentContext;
import com.limegroup.bittorrent.TorrentFile;
import com.limegroup.bittorrent.TorrentFileSystem;
import com.limegroup.bittorrent.handshaking.piecestrategy.EndGamePieceStrategy;
import com.limegroup.bittorrent.handshaking.piecestrategy.PartialPieceStrategy;
import com.limegroup.bittorrent.handshaking.piecestrategy.PieceStrategy;
import com.limegroup.bittorrent.handshaking.piecestrategy.RandomPieceStrategy;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.serial.BTDiskManagerMemento;
import com.limegroup.gnutella.downloader.serial.BTDiskManagerMementoImpl;

import org.limewire.core.settings.BittorrentSettings;
import org.limewire.core.settings.SharingSettings;

/**
 * Verifies the Pieces of the torrent to the .torrent meta information 
 * and writes the files to disk.
 */
class VerifyingFolder implements TorrentDiskManager {
	
	private static final Log LOG = LogFactory.getLog(VerifyingFolder.class);
	
	/**
	 * The thread that does the reading, writing and
	 * on-the-fly verification 
	 */
	private static final RRProcessingQueue QUEUE = new RRProcessingQueue(
			"TorrentDiskQueue");
	
	/**
	 * Queue for verifying torrents that already exist on the hard
	 * disk.
	 */
	private static final RRProcessingQueue VERIFY_QUEUE = 
		new RRProcessingQueue("TorrentVerifier");
	
	
	/**
	 * This is the max size of a block that we will ever request. Requesting
	 * larger ranges is not encouraged by the protocol.
	 */
	private static final int BLOCK_SIZE = 16384;
	
	/*
	 * The files of this torrent as an array
	 */
	private final List<TorrentFile> _files;

	/**
	 * Mapping of the index of each partial block and the written ranges within 
	 */
	private BlockRangeMap partialBlocks;
	
	/**
	 * Mapping of the index of each requested block and the requested ranges within 
	 */
	private BlockRangeMap requestedRanges;
	
	/**
	 * Mapping of the index of each block pending write and 
	 * pending ranges within 
	 */
	private BlockRangeMap pendingRanges;
	
	/**
	 * A BitSet for all blocks that are verified.
	 */
	private BitSet verifiedBlocks;
	
	private BitField missing, verified;
	
	/** a cached bitfield. LOCKING: this*/
	private byte [] bitField;
	/** whether the cached bitfield is dirty LOCKING: this */
	private boolean bitFieldDirty = true;

	/**
	 * the number of bytes that failed verification, either
	 * during download or initial checking.
	 */
	private long _corruptedBytes;

	private final TorrentContext context;
	
	/**
	 * an exception indicating Disk operation failed.
	 * a nice feature would be to have a separate
	 * exception for each file so that multi-file torrents would 
	 * not fail if an operation on one of the files throws.
	 */
	private volatile IOException storedException;
	
	/**
	 * The <tt>DiskManagerListener</tt> to notify.
	 */
	private volatile DiskManagerListener listener;
	
	/** Whether the files on disk are currently being verified */
	private volatile boolean isVerifying;
	
	/**
	 * Disk controller for performing the reads and writes.
	 */
	private final DiskController<TorrentFile> diskController;
    
    /** 
     * the last continuous offset in the torrent file that has been verified.
     */
    private volatile long lastVerifiedOffset;

	/**
	 * constructs instance of this
	 * 
	 * @param complete if the download is completed
	 */
	VerifyingFolder(TorrentContext context, 
			boolean complete, 
            BTDiskManagerMemento data,
            DiskController<TorrentFile> diskController) {
        TorrentFileSystem system = context.getFileSystem();
        _files = complete? system.getFiles() : system.getIncompleteFiles();
        this.context = context;
        _corruptedBytes = 0;
        int numBlocks = context.getMetaInfo().getNumBlocks();
        partialBlocks = new BlockRangeMap(numBlocks);
        requestedRanges = new BlockRangeMap(numBlocks);
        pendingRanges = new BlockRangeMap(numBlocks);
        this.diskController = diskController;
                
        if (complete) {
            verifiedBlocks = context.getFullBitSet();
            verified = context.getFullBitField();
        } else {
            verifiedBlocks = new BitSet(context.getMetaInfo().getNumBlocks());
            if (data != null)
                initialize(data);
            verified = new BitFieldSet(verifiedBlocks, context.getMetaInfo().getNumBlocks());
        }
        
        missing = new NotView(verified);
    }
    
    /**
     * populates various fields from data object that was 
     * deserialized.
     */
    private void initialize(BTDiskManagerMemento data) {
        if (data.getPartialBlocks() != null) 
            partialBlocks.putAll(data.getPartialBlocks());
        
        if (data.getVerifiedBlocks() != null) 
            verifiedBlocks = data.getVerifiedBlocks();
        
        isVerifying = data.isVerifying();
    }
    
    
    public void writeBlock(NECallable<BTPiece> factory) {
        if (storedException != null)
            return;
        QUEUE.execute(new WriteJob(factory),context.getMetaInfo().getURN());
    }
    
    /**
     * This duplicates lots of code from VerifyingFile..
     * the two should eventually be abstracted somehow.
     */
    private class WriteJob implements Runnable {
        private final NECallable<BTPiece> factory;
        
        WriteJob(NECallable<BTPiece> factory) {
            this.factory = factory;
        }
        
        public void run() {
            if (storedException != null)
                return;
            
            BTPiece piece = factory.call();
            
            BTInterval in = piece.getInterval();
            if (in.getId() >=  verified.maxSize()) 
                return; // bad piece.
            byte [] data = piece.getData();
            
            synchronized(VerifyingFolder.this) {
                if (hasBlock(in.getId()))
                    return;
                pendingRanges.addInterval(in);
                requestedRanges.removeInterval(in);
            }
            
            try {
                writeBlockImpl(in, data);
            } catch (IOException iox) {
                if (isOpen()) {
                    storedException = iox;
                    notifyDiskProblem(iox);
                }
            } finally {
                synchronized(VerifyingFolder.this) {
                    pendingRanges.removeInterval(in);
                }
            }
        }
    }
    /**
     * Writes bytes to the underlying files.
     * 
     * @param in the BTInterval that is to be written
     * @param buf
     *            an array of byte containing the bytes to write
     */
    private void writeBlockImpl(BTInterval in, byte[] buf) 
    throws IOException {
        
        long startOffset = (long)in.getId() * context.getMetaInfo().getPieceLength() + in.getLow();
        diskController.write(startOffset, buf);
        
        synchronized(this) {
            pendingRanges.removeInterval(in);
            partialBlocks.addInterval(in);
            if (!context.getMetaInfo().isCompleteBlock(in.getId(), partialBlocks))
                return;
        }
        
        boolean verified = verifyQuick(in.getId());
        
        synchronized(this) {
            partialBlocks.remove(in.getId());
            if (verified) 
                markPieceCompleted(in.getId());
            else 
                _corruptedBytes += context.getMetaInfo().getPieceSize(in.getId());
        }
        if (verified)
            handleVerified(in.getId());
    }
    
    private synchronized void markPieceCompleted(int blockId) {
        partialBlocks.remove(blockId);
        requestedRanges.remove(blockId);
        verifiedBlocks.set(blockId);
        bitFieldDirty = true;
        if (verifiedBlocks.cardinality() == context.getMetaInfo().getNumBlocks()) {
            verifiedBlocks = context.getFullBitSet();
            verified = context.getFullBitField();
            missing = new NotView(verified);
        }
        
        // calculate the last verified offset here rather than on the
        // gui thread if single-file torrent.
        if (context.getFileSystem().getFiles().size() != 1)
            return;
        
        // this is a little roundabout to make iteration shorter 
        long length = context.getMetaInfo().getPieceLength();
        int i = (int) (lastVerifiedOffset / length);
        while(i < verified.maxSize() && verified.get(i))
            i++;
        lastVerifiedOffset = Math.min(context.getFileSystem().getTotalSize(), i * length);
    }
    
    private boolean verifyQuick(int pieceNum) throws IOException {
        try {
            return verify(pieceNum, false);
        } catch (InterruptedException impossible) {
            ErrorService.error(impossible);
            return false;
        }
    }
    
    /**
     * @param pieceNum the piece to verify
     * @param slow whether to not max out cpu
     * @return if the block at pieceNum is ok.
     */
    private boolean verify(int pieceNum, boolean slow) 
    throws IOException, InterruptedException {
        MessageDigest md = context.getMetaInfo().getMessageDigest();
        md.reset();
        int pieceSize = context.getMetaInfo().getPieceSize(pieceNum);
        byte [] buf = new byte[Math.min(65536,pieceSize)];
        int read = 0;
        long offset = (long)pieceNum * context.getMetaInfo().getPieceLength();
        while (read < pieceSize) {
            
            int readNow = diskController.read(offset, buf, 0, buf.length);
            if (readNow == 0)
                return false;
            
            long start = System.nanoTime();
            md.update(buf, 0, readNow);
            
            if (slow && SystemUtils.getIdleTime() < URN.MIN_IDLE_TIME &&
                    SharingSettings.FRIENDLY_HASHING.getValue()) {
                long interval = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                // go extra slow if there are active torrents
                interval *= QUEUE.size() > 0 ? 5 : 3; 
                if (interval > 0) 
                    Thread.sleep(interval);
                else
                    Thread.yield();
            }
            
            read += readNow;
            offset += readNow;
        }
        
        byte [] sha1 = md.digest();
        return context.getMetaInfo().verify(sha1, pieceNum);
    }
    
    /**
     * performs various tasks after a block has been verified
     * such as notifying any listeners or closing files.
     */
    private void handleVerified(int pieceNum) throws IOException {
        closeAnyCompletedFiles(pieceNum);
        notifyOfChunkCompletion(pieceNum);
    }
    
    private void notifyOfChunkCompletion(int pieceNum) {
        DiskManagerListener t = listener;
        if (t != null)
            t.chunkVerified(pieceNum);
    }
    
    private void closeAnyCompletedFiles(int pieceNum) throws IOException {
        List<TorrentFile> possiblyCompleted = null;
        for (TorrentFile t : _files) {
            if (t.getBeginPiece() > pieceNum)
                continue;
            if (t.getEndPiece() < pieceNum)
                break;
            
            if (possiblyCompleted == null)
                possiblyCompleted = new ArrayList<TorrentFile>();
            
            possiblyCompleted.add(t);
        }
        
        if (possiblyCompleted == null)
            return;
        
        for (TorrentFile file : possiblyCompleted) {
            boolean done = true;
            for(int i = file.getBeginPiece(); i <= file.getEndPiece(); i++) {
                if (!hasBlock(i)) {
                    done = false;
                    break;
                }
            }
            if (done) 
                diskController.setReadOnly(file);
        }
    }
    
    public synchronized boolean hasBlock(int block) {
        return verified.get(block);
    }

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.TorrentFileManager#open(com.limegroup.bittorrent.ManagedTorrent)
	 */
	public void open(final DiskManagerListener torrent) throws IOException {
		//TODO refactor to be able to add multiple listeners
		this.listener = torrent;
		storedException = null;
		
		boolean wasVerifying = isVerifying;
		// whether the data on disk should be re-verified
		isVerifying |= (getBlockSize() == 0);
		
		List<TorrentFile> filesToVerify = diskController.open(_files, isComplete(), isVerifying);
		
		// verify any files that needed verification
		if (filesToVerify != null) {
			isVerifying = true;
			// pretend nothing was downloaded
			if (!wasVerifying) {
				synchronized(this) {
					verifiedBlocks.clear();
					partialBlocks.clear();
				}
			}
			verifyFiles(filesToVerify);
		} else
			isVerifying = false;

		// always verify any partial blocks that are large enough
		// (could happen if lw was shutdown during verification)
		for (int block : partialBlocks.keySet() ) {
			if (context.getMetaInfo().isCompleteBlock(block, partialBlocks)) {
				isVerifying = true;
				VERIFY_QUEUE.execute(new VerifyJob(block),context.getMetaInfo().getURN());
			}
		}
		
		// if we had to verify anything, enqueue a notification
		// after we're done.
		if (isVerifying) {
			VERIFY_QUEUE.execute(new Runnable(){
				public void run() {
					if (isOpen()) {
						isVerifying = false;
						_corruptedBytes = 0;
						torrent.verificationComplete();
					}
				}
			}, context.getMetaInfo().getURN());
		}
	}
	
	public boolean isVerifying() {
		return isVerifying;
	}

	/**
	 * @return true if the whole torrent has been written and verified
	 */
	public synchronized boolean isComplete() {
		return verified == context.getFullBitField();
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.TorrentFileManager#close()
	 */
	public void close() {
		LOG.debug("closing the file");
		synchronized(this) {
			pendingRanges.clear();
		}
		diskController.close();
		
		listener = null;
		// kill all jobs for this torrent
		URN urn = context.getMetaInfo().getURN();
		VERIFY_QUEUE.clear(urn);
		QUEUE.clear(urn);
		
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.TorrentFileManager#isOpen()
	 */
	public boolean isOpen() {
		return diskController != null & diskController.isOpen();
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.TorrentFileManager#requestPieceRead()
	 */
	public void requestPieceRead(BTInterval in, PieceReadListener c) {
		if (storedException != null)
				return;
		QUEUE.execute(new SendJob(in, c),context.getMetaInfo().getURN());
	}
	
	private class SendJob implements Runnable {
		private final BTInterval in;
		private final PieceReadListener listener;
		SendJob(BTInterval in, PieceReadListener listener) {
			this.in = in;
			this.listener = listener;
		}
		
		public void run() {
			if (!isOpen())
				return;
			if (storedException != null)
				return;
			
			if (LOG.isDebugEnabled())
				LOG.debug("reading piece " + in);
            long length64 = in.getHigh() - in.getLow() + 1;
            assert length64 <= Integer.MAX_VALUE;
            int length = (int)length64;
            long position = (long)in.getId() * context.getMetaInfo().getPieceLength() + in.getLow();
            int offset = 0;
            byte[] buf = new byte[length];
            boolean success = false;
            try {
                do {
                    offset += diskController.read(position + offset, buf, offset, length - offset);
                } while (offset < length);
                success = true;
            } catch (IOException bad) {
                if (isOpen()) {
                    storedException = bad;
                    notifyDiskProblem(new DiskException(bad));
                }
            } finally {
                if (success)
                    listener.pieceRead(in, buf);
                else
                    listener.pieceReadFailed(in);
            }
        }
    }
    
    private void notifyDiskProblem(IOException e) {
        DiskManagerListener t = listener;
        if (t != null)
            t.diskExceptionHappened(new DiskException(e));
    }
    
    /**
     * returns a random available range that has preferably not yet been
     * requested
     * 
     * @param bs the BitField of available ranges
     * @param exclude the set of ranges that the connection is already about to
     * request
     * @return a BTInterval that should be requested next.
     */
    public synchronized BTInterval leaseBTInterval(BitField bs, Set<BTInterval> exclude,
            PieceStrategy pieceStrategy) {
        if (isComplete())
            return null;

        if (LOG.isDebugEnabled())
            LOG.debug("leasing random chunk from available cardinality " + bs.cardinality());

        BitField interesting = new AndView(bs, missing);
        List<BTInterval> toLease = findPiecesWithEndGame(interesting, exclude, pieceStrategy);
        BTInterval lease = null;

        if (toLease != null && toLease.size() > 0) {
            lease = resize(toLease.get(0));
            requestedRanges.addInterval(lease);

            if (LOG.isDebugEnabled())
                LOG.debug("assigning " + lease);
        } else if (LOG.isDebugEnabled())
            LOG.debug("couldn't find anything to assign " + exclude);

        return lease;
    }

    private BTInterval resize(BTInterval lease) {
        if (lease.getHigh() - lease.getLow() + 1 > BLOCK_SIZE)
            lease = new BTInterval(lease.getLow(), lease.getLow() + BLOCK_SIZE - 1, lease.getId());
        return lease;
    }

    public synchronized List<BTInterval> lease(BitField bs, Set<BTInterval> exclude,
            PieceStrategy pieceStrategy) {
        if (isComplete())
            return null;

        if (LOG.isDebugEnabled())
            LOG.debug("leasing random chunk from available cardinality " + bs.cardinality());

        BitField interesting = new AndView(bs, missing);
        List<BTInterval> toLease = findPiecesWithEndGame(interesting, exclude, pieceStrategy);

        if (toLease != null && toLease.size() > 0) {
            for (BTInterval lease : toLease) {
                requestedRanges.addInterval(lease);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("assigning " + lease);
                }
            }
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("couldn't find anything to assign " + exclude);
        }

        return toLease;
    }

    private List<BTInterval> findPiecesWithEndGame(BitField bs, Set<BTInterval> exclude,
            PieceStrategy pieceStrategy) {

        PartialPieceStrategy partialPieceStrategy = new PartialPieceStrategy(context.getMetaInfo(),
                exclude, false, partialBlocks, pendingRanges, requestedRanges);

        // first try to complete any partial pieces that are not requested
        List<BTInterval> nextPieces = partialPieceStrategy.getNextPieces(bs, missing);
        if (nextPieces != null && nextPieces.size() > 0) {
            return nextPieces;
        }
        LOG.debug("couldn't find partial, looking for unnassigned");

        // then see if the remote has any pieces that are neither
        // partial nor already requested
        nextPieces = findUnassigned(bs, pieceStrategy);
        if (nextPieces != null && nextPieces.size() > 0) {
            return nextPieces;
        }
        LOG.debug("couldn't find unassigned, looking for already requested");

        EndGamePieceStrategy endGameStrategy = new EndGamePieceStrategy(context.getMetaInfo(), exclude,
                partialBlocks, pendingRanges, requestedRanges);
        nextPieces = endGameStrategy.getNextPieces(bs, missing);

        return nextPieces;
    }
    
    private List<BTInterval> findUnassigned(BitField availableBlocks, PieceStrategy pieceStrategy) {
        if (pieceStrategy == null) {
            pieceStrategy = new RandomPieceStrategy(context.getMetaInfo());
        }
        BitField uninteresting = new OrView(pendingRanges.getBitField(), requestedRanges
                .getBitField(), partialBlocks.getBitField());
        BitField neededBlocks = new AndView(missing, new NotView(uninteresting));

        List<BTInterval> nextPieces = pieceStrategy.getNextPieces(availableBlocks, neededBlocks);
        return nextPieces;
    }
    
    
    /**
     * Removes an interval from the internal list of already requested intervals.
     * <p>
     * Note that during endgame several connections may be requesting the same interval
     * and as one of them fails that interval will no longer be considered requested.
     * That's OK as it will only result in that interval requested again.
     */
    public synchronized void releaseInterval(BTInterval in) {
        if (LOG.isDebugEnabled())
            LOG.debug("releasing "+in);
        requestedRanges.removeInterval(in);
    }

    /**
     * @return returns an array of byte where the i'th byte is 1 if we have
     *         written and verified the i'th piece of the torrent and 0
     *         otherwise
     */
    public synchronized byte[] createBitField() {
        if (bitField == null) 
            bitField = new byte[(context.getMetaInfo().getNumBlocks() + 7) / 8];
        
        if (bitFieldDirty) {
            if (isComplete()) {
                for(int i = 0; i < bitField.length; i++)
                    bitField[i] = (byte)0xFF;
                int odd = context.getMetaInfo().getNumBlocks() % 8;
                if (odd != 0) 
                    bitField[bitField.length - 1] <<= (8 - odd);
                
            } else {
                for(int i = verified.nextSetBit(0); i >= 0; i = verified.nextSetBit(i+1)) 
                    bitField[i / 8] = (byte) (bitField[i / 8] | (1 << (7 - i % 8)));
            }
            bitFieldDirty = false;
        }
            
        return bitField;
    }

    /**
     * verifies all the chunks that are associated with a list of files.
     */
    private void verifyFiles(List<TorrentFile> l) {
        int lastSet;
        synchronized(this) {
            lastSet = verifiedBlocks.length();
        }
        for (TorrentFile f : l) {
            for (int i = Math.max(lastSet,f.getBeginPiece()); i <= f.getEndPiece(); i++) 
                VERIFY_QUEUE.execute(new VerifyJob(i),context.getMetaInfo().getURN());
        }
    }

    /**
     * A task that checks an already existing file on disk
     * against the .torrent metadata.  
     */
    private class VerifyJob implements Runnable {
        private final int pieceNum;
        public VerifyJob(int pieceNum) {
            this.pieceNum = pieceNum;
        }
        
        public void run() {
            if (storedException != null ||
                    !isOpen() ||
                    hasBlock(pieceNum))
                return;
            try {
                
                if (verify(pieceNum, true)) {
                    markPieceCompleted(pieceNum);
                    handleVerified(pieceNum);
                } else 
                    _corruptedBytes += context.getMetaInfo().getPieceSize(pieceNum);
            } catch (IOException bad) {
                storedException = bad;
            } catch (InterruptedException iex) { // should not happen
                storedException = new InterruptedIOException();
            } finally {
                if (storedException != null && isOpen())
                    notifyDiskProblem(storedException);
            }
        }
    }

    /**
     * @return number of bytes written and verified
     */
    public synchronized long getVerifiedBlockSize() {
        BTMetaInfo info = context.getMetaInfo();
        long ret = verified.cardinality() * (long)info.getPieceLength();
        if (verified.get(info.getNumBlocks() - 1)) {
            ret = ret - info.getPieceLength() + 
                info.getPieceSize(info.getNumBlocks() -1 );
        }
        return ret;
    }
    
   /*
    * (non-Javadoc)
    * @see com.limegroup.bittorrent.disk.TorrentDiskManager#getBlockSize()
    */
    public synchronized long getBlockSize() {
        long written = getVerifiedBlockSize();
        return written + partialBlocks.byteSize();
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.bittorrent.disk.TorrentDiskManager#getNumCorruptedBytes()
     */
    public synchronized long getNumCorruptedBytes() {
        return _corruptedBytes;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.bittorrent.disk.TorrentDiskManager#toMemento()
     */
    public BTDiskManagerMemento toMemento() {
        BTDiskManagerMemento memento = new BTDiskManagerMementoImpl();
        synchronized(this) {
            // Deep-copy the IntervalSet to avoid ConcurrentModificationExceptions
            Map<Integer, IntervalSet> partial = new HashMap<Integer, IntervalSet>(partialBlocks.size());
            for(Map.Entry<Integer, IntervalSet> entry : partialBlocks.entrySet())
                try {
                    partial.put(entry.getKey(), entry.getValue().clone());
                    memento.setVerifiedBlocks((BitSet)verifiedBlocks.clone());
                } catch (CloneNotSupportedException e) {
                   throw new RuntimeException(e);
                }
            memento.setPartialBlocks(partial);
            memento.setVerifying(isVerifying);
        }
        if (BittorrentSettings.TORRENT_FLUSH_VERIRY.getValue()) {
            try {
                diskController.flush();
            } catch (IOException iox) {
                // don't abort the serialization, but mark the file corrupt
                storedException = iox; 
            }
        }
        return memento;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.bittorrent.TorrentFileManager#getAmountPending()
     */
    public synchronized int getAmountPending() {
        return (int)pendingRanges.byteSize();
    }
    
    
    /* (non-Javadoc)
     * @see com.limegroup.bittorrent.TorrentFileManager#getNumMissing(com.limegroup.gnutella.util.BitField)
     */
    public synchronized int getNumMissing(BitField other) {
        if (isComplete())
            return verified.cardinality() - other.cardinality();
        
        BitField theirMissing = new NotView(other);
        return (new AndView(verified, theirMissing)).cardinality();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.bittorrent.disk.TorrentDiskManager#containsAnyWeMiss(org.limewire.collection.BitField)
     */
    public synchronized boolean containsAnyWeMiss(BitField other) {
        // if we are complete we miss nothing
        if (isComplete())
            return false;
        
        BitField interesting = new AndView(other,missing);
        return interesting.nextSetBit(0) > -1;
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.bittorrent.disk.TorrentDiskManager#getLastVerifiedOffset()
     */
    public long getLastVerifiedOffset() {
        return lastVerifiedOffset;
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.bittorrent.disk.TorrentDiskManager#renewLease(java.util.List, java.util.List)
     */
    public void renewLease(List<BTInterval> oldIntervals, List<BTInterval> newIntervals) {
        synchronized (VerifyingFolder.this) {
            for (BTInterval btInterval : oldIntervals) {
                requestedRanges.removeInterval(btInterval);
            }
            for (BTInterval btInterval : newIntervals) {
                requestedRanges.addInterval(btInterval);
            }

        }
    }
}
