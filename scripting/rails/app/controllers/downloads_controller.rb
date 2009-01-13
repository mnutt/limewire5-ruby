class DownloadsController < ApplicationController
  self.allow_forgery_protection = false

  def create
    download = Limewire::Download.create(params[:urn], params[:guid])
    render :json => "ok"
  end

  def index
    @downloads = Limewire::Download.all
    render :json => @downloads.map{|d| d.attributes }
  end

  def show
    @download = Limewire::Download.find(params[:id])
    render :json => @download.attributes
  end
end
