# The Limewire library is a Ruby interface for the LimeWire peer-to-peer client.  It exposes various
# LimeWire services such as searching, downloading, and library management.  In this early version, the
# library must be instantiated from JRuby running as a service inside of LimeWire.
#
# Note: there is somewhat inconsistent use of URN and SHA1 hashes.  URNs are of the form
# <tt>urn:sha1:823A3F24DF34012BB35823A3F24DF34012BB35</tt>, wherease SHA1s are of the form
# <tt>823A3F24DF34012BB35823A3F24DF34012BB35</tt>

# The Limewire module should be the sole method used to interact with LimeWire's core.
module Limewire
  # The global variable $core is passed in from LimeWire.  It should not be used outside of the Limewire
  # gem, if possible.
  def self.core
    @core ||= $core rescue nil
  end

  # The length of time the LimeWire client has been running, in seconds.
  def self.uptime
    Limewire.core.get_statistics.uptime / 1000
  end

  def self.my_guid
    Core::GUID.new(self.core.application_services.get_my_guid)
  end

  # The average length of time the LimeWire has been run so far today.
  def self.daily_uptime
    Limewire.core.get_statistics.calculate_daily_uptime
  end

  # Searching in LimeWire is an incremental process: a search is created, then polled periodically to find
  # the results that have been returned.
  class Search
    # Fetches a search by its guid.  This creates a new Search object from LimeWire's SearchWithResults object
    #
    # - +guid+: The identifier (as a String) of the search
    def self.find(guid)
      self.new Core::SearchManager.getSearchByGuid(Core::GUID.new(guid))
    end

    # Creates a new search based on a search term.  Returns a Search, which provides a +guid+ for lookup later.
    # 
    # - +query+: The term to search for
    def self.query(query)
      self.new Core::SearchManager.createSearchFromQuery(query)
    end

    def initialize(search)
      @search = search
    end

    # The LimeWire Client's SearchWithResults java object
    def raw_results
      @search.getSearchResults
    end

    # Results of the search, up until this point.  The search is started on +self.query+, and runs until it is
    # stopped.  +results+ can be called to get an updated list of search results.  Results are returned as an
    # array of hashes.
    #
    # Example:
    #
    #   >> search = Search.query("grapefruit")
    #   >> results = search.results
    #   
    #   => { :filename => "grapefruit.txt",
    #        :magnet_url => "magnet:?urn:sha1:823A3F24DF34012BB35823A3F24DF34012BB35",
    #        :spam => false,
    #        :sha1 => "urn:sha1:823A3F24DF34012BB35823A3F24DF34012BB35",
    #        :properties => {} }
    def results
      results = @search.getSearchResults
      ret=results.map do |result|
        is_spam = result.isSpam? rescue false
        {
          :filename => result.fileName, 
          :magnet_url => result.getMagnetURL,
          :spam => is_spam,
          :sha1 => result.urn.to_s.split(':').last,
          :properties => result.getProperties.inject({}) do |memo, obj|
            memo[obj[0].to_s] = obj[1].to_s
            memo
          end
        }
      end

      ret
    end

    # Start looking for search results.
    def start
      @search.start
    end

    # Retrieve the search term
    def query_string
      @search.getQueryString
    end

    # Returns the search guid, which is a java Guid and needs <tt>to_s</tt> to be turned into a string guid.
    def guid
      @search.getQueryGuid
    end

    # Stop searching for results.
    def stop
      @search.stop
    end

    # Clear the results and begin the search again.
    def restart
      @search.restart
    end
  end

  # The LimeWire Client can download files from the gnutella network based solely off the hash, but that is
  # unreliable for reasons unknown to the authors of this library.  Downloads based off a search are much
  # more reliable, so that is how they are generally done here.
  #
  # Downloading happens in two steps: first, a new download is created from a URN (String) and a Search guid.
  # From there, it will begin downloading and the status can be retrieved using +find+.
  class Download
    attr_accessor :attributes

    ##
    # :method: title
    # The title (from the metadata) of the file being downloaded
    
    ##
    # :method: sha1
    # The SHA1 hash of the file being downloaded
    
    ##
    # :method: download_speed
    # Speed (in bytes per second) of the download

    ##
    # :method: remaining time
    # Estimated remaining time (in seconds) of the download

    ##
    # :method: percent_complete
    # Percent complete is an integer between 0 and 100

    ##
    # :method: source_count
    # Number of nodes on the network advertising the file

    ##
    # :method: file_name
    # 

    ##
    # :method: total_size
    # File size, in bytes
    
    # Create a new Download from a java download object
    def initialize(download)
      @download = download
      @attributes = {
        :title => @download.title,
        :sha1 => @download.urn.to_s.split(":").last,
        :download_speed => @download.download_speed,
        :percent_complete => @download.percent_complete,
        :remaining_time => @download.remaining_download_time,
        :source_count => @download.download_source_count,
        :file_name => @download.file_name,
        :total_size => @download.total_size
      }
    end

    # Start a new download.
    # - +urn+: the urn of the file to dowload (in the form <tt>urn:sha1:XXXXXXXXXXXXXXXXXXXX</tt>)
    # - +guid+: the guid of the search that found the file.
    def self.create(urn, guid)
      search = Search.find(guid)
      result = search.raw_results.select {|r| r.urn.to_s.split(':').last == urn }.first
      rfi = org.limewire.core.impl.library.CoreRemoteFileItem.new(result)
      Core::DownloadListManager.add_download(rfi)
    end
    
    # Retrieve all of the searches, as an array of Downloads.
    def self.all
      Core::DownloadListManager.downloads.map {|d| self.new(d) }
    end

    # Retrieve a single Download, by its sha1 hash.
    def self.find(sha1)
      self.all.select {|d| d.sha1 == sha1}.first
    end

    # Return the yamlized version of the download's properties.
    def to_yaml
      @attributes.to_yaml
    end
    
    def method_missing(name) #nodoc#
      if @attributes.has_key?(name)
        @attributes[name]
      else
        super
      end
    end
  end

  # The Library manages all of the files that LimeWire has scanned.
  module Library
    def self.core_file_list #nodoc#
      @core_file_list ||= Core::LibraryManager.library_managed_list.core_file_list
    end
      
    def self.all_files
      file_list = self.core_file_list.map{ |file| Limewire::File.new(file) }.compact
      file_list.extend(Filterable)
      file_list
    end

    def self.count
      self.core_file_list.size
    end

    # Find the first file in the library.
    def self.first(limit=1)
      self.all_files.first(limit)
    end

    def self.filter(&b)
      all_files.find_all(&b)
    end

    # Find a file in the library.  The method takes either :first, :all, or a SHA1 hash.
    #
    # Examples:
    #
    #   >> Library.find(:first)
    #   => #<Limewire::File "grapefruit.txt">
    #
    #   >> Library.find(:all, :extension => :pdf)
    #   => [#<Limewire::File "rollingwithrails.pdf">, #<Limewire::File "recipes.pdf">]
    #
    #   >> Library.find("823A3F24DF34012BB35823A3F24DF34012BB35")
    #   => #<Limewire::File "racquetball.mov">
    #
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

    # Find a file by its SHA1 hash
    def self.find_by_sha1(sha1)
      old_urn = Core::OldURN.createSHA1Urn(sha1)
      urn = Core::URNImpl.new(old_urn)
      file = self.core_file_list.get_file_descs_matching(old_urn)
      file[0]
    rescue
      nil
    end

    # Find an array of files by passing an array of SHA1 hashes
    def self.find_by_sha1s(sha1s)
      sha1s.collect{ |sha1| self.find_by_sha1(sha1) }.compact
    end
    
    # Return a list of Category items available for search
    def self.categories
      Limewire.core.get_file_manager.get_managed_file_list.managed_categories rescue []
    end
    
  end

  # Limewire::File is a class that wraps LimeWire's file class and provides an easier interface.  It passes
  # missing methods through to LimeWire's File class.
  class File
    def initialize(file)
      @file = file
      @metadata = Core::MetaDataFactory.parse(file.get_file) rescue nil
    end

    def metadata
      @metadata
    end

    # The <tt>artist</tt> metadata
    def artist
      @metadata.artist.to_s.gsub(/\x00/, "")
    end

    # Either the title from the metadata, or the filename of the file if the metadata is blank
    def title
      title = @metadata.title.to_s.gsub(/\x00/, "") rescue nil
      title.blank? ? self.file_name : title
    end

    # The <tt>album</tt> metadata
    def album
      @metadata.title.to_s.gsub(/\x00/, "")
    end

    # The <tt>genre</tt> metadata
    def genre
      @metadata.title.to_s.gsub(/\x00/, "")
    end

    # The SHA1 hash of the file, as a String
    def sha1
      self.sHA1Urn.to_s.split(":").last
    end

    # A hash of file properties, meant to be exported as json or xml.
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

  # The Filterable module does brute-force filtering on an array via +find_all+.  It mixes itself into the
  # result in order to enable chaining filters.
  module Filterable
    # Match all files that contain the regex in the artist metadata, title metadata, or filename
    def filter_by_name(regex)
      filtered = self.find_all{ |f| f.file_name =~ regex || f.metadata.artist.to_s =~ regex || f.metadata.title =~ regex rescue false }
      filtered.extend(Filterable)
    end
    
    # Match all files with a specific file extension
    def filter_by_extension(extension)
      filtered = self.find_all{ |f| f.file_name =~ /#{extension.to_s}$/ }
      filtered.extend(Filterable)
    end
    
    # Match all files by the genre metadata
    def filter_by_genre(genre)
      filtered = self.find_all{ |f| f.metadata.genre.downcase == genre.downcase rescue false }
      filtered.extend(Filterable)
    end
    
    # Match all files by the artist metadata
    def filter_by_artist(artist)
      filtered = self.find_all{ |f| f.metadata.artist.downcase == artist.downcase rescue false }
      filtered.extend(Filterable)
    end
  end
end

