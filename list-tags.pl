#!/usr/bin/perl

# Simple perl script to list files with a given CVS tag
# Created 2002/09/10 ec
# $Id: lstag,v 1.1 2002/09/26 10:02:53 ec Exp $

use strict;

$::VERSION = "1.0";
$::cvs_ID = '$Id: lstag,v 1.1 2002/09/26 10:02:53 ec Exp $'; #'

undef ($::repo);
# Try #1 to get CVS repository location
if (-r "CVS/Root") {
    open (INF, "<CVS/Root") || die "Failed to read CVS/Root file!\n";
    ###chop ($::repo = <INF>);
    $::repo = <INF>;
    close (INF);
} else {
    # Try #2 to get CVS repository location
    if (!$::ENV{"CVSROOT"}) {
        print "CVSROOT environment variable not found!\n";
        print "CVS not detected...\n";
        exit (10);
    }
}
$::repo =~ s/\n$//g;
$::repo =~ s/\r$//g;
($::repo) = $::repo =~ /([^:]+)$/;
$::repo =~ s/\/*$/\//;
###print "CVS repository at $::repo\n";

# Check commandline arguments
if ($#ARGV < 0) {
    print "Missing argument!\n";
    print "Usage: $0 [-l | tag]\n\n";
    print "Where: -l    shows list of all known tags\n";
    print "       tag   shows list of files with this tag\n";
    print "\n";
    exit (1);
}

# Got desired tagname
$::tag = $ARGV[0];
$::taglist = 0;
if ($::tag eq "-l") {
    $::taglist = 1;
}

# Run cvs status and catch output
open (INF, "cvs -q status -R -v |") || die "Failed to run cvs status command!\n";
chop (@::STATUS = <INF>);
close (INF);

# Parse status
$::state = 0;
$::fpath = $::frpath = $::fname = $::fstatus = $::ftag = $::ftagrev = "!UNINITIALIZED VARIABLE!";
undef (%::TAGS);
$::found = 0;
for $::lc (0 .. $#::STATUS) {
    $_ = $::STATUS[$::lc];
    if ($::state == 0) {
        if (/^File:/) {
            ($::fname, $::fstatus) = /^File:\s+(\S+\s*\S+)\s+Status:\s+(\S+)/;
            $::state = 1;
        }
        next;
    }
    if ($::state == 1) {
        if (/^\s+Repository revision:/) {
            ($::frpath) = /(\/.*),v/;
            ($::fpath) = $::frpath =~ /^$::repo(.*)$/;
            push @::INFOL, ( $::fpath );
            $::current = $::fpath;
            $::INFO{$::current}->{"rpath"}  = $::frpath;
            $::INFO{$::current}->{"name"}   = $::fname;
            $::INFO{$::current}->{"status"} = $::fstatus;
            $::fpath = $::frpath = $::fname = $::fstatus = "!UNINITIALIZED VARIABLE!";
            $::state = 2;
        }
        next;
    }
    if ($::state == 2) {
        if (/^\s+Existing Tags:/) {
            $::state = 3;
        }
        next;
    }
    if (/^\s+\S+\s+\([^:]+:/) {
        ($::ftag, $::ftagrev) = /^\s+(\S+)\s+\([^:]+:\s+([^\)]+)\)/;
        if ($::taglist) {
            $::TAGL{$::ftag}++;
        }
        if ($::ftag eq $::tag) {
            $::found++;
            $::INFO{$::current}->{"tag"} = $::ftag;
            $::INFO{$::current}->{"tagrev"} = $::ftagrev;
            $::ftag = $::ftagrev = "!UNINITIALIZED VARIABLE!";
        }
    } else { $::state = 0; }
}

# Print results
print "$0 - CVS tag and file lister version $::VERSION\n";
print "ID: $::cvs_ID\n\n";
if ($::taglist) {
    print "List of all known tags:\n\n";
    foreach $::key (sort {uc($a) cmp uc($b)} keys %::TAGL) {
        print "$::key\n";
    }
} else {
    print "Files with tag \"$::tag\":";
    if ($::found > 0) {
        print "\n\n";
    } else {
        print "  NONE\n";
    }
    for $::i (0 .. $#::INFOL) {
        if (!defined($::INFO{$::INFOL[$::i]}->{"tag"})) {
            next;
        }
        $::name = $::INFOL[$::i];
        $::status = $::INFO{$::name}->{"status"};
        $::tagrev = $::INFO{$::name}->{"tagrev"};
        printf "%10s %10s %s\n", ($::status, $::tagrev, $::name);
    }
}
print "\n";