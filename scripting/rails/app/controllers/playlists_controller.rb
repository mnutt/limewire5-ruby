class PlaylistsController < ApplicationController
  self.allow_forgery_protection = false

  def index
    @playlists = Playlist.find(:all, :order => "list_position ASC")
    @ordered_playlists = []
    @playlists.each_with_index do |p, i| 
      @ordered_playlists << { :playlist => p.to_playlist, :position => i, :is_owner => true } 
    end

    render :json => @ordered_playlists.to_json()
  end

  def create
    @playlist = Playlist.new(params[:playlist])
    @playlist.save
    render :text => { :playlist => @playlist.to_playlist, 
                      :position => Playlist.count, 
                      :is_owner => true }.to_json
  end

  def destroy
    @playlist = Playlist.find(params[:id])
    @playlist.destroy
    render :json => {:response => 200}
  end

  def update
    @playlist = Playlist.find(params[:id])
    @playlist.attributes = params[:playlist]
    @playlist.save
    render :json => {:response => 200}
  end
end
