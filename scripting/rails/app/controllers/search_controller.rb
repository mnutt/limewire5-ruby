class SearchController < ApplicationController
  def index
  end

  def perform
    search = Limewire::Search.query(params[:query])
    search.start
    render :json => {:guid => search.guid.to_s}

  end

  def control
    case params[:query]
    when "start"
      Limewire::Search.find(params[:guid]).start
      render :json => "Ok"
    when "stop"
      Limewire::Search.find(params[:guid]).stop
      render :json => "Ok"
    when "results"
      results = Limewire::Search.find(params[:guid]).results
      render :xml => results
    when "query_string"
      render :json => Limewire::Search.find(params[:guid]).query_string
    else
      render :json => "No"
    end
  end

end
