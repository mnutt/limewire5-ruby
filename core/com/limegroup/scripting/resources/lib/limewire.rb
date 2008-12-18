import 'org.limewire.geocode.Geocoder'
import 'com.limegroup.gnutella.URN'
import 'com.limegroup.gnutella.metadata.MetaDataFactoryImpl'
import 'org.limewire.io.GUID'

module Limewire

  def self.core=(c)
    @core = c
  end
  def self.core
    @core
  end

  def self.uptime
    Limewire.core.get_statistics.uptime / 1000
  end

  def self.daily_uptime
    Limewire.core.get_statistics.calculate_daily_uptime
  end
      
  module Search
    def self.new
      GUID.new(Limewire.core.search_services.newQueryGUID).to_s
    end

    def self.query(guid, str)
      guid = GUID.new(guid).bytes
      Limewire.core.search_services.query(guid, str.slice(0,29))
    end

    def self.stop(guid)
      guid = GUID.new(guid).bytes
      Limewire.core.search_services.stopQuery(guid)
    end

    def self.get_response(guid)
      
    end
  end

  module Library
    def self.all_files
      file_list = Limewire.core.get_file_manager.get_gnutella_file_list
      file_list.map{ |file| Limewire::File.new(file) }.compact
    end

    def self.filter(&b)
      all_files.find_all(&b)
    end

    def self.filter_by_name(regex)
      all_files.find_all{ |f| f.file_name =~ regex }
    end
    
    def self.categories
      Limewire.core.get_file_manager.get_managed_file_list.managed_categories rescue []
    end
    
  end

  class File
    def initialize(file)
      @file = file
      @metadata = Limewire.core.meta_data_factory.parse(file.get_file) rescue nil
    end

    def metadata
      @metadata
    end
    def to_cloud
      {
        'duration' => metadata.get_length,
        'permalink' => metadata.getTitle,
        'uri' => "/library/#{self.sHA1Urn}",
        'downloadable' => true,
        'title' => metadata.getTitle,
        'id' => self.object_id,
        'streamable' => true,
        'stream_url' => "/library/#{self.sHA1Urn}",
        'description' => metadata.getComment,
        'permalink_url' => "/library/#{self.sHA1Urn}",
        'user' => {"username"=>"derek"},
        'sharing' => 'public',
        'purchase_url' => 'http://store.limewire.com'
      }
    end

    def method_missing(name, *args)
      if @file.respond_to?(name)
        @file.send(name, *args)
      else
        super
      end
    end
  end
end 

