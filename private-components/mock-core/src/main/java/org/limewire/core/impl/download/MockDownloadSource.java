package org.limewire.core.impl.download;

import org.limewire.io.Address;

public class MockDownloadSource implements Address {
	private String address;

	public MockDownloadSource(String address) {
		this.address = address;
	}

    @Override
    public String getAddressDescription() {
        return address;
    }
	
	
}
