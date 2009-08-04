module Limewire
  # The LimeWire Client can download files from the gnutella network based solely off the hash, but that is
  # unreliable for reasons unknown to the authors of this library.  Downloads based off a search are much
  # more reliable, so that is how they are generally done here.
  #
  # Downloading happens in two steps: first, a new download is created from a URN (String) and a Search guid.
  # From there, it will begin downloading and the status can be retrieved using +find+.
  class Download
    attr_accessor :attributes

    # The title (from the metadata) of the file being downloade
    def title
      @attributes[:title]
    end
    
    # The SHA1 hash of the file being downloaded
    def sha1
      @attributes[:sha1]
    end
    
    # Speed (in bytes per second) of the download
    def download_speed
      @attributes[:download_speed]
    end

    # Estimated remaining time (in seconds) of the download
    def remaining_time
      @attributes[:remaining_time]
    end

    # Percent complete is an integer between 0 and 100
    def percent_complete
      @attributes[:percent_complete]
    end

    # Number of nodes on the network advertising the file
    def source_count
      @attributes[:source_count]
    end

    def file_name
      @attributes[:file_name]
    end

    # File size, in bytes
    def total_size
      @attributes[:total_size]
    end
    
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
      results = search.raw_results.select {|r| r.urn.to_s.split(':').last == urn }
      Core::DownloadListManager.add_download(search, results)
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
  end
end
