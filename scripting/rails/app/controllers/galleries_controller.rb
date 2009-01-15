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
    RAILS_DEFAULT_LOGGER.warn "#{Limewire::Library.all_files.filter_by_extension('(png|gif|bmp|jpg)').map{|n| n.sha1}.join("\n")}"
    @photos = Limewire::Library.all_files.filter_by_extension('(png|gif|bmp|jpg|jpeg)')
#    render :text => "Hi"
  end
end
