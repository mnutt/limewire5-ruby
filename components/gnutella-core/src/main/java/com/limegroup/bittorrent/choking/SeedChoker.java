package com.limegroup.bittorrent.choking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.collection.NECallable;

import com.limegroup.bittorrent.Chokable;
import com.limegroup.gnutella.UploadServices;

/**
 * Choker that implements the choking logic during seeding.
 */
class SeedChoker extends Choker {

	private static final Comparator<Chokable> UPLOAD_SPEED_COMPARATOR = 
		new SpeedComparator(false);
	
	/*
	 * orders BTConnections by the round they were unchoked.
	 */
	private static final Comparator<Chokable> UNCHOKE_COMPARATOR =
		new UnchokeComparator();
	
	/**
	 * # of unchoked chokables since the last scheduled invocation of this
	 */
	private int unchokesSinceLast;
	
	SeedChoker(NECallable<List<? extends Chokable>> chokables,
			ScheduledExecutorService invoker, UploadServices uploadServices) {
		super(chokables, invoker, uploadServices);
		round = Integer.MIN_VALUE;
	}

	private void initRound(List<? extends Chokable> chokables) {
		if (round >= 0)
			return;
		int maxRound = 0;
		for (Chokable c : chokables)
			maxRound = Math.max(maxRound,c.getUnchokeRound());
		round = maxRound;
	}
	
	@Override
	protected void rechokeImpl(boolean forceUnchokes) {
		List<? extends Chokable> chokables = chokablesSource.call();
		initRound(chokables);
		int numForceUnchokes = 0;
		if (forceUnchokes) {
			int x = (getNumUploads() + 2) / 3;
			numForceUnchokes = Math.max(0, x + round % 3) / 3 -
			unchokesSinceLast;
		}
		
		List<Chokable> preferred = new ArrayList<Chokable>();
		int newLimit = round - 3;
		for (Chokable con : chokables) {
			if (!con.isChoked() && con.isInterested() && 
					con.shouldBeInterested()) {
				if (con.getUnchokeRound() < newLimit)
					con.clearUnchokeRound();
				preferred.add(con);
			}
		}
		
		int numKept = getNumUploads() - numForceUnchokes;
		if (preferred.size() > numKept) {
			Collections.sort(preferred,UNCHOKE_COMPARATOR);
			preferred = preferred.subList(0, numKept);
		}
		
		int numNonPref = getNumUploads() - preferred.size();
		
		if (forceUnchokes)
			unchokesSinceLast = 0;
		else
			unchokesSinceLast += numNonPref;
		
		for (Chokable con : chokables) {
			if (preferred.contains(con))
				continue;
			if (!con.isInterested())
				con.choke();
			else if (con.isChoked() && numNonPref > 0 && 
					con.shouldBeInterested()) {
				con.unchoke(round);
				numNonPref--;
			}
			else {
				if (numNonPref == 0 || !con.shouldBeInterested())
					con.choke();
				else
					numNonPref--;
			}
		}

	}

	/**
	 * A comparator that compares BT connections by the number of
	 * unchoke round they were unchoked.  Connections with higher 
	 * round get preference.
	 * 
	 * Connections with the same uchoke round are compared by upload speed.
	 */
	private static class UnchokeComparator implements Comparator<Chokable> {
		public int compare(Chokable con1, Chokable con2) {
			if (con1 == con2)
				return 0;
			if (con1.getUnchokeRound() != con2.getUnchokeRound())
				return -1 * (con1.getUnchokeRound() - con2.getUnchokeRound());
			return UPLOAD_SPEED_COMPARATOR.compare(con1, con2);
		}
	}
	
}
