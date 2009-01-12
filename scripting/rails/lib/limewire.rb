module Limewire
  def self.core
    @core ||= $core rescue nil
  end

  def self.uptime
    Limewire.core.get_statistics.uptime / 1000
  end

  def self.daily_uptime
    Limewire.core.get_statistics.calculate_daily_uptime
  end
      
  class Search

    
    def self.find(guid)
      self.new Core::SearchManager.getSearchByGuid(Core::GUID.new(guid))
    end

    def self.query(query)
      self.new Core::SearchManager.createSearchFromQuery(query)
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

    def self.core_file_list
      @core_file_list ||= Core::LibraryManager.library_managed_list.core_file_list
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
          files = files.select{|f| f.metadata.artist rescue false}.sort_by {|f| f.metadata.artist.to_s.downcase }.sort_by{|f| f.metadata.artist.empty? ? 1 : 0 }
        elsif options[:order] == "created_at"
          files = files.sort_by {|f| f.last_modified.to_s rescue 9999999999999 }
        end
      end
      
      files.extend(Filterable)
    end

    def self.find_by_sha1(sha1)
      old_urn = Core::OldURN.createSHA1Urn(sha1)
      urn = Core::URNImpl.new(old_urn)
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
    def initialize(file)
      @file = file
      @metadata = Core::MetaDataFactory.parse(file.get_file) rescue nil
    end

    def metadata
      @metadata
    end

    def artist
      @metadata.artist.to_s.gsub(/\x00/, "")
    end

    def title
      title = @metadata.title.to_s.gsub(/\x00/, "") rescue nil
      title.blank? ? self.file_name : title
    end

    def album
      @metadata.title.to_s.gsub(/\x00/, "")
    end

    def genre
      @metadata.title.to_s.gsub(/\x00/, "")
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
        'genre' => genre,
        'title' => title,
        'id' => self.sHA1Urn.to_s,
        'streamable' => true,
        'stream_url' => "/library/#{self.sha1}.mp3",
        'description' => album,
        'permalink_url' => "/library/#{self.sha1}.mp3",
        'user' => {
          "username" => artist, 
          "permalink" => artist },
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

