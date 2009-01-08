class CloudController < ApplicationController
  layout 'cloud'

  def index
  end

  def tracks
    if params[:sha1s]
      sha1s = params[:sha1s].split(",")
      @tracks = Limewire::Library.find_by_sha1s(sha1s)
    else
      @tracks = Limewire::Library.find(:all, 
                                       :limit => params[:limit], 
                                       :offset => params[:offset],
                                       :extension => :mp3,
                                       :artist => params[:artist],
                                       :search => params[:q],
                                       :order => params[:order],
                                       :genres => params[:genres])
    end

    render :json => @tracks.collect{|x| x.to_cloud}
  end
end
