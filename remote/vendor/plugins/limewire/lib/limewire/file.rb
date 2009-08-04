module Limewire
  # Limewire::File is a class that wraps LimeWire's file class and provides an easier interface.  It passes
  # missing methods through to LimeWire's File class.
  class File
    def initialize(file)
      @file = file
      @metadata = Core::MetaDataFactory.parse(file.get_file) rescue nil
      def @metadata.method_missing(name); nil; end
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
      @metadata.album.to_s.gsub(/\x00/, "")
    end

    # The <tt>genre</tt> metadata
    def genre
      @metadata.genre.to_s.gsub(/\x00/, "")
    end

    # The SHA1 hash of the file, as a String
    def sha1
      self.sHA1Urn.to_s.split(":").last
    end

    def duration
      @metadata.length.to_i
    end

    def waveform
      '/images/waveform.png'
    end

    # A hash of file properties, meant to be exported as json or xml.
    def to_cloud
      return nil if metadata.nil?
      {
        'duration' => metadata.length * 1000,
        'permalink' => title,
        'uri' => "/library/#{self.sha1}.mp3",
        'artwork_url' => "/library/#{self.sha1}/thumbnail/154",
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
        'waveform_url' => '/images/waveform.png' # until we actually do waveform calculations
      }
    end

    def to_yaml
      self.to_cloud
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
