class StatsController < PluginController
  def index
    @uptime = Limewire.uptime
  end

  def widget
    index
    render :layout => 'widget'
  end

end
