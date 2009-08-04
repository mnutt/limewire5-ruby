module Limewire

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
        {
          :filename => result.fileName, 
          :magnet_url => result.getMagnetURL,
          :category => (result.category.singular_name rescue nil),
          :spam => (result.spam? rescue false),
          :sha1 => result.urn.to_s.split(':').last,
					:album => result.getProperty(Core::FilePropertyKey::ALBUM),
					:name => result.getProperty(Core::FilePropertyKey::NAME),
					:title => result.getProperty(Core::FilePropertyKey::TITLE),
					:author => result.getProperty(Core::FilePropertyKey::AUTHOR),
					:created_at => result.getProperty(Core::FilePropertyKey::DATE_CREATED),
					:length => result.getProperty(Core::FilePropertyKey::LENGTH),
					:genre => result.getProperty(Core::FilePropertyKey::GENRE),
          :sources => result.getRfd.address.address_description.to_s, #result.alts.to_a.map{|loc| loc.address.to_s },
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

    def destroy
      @search.destroy
    end
  end
end
