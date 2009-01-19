class HomeController < ApplicationController
  def index
    @plugins = Plugin.all
  end
end
