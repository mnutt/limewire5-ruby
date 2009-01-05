

module Limewire

if($core)
  puts "Running from Limewire, good..."
  include Java

  java_import org.limewire.geocode.Geocoder
  #include     org.limewire.geocode
  java_import com.limegroup.gnutella.URN
  #include     com.limegroup.gnutella
  java_import com.limegroup.gnutella.metadata.MetaDataFactoryImpl
  java_import com.limegroup.gnutella.metadata.MetaDataFactory
  #include     com.limegroup.gnutella.metadata
  java_import org.limewire.io.GUID
  #include     org.limewire.io
  java_import org.limewire.core.api.URN
  #include     org.limewire.core.api
  java_import org.limewire.core.impl.URNImpl
  #include     org.limewire.core.impl
  java_import org.limewire.core.api.library.LibraryManager
  #include     org.limewire.core.api.library
  java_import org.limewire.core.api.search.SearchManager
  #include     org.limewire.core.api.search
else
  puts "Not running from limewire, no $core available"
end

  def self.core
    @core ||= $core rescue nil
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
      file_list = Limewire.get_singleton(org.limewire.core.api.library.LibraryManager).library_managed_list.core_file_list
      file_list = file_list.map{ |file| Limewire::File.new(file) }.compact
      
      file_list.extend(Filterable)
      file_list
    end

    def self.first(limit=1)
      self.all_files.first(limit)
    end

    def self.filter(&b)
      all_files.find_all(&b)
    end

    def self.find(type_or_sha1, options={})
      if(type_or_sha1 == :all)
        files = all_files || []
      elsif(String === type_or_sha1)
        old_urn = com.limegroup.gnutella.URN.createSHA1Urn(type_or_sha1)
        urn = org.limewire.core.impl.URNImpl.new(old_urn)
        library_manager = Limewire.get_singleton(org.limewire.core.api.library.LibraryManager)
        file = library_manager.library_managed_list.core_file_list.get_file_descs_matching(old_urn)
        return file[0]
      end
      
      if options[:genres]
        all_files = all_files.select{|f| f.metadata.genre == options[:genres] }
      end

      limit = options[:limit].to_i || (type == :first) ? 1 : 40
      offset = options[:offset].to_i || 0
      
      all_files[offset..(offset + limit - 1)]
    end
    
    def self.categories
      Limewire.core.get_file_manager.get_managed_file_list.managed_categories rescue []
    end
    
  end

  class File
    def initialize(file)
      @file = file
      @metadata = Limewire.get_singleton(com.limegroup.gnutella.metadata.MetaDataFactory).parse(file.get_file) rescue nil
    end

    def metadata
      @metadata
    end

    def title
      @metadata.title || self.file_name
    end

    def to_cloud
      return nil if metadata.nil?
      {
        'duration' => metadata.length * 1000,
        'permalink' => title,
        'uri' => "/library/#{self.sHA1Urn}",
        'downloadable' => true,
        'genre' => metadata.genre.to_s.gsub(/\00/, ""),
        'title' => title.to_s.gsub(/\00/, ""),
        'id' => self.object_id,
        'streamable' => true,
        'stream_url' => "/library/#{self.sHA1Urn}",
        'description' => metadata.album.to_s.gsub(/\00/, ""),
        'permalink_url' => "/library/#{self.sHA1Urn}",
        'user' => {"username"=>metadata.artist.to_s.gsub(/\00/, "")},
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

module Filterable
  def filter_by_name(regex)
    filtered = self.find_all{ |f| f.file_name =~ regex || f.metadata.artist.to_s =~ regex || f.metadata.title =~ regex rescue false }
    filtered.extend(Filterable)
  end

  def filter_by_extension(extension)
    filtered = self.find_all{ |f| f.file_name =~ /#{extension.to_s}$/ }
    filtered.extend(Filterable)
  end

  def filter_by_genre(genre)
    filtered = self.find_all{ |f| f.metadata.genre.downcase == genre.downcase rescue false }
    filtered.extend(Filterable)
  end

  def filter_by_artist(artist)
    filtered = self.find_all{ |f| f.metadata.artist.downcase == artist.downcase rescue false }
    filtered.extend(Filterable)
  end
end
