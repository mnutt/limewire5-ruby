package com.limegroup.gnutella;


/**
 * Simple serializable wrapper for Endpoint.
 */
public class EndpointData extends Endpoint 
  implements Cloneable, java.io.Serializable {
    
    private long speed = 56;

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        if(getAddress() == null)
           setHostname("");
    }

    /**
    * serial version
    */
    private static final long serialVersionUID = 5045818571228406227L;    
    
    // for deserializing.
    @SuppressWarnings("unused")
    private EndpointData() { }
    
    /**
    * Creates a new EndpointData instance from another endpoint
    * @param endpoint from where to copy the fields
    */
    public EndpointData(Endpoint endpoint) {   
        //initialize the fields in the super class
        super(endpoint);
    }
    
    /**
    * Creates a new EndpointData instance
    * @param hostname Hostname of the node this endpoint refers to
    * @param port The port number for the host
    */
    public EndpointData(String hostname, int port) {
        //initialize the fields in the super class
        super(hostname,port);
    }
    
    /**
    * Creates a new EndpointData instance
    * @param hostBytes IP address of the host (MSB first)
    * @param port The port number for the host
    */
    public EndpointData(byte[] hostBytes, int port) {
        //initialize the fields in the super class
        super(hostBytes,port);
    }
    
    /**
    * Creates a new EndpointData instance
    * @param hostBytes IP address of the host (MSB first)
    * @param port The port number for the host
    * @param speed Spped in kbps of the host
    */
    public EndpointData(byte[] hostBytes, int port, long speed) {
        //initialize the fields in the super class
        super(hostBytes,port);
        this.speed = speed;
    }
    
    /**
    * Creates a new EndpointData instance
    * @param hostBytes IP address of the host (MSB first)
    * @param port The port number for the host
    * @param files the number of files the host has
    * @param kbytes the size of all of the files, in kilobytes
    */
    public EndpointData(byte[] hostBytes, int port, long files, long kbytes) {
        //initialize the fields in the super class
        super(hostBytes, port, files, kbytes);
    }
    
    
    /**
    * Creates a new EndpointData instance
    * @param endpoint from where to copy the fields for the super class
    * @param speed Speed of the host
    * @param clientGUID Unique GUID for the host
    */
    public EndpointData(Endpoint endpoint, long speed) {
        //initialize the fields in the super class
        super(endpoint);
        //set the speed and client GUID
        this.setSpeed(speed);
    }
    
    
    /**
    * returns the speed of the host
    */
    public long getSpeed() {
        return speed;
    }
    
    
    
    /**
    * Sets the speed and clientGUID for the host this endpoint refers to
    * @param speed Speed of the host
    */
    public void setSpeed(long speed)
    {
        //set the fields
        this.speed = speed;
    }
    
}
