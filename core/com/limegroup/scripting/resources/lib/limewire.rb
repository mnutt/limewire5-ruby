import 'org.limewire.geocode.Geocoder'
import 'com.limegroup.gnutella.URN'
import 'com.limegroup.gnutella.metadata.MetaDataFactoryImpl'
import 'org.limewire.io.GUID'
import 'org.limewire.core.api.library.LibraryManager'
import 'org.limewire.core.api.search.SearchManager'


module Limewire

  def self.core=(c)
    @core = c
  end
  def self.core
    @core
  end

  def self.get_singleton(klass)
    $core.injector.get_instance(klass.java_class)
  end

  def self.uptime
    Limewire.core.get_statistics.uptime / 1000
  end

  def self.daily_uptime
    Limewire.core.get_statistics.calculate_daily_uptime
  end
      
  class Search
    def self.find(guid)
      self.new Limewire.get_singleton(SearchManager).getSearchByGuid(GUID.new(guid))
    end

    def self.query(query)
      self.new Limewire.get_singleton(SearchManager).createSearchFromQuery(query)
    end

    def initialize(search)
      @search = search
    end

    def results
      results = @search.getSearchResults
      results.map {|result| {:filename => result.fileName }}
    end

    def start
      @search.start
    end

    def query_string
      @search.getQueryString
    end

    def guid
      @search.getQueryGuid
    end

    def stop
      @search.stop
    end

    def restart
      @search.restart
    end
  end

  module Library
    def self.all_files
      file_list = Limewire.get_singleton(LibraryManager).library_managed_list.core_file_list
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
      @metadata = Limewire.get_singleton(MetaDataFactory).parse(file.get_file) rescue nil
    end

    def metadata
      @metadata
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

