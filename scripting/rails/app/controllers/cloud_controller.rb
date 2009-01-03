class CloudController < ApplicationController
  layout 'cloud'

  def index
  end

  def tracks
    limit = params[:limit].to_i || 40
    offset = params[:offset].to_i || 0

    @tracks = Limewire::Library.filter_by_name(/mp3$/)[offset..(offset+limit-1)]
    render :json => @tracks.collect{|x| x.to_cloud}
  end
end
