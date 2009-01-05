class PlaylistsController < ApplicationController
  def index
    @playlists = Playlist.find(:all)
    render :json => @playlists.to_json
  end
end
