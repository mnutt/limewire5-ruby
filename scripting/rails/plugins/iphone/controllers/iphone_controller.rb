class IphoneController < PluginController
  layout 'iphone'

  def index
    @tracks = Limewire::Library.find(:all)

    @tracks_by_alpha = @tracks.group_by{|t| t.title.split('')[0] rescue "misc" }.sort_by{|t| t[0].downcase rescue 'z' }
    @tracks_by_artist = @tracks.group_by{|t| t.artist.downcase rescue "misc" }
    @artists_by_alpha = @tracks_by_artist.keys.to_a.sort.group_by{|t| t.split('')[0] rescue "misc" }
  end

  def search
    render :layout => false
  end
end
