module Core
  def self.get_singleton(klass)
    $core.injector.get_instance(klass.java_class)
  end
  
  if($core)
    # Running from Limewire
    include Java
    
    Geocoder               = org.limewire.geocode.Geocoder
    OldURN                 = com.limegroup.gnutella.URN
    MetaDataFactoryImpl    = com.limegroup.gnutella.metadata.MetaDataFactoryImpl
    MetaDataFactoryRef     = com.limegroup.gnutella.metadata.MetaDataFactory
    GUID                   = org.limewire.io.GUID
    URN                    = org.limewire.core.api.URN
    URNImpl                = org.limewire.core.impl.URNImpl
    LibraryManagerRef      = org.limewire.core.api.library.LibraryManager
    SearchManagerRef       = org.limewire.core.api.search.SearchManager
    DownloadListManagerRef = org.limewire.core.api.download.DownloadListManager
    
    SearchManager       = self.get_singleton(SearchManagerRef)
    LibraryManager      = self.get_singleton(LibraryManagerRef)
    MetaDataFactory     = self.get_singleton(MetaDataFactoryRef)
    DownloadListManager = self.get_singleton(DownloadListManagerRef)
  else
    # Not running from limewire, no $core available
  end
end

