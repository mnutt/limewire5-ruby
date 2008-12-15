package org.limewire.setting;

final class NullRemoteManager implements RemoteSettingManager {

    public void setRemoteSettingController(RemoteSettingController controller) {
    }

    public String getUnloadedValueFor(String remoteKey) {
        return null;
    }

}
