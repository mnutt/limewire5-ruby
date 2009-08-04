module Limewire
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
