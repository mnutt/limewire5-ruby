class DownloadsController < ApplicationController
  self.allow_forgery_protection = false

  def create
    Limewire::Download.create(params[:magnet])
  end

  def index
    @downloads = Limewire::Download.all
    render :json => @downloads.map{|d| d.attributes }
  end

  def show
  end
end
