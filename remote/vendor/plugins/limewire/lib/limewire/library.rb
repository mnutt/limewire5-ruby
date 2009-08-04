module Limewire

  # The Library manages all of the files that LimeWire has scanned.
  module Library
    def self.add_folder(path)
      Core::LibraryManager.library_managed_list.addFolder(java.io.File.new(path))
      @core_collection = nil #force recaching of files
    end
    def self.core_file_list #nodoc#
      @core_collection ||= Core::LibraryManager.library_managed_list.core_collection
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
      urn = Core::OldURN.createSHA1Urn(sha1)
      file = self.core_file_list.get_file_descs_matching(urn)
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
end
