class IphoneController < ApplicationController
  layout 'iphone'

  def index
    @tracks = Limewire::Library.find(:all)

    @tracks_by_alpha = @tracks.group_by{|t| t.metadata.title.split('')[0] rescue "misc" }
    @tracks_by_artist = @tracks.group_by{|t| t.metadata.artist.downcase rescue "misc" }
    @artists_by_alpha = @tracks_by_artist.keys.to_a.sort.group_by{|t| t.split('')[0] rescue "misc" }
  end
end
