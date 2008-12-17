import 'org.limewire.geocode.Geocoder'
import 'com.limegroup.gnutella.URN'
import 'com.limegroup.gnutella.metadata.MetaDataFactoryImpl'

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
      Limewire.core.search_services.newQueryGUID
    end

    def self.query(guid, str)
      Limewire.core.search_services.query(guid, str.slice(0,29))
    end

    def self.stop(guid)
      Limewire.core.search_services.stopQuery(guid)
    end

    def self.get_response(guid)
      
    end
  end

  module Library
    def self.all_files
      all_files = []
#    puts "dbg=>" +
      #    c.file_manager.get_managed_file_list.getLibraryData.getManagedFiles.to_s
#    #.methods.sort.find_all{|x|x=~/^get/}.join("\n\t").to_s
      Limewire.core.file_manager.get_managed_file_list.getLibraryData.getManagedFiles.each do |file|
        metadata_reader = Limewire.core.get_meta_data_factory
        metadata = metadata_reader.parse(file).meta_data rescue nil
        
        def file.metadata=(metadata); @metadata = metadata; end
        def file.metadata; @metadata; end
        file.metadata = metadata
        def file.to_cloud; Limewire::CloudPlayer.convert(self); end
        all_files << file unless file.nil?
      end
      all_files
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
  
  module CloudPlayer
    def self.convert(track)
      puts track.metadata
    end
  end

end 
