module Limewire

  if($core)
    # Running from Limewire
    include Java
    
    Geocoder            = org.limewire.geocode.Geocoder
    OldURN              = com.limegroup.gnutella.URN
    MetaDataFactoryImpl = com.limegroup.gnutella.metadata.MetaDataFactoryImpl
    MetaDataFactory     = com.limegroup.gnutella.metadata.MetaDataFactory
    GUID                = org.limewire.io.GUID
    URN                 = org.limewire.core.api.URN
    URNImpl             = org.limewire.core.impl.URNImpl
    LibraryManager      = org.limewire.core.api.library.LibraryManager
    SearchManager       = org.limewire.core.api.search.SearchManager
  else
    # Not running from limewire, no $core available
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
    def self.search_manager
      @search_manager ||= Limewire.get_singleton(SearchManager)
    end
    
    def self.find(guid)
      self.new self.search_manager.getSearchByGuid(GUID.new(guid))
    end

    def self.query(query)
      self.new self.search_manager.createSearchFromQuery(query)
    end

    def initialize(search)
      @search = search
    end

    def results
      results = @search.getSearchResults
      ret=results.map do |result| 
        {
          :filename => result.fileName, 
          :magnet_url => result.getMagnetURL,
          :spam => result.isSpam?,
          :properties => result.getProperties.inject({}) do |memo, obj|
            memo[obj[0].to_s] = obj[1].to_s
            memo
          end
        }
      end

      ret
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
    def self.library_manager
      @library_manager ||= Limewire.get_singleton(LibraryManager)
    end

    def self.core_file_list
      @core_file_list ||= self.library_manager.library_managed_list.core_file_list
    end
      
    def self.all_files
      file_list = self.core_file_list.map{ |file| Limewire::File.new(file) }.compact
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
      return self.find_by_sha1(type_or_sha1) if String === type_or_sha1
      return all_files[0] if :first == type_or_sha1

      files = all_files

      if options[:search]
        files = files.filter_by_name(/#{options[:search].downcase}/i)
      end

      if options[:artist]
        files = files.filter_by_artist(options[:artist])
      end
      
      if options[:genres]
        files = files.filter_by_genre(options[:genres])
      end

      if options[:extension]
        files = files.filter_by_extension(options[:extension])
      end

      if options[:limit] || options[:offset]
        limit = options[:limit].to_i || 40
        offset = options[:offset].to_i || 0
        files = files[offset..(offset + limit - 1)]
      end

      if options[:order]
        if options[:order] == "artist"
          files.select{|f| f.metadata.artist rescue false}.sort_by {|f| f.metadata.artist.to_s.downcase }.sort_by{|f| f.metadata.artist.empty? ? 1 : 0 }
        elsif options[:order] == "created_at"
          files.sort_by {|f| f.last_modified.to_s rescue 9999999999999 }
        end
      else
        files
      end
    end

    def self.find_by_sha1(sha1)
      old_urn = OldURN.createSHA1Urn(sha1)
      urn = URNImpl.new(old_urn)
      file = self.core_file_list.get_file_descs_matching(old_urn)
      file[0]
    rescue
      nil
    end

    def self.find_by_sha1s(sha1s)
      sha1s.collect{ |sha1| self.find_by_sha1(sha1) }.compact
    end
    
    def self.categories
      Limewire.core.get_file_manager.get_managed_file_list.managed_categories rescue []
    end
    
  end

  class File
    def self.metadata_factory
      @metadata_factory ||= Limewire.get_singleton(MetaDataFactory)
    end

    def initialize(file)
      @file = file
      @metadata = File.metadata_factory.parse(file.get_file) rescue nil
    end

    def metadata
      @metadata
    end

    def title
      @metadata.title || self.file_name
    end

    def sha1
      self.sHA1Urn.to_s.split(":").last
    end

    def to_cloud
      return nil if metadata.nil?
      {
        'duration' => metadata.length * 1000,
        'permalink' => title,
        'uri' => "/library/#{self.sha1}.mp3",
        'downloadable' => true,
        'genre' => metadata.genre.to_s.gsub(/\00/, ""),
        'title' => title.to_s.gsub(/\00/, ""),
        'id' => self.sHA1Urn.to_s,
        'streamable' => true,
        'stream_url' => "/library/#{self.sha1}.mp3",
        'description' => metadata.album.to_s.gsub(/\00/, ""),
        'permalink_url' => "/library/#{self.sha1}.mp3",
        'user' => {
          "username"=>metadata.artist.to_s.gsub(/\00/, ""), 
          "permalink" => metadata.artist.to_s.gsub(/\00/, "") },
        'sharing' => 'public',
        'waveform_url' => '/images/waveform.png', # until we actually do waveform calculations
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
end

