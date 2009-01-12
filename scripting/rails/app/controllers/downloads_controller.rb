class DownloadsController < ApplicationController
  self.allow_forgery_protection = false

  def create
    start = params.delete(:magnet)
    params.delete(:controller)
    params.delete(:action)
    urn = start + "?&" + params.map{|k,v| "#{k}=#{v}"}.join("&")

    download = Limewire::Download.create(urn)
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
