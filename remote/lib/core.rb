module Core
  def self.injector
    if($servlet_context)
      $servlet_context.getAttribute("injector")
    else
      $injector
    end
  end

  def self.get_singleton(klass)
    self.injector.get_instance(klass.java_class)
  end
  
  if(self.injector)
    # Running from Limewire
    include Java
    
    Geocoder               = org.limewire.geocode.Geocoder
    OldURN                 = com.limegroup.gnutella.URN
    ApplicationServicesRef = com.limegroup.gnutella.ApplicationServices
    MetaDataFactoryImpl    = com.limegroup.gnutella.metadata.MetaDataFactoryImpl
    MetaDataFactoryRef     = com.limegroup.gnutella.metadata.MetaDataFactory
    GUID                   = org.limewire.io.GUID
    URN                    = org.limewire.core.api.URN
    StatisticsRef          = com.limegroup.gnutella.Statistics
    FilePropertyKey        = org.limewire.core.api.FilePropertyKey
    MojitoManagerRef       = org.limewire.core.api.mojito.MojitoManager
    LibraryManagerRef      = org.limewire.core.api.library.LibraryManager
    SearchManagerRef       = org.limewire.core.api.search.SearchManager
    DownloadListManagerRef = org.limewire.core.api.download.DownloadListManager
    MongrelManagerRef      = org.limewire.http.mongrel.MongrelManager # meta
    HostCatcherRef         = com.limegroup.gnutella.HostCatcher
    NetworkManagerRef      = com.limegroup.gnutella.NetworkManager

    ApplicationServices = self.get_singleton(ApplicationServicesRef)
    Statistics          = self.get_singleton(StatisticsRef)
    MojitoManager       = self.get_singleton(MojitoManagerRef)
    SearchManager       = self.get_singleton(SearchManagerRef)
    LibraryManager      = self.get_singleton(LibraryManagerRef)
    MetaDataFactory     = self.get_singleton(MetaDataFactoryRef)
    DownloadListManager = self.get_singleton(DownloadListManagerRef)
    MongrelManager      = self.get_singleton(MongrelManagerRef)
    HostCatcher         = self.get_singleton(HostCatcherRef)
    NetworkManager      = self.get_singleton(NetworkManagerRef)
  else
    # Not running from limewire, no injector available
  end
end

