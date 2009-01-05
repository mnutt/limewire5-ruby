class CloudController < ApplicationController
  layout 'cloud'

  def index
  end

  def tracks
    limit = params[:limit].to_i || 40
    offset = params[:offset].to_i || 0

    @tracks = Limewire::Library.all_files.filter_by_extension(:mp3)

    if params[:q]
      @search = params[:q].downcase
      @tracks = @tracks.filter_by_name(/#{@search}/i)
    end

    if params[:artist]
      @tracks = @tracks.filter_by_artist(params[:artist])
    end

    if params[:genres]
      @tracks = @tracks.filter_by_genre(params[:genres])
    end

    @tracks = @tracks[offset..(offset+limit-1)]
    render :json => @tracks.collect{|x| x.to_cloud}
  end
end
