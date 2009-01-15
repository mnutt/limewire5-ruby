class GalleriesController < ApplicationController
  self.allow_forgery_protection = false

  def index
    @playlists = Playlist.find(:all, :order => "list_position ASC")
    render :text => @playlists.to_json
  end

  def show
    @playlist = Playlist.find(params[:id])
  end

  def all
    @photos = Limewire::Library.all_files.filter_by_extension('(png|gif|bmp|jpg|jpeg)')
  end
end
