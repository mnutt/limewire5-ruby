class HomeController < ApplicationController
  def index
    @plugins = Plugin.all
    @positions = DashboardPosition.find(:all, :order => 'list_position ASC')

    @columns = @positions.group_by {|p| p.column}.map{ |key, column| 
      column.map {|p|
         plugin = @plugins.select{|plugin| p.name == plugin.name}[0]
         @plugins.delete(plugin)
         plugin
      }
    }
    # @positions.each do |p|
    #  @columns[p.column] ||= []
    #  plugin = @plugins.select{|plugin| p.name == plugin.name}
    #  @columns[p.column] << plugin
    #  @plugins.delete(plugin)
    # end

    @columns[0] = [] unless @columns[0]
    @columns[1] = [] unless @columns[1]

    @plugins.each_with_index do |plugin, i|
      @columns[(i % 2 == 0) ? 0 : 1] << plugin
    end
  end
end
