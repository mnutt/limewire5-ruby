package org.limewire.lws.server;

/**
 * Represents the functionality needed for a remote server. This does <b>NOT</b>
 * represent something that will be on the client, it represents the Lime Wire
 * Store, for example.
 */
public interface RemoteServer {
    
    /**
     * Returns the {@link Thread} that started this server, after starting it.
     * 
     * @return the {@link Thread} that started this server, after starting it
     */
    Thread start();

    /**
     * Returns <code>true</code> after storing a mapped tuple
     * <code>publicKey -> (privateKey,ip)</code> so that Javascript code can
     * retrieve this private key with <code>publicKey</code>, and
     * <code>false</code> if it couldn't be stored.
     * 
     * @param publicKey public key used to retrieve <code>privateKey</code>
     *        through calling {@link #lookUpprivateKey(String, String)}
     * @param privateKey private key returned from
     *        {@link #lookUpprivateKey(String, String)} when
     *        <code>publicKey</code> and <code>ip</code> are passed in
     * @param ip IP address of the request
     * @return
     */
    boolean storeKeys(String publicKey,String privateKey, String ip);

    /**
     * Returns the private key if a call to
     * {@link #storeKeys(String, String, String)} was made using
     * <code>publicKey</code> and <code>ip</code>, and <code>null</code>
     * if no mapped tuple <code>publicKey -> (*,ip)</code> exists.
     * 
     * @param publicKey public key to retrieve the private key
     * @param ip ip of the request
     * @return the private key if a call to
     *         {@link #storeKeys(String, String, String)} was made using
     *         <code>publicKey</code> and <code>ip</code>, and
     *         <code>null</code> if no mapped tuple
     *         <code>publicKey -> (*,ip)</code> exists
     */
    String lookupPrivateKey(String publicKey, String ip);

}