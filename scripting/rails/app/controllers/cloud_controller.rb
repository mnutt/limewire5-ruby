class CloudController < ApplicationController
  layout 'cloud'

  def index
  end

  def tracks
    @tracks = Limewire::Library.find(:all, 
                                     :limit => params[:limit], 
                                     :offset => params[:offset],
                                     :extension => :mp3,
                                     :search => params[:q],
                                     :order => params[:order],
                                     :genres => params[:genres])
    render :json => @tracks.collect{|x| x.to_cloud}
  end
end
