package com.limegroup.gnutella.stubs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

import org.limewire.security.AddressSecurityToken;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class QueryRequestStub implements QueryRequest {

    public boolean canDoFirewalledTransfer() {
        return false;
    }

    public boolean desiresAll() {
        return true;
    }

    public boolean desiresAudio() {
        return false;
    }

    public boolean desiresDocuments() {
        return false;
    }

    public boolean desiresImages() {
        return false;
    }

    public boolean desiresLinuxOSXPrograms() {
        return false;
    }

    public boolean desiresOutOfBandReplies() {
        return false;
    }

    public boolean desiresOutOfBandRepliesV2() {
        return false;
    }

    public boolean desiresOutOfBandRepliesV3() {
        return false;
    }

    public boolean desiresPartialResults() {
        return false;
    }

    public boolean desiresVideo() {
        return false;
    }

    public boolean desiresWindowsPrograms() {
        return false;
    }

    public boolean desiresXMLResponses() {
        return false;
    }

    public boolean doNotProxy() {
        return false;
    }

    public int getFeatureSelector() {
        return 0;
    }

    public int getMetaMask() {
        return 0;
    }

    public int getMinSpeed() {
        return SPECIAL_MINSPEED_MASK;
    }

    public byte[] getPayload() {
        return null;
    }

    public String getQuery() {
        return null;
    }

    public AddressSecurityToken getQueryKey() {
        return null;
    }

    public Set<URN> getQueryUrns() {
        return Collections.emptySet();
    }

    public String getReplyAddress() {
        return null;
    }

    public int getReplyPort() {
        return 0;
    }

    public LimeXMLDocument getRichQuery() {
        return null;
    }

    public String getRichQueryString() {
        return null;
    }

    public boolean hasQueryUrns() {
        return false;
    }

    public boolean isBrowseHostQuery() {
        return false;
    }

    public boolean isFeatureQuery() {
        return false;
    }

    public boolean isFirewalledSource() {
        return false;
    }

    public boolean isLimeRequery() {
        return false;
    }

    public boolean isOriginated() {
        return false;
    }

    public boolean isQueryForLW() {
        return false;
    }

    public boolean isSecurityTokenRequired() {
        return false;
    }

    public boolean isWhatIsNewRequest() {
        return false;
    }

    public boolean matchesReplyAddress(byte[] ip) {
        return false;
    }

    public void originate() {

    }

    public void recordDrop() {

    }

    public long getCreationTime() {
        return 0;
    }

    public byte getFunc() {
        return 0;
    }

    public byte[] getGUID() {
        return null;
    }

    public Class<? extends Message> getHandlerClass() {
        return null;
    }

    public byte getHops() {
        return 0;
    }

    public int getLength() {
        return 0;
    }

    public Network getNetwork() {
        return null;
    }

    public int getPriority() {
        return 0;
    }

    public byte getTTL() {
        return 0;
    }

    public int getTotalLength() {
        return 0;
    }

    public byte hop() {
        return 0;
    }

    public boolean isMulticast() {
        return false;
    }

    public boolean isTCP() {
        return false;
    }

    public boolean isUDP() {
        return false;
    }

    public boolean isUnknownNetwork() {
        return false;
    }

    public void setHops(byte hops) throws IllegalArgumentException {

    }

    public void setPriority(int priority) {

    }

    public void setTTL(byte ttl) throws IllegalArgumentException {

    }

    public void write(OutputStream out, byte[] buf) throws IOException {

    }

    public void write(OutputStream out) throws IOException {

    }

    public void writeQuickly(OutputStream out) throws IOException {

    }

    public int compareTo(Message o) {
        return 0;
    }

    public boolean shouldIncludeXMLInResponse() {
        return false;
    }

}
